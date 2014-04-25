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
  val timeout: Long = 3600
  //000
  val sessionVariable = "lastAccessedTime"

  def apply(nextFilter: (RequestHeader) => Future[SimpleResult])
           (requestHeader: RequestHeader): Future[SimpleResult] = {
    val startTime = System.currentTimeMillis
    if (!requestHeader.uri.startsWith("/login/") && (System.currentTimeMillis() - requestHeader.session.get(sessionVariable).getOrElse(System.currentTimeMillis().toString).toLong) > timeout) {
      println("++++++******----------------------------------------------------------------------")
      println(" : uri = "+requestHeader.uri)
      nextFilter(requestHeader).map {
        _.withSession((sessionVariable -> ("" + System.currentTimeMillis())))
      }
    }
    else {
      nextFilter(requestHeader).map {

        result =>{
          println("connected user = "+requestHeader.session.get("user"))

          result.withSession(requestHeader.session + (sessionVariable -> ("" + System.currentTimeMillis())))
        }
      }
    }
  }
}


object Global extends WithFilters(LoggingFilter) {

}
