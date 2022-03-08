package volk.steam.libraryexport
package cache

import util.{ IOOperations => IOOps }

import cats.effect.IO
import io.circe.Decoder

import java.nio.file.Path

object ListFileCache {

  def of[T](path: Path)(implicit decoder: Decoder[T]): IO[ListFileCache[T]] =
    IOOps.parseFile[T].apply(path).map(ListFileCache(_, path))

}

case class ListFileCache[T](entities: List[T], path: Path) {}
