package volk.steam.libraryexport

import cats.effect._

object Main extends IOApp {

  override def run(args: List[String]): IO[ExitCode] =
    readArgs(args) match {
      case Left(value)                  => IO.raiseError(new Throwable(value))
      case Right(Args(uid, akey, file)) => App.runApp(akey, uid, file)
    }

  case class Args(userID: String, apiKey: String, filename: String)

  def readArgs(args: List[String]): Either[String, Args] = {
    @scala.annotation.tailrec
    def go(userID: Option[String], apiKey: Option[String], rest: List[String]): Either[String, Args] =
      rest match {
        case Nil => Left("not enough arguments")
        case "-f" :: xs =>
          userID.zip(apiKey) match {
            case None              => Left("please input the filename as the last argument")
            case Some((uid, akey)) => Right(Args(uid, akey, xs.mkString("").replace("\"", "")))
          }
        case "-usr" :: uid :: xs  => go(Some(uid), apiKey, xs)
        case "-api" :: akey :: xs => go(userID, Some(akey), xs)
        case _                    => Left("weird arguments")
      }
    go(None, None, args)
  }
}
