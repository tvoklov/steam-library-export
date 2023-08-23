package volk.steam.libraryexport.spreadsheet.sheets

import spoiwo.model.{ Column, Row, Sheet }

import volk.steam.libraryexport.howlongtobeat.HLTBAPI.GameLength
import volk.steam.libraryexport.steam.Entities.*
import volk.steam.libraryexport.util.Utils.*

object HLTBSuggestions {

  def make(
      games: List[(GameInfo, ReviewStats, GameLength)]
    ): Sheet = {
    def gameOrdering(
        totalPlaytime: Int,
        recentPlaytime: Int,
        timeToBeat: Int,
        timeToBeatWithExtras: Int,
        reviewStats: ReviewStats,
      ): Float = {
      // 1. the game is higher in the list if it has a higher positive review percentage
      // 2. the game is higher in the list, if the player has not recently engaged with it
      // 3. the game is higher in the list the lower the amount of it the player has completed

      val relativeCompletion = totalPlaytime / (timeToBeatWithExtras / 100f)

      val recentEngagement =
        if recentPlaytime != 0 then {
          // the higher this is, the lower this should be
          // since the player has played this game recently
          /* that said, there is a possibility of a [0.2h / 0.1h] being lower in the list than a
           * [30h / 29h] */
          val relativeRecentEngagement = totalPlaytime / recentPlaytime

          // so here I am adjusting based on the actual playtime that this was relative to
          /* if the player had a game that is [10h / 1h] it should be higher on the list than a [1h
           * / 0.1h] game */
          // even though they both have a 1/10 relativeRecentEngagement
          relativeRecentEngagement * totalPlaytime
        } else
          0

      (10f * reviewStats.negativeReviewPercentage) +
        (relativeCompletion * reviewStats.positiveReviewPercentage) + recentEngagement +
        (100 - (reviewStats.totalReviewCount / 1000))
    } |> (_ * 10000) |> (_.toInt)

    val gamesSorted =
      games
        .filter {
          // only showing games that are completable and haven't been completed by 25%
          case (g, gr, gl) =>
            gl.main != 0 && (g.playtime.allPlaytime / (gl.mainMinutes / 100f) < 25)
        }
        .sortBy { case (ge, rs, length) =>
          gameOrdering(
            ge.playtime.allPlaytime,
            ge.playtime.playtimeLast2Weeks.getOrElse(0),
            length.mainMinutes,
            length.plusMinutes,
            rs,
          )
        }

    def withHeader: Sheet => Sheet =
      _.addRows(
        List(
          Row().withCellValues(
            "Name",
            "Your total playtime",
            "Time to complete",
            "Time to complete + extras",
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
        gamesSorted.map { case (ge, rs, gl) =>
          Row().withCellValues(
            ge.name,
            ge.playtime.allPlaytime,
            gl.main,
            gl.plus,
            rs.positiveReviewPercentage,
            rs.totalReviewCount,
            rs.positiveReviewCount,
            rs.negativeReviewCount,
            ge.playtime.playtimeLast2Weeks.getOrElse(0),
          )
        }
      )

    Sheet(name = "Game Suggestions 2") |> withColumns |> withHeader |> withRows
  }

}
