package volk.steam.libraryexport
package spreadsheet

import spreadsheet.sheets.InfoDumpSheet
import steam.Entities.OwnedGames

import spoiwo.model.Workbook

object WorkbookGenerator {

  def createWorkbook(games: OwnedGames): Workbook = {
    Workbook(
      sheets = List(
        InfoDumpSheet.make(games, includeHeader = true)
      )
    )
  }

}
