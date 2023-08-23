package volk.steam.libraryexport.exporter

import cats.effect.{ ExitCode, IO }

import org.http4s.blaze.client.BlazeClientBuilder
import spoiwo.model.Workbook

import java.io.File

import volk.steam.libraryexport.cache.{ Caches, MapFileCache }
import volk.steam.libraryexport.howlongtobeat.HLTBAPI
import volk.steam.libraryexport.spreadsheet.sheets.*
import volk.steam.libraryexport.spreadsheet.IOWrap.*
import volk.steam.libraryexport.steam.SteamAPI as Steam
import volk.steam.libraryexport.steam.Types.*
import volk.steam.libraryexport.util.Utils.*

/**
 * customizable export with multiple sheets. if you want suggestions and/or more in-depth data - use
 * this one.
 */
object CustomExport {

  def run(
      steamApiKey: SteamAPIKey,
      steamId: String,
      resultFile: String,
    ): IO[ExitCode] =
    BlazeClientBuilder[IO]
      .resource
      .use { implicit client =>
        implicit val key: SteamAPIKey = steamApiKey

        for {
          maybeId <- // let's hope nobody using this will have a vanity url that consists of 17 digits
            if steamId.length == 17 && steamId.forall(_.isDigit) then
              IO.pure(Some(steamId))
            else
              Steam.resolveVanityURL(steamId)

          userId <-
            maybeId match {
              case None        =>
                IO.raiseError(new Throwable("could not get userid, please make sure you're using"))
              case Some(value) =>
                IO.pure(value)
            }

          allOwnedApps <- {
            scribe.info("getting your games")
            Steam.getOwnedApps(userId)
          }

          actualGamesCache <-
            MapFileCache.of[AppID, Boolean](
              new File(System.getProperty("user.dir") + "\\caches\\valid-app-ids.cache").toPath
            )

          allApps = allOwnedApps.games

          actualGames <- {
            for {
              cache <-
                if allApps.size == actualGamesCache.entities.size then
                  IO.pure(actualGamesCache)
                else
                  Caches.buildIsGameCache(allOwnedApps.games.map(_.appid), actualGamesCache)

              games =
                cache
                  .entities
                  .collect { case (appId, true) =>
                    appId
                  }
                  .toList
            } yield allApps.filter(_.appid |> games.contains)
          }

          _ = scribe.info("getting the amount of time to beat & review scores")
          gamesScoresMaybeLengths <-
            IO.parSequenceN(30)(
              actualGames.map(game =>
                for {
                  sl <- Steam.getUserReviewScores(game.appid).both(HLTBAPI.howLongToBeat(game.name))
                } yield (game, sl._1, sl._2)
              )
            )

          gamesScores = gamesScoresMaybeLengths.map(x => x._1 -> x._2)

          gamesThatCanBeBeaten =
            gamesScoresMaybeLengths.collect { case (info, stats, Some(length)) =>
              (info, stats, length)
            }

          wb = {
            scribe.info("generating xlsx workbook")
            Workbook(
              GameList.make(actualGames),
              GamesWithScores.make(gamesScores),
              HLTBSuggestions.make(gamesThatCanBeBeaten),
            ).withActiveSheet(0)
          }

          _ <- {
            scribe.info(s"writing the workbook to $resultFile")
            wb.toXlsx(resultFile)
          }
        } yield ExitCode.Success
      }

}
