package volk.steam.libraryexport
package steam

import steam.Types._

import cats.effect.IO
import io.circe.generic.auto._
import org.http4s.EntityDecoder
import org.http4s.circe.jsonOf

object Entities {

  private[steam] case class ResolveVanityJson(response: ResolveVanityResponse)
  implicit private[steam] val ResolveVanityJsonDecoder: EntityDecoder[IO, ResolveVanityJson] = jsonOf[IO, ResolveVanityJson]
  private[steam] case class ResolveVanityResponse(steamid: SteamID, success: Int)

  private[steam] case class GetOwnedGamesJson(response: GetOwnedGamesResponse)
  implicit private[steam] val GetOwnedGamesJsonDecoder: EntityDecoder[IO, GetOwnedGamesJson] = jsonOf[IO, GetOwnedGamesJson]
  private[steam] case class GetOwnedGamesResponse(game_count: Int, games: List[GameInfoJson])

  /** represents the GameInfo part of a response to GetOwnedGames with parameter include_appinfo=true */
  private[steam] case class GameInfoJson(
      appid: AppID,
      name: String,
      playtime_2weeks: Option[Minutes],
      playtime_forever: Minutes,
      img_icon_url: String,
      img_logo_url: String,
      has_community_visible_stats: Option[Boolean],
      playtime_windows_forever: Minutes,
      playtime_mac_forever: Minutes,
      playtime_linux_forever: Minutes
  )

  private[steam] case class GetUserStatsForGameJson(playerstats: GetUserStatsForGameResponse)
  implicit private[steam] val GetUserStatsForGameJsonDecoder: EntityDecoder[IO, GetUserStatsForGameJson] = jsonOf[IO, GetUserStatsForGameJson]
  private[steam] case class GetUserStatsForGameResponse(
      steamID: SteamID,
      gameName: String,
      achievements: List[AchievementJson],
      stats: List[AchievementStatJson]
  )

  private[steam] case class AchievementJson(name: String, achieved: Int)
  private[steam] case class AchievementStatJson(name: String, value: Int)

  case class UserStats(
      achievementCount: Int
  )

  case class OwnedGames(
      gameCount: Int,
      games: List[GameInfo]
  )

  object GameInfo {
    def apply(gir: GameInfoJson): GameInfo =
      GameInfo(
        appid = gir.appid,
        name = gir.name,
        playtime = PlayTime(
          gir.playtime_forever,
          gir.playtime_2weeks,
          gir.playtime_windows_forever,
          gir.playtime_mac_forever,
          gir.playtime_linux_forever
        )
      )
  }

  case class GameInfo(
      appid: AppID,
      name: String,
      playtime: PlayTime
  )

  case class PlayTime(
      allPlaytime: Minutes,
      playtimeLast2Weeks: Option[Minutes],
      playtimeWindows: Minutes,
      playtimeMac: Minutes,
      playTimeLinux: Minutes
  )

}
