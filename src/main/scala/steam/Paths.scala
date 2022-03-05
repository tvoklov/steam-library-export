package volk.steam.libraryexport
package steam

import org.http4s.Uri
import org.http4s.implicits._

object Paths {

  private[steam] val root: Uri = uri"https://api.steampowered.com"

  private[steam] val ISteamUserRoot: Uri     = root / "ISteamUser"
  private[steam] val ISteamUserStats: Uri     = root / "ISteamUserStats"
  private[steam] val IPlayerServiceRoot: Uri = root / "IPlayerService"

  private[steam] val resolveVanityURLRoot: Uri = ISteamUserRoot / "ResolveVanityURL" / "v1"
  private[steam] val getOwnedGamesRoot: Uri    = IPlayerServiceRoot / "GetOwnedGames" / "v1"
  private[steam] val getUserStatsForGameRoot: Uri    = ISteamUserStats / "GetUserStatsForGame" / "v2"

}
