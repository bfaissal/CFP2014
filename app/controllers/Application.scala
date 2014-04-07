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
import play.api.libs.json.Reads._
import java.io.{FileInputStream, FileOutputStream, FileNotFoundException, File}

import play.modules.reactivemongo.json.collection.JSONCollection
import play.modules.reactivemongo._
import play.modules.reactivemongo.json._
import reactivemongo.api.Cursor
import reactivemongo.bson.BSONObjectID
import play.api.libs.ws.WS
import play.api.libs.iteratee.Iteratee
import scala.Array
import play.api.libs.ws.WS.WSRequestHolder


object Application extends Controller with MongoController {
  def collection: JSONCollection = db.collection[JSONCollection]("users")

  def talks: JSONCollection = db.collection[JSONCollection]("talks")
  def config: JSONCollection = db.collection[JSONCollection]("config")

  val generateId = (__ \ '_id \ '$oid).json.put( JsString(BSONObjectID.generate.stringify) )


  def index() = Action {
    Ok(views.html.index("JMaghreb"))
  }

  def loginPage = Action {
    Ok(views.html.login("JMaghreb"))
  }

  def profile = Action {
    Ok(views.html.profile("JMaghreb"))
  }
  def admin() = Action {
    Ok(views.html.admin("JMaghreb"))
  }

  def logout = Action {
    Ok(views.html.login("JMaghreb")).withNewSession
  }

  def register = Action.async {
    implicit request => {
      request.body.asJson.map {
        json => {
          collection.insert(json).map(_ => Ok(Messages("registration.creationsuccess.message", json \ "email")))
        }
      }.getOrElse(Future.successful(BadRequest(Messages("globals.serverInternalError.message"))))
    }
  }

  val bodyParser = BodyParser(rh => Iteratee.fold[Array[Byte], Array[Byte]](Array[Byte]())((c,a) => c ++ a ).map(Right(_)) )
  val xbodyParser = BodyParser(rh => Iteratee.foldM[Array[Byte], Array[Byte]](Array[Byte]())((c, a) => {
    println("uploading ... + " + c); Promise.timeout({
      c ++ a
    }, 300)
  }).map(e => Right(e)))

