package volk.steam.libraryexport
package util

import cats.effect._
import fs2.io.file.FileSystemException

import java.io.{ BufferedReader, BufferedWriter, File, FileNotFoundException }
import java.nio.charset.Charset
import java.nio.file.{ Files, Path }

object Resources {

  implicit class IOStuff[T](io: IO[T]) {
    def flatMapIfMatches[X >: T](pf: PartialFunction[T, IO[X]]): IO[X] =
      io.flatMap(
        t => pf.lift.apply(t).getOrElse(IO.pure(t))
      )
  }

  def reader(path: Path, charset: Charset = Charset.forName("utf-8")): Resource[IO, BufferedReader] =
    file(path, createNewIfNotExists = false).flatMap(
      file =>
        Resource.make(
          IO(
            Files.newBufferedReader(file.toPath, charset)
          )
        )(
          r => IO(r.close())
        )
    )

  def writer(path: Path, canCreateFile: Boolean = true, charset: Charset = Charset.forName("utf-8")): Resource[IO, BufferedWriter] =
    file(path, writable = true, createNewIfNotExists = canCreateFile)
      .flatMap(
        file =>
          Resource.make(
            IO(
              Files.newBufferedWriter(file.toPath, charset)
            )
          )(
            w =>
              IO {
                w.flush()
                w.close()
              }
          )
      )

  def file(
      path: Path,
      readable: Boolean = true,
      writable: Boolean = false,
      executable: Boolean = false,
      createNewIfNotExists: Boolean = true
  ): Resource[IO, File] = Resource.make {
    val f = path.toFile

    for {
      _ <- // check if file exists or create it
        IO(f.exists()).flatMapIfMatches {
          case false =>
            if (createNewIfNotExists) createFile(f)
            else IO.raiseError(new FileNotFoundException(s"File ${f.getPath} does not exist."))
        }

      _ <-
        IO { // check if file is a good file
          if (f.isDirectory) throw new IllegalArgumentException(s"${f.getPath} is a directory.")
          else if (writable && !f.canWrite) throw new FileSystemException(s"File ${f.getPath} is not writable.")
          else if (readable && !f.canRead) throw new FileSystemException(s"File ${f.getPath} is not readable.")
          else if (executable && !f.canExecute) throw new FileSystemException(s"File ${f.getPath} is not executable.")
        }

    } yield f
  }(
    _ => IO.unit
  )

  def createFile(file: File): IO[Unit] =
    IO {
      val pf = file.getParentFile
      if (!(pf.exists() || pf.mkdirs()))
        throw new IllegalArgumentException(s"Could not create the parent directory for file ${file.getPath}")

      if (!file.createNewFile())
        throw new FileSystemException(s"Could not create the file ${file.getPath}")
    }

}
