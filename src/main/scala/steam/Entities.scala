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

  // ------------------------------

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

  // ------------------------------

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

  // ------------------------------

  private[steam] case class AppReviewJson(success: Int, query_summary: AppReviewSummary)
  implicit private[steam] val AppReviewJsonDecoder: EntityDecoder[IO, AppReviewJson] = jsonOf[IO, AppReviewJson]

  private[steam] case class AppReviewSummary(
      num_reviews: Int, // the number of *** returned **** reviews, basically useless if requested only for summary
      review_score: Int,
      review_score_desc: String,
      total_positive: Int,
      total_negative: Int,
      total_reviews: Int
  )

  // ------------------------------

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

  case class ReviewStats(
      reviewScore: Int,
      scoreDescription: String,
      positiveReviewCount: Int,
      negativeReviewCount: Int,
      totalReviewCount: Int
  ) {
    lazy val positiveReviewPercentage: Int =
      (positiveReviewCount / (totalReviewCount / 100f)).floor.toInt
  }

  object AppType {
    def toAppType: String => Option[AppType] = {
      case "game"   => Some(Game)
      case "mod"    => Some(Mod)
      case "dlc"    => Some(DLC)
      case "series" => Some(Series)
      case "advertising" => Some(Advertising)
      case "video" => Some(Video)
      case "demo" => Some(Demo)
      case _        => None
    }
  }

  trait AppType

  trait Playable extends AppType
  case object Game   extends Playable
  case object Mod    extends Playable

  trait Additions extends AppType
  case object DLC    extends Additions
  case object Demo    extends Additions

  trait Viewable extends AppType
  case object Series extends Viewable
  case object Video extends Viewable
  case object Advertising extends Viewable

  case class GameTypeInfo(
      free: Boolean,
      appType: AppType
  ) {
    def isPlayable: Boolean = appType.isInstanceOf[Playable]
  }

}
