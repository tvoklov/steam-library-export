package volk.steam.libraryexport.spreadsheet.sheets

import spoiwo.model.{ Column, Row, Sheet }

import volk.steam.libraryexport.steam.Entities.*
import volk.steam.libraryexport.util.Utils.*

@deprecated("use volk.steam.libraryexport.spreadsheet.sheets.HLTBSuggestions")
object SuggestionsSheet {

  def make(
      games: List[(GameInfo, ReviewStats)]
    ): Sheet = {
    def gameOrdering(
        totalPlaytime: Int,
        recentPlaytime: Int,
        reviewStats: ReviewStats,
      ): Float = {
      // i'm trying to include these factors:
      // 1. the game is higher in the list if it has a higher positive review percentage
      // 2. the game is lower in the list if the player had already played it
      // 3. the game is lower in the list if the player had RECENTLY played it

      (1000f * reviewStats.negativeReviewPercentage) +
        ((totalPlaytime / 100f) * reviewStats.positiveReviewPercentage) +
        (recentPlaytime * 150f)
    } |> (_ * 10000) |> (_.toInt)

    val gamesSorted =
      games
        .filter {
          // only showing games that haven't been played for over 120 minutes
          case (g, gr) =>
            g.playtime.allPlaytime <= 120
        }
        .sortBy { case (ge, rs) =>
          gameOrdering(ge.playtime.allPlaytime, ge.playtime.playtimeLast2Weeks.getOrElse(0), rs)
        }

    def withHeader: Sheet => Sheet =
      _.addRows(
        List(
          Row().withCellValues(
            "Name",
            "Your total playtime",
            "Steam review percentage",
            "Review count",
            "Positive review count",
            "Negative review count",
            "Recent play time",
          )
        )
      )

    def withColumns: Sheet => Sheet =
      _.withColumns(
        Column(index = 0, autoSized = true),
        Column(index = 1, autoSized = true),
        Column(index = 2, autoSized = true),
      )

    def withRows: Sheet => Sheet =
      _.addRows(
        gamesSorted.map { case (ge, rs) =>
          Row().withCellValues(
            ge.name,
            ge.playtime.allPlaytime,
            rs.positiveReviewPercentage,
            rs.totalReviewCount,
            rs.positiveReviewCount,
            rs.negativeReviewCount,
            ge.playtime.playtimeLast2Weeks.getOrElse(0),
          )
        }
      )

    Sheet(name = "Game Suggestions") |> withColumns |> withHeader |> withRows
  }

}
