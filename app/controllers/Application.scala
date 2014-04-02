package controllers


import play.api._
import play.api.mvc._
import play.modules.reactivemongo.MongoController
import play.modules.reactivemongo.json.collection.JSONCollection
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.libs.concurrent.Promise
import scala.concurrent.Future
import play.api.i18n.Messages

import play.api.libs.functional.syntax._
import play.api.libs.json._
import java.io.{FileInputStream, FileOutputStream, FileNotFoundException, File}

import play.modules.reactivemongo.json.collection.JSONCollection
import reactivemongo.api.Cursor


object Application extends Controller with MongoController {
  def collection: JSONCollection = db.collection[JSONCollection]("users")

  def talks: JSONCollection = db.collection[JSONCollection]("talks")

  def index = Action {
    Ok(views.html.index("Your new application is ready."))
  }

  def loginPage = Action {
    Ok(views.html.login("Your new application is ready."))
  }

  def register = Action.async {
    implicit request => {
      request.body.asJson.map {
        json => {
          collection.insert(json).map(lastError =>
            Ok(Messages("registration.creationsuccess.message", json \ "email")))
        }
      }.getOrElse(Future.successful(BadRequest(Messages("globals.serverInternalError.message"))))
    }

  }

  def upload = Action(parse.maxLength(maxLength = 1024000, parse.multipartFormData)) {
    request =>
      request.body match {
        case Right(multiPartBody) =>
          multiPartBody.file("file").map {
            picture =>
              picture.contentType match {
                case Some("image/gif") | Some("image/jpg") | Some("image/png") | Some("image/jpeg") =>
                  val image = System.currentTimeMillis().toString
                  val res = Json.obj("files" -> Json.arr(Json.obj("name" -> image)))
                  picture.ref.moveTo(new File(System.getenv("TMPDIR") + "uploads/" + image + ".gif"))
                  Ok(res).as(JSON)
                case _ => BadRequest("incorrect file Type")
              }
          }.getOrElse {
            Redirect(routes.Application.index).flashing("error" -> "Missing file")
          }
        case Left(multiPartBody) => BadRequest("Max size exceeded")
        case _ => BadRequest("Other")
      }
  }

  def images(id: String) = Action {
    try {
      Ok.sendFile(new File(System.getenv("OPENSHIFT_DATA_DIR") + "images/" + id + ".gif")).as("image/png")
    }
    catch {
      case e: FileNotFoundException => NotFound("Image not found")
    }

  }

  def tempImages(id: String) = Action {
    try {
      Ok.sendFile(new File(System.getenv("TMPDIR") + "uploads/" + id + ".gif")).as("image/png")
    }
    catch {
      case e: FileNotFoundException => NotFound("Image not found");
    }

  }

  def deleteImages(id: String, action: Boolean) = Action {
    implicit request => {
      try {
        val repo = if (action) System.getenv("OPENSHIFT_DATA_DIR") + "images/" + id + ".gif" else System.getenv("TMPDIR") + "uploads/" + id + ".gif"
        val imgSrc = new File(repo)
        imgSrc.delete()
        Ok("File Deleted");
      }
      catch {
        case e: FileNotFoundException => Ok("No file deleted");
      }
    }
  }


  def login(email: String, password: String) = Action.async {
    val cursor: Cursor[JsObject] = collection.find(Json.obj(("_id" -> email), ("password" -> password))).cursor[JsObject]
    val res = cursor.headOption
    res.map(value => {
      value.map(content => {
        val sessionUser = content.transform((__ \ 'password).json.prune andThen (__ \ 'cpassword).json.prune)
        Ok("ok").withSession(("user", sessionUser.get.toString()))
      }).getOrElse(BadRequest("ooof"))
    })
  }

  def talksList() = Action.async {
    implicit request => {
      session.get("user").map(user => {
        val userJson = Json.parse(user)
        val query = Json.obj(("speaker._id" -> userJson \ "_id"))
        val cursor: Cursor[JsObject] = talks.find(query).cursor[JsObject]
        val futurePersonsList: Future[List[JsObject]] = cursor.collect[List]()
        val futurePersonsJsonArray: Future[JsArray] = futurePersonsList.map {
          persons =>
            Json.arr(persons)
        }
        futurePersonsJsonArray.map {
          persons =>
            Ok(persons)
        }
      }).getOrElse(Future.successful(BadRequest(Messages("globals.serverInternalError.message"))))

    }
  }

  def editTalk = Action.async {
    implicit request => {
      request.body.asJson.flatMap {
        json => {
          session.get("user").map(user => {
            val userJson = Json.parse(user)
            json \ "_id" match {
              case _: JsUndefined => {
                val res = json.as[JsObject] ++ Json.obj(("speaker" -> userJson.as[JsObject]))
                talks.insert(res).map(lastError =>
                  Ok(Messages("talk.creationsuccess.message", json \ "email")))
              }
              case value: JsValue => {
                val query = Json.obj(("_id" -> value), ("speaker._id" -> userJson \ "_id"));
                val res = json.transform((__ \ '_id).json.prune andThen((__ \ '$$hashKey).json.prune).andThen((__ \ 'loading).json.prune).andThen((__ \ 'error).json.prune))
                talks.update(query, res.get).map(lastError =>
                  Ok(Messages("registration.creationsuccess.message", json \ "email")))
              }

            }

          })
        }
      }.getOrElse(Future.successful(BadRequest(Messages("globals.serverInternalError.message"))))
    }

  }

}