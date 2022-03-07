package volk.steam.libraryexport
package cache

import util.{ IOOperations => IOOps }

import cats.effect.IO
import io.circe._
import io.circe.syntax._

import java.io.FileNotFoundException
import java.nio.file.Path

object MapFileCache {

  def of[K, V](path: Path)(implicit decoder: Decoder[(K, V)]): IO[MapFileCache[K, V]] =
    IOOps
      .parseFile[(K, V)]
      .apply(path)
      .handleErrorWith {
        case _: FileNotFoundException => IO.pure(Nil)
        case th                       => IO.raiseError(th)
      }
      .map(_.toMap)
      .map(MapFileCache(_, path))

}

case class MapFileCache[Key, Value](entities: Map[Key, Value], path: Path) {

  def save(implicit keyEncoder: Encoder[(Key, Value)], valueEncoder: Encoder[Value]): IO[Unit] =
    IOOps.writeToFile(
      entities.map(_.asJson.noSpaces).toList
    )(path)

  def ++(otherEntities: Map[Key, Value]): MapFileCache[Key, Value] = MapFileCache(entities ++ otherEntities, path)

}
