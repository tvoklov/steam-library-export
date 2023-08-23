package volk.steam.libraryexport.howlongtobeat

import cats.effect.IO

import io.circe.derivation
import io.circe.derivation.{ Configuration, ConfiguredCodec }
import io.circe.generic.auto.*
import io.circe.generic.auto.given
import io.circe.syntax.*
import org.http4s.{ Header, Headers, Method, Request }
import org.http4s.circe.CirceEntityCodec.given
import org.http4s.circe.JsonDecoder
import org.http4s.client.Client
import org.http4s.headers.{ Authorization, Referer }
import org.http4s.implicits.uri

object HLTBAPI {

  case class GameLength(
      main: Int,
      plus: Int,
      completionist: Int,
    ) {

    val mainMinutes: Int          = main / 60
    val plusMinutes: Int          = plus / 60
    val completionistMinutes: Int = completionist / 60

  }

  def howLongToBeat(
      gameName: String
    )(
      using
      c: Client[IO]
    ): IO[Option[GameLength]] =
    for {
      hltbr <- search(gameName)

      foundGameOpt = hltbr.data.find(_.gameName.toLowerCase.equals(gameName.toLowerCase()))
    } yield foundGameOpt.map(gd => GameLength(gd.compMain, gd.compPlus, gd.comp100))

  private def search(
      gameName: String
    )(
      using
      c: Client[IO]
    ): IO[json.HLTBSearchRes] =
    c.expect[json.HLTBSearchRes](
      Request[IO](
        method = Method.POST,
        uri = uri.searchUri,
        headers =
          Headers
            .empty
            .put(
              // tricking hltb into thinking this is a request made from their website
              // without this, hltb returns 401
              Header.Raw(Referer.headerInstance.name, uri.baseUri.toString)
            ),
      ).withEntity(json.HLTBSearchReq(gameName).asJson)
    )

  private object json {

    object HLTBSearchReq {

      def apply(
          gameName: String
        ): HLTBSearchReq = HLTBSearchReq(gameName.split(' ').toList)

    }

    case class HLTBSearchReq(
        searchTerms: List[String]
      )

    case class HLTBSearchRes(
        count: Option[Int],
        pageTotal: Option[Int],
        pageSize: Int,
        pageCurrent: Int,
        data: List[HLTBGameData],
      )

    /**
     * @param count
     *   ???
     * @param gameId
     *   id on hltb
     * @param gameName
     *   name on hltb
     * @param compMain
     *   "main story", in seconds
     * @param compPlus
     *   "main + extra", in seconds
     * @param comp100
     *   "completionist", in seconds
     * @param releaseWorld
     *   year of release
     */
    case class HLTBGameData(
        count: Int,
        gameId: Int,
        gameName: String,
        compMain: Int,
        compPlus: Int,
        comp100: Int,
        releaseWorld: Int,
      ) derives ConfiguredCodec

    object HLTBGameData {

      // doesn't snake case imply this already???
      private def realSnakeCase: String => String = {
        def go(
            rest: List[Char],
            prevIsDigit: Boolean = false,
          ): List[Char] =
          rest match {
            case Nil     =>
              Nil
            case x :: xs =>
              if x.isUpper then
                '_' :: x.toLower :: go(xs)
              else if x.isDigit then {
                if !prevIsDigit then
                  '_' :: x.toLower :: go(xs, true)
                else
                  x :: go(xs, true)
              } else
                x :: go(xs)
          }

        x => go(x.toList).mkString("")
      }

      given Configuration = Configuration.default.withTransformMemberNames(realSnakeCase)

    }

  }

  private object uri {

    val baseUri = uri"https://howlongtobeat.com"

    val apiUri = baseUri / "api"

    val searchUri = apiUri / "search"

  }

}
