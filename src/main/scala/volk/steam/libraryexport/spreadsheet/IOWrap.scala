package volk.steam.libraryexport.spreadsheet

import cats.effect.IO

import spoiwo.model.{ Sheet, Workbook }
import spoiwo.natures.csv.CsvProperties

object IOWrap {

  extension (
      workbook: Workbook
    )

    def toXlsx(
        path: String
      ): IO[Unit] = {
      import spoiwo.natures.xlsx.Model2XlsxConversions.*

      IO(workbook.saveAsXlsx(path))
    }

    def toCsv(
        path: String,
        separator: Option[Char],
      ): IO[Unit] = {
      import spoiwo.natures.csv.Model2CsvConversions.*

      IO(
        workbook.saveAsCsv(
          path,
          properties =
            separator.fold(CsvProperties.Default)(s => CsvProperties.Default.copy(separator = s)),
        )
      )
    }

    def toCsv(
        path: String,
        csvProperties: CsvProperties,
      ): IO[Unit] = {
      import spoiwo.natures.csv.Model2CsvConversions.*

      IO(workbook.saveAsCsv(path, properties = csvProperties))
    }

  extension (
      sheet: Sheet
    )

    def toXlsx(
        path: String
      ): IO[Unit] = {
      import spoiwo.natures.xlsx.Model2XlsxConversions.*
      IO(sheet.saveAsXlsx(path))
    }

    def toCsv(
        path: String,
        separator: Option[Char],
      ): IO[Unit] = {
      import spoiwo.natures.csv.Model2CsvConversions.*
      IO(
        sheet.saveAsCsv(
          path,
          properties =
            separator.fold(CsvProperties.Default)(s => CsvProperties.Default.copy(separator = s)),
        )
      )
    }

    def toCsv(
        path: String,
        csvProperties: CsvProperties,
      ): IO[Unit] = {
      import spoiwo.natures.csv.Model2CsvConversions.*

      IO(sheet.saveAsCsv(path, properties = csvProperties))
    }

}
