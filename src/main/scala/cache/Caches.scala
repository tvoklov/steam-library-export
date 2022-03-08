package volk.steam.libraryexport
package cache

import steam.Types.{ AppID, SteamAPIKey }
import steam.{ SteamAPI => Steam }
import util.IOOperations

import cats.effect.{ ExitCode, IO }
import io.circe.Encoder
import org.http4s.blaze.client.BlazeClientBuilder
import org.http4s.client.Client

import java.io.File
import scala.concurrent.duration.DurationInt

object Caches {

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

            _ <- buildIsGameCache(allOwnedApps.games.map(_.appid), validAppIdCache)
          } yield ExitCode.Success
      }

  /** caches the result of a "is this an actual game" check to a file.
    * have to use this because on big libraries steam complains that i'm making too many requests
    */
  def buildIsGameCache(
      allOwnedApps: List[AppID],
      cache: MapFileCache[AppID, Boolean]
  )(implicit client: Client[IO]): IO[MapFileCache[Int, Boolean]] =
    buildCache(cache)(
      allOwnedApps,
      appId => implicit client => Steam.getAppDetails(appId).map(_.exists(_.isPlayable))
    )

  /** general function for building a cache of responses from steam */
  def buildCache[Key, Value](inputCache: MapFileCache[Key, Value])(from: List[Key], using: Key => Client[IO] => IO[Value])(implicit
      client: Client[IO],
      keyEncoder: Encoder[(Key, Value)]
  ): IO[MapFileCache[Key, Value]] = {
    val notProcessed = from.filterNot(inputCache.entities.contains)

    if (notProcessed.isEmpty) IO.pure(inputCache)
    else {
      for {
        processed <- {
          scribe.info("querying steam...")
          IOOperations.runUntilOneCrashes(
            notProcessed.map(
              key => using(key)(client).map(key -> _)
            )
          )
        }

        _ = scribe.info("writing cached values to file")

        newCache = inputCache ++ processed.toMap
        _ <- newCache.save

        finished = processed.size == notProcessed.size

        _ = scribe.info(
          if (finished) "done querying"
          else s"not done querying, ${notProcessed.size - processed.size}/${from.size} queries left"
        )

        res <-
          if (finished) IO.pure(newCache)
          else
            IO.sleep(
              if (processed.size < 15) 1.minutes // most probably steam's timeout machine just wasn't fast enough for me
              else 5.minutes + 15.seconds
            ) >> buildCache(newCache)(from, using)

      } yield res
    }
  }

}
