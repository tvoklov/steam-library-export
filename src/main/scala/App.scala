package volk.steam.libraryexport

import spreadsheet.IOWrap._
import spreadsheet.sheets._
import steam.Entities.{Game, GameTypeInfo}
import steam.Types.SteamAPIKey
import steam.{SteamAPI => Steam}

import cats.effect._
import cats.implicits._
import org.http4s.{Response, Status}
import org.http4s.blaze.client._
import org.http4s.client.Client
import org.http4s.client.middleware.{Retry, RetryPolicy}
import spoiwo.model.Workbook

import scala.concurrent.duration.DurationInt

object App {

  def runApp(steamApiKey: SteamAPIKey, steamId: String, resultFile: String): IO[ExitCode] =
    BlazeClientBuilder[IO]
      .resource
      .use {
        implicit client =>
          implicit val key: SteamAPIKey = steamApiKey

          // this doesn't really work
//          val retryPolicy: RetryPolicy[IO] = (_, eit, _) => eit match {
//            case Right(Response(Status(429), _, _, _, _)) =>
//              scribe.info("woah, your library is really big. we ran out of requests steam allows us to do. please wait a minute.")
//              1.minutes.some
//            case _ => None
//          }
//
//          implicit val retryingClient: Client[IO] = Retry[IO](policy = retryPolicy)(client)

          for {
            id <- // let's hope nobody using this will have a vanity url that consists of 17 digits
              if (steamId.length == 17 && steamId.forall(_.isDigit)) IO.pure(steamId)
              else Steam.resolveVanityURL(steamId)

            allOwnedApps <- {
              scribe.info("getting your games")
              Steam.getOwnedGames(id)
            }

            ownedGames <- {
              scribe.info("filtering out free games and non-game apps")
              for {
                validStoreAppIds <- Steam.filterInvalidStoreApps(allOwnedApps.games.map(_.appid), filterFree = true)
                filtered = allOwnedApps.games.filter(
                  g => validStoreAppIds.contains(g.appid)
                )
                appDetails <-
                  IO.parSequenceN(30)(
                    filtered.map(
                      game => for { res <- Steam.getAppDetails(game.appid) } yield game -> res
                    )
                  )
              } yield appDetails.collect {
                case (game, Some(GameTypeInfo(false, Game))) => game
              }
            }

            gamesWithScores <- {
              scribe.info("getting review scores of your least played games")
              IO.parSequenceN(30)(
                ownedGames
                  .filter(_.playtime.allPlaytime <= 120)
                  .map(
                    game => for { score <- Steam.getUserReviewScores(game.appid) } yield game -> score
                  )
              ).map(_.filter(_._2.totalReviewCount > 0))
            }

            wb = {
              scribe.info("generating xlsx workbook")
              Workbook(
                GameList.make(ownedGames),
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
