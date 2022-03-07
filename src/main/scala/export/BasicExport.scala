package volk.steam.libraryexport
package `export`

import spreadsheet.IOWrap.SPOIWOIOWB
import spreadsheet.sheets._
import steam.Types._
import steam.{SteamAPI => Steam}

import cats.effect.{ExitCode, IO}
import org.http4s.blaze.client.BlazeClientBuilder
import spoiwo.model.Workbook

/** the most basic export, all this does is get your library and put it into an xlsx file*/
object BasicExport {

  def run(steamAPIKey: SteamAPIKey, steamID: SteamID, resultFile: String): IO[ExitCode] =
    BlazeClientBuilder[IO].resource
      .use {
        implicit client =>
          implicit val key: SteamAPIKey = steamAPIKey

          for {
            maybeId <- // let's hope nobody using this will have a vanity url that consists of 17 digits
              if (steamID.length == 17 && steamID.forall(_.isDigit)) IO.pure(Some(steamID))
              else Steam.resolveVanityURL(steamID)

            userId <- maybeId match {
              case None        => IO.raiseError(new Throwable("could not get userid, please make sure you're using"))
              case Some(value) => IO.pure(value)
            }

            allOwnedApps <- {
              scribe.info("getting your games")
              Steam.getOwnedApps(userId)
            }

            gamesWithScores <- {
              scribe.info("getting review scores of your least played games")
              IO.parSequenceN(30)(
                allOwnedApps.games
                  .filter(_.playtime.allPlaytime <= 120)
                  .map(
                    game => for { score <- Steam.getUserReviewScores(game.appid) } yield game -> score
                  )
              ).map(_.filter(_._2.totalReviewCount > 0))
            }

            wb = {
              scribe.info("generating xlsx workbook")
              Workbook(
                GameList.make(allOwnedApps.games),
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
