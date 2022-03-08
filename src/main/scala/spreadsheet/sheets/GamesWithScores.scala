package volk.steam.libraryexport
package spreadsheet.sheets

import steam.Entities.{ GameInfo, ReviewStats }

import spoiwo.model._

object GamesWithScores {

  private val headerRows = List(
    Row(style = CellStyle(fillBackgroundColor = Color.Yellow))
      .withCellValues(
        List(
          "Name",
          "Minutes Played",
          "Review Score",
          "Total reviews"
        )
      )
  )

  private val columns = List(
    Column(index = 0, autoSized = true),
    Column(index = 1, autoSized = true),
    Column(index = 2, autoSized = true),
    Column(index = 3, autoSized = true),
  )

  def make(gamesWithScores: List[(GameInfo, ReviewStats)]): Sheet = {
    def makeRows: List[Row] =
      gamesWithScores
        .sortBy(_._1.name)
        .map {
          case (gi, rs) =>
            Row().withCellValues(
              List(
                gi.name,
                gi.playtime.allPlaytime,
                rs.positiveReviewPercentage,
                rs.totalReviewCount
              )
            )
        }

    Sheet(name = "Game review scores")
      .withRows(headerRows)
      .withColumns(columns)
      .addRows(makeRows)
  }

}
