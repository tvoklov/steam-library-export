package volk.steam.libraryexport.cache

import cats.effect.IO

import io.circe.Decoder

import java.nio.file.Path

import volk.steam.libraryexport.util.IOOperations

object ListFileCache {

  def of[T](
      path: Path
    )(
      implicit
      decoder: Decoder[T]
    ): IO[ListFileCache[T]] = IOOperations.parseFile[T].apply(path).map(ListFileCache(_, path))

}

case class ListFileCache[T](
    entities: List[T],
    path: Path,
  )
