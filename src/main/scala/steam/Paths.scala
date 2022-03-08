package volk.steam.libraryexport
package steam

import steam.Types._

import org.http4s.Uri
import org.http4s.implicits._

private[steam] object Paths {

  private val apiRoot: Uri   = uri"https://api.steampowered.com"
  private val storeRoot: Uri = uri"https://store.steampowered.com"

  private val ISteamUserRoot: Uri     = apiRoot / "ISteamUser"
  private val ISteamUserStats: Uri    = apiRoot / "ISteamUserStats"
  private val IPlayerServiceRoot: Uri = apiRoot / "IPlayerService"

  val resolveVanityURLRoot: Uri    = ISteamUserRoot / "ResolveVanityURL" / "v1"
  val getOwnedGamesRoot: Uri       = IPlayerServiceRoot / "GetOwnedGames" / "v1"
  val getUserStatsForGameRoot: Uri = ISteamUserStats / "GetUserStatsForGame" / "v2"

  val reviewsRoot: AppID => Uri = storeRoot / "appreviews" / _
  val detailsRoot: Uri          = storeRoot / "api" / "appdetails"

}
