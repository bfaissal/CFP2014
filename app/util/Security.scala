package util

import play.api.mvc.{SimpleResult, Request, ActionBuilder}
import scala.concurrent.Future
import play.api.Logger
import controllers.Application

import play.api.libs.functional.syntax._
import play.api.libs.json._
import play.api.libs.json.Reads._

/**
 * Created with IntelliJ IDEA.
 * User: faissalboutaounte
 * Date: 2014-04-20
 * Time: 10:36
 * To change this template use File | Settings | File Templates.
 */
object AdminAction extends ActionBuilder[Request] {
  def invokeBlock[A](request: Request[A], block: (Request[A]) => Future[SimpleResult]) = {
    Logger.info("Calling action")
    request.session.get("user") match {
      case Some(x) => {(Json.parse(x)\ "admin").toString() +" - "+println((Json.parse(x)\ "admin").toString().equals("true") );if((Json.parse(x)\ "admin").toString().equals("true") ) block(request) else Future.successful(Application.Unauthorized(":x"))}
      case _ => { Future.successful(Application.Unauthorized(":x"))};
    }

  }

  }
object ReviewAction extends ActionBuilder[Request] {
  def invokeBlock[A](request: Request[A], block: (Request[A]) => Future[SimpleResult]) = {
    Logger.info("Calling action")
    request.session.get("reviewer") match {
      case Some("true") => block(request)
      case _ => { Future.successful(Application.Unauthorized(":x"))};
    }
  }
}
