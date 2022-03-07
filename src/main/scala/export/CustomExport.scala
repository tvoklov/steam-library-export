package volk.steam.libraryexport
package `export`

import spreadsheet.sheets._
import steam.Types._
import steam.{SteamAPI => Steam}

import cats.effect.{ExitCode, IO}
import org.http4s.blaze.client.BlazeClientBuilder
import spoiwo.model.Workbook
import volk.steam.libraryexport.cache.{CacheStuff, MapFileCache}
import volk.steam.libraryexport.spreadsheet.IOWrap.SPOIWOIOWB
import volk.steam.libraryexport.util.Utils.PipingUtil

import java.io.File

/** customizable export */
object CustomExport {

  def run(steamApiKey: SteamAPIKey, steamId: String, resultFile: String): IO[ExitCode] =
    BlazeClientBuilder[IO].resource
      .use {
        implicit client =>
          implicit val key: SteamAPIKey = steamApiKey

          for {
            maybeId <- // let's hope nobody using this will have a vanity url that consists of 17 digits
              if (steamId.length == 17 && steamId.forall(_.isDigit)) IO.pure(Some(steamId))
              else Steam.resolveVanityURL(steamId)

            userId <- maybeId match {
              case None        => IO.raiseError(new Throwable("could not get userid, please make sure you're using"))
              case Some(value) => IO.pure(value)
            }

            allOwnedApps <- {
              scribe.info("getting your games")
              Steam.getOwnedApps(userId)
            }

            actualGamesCache <- MapFileCache.of[AppID, Boolean](
              new File(System.getProperty("user.dir") + "\\caches\\valid-app-ids.cache").toPath
            )

            allApps = allOwnedApps.games

            actualGames <- {
              for {
                cache <-
                  if (allApps.size == actualGamesCache.entities.size) IO.pure(actualGamesCache)
                  else CacheStuff.buildCache(allOwnedApps.games.map(_.appid), actualGamesCache)

                games = cache.entities.collect { case (appId, true) => appId }.toList
              } yield allApps.filter(_.appid |> games.contains)
            }

            gamesWithScores <- {
              scribe.info("getting review scores of your least played games")
              IO.parSequenceN(30)(
                actualGames
                  .filter(_.playtime.allPlaytime <= 120)
                  .map(
                    game => for { score <- Steam.getUserReviewScores(game.appid) } yield game -> score
                  )
              ).map(_.filter(_._2.totalReviewCount > 0))
            }

            wb = {
              scribe.info("generating xlsx workbook")
              Workbook(
                GameList.make(actualGames),
                SuggestionsSheet.make(gamesWithScores)
              ).withActiveSheet(0)
            }

            _ <- {
              scribe.info(s"writing the workbook to $resultFile")
              wb.toXlsx(resultFile)
            }
          } yield ExitCode.Success
      }

}
