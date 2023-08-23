package volk.steam.libraryexport.cache

import cats.effect.IO

import io.circe.*
import io.circe.syntax.*

import java.io.FileNotFoundException
import java.nio.file.Path

import volk.steam.libraryexport.util.IOOperations as IOOps

object MapFileCache {

  def of[K, V](
      path: Path
    )(
      implicit
      decoder: Decoder[(K, V)]
    ): IO[MapFileCache[K, V]] =
    IOOps
      .parseFile[(K, V)]
      .apply(path)
      .handleErrorWith {
        case _: FileNotFoundException =>
          IO.pure(Nil)
        case th                       =>
          IO.raiseError(th)
      }
      .map(_.toMap)
      .map(MapFileCache(_, path))

}

case class MapFileCache[Key, Value](
    entities: Map[Key, Value],
    path: Path,
  ) {

  def save(
      implicit
      keyEncoder: Encoder[(Key, Value)]
    ): IO[Unit] = IOOps.writeToFile(entities.map(_.asJson.noSpaces).toList)(path)

  def ++ (
      otherEntities: Map[Key, Value]
    ): MapFileCache[Key, Value] = MapFileCache(entities ++ otherEntities, path)

}
