package volk.steam.libraryexport
package steam

import steam.Entities._
import steam.Paths._
import steam.Types._

import cats.effect._
import org.http4s.client.Client

object SteamAPI {

  /** @return a proper 64 steam profile id for the given vanityUrl */
  def resolveVanityURL(vanityUrl: String)(implicit key: SteamAPIKey, client: Client[IO]): IO[SteamID] =
    client
      .expect[ResolveVanityJson](
        resolveVanityURLRoot +? ("key", key) +? ("vanityurl", vanityUrl)
      )
      .map(_.response)
      .flatMap {
        case ResolveVanityResponse(steamId, 1) => IO.pure(steamId)
        case ResolveVanityResponse(steamId, success) =>
          IO.raiseError(new Throwable(s"ResolveVanityURL returned $success in 'success' field (steamid: $steamId)"))
      }

  /** @return a list of games a player owns along with some playtime information */
  def getOwnedGames(userId: SteamID, includeFreeGames: Boolean = false)(implicit key: SteamAPIKey, client: Client[IO]): IO[OwnedGames] =
    client
      .expect[GetOwnedGamesJson](
        getOwnedGamesRoot +? ("key", key) +? ("steamid", userId) +? ("include_appinfo", true) +? ("include_played_free_games", includeFreeGames)
      )
      .map(_.response)
      .map {
        case GetOwnedGamesResponse(game_count, games) =>
          OwnedGames(
            game_count,
            games.map(GameInfo(_))
          )
      }

  /** @return a list of achievements for this user by app id */
  def getUserStatsForGame(userId: SteamID, appId: AppID)(implicit key: SteamAPIKey, client: Client[IO]): IO[UserStats] =
    client
      .expect[GetUserStatsForGameJson](
        getUserStatsForGameRoot +? ("key", key) +? ("steamid", userId) +? ("appid", appId)
      )
      .map(_.playerstats)
      .map {
        case GetUserStatsForGameResponse(steamID, gameName, achievements, stats) =>
          UserStats(achievements.size)
      }

}
