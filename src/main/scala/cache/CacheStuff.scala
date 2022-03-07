package volk.steam.libraryexport
package cache

import steam.Types.{AppID, SteamAPIKey}
import steam.{SteamAPI => Steam}
import util.IOOperations

import cats.effect.{ExitCode, IO}
import org.http4s.blaze.client.BlazeClientBuilder
import org.http4s.client.Client

import java.io.File
import scala.concurrent.duration.DurationInt

object CacheStuff {

  /** standalone caching function that will cache to file and then stop */
  def run(steamApiKey: SteamAPIKey, steamId: String): IO[ExitCode] =
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

            _ = scribe.info("getting your games")
            allOwnedApps <- Steam.getOwnedApps(userId)

            _ = scribe.info("reading already cached values")
            validAppIdCache <- MapFileCache.of[AppID, Boolean](new File(System.getProperty("user.dir") + "\\caches\\valid-app-ids.cache").toPath)

            _ = scribe.info("beginning to query steam...")

            _ <- buildCache(allOwnedApps.games.map(_.appid), validAppIdCache)
          } yield ExitCode.Success
      }

  /** caches the result of a "is this an actual game" check to a file.
    * have to use this because on big libraries steam complains that i'm making too many requests
    */
  def buildCache(
      allOwnedApps: List[AppID],
      cache: MapFileCache[AppID, Boolean]
  )(implicit client: Client[IO]): IO[MapFileCache[Int, Boolean]] = {
    val notScannedApps = allOwnedApps.filterNot(cache.entities.contains)

    for {
      scannedApps <- {
        scribe.info("querying steam...")
        IOOperations.runUntilOneCrashes(
          notScannedApps
            .map(
              appId => for { gti <- Steam.getAppDetails(appId) } yield appId -> gti.exists(_.isPlayable)
            )
        )
      }

      _ = scribe.info("writing cached values to file")

      newCache = cache ++ scannedApps.toMap
      _ <- newCache.save

      finished = scannedApps.size == notScannedApps.size

      _ = scribe.info(
        if (finished) "done querying"
        else s"not done querying, ${notScannedApps.size - scannedApps.size}/${allOwnedApps.size} apps left"
      )

      res <-
        if (finished) IO.pure(cache)
        else
          IO.sleep(
            if (scannedApps.size < 15) 1.minutes // most probably steam's timeout machine just wasn't fast enough for me
            else 5.minutes + 15.seconds
          ) >> buildCache(allOwnedApps, newCache)

    } yield res
  }

}
