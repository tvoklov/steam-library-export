package volk.steam.libraryexport
package spreadsheet.sheets

import steam.Entities.OwnedGames
import util.Utils._

import spoiwo.model._

/** the most basic sheet, contains game count + the entire library and the amount of minutes played for each game */
object InfoDumpSheet {

  def make(games: OwnedGames, includeHeader: Boolean = true): Sheet = {
    def withHeader: Sheet => Sheet = s =>
      if (!includeHeader) s
      else {
        val (played, unplayed) = games.games.map(_.playtime.allPlaytime).countBoth(_ > 0)

        s.withRows(
          Row().withCellValues("Total games owned", games.gameCount),
          Row().withCellValues("Played games", played, "Unplayed games", unplayed),
          Row.Empty,
          Row(style = CellStyle(fillBackgroundColor = Color.Yellow)).withCellValues("Name", "Minutes played")
        )
      }

    def withRows: Sheet => Sheet =
      _.addRows(
        games.games
          .sortBy(_.name)
          .map(
            gi => Row().withCellValues(gi.name, gi.playtime.allPlaytime)
          )
      )

    def withColumns: Sheet => Sheet =
      _.withColumns(
        Column(index = 0, autoSized = true),
        Column(index = 1, autoSized = true),
        Column(index = 2, autoSized = true),
        Column(index = 3, autoSized = true),
      )

    Sheet(name = "info dump") |> withHeader |> withRows |> withColumns
  }

}
