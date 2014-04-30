import controllers.Application
import play.api.Logger

import play.api.mvc._
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext

/**
 * Created with IntelliJ IDEA.
 * User: faissalboutaounte
 * Date: 2014-04-22
 * Time: 23:09
 * To change this template use File | Settings | File Templates.
 */


object LoggingFilter extends Filter {
  val timeout: Long = 720000
  //000
  val sessionVariable = "lastAccessedTime"

  def apply(nextFilter: (RequestHeader) => Future[SimpleResult])
           (requestHeader: RequestHeader): Future[SimpleResult] = {
    /*
    val startTime = System.currentTimeMillis
    if (requestHeader.uri.startsWith("/login/") || requestHeader.uri.startsWith("/logout")
      || requestHeader.uri.equals("/") || requestHeader.uri.startsWith("/assets/")) {
      nextFilter(requestHeader)
    }
    else {
      if ((startTime - requestHeader.session.get(sessionVariable).getOrElse(startTime.toString).toLong) > timeout) {
        Future.successful(Results.TemporaryRedirect("/logout"))
      }
      else {
        nextFilter(requestHeader).map {
          _//.withSession(requestHeader.session + (sessionVariable -> ("" + startTime)))
        }
      }
    }
    */
    nextFilter(requestHeader)
  }
}


object Global extends WithFilters(LoggingFilter) {

}
