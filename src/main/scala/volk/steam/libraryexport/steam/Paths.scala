package volk.steam.libraryexport.steam

import org.http4s.implicits.*
import org.http4s.Uri

import volk.steam.libraryexport.steam.Types.*

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
