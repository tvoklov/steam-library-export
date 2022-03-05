package volk.steam.libraryexport
package steam

import steam.Entities._
import steam.Paths._
import steam.Types._

import cats.effect._
import io.circe.{ ACursor, Decoder, DecodingFailure, Json }
import org.http4s.circe._
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

  def getUserReviewScores(appId: AppID)(implicit client: Client[IO]): IO[ReviewStats] =
    for {
      json <-
        client.expect[AppReviewJson](
          reviewsRoot(appId) +? ("json", 1) +? ("num_per_page", 0) +? ("language", "all") +? ("purchase_type", "all")
        )

      summary <-
        if (json.success == 1) IO.pure(json.query_summary)
        else IO.raiseError(new Throwable(s"User review query returned success value of ${json.success}"))

    } yield ReviewStats(
      summary.review_score,
      summary.review_score_desc,
      summary.total_positive,
      summary.total_negative,
      summary.total_reviews
    )

  def getAppDetails(appId: AppID)(implicit client: Client[IO]): IO[Option[GameTypeInfo]] =
    client
      .expect[Json](
        detailsRoot +? ("appids", appId)
      )
      .map {
        json =>
          val cursor = json.hcursor
            .downField(appId.toString)

          for {
            success <- cursor.get[Boolean]("success")
            gti <-
              if (!success) Right(None)
              else decodeAppDetails(cursor.downField("data")).map(Some(_))
          } yield gti
      }
      .flatMap {
        case Left(value) => IO.raiseError(value)
        case Right(result) =>
          IO.pure(result)
      }

  private def decodeAppDetails(cursor: ACursor): Decoder.Result[GameTypeInfo] =
    for {
      isFree      <- cursor.get[Boolean]("is_free")
      appTypeJson <- cursor.get[String]("type")
      appType <-
        AppType
          .toAppType(appTypeJson)
          .toRight(DecodingFailure(s"Weird app type in json: $appTypeJson", Nil))
    } yield GameTypeInfo(
      isFree,
      appType
    )

  def filterInvalidStoreApps(ids: List[AppID], filterFree: Boolean)(implicit client: Client[IO]): IO[List[AppID]] =
    IO.parSequenceN(3)(
      ids
        .grouped(50)
        .toList
        .map {
          ids =>
            client
              .expect[Json](
                detailsRoot +? ("appids", ids.mkString(",")) +? ("filters", "price_overview")
              )
              .map {
                json =>
                  val cursor = json.hcursor
                  ids.foldLeft(List.empty[AppID]) {
                    case (prev, id) =>
                      val appJson = cursor.downField(id.toString)

                      val result = for {
                        success <- appJson.get[Boolean]("success")
                        isPaid =
                          if (!filterFree) true
                          else
                            appJson
                              .downField("data")
                              .downField("price_overview")
                              .succeeded

                      } yield success && isPaid

                      result match {
                        case Right(true) => id :: prev
                        case _           => prev
                      }
                  }
              }
        }
    ).map(_.flatten)

}