  def upload = Action.async(bodyParser) {
    rq => {
      val fileName = ""+System.nanoTime()
      val url = WS.url(Play.current.configuration.getString("upload.server").get+"uploadPictures.php?name="+fileName)
      rq.headers.keys.foldLeft[WSRequestHolder](url)((h, a) => h.withHeaders(a -> {
        rq.headers.get(a).get
      })).post(rq.body).map(r => {
        Ok(Json.obj("files" -> Json.arr(Json.obj("name" -> (Play.current.configuration.getString("upload.server").get+"cfp/"+fileName+".png")))))
      })
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
    cursor.headOption.map(value => {
      value.map(content => {
        val sessionUser = content.transform((__ \ 'password).json.prune andThen (__ \ 'cpassword).json.prune)
        Ok("ok").withSession(("user", sessionUser.get.toString()))
      }).getOrElse(BadRequest(Messages("globals.serverInternalError.message")))
    })
  }

  def speaker(email: String) = Action.async {
    val cursor: Cursor[JsObject] = collection.find(Json.obj(("_id" -> email))).cursor[JsObject]
    cursor.headOption.map(value => {
      value.map(content => {
        val sessionUser = content.transform(((__ \ 'fname).json.pickBranch and (__ \ 'lname).json.pickBranch  and (__ \ 'image).json.pickBranch).reduce)
        Ok(sessionUser.get)
      }).getOrElse(BadRequest(Messages("globals.serverInternalError.message")))
    })
  }

  def connectedUser() = Action.async {
    implicit request => {
      session.get("user").map {
        connectedUser => {
          val userJson = Json.parse(connectedUser)
          val cursor: Cursor[JsObject] = collection.find(Json.obj(("_id" -> userJson \ "_id"))).cursor[JsObject]
          cursor.headOption.map{_.map{Ok(_)}
            .getOrElse(BadRequest(Messages("globals.serverInternalError.message")))
          }

        }
      }.getOrElse(Future.successful(BadRequest(Messages("globals.serverInternalError.message"))))

    }
  }
  def saveProfile = Action.async {
    implicit request => {
      request.body.asJson.flatMap {
        json => {
          session.get("user").map(connectedUser => {
            println("conncus "+connectedUser)
            println("json "+json)
            collection.update(Json.obj(("_id" -> Json.parse(connectedUser) \ "_id")),json).map(_ => Ok(Messages("registration.save.message")))
          })
        }
      }.getOrElse(Future.successful(BadRequest(Messages("globals.serverInternalError.message"))))
    }
  }

  def talksList() = Action.async {
    implicit request => {
      session.get("user").map(user => {
        val userJson = Json.parse(user)
        val query = Json.obj(("speaker._id" -> userJson \ "_id"), ("status" -> Json.obj(("$ne" -> 5))))
        val cursor: Cursor[JsObject] = talks.find(query).sort(Json.obj(("title" -> 1))).cursor[JsObject]
        val futurePersonsList: Future[List[JsObject]] = cursor.collect[List]()
        //val futurePersonsJsonArray: Future[JsArray] =
        futurePersonsList.map {
          persons =>
            Ok(Json.toJson(persons))
        }

      }).getOrElse(Future.successful(BadRequest(Messages("globals.serverInternalError.message"))))

    }
  }
  def resOk(message:String, data:JsValue) = {
    Ok(Json.obj("message" -> message)++Json.obj("data"-> data))
  }
  def editTalk = Action.async {
    implicit request => {
      request.body.asJson.flatMap {
        json => {
          session.get("user").map(user => {
            val userJson = Json.parse(user)
            json \ "_id" match {
              // creation
              case _: JsUndefined => {

                val generateCreated = (__ \ 'created \ '$date).json.put( JsNumber((new java.util.Date).getTime) )
                val addMongoIdAndDate: Reads[JsObject] = __.json.update( (generateId and generateCreated).reduce )
                val res = json.transform( addMongoIdAndDate  andThen (__ \ 'loading).json.prune ).get
                talks.insert(res ++ Json.obj(("speaker" -> userJson.as[JsObject]))).map(lastError => {
                  resOk(Messages("talk.creationsuccess.message", json \ "email"),res)
                })
              }
              // edit
              case value: JsValue => {
                val query = Json.obj(("_id" -> value), ("speaker._id" -> userJson \ "_id"), ("status" -> 1))
                val generateUpdated = (__ \ 'updated \ '$date).json.put( JsNumber((new java.util.Date).getTime) )
                val res = json.transform(__.json.update(generateUpdated) andThen (__ \ '_id).json.prune andThen ((__ \ '$$hashKey).json.prune).andThen((__ \ 'loading).json.prune).andThen((__ \ 'error).json.prune))
                println("res.get = "+res.get)
                talks.update(query, Json.obj(("$set" -> res.get))).map(lastError =>
                  Ok(Messages("registration.creationsuccess.message", json \ "email")))
              }

            }

          })
        }
      }.getOrElse(Future.successful(BadRequest(Messages("globals.serverInternalError.message"))))
    }
  }

  def getConfig() = Action.async {
    val cursor: Cursor[JsObject] = config.find(Json.obj()).cursor[JsObject]
    cursor.headOption.map(value => {
      value.map(content => {
        Ok(content)
      }).getOrElse(BadRequest(Messages("globals.serverInternalError.message")))
    })
  }

  def saveConfig = Action.async {
    implicit request => {
      request.body.asJson.map {
        json => {
          val res = json \ "_id" match {
            case _: JsUndefined => {
              json.transform(__.json.update(generateId)).get
            }
            case _ => {
              json
            }
          }
          config.save(res).map(lastError =>
            resOk(Messages("registration.configSaved.message"),res))
        }
      }.getOrElse(Future.successful(BadRequest(Messages("globals.serverInternalError.message"))))
    }

  }

}