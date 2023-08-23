package volk.steam.libraryexport.util

import cats.effect.IO

import io.circe.*
import io.circe.parser.*

import scala.jdk.CollectionConverters.*

import java.nio.file.Path

object IOOperations {

  def readFile: Path => IO[List[String]] =
    Resources.reader(_).use(r => IO(r.lines().iterator().asScala.toList))

  def parseFile[T](
      implicit
      decoder: Decoder[T]
    ): Path => IO[List[T]] = readFile.andThen(_.map(_.flatMap(decode[T](_).toOption)))

  def writeToFile(
      lines: List[String],
      append: Boolean = false,
    ): Path => IO[Unit] =
    Resources
      .writer(_)
      .use(bf =>
        if append then
          IO(bf.append(lines.mkString("\n")))
        else
          IO(bf.write(lines.mkString("\n"))),
      )

  def runUntilOneCrashes[T](
      ios: List[IO[T]]
    ): IO[List[T]] = runUntilOneCrashes(ios, None)

  def runUntilOneCrashes[T, Err](
      ios: List[IO[T]],
      errorType: Option[Class[Err]] = None,
    ): IO[List[T]] =
    ios match {
      case Nil     =>
        IO.pure(Nil)
      case x :: xs =>
        val io =
          for {
            xRes    <- x
            restRes <- runUntilOneCrashes(xs)
          } yield xRes :: restRes

        io.handleError {
          case err if errorType.forall(err.getClass == _) =>
            err.printStackTrace()
            Nil
        }
    }

}
