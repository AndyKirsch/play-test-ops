package play.api.test.ops

import akka.actor.Cancellable
import akka.stream.{Attributes, ClosedShape, Graph, Materializer}
import akka.util.ByteString
import play.api.http.HeaderNames._
import play.api.http.Status._
import play.api.libs.json.{JsValue, Json}
import play.api.libs.streams.Accumulator
import play.api.mvc._
import play.twirl.api.Content

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}

/**
  * Similar to play.api.test.ResultExtractors except that it is designed to work better
  * for org.scalatest.AsyncSuite in ScalaTest 3.0.
  *
  * All methods return either the expected value or a [[Future]] and does not perform
  * any awaits or timeouts. This improves the total time spent on tests and cuts
  * down on processor usage.
  */
trait AsyncResultExtractors {

  //$COVERAGE-OFF$
  /**
    * Extracts the Content-Type of this Content value.
    */
  @deprecated("Use _.contentType instead.", "1.1.0")
  def contentType(of: Content): String = of.contentType

  /**
    * Extracts the content as String.
    */
  @deprecated("Use _.body instead.", "1.1.0")
  def contentAsString(of: Content): String = of.body

  /**
    * Extracts the content as bytes.
    */
  @deprecated("Use _.body.getBytes instead.", "1.1.0")
  def contentAsBytes(of: Content): Array[Byte] = of.body.getBytes

  /**
    * Extracts the content as Json.
    */
  @deprecated("Use Json.parse(_.body) instead.", "1.1.0")
  def contentAsJson(of: Content): JsValue = Json.parse(of.body)

  /**
    * Extracts the Content-Type of this Result value.
    */
  @deprecated("Use for action.run().map(contentType) instead.", "1.1.0")
  def contentType(result: Accumulator[ByteString, Result])(
    implicit ec: ExecutionContext, mat: Materializer = NoMaterializer): Future[Option[String]] = {
    result.run() map contentType
  }
  // $COVERAGE-ON$

  /**
    * Extracts the Content-Type of this Result value.
    */
  def contentType(result: Result): Option[String] = {
    result.body.contentType.map(_.split(";").take(1).mkString.trim)
  }

  /**
    * Extracts the Charset of this Result value.
    */
  def charset(result: Result): Option[String] = {
    result.body.contentType match {
      case Some(s) if s.contains("charset=") => Some(s.split("; *charset=").drop(1).mkString.trim)
      case _ => None
    }
  }

  /**
    * Extracts the content as String.
    */
  def contentAsString(result: Result)(implicit ec: ExecutionContext, mat: Materializer = NoMaterializer): Future[String] = {
    contentAsBytes(result).map(_.decodeString(charset(result).getOrElse("utf-8")))
  }

  /**
    * Extracts the content as bytes.
    */
  def contentAsBytes(result: Result)(implicit mat: Materializer = NoMaterializer): Future[ByteString] = {
    result.body.consumeData
  }

  /**
    * Extracts the content as Json.
    */
  def contentAsJson(result: Result)(implicit ec: ExecutionContext, mat: Materializer = NoMaterializer): Future[JsValue] = {
    contentAsString(result).map(Json.parse)
  }

  /**
    * Extracts the Status code of this Result value.
    */
  def status(result: Result): Int = result.header.status

  /**
    * Extracts the Cookies set by this Result value.
    */
  def cookies(result: Result): Cookies = Cookies.fromSetCookieHeader(header(SET_COOKIE, result))

  /**
    * Extracts the Flash values set by this Result value.
    */
  def flash(result: Result): Flash = Flash.decodeFromCookie(cookies(result).get(Flash.COOKIE_NAME))

  /**
    * Extracts the Session values set by this Result value.
    */
  def session(result: Result): Session = Session.decodeFromCookie(cookies(result).get(Session.COOKIE_NAME))

  /**
    * Extracts the Location header of this Result value if this Result is a Redirect.
    */
  def redirectLocation(result: Result): Option[String] = result.header match {
    case ResponseHeader(FOUND, headers, _) => headers.get(LOCATION)
    case ResponseHeader(SEE_OTHER, headers, _) => headers.get(LOCATION)
    case ResponseHeader(TEMPORARY_REDIRECT, headers, _) => headers.get(LOCATION)
    case ResponseHeader(MOVED_PERMANENTLY, headers, _) => headers.get(LOCATION)
    case ResponseHeader(_, _, _) => None
  }

  /**
    * Extracts an Header value of this Result value.
    */
  def header(header: String, result: Result): Option[String] = result.header.headers.get(header)

  /**
    * Extracts all Headers of this Result value.
    */
  def headers(result: Result): Map[String, String] = result.header.headers
}

// $COVERAGE-OFF$
/**
  * In 99% of cases, when running tests against the result body, you don't actually need a materializer since it's a
  * strict body. So, rather than always requiring an implicit materializer, we use one if provided, otherwise we have
  * a default one that simply throws an exception if used.
  */
private[ops] object NoMaterializer extends Materializer {

  override def withNamePrefix(name: String): Materializer =
    throw new UnsupportedOperationException("NoMaterializer cannot be named")

  override def materialize[Mat](runnable: Graph[ClosedShape, Mat]): Mat =
    throw new UnsupportedOperationException("NoMaterializer cannot materialize")

  override def materialize[Mat](runnable: Graph[ClosedShape, Mat], initialAttributes: Attributes): Mat =
    throw new UnsupportedOperationException("NoMaterializer cannot materialize")

  override def executionContext: ExecutionContextExecutor =
    throw new UnsupportedOperationException("NoMaterializer does not provide an ExecutionContext")

  override def scheduleOnce(delay: FiniteDuration, task: Runnable): Cancellable =
    throw new UnsupportedOperationException("NoMaterializer cannot schedule a single event")

  override def schedulePeriodically(initialDelay: FiniteDuration, interval: FiniteDuration, task: Runnable): Cancellable =
    throw new UnsupportedOperationException("NoMaterializer cannot schedule a repeated event")
}
// $COVERAGE-ON$
