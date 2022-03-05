package volk.steam.libraryexport

import spreadsheet.IOWrap._
import spreadsheet.WorkbookGenerator
import steam.SteamAPI
import steam.Types.SteamAPIKey

import cats.effect._
import org.http4s.blaze.client._

object App {

  def runApp(steamApiKey: SteamAPIKey, steamId: String, resultFile: String): IO[ExitCode] =
    BlazeClientBuilder[IO].resource
      .use {
        implicit client =>
          implicit val key: SteamAPIKey = steamApiKey

          for {
            id <- // let's hope nobody using this will have a vanity url that consists of 17 digits
              if (steamId.length == 17 && steamId.forall(_.isDigit)) IO.pure(steamId)
              else SteamAPI.resolveVanityURL(steamId)
            games <- SteamAPI.getOwnedGames(id)
            spreadsheet = WorkbookGenerator.createWorkbook(games)
            _ <- spreadsheet.toXlsx(resultFile)
          } yield ExitCode.Success
      }

}
