package volk.steam.libraryexport
package spreadsheet

import cats.effect.IO
import spoiwo.model.{ Sheet, Workbook }

object IOWrap {

  implicit class SPOIWOIOWB(workbook: Workbook) {
    def toXlsx(path: String): IO[Unit] = {
      import spoiwo.natures.xlsx.Model2XlsxConversions._
      IO(workbook.saveAsXlsx(path))
    }

    import spoiwo.natures.csv.CsvProperties

    def toCsv(path: String, separator: Option[Char]): IO[Unit] = {
      import spoiwo.natures.csv.Model2CsvConversions._

      IO(
        workbook.saveAsCsv(
          path,
          properties = separator.fold(CsvProperties.Default)(
            s => CsvProperties.Default.copy(separator = s)
          )
        )
      )
    }

    def toCsv(path: String, csvProperties: CsvProperties): IO[Unit] = {
      import spoiwo.natures.csv.Model2CsvConversions._

      IO(workbook.saveAsCsv(path, properties = csvProperties))
    }
  }

  implicit class SPOIWOIOSheet(sheet: Sheet) {
    def toXlsx(path: String): IO[Unit] = {
      import spoiwo.natures.xlsx.Model2XlsxConversions._
      IO(sheet.saveAsXlsx(path))
    }

    import spoiwo.natures.csv.CsvProperties

    def toCsv(path: String, separator: Option[Char]): IO[Unit] = {
      import spoiwo.natures.csv.Model2CsvConversions._

      IO(
        sheet.saveAsCsv(
          path,
          properties = separator.fold(CsvProperties.Default)(
            s => CsvProperties.Default.copy(separator = s)
          )
        )
      )
    }

    def toCsv(path: String, csvProperties: CsvProperties): IO[Unit] = {
      import spoiwo.natures.csv.Model2CsvConversions._

      IO(sheet.saveAsCsv(path, properties = csvProperties))
    }
  }

}
