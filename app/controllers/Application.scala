package controllers


import _root_.util._
import play.api._
import play.api.mvc._
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.libs.concurrent.Promise
import scala.concurrent.Future
import play.api.i18n.Messages

import play.api.libs.functional.syntax._
import play.api.libs.json._
import play.api.libs.json.Reads._
import java.io.{FileInputStream, FileOutputStream, FileNotFoundException, File}
import play.modules.reactivemongo._
import reactivemongo.api.Cursor
import reactivemongo.bson.{BSONInteger, BSONObjectID}
import play.api.libs.ws.WS
import play.api.libs.iteratee.Iteratee
import scala.Array
import reactivemongo.core.errors.DatabaseException
import play.modules.reactivemongo.json.collection.JSONCollection
import play.api.libs.json.JsString
import scala.Some
import play.api.libs.json.JsNumber
import reactivemongo.bson.BSONInteger
import play.api.libs.ws.WS.WSRequestHolder
import play.api.libs.json.JsObject
import scala.concurrent.duration._


object Application extends Controller with MongoController {
  def collection: JSONCollection = db.collection[JSONCollection]("users")

  def talks: JSONCollection = db.collection[JSONCollection]("talks")

  def config: JSONCollection = db.collection[JSONCollection]("config")

  val generateId = (__ \ '_id \ '$oid).json.put(JsString(BSONObjectID.generate.stringify))
  val generatActivationCode = __.json.update((__ \ 'activationCode).json.put(JsString(CFPUtil.randomString())))


  def index() = Action {
    Ok(views.html.index("JMaghreb",openCFP))
  }

  def loginPage = Action {
    Ok(views.html.login("JMaghreb",openCFP)).withCookies(List(Cookie("test", "test"), Cookie("mtest", "tmmmest")): _*).withHeaders(("3ajibe", "hona"))
  }

  def profile = Action {
    Ok(views.html.profile("JMaghreb"))
  }

  def revProfile = Action {
    Ok(views.html.revProfile("JMaghreb"))
  }

  def admin() = AdminAction {
    Ok(views.html.admin("JMaghreb"))
  }

  def adminTalks() = AdminAction {
    Ok(views.html.adminTalks("JMaghreb"))
  }
  def adminSpeaker() = AdminAction {
    Ok(views.html.speakerAdmin("JMaghreb"))
  }
  def logout = Action {
    Ok(views.html.login("JMaghreb",openCFP)).withNewSession
  }

  def adminCreateSpeaker = AdminAction.async {
    implicit request => {
      request.body.asJson.map {
        gjson => {
          val speaker = (gjson \ "speaker")

          val userJson = speaker.transform(generateId andThen (__.json.update((__ \ 'accepted).json.put(JsBoolean(true)))) andThen (__.json.update((__ \ 'actif).json.put(JsNumber(1)))) andThen (__ \ 'admin).json.prune andThen (__ \ 'reviewer).json.prune).get
          collection.insert(userJson).map(_ => {
            {
              val insertedUser = userJson.transform((__ \ 'password).json.prune andThen (__ \ 'cpassword).json.prune
                andThen (__ \ 'activationCode).json.prune andThen (__ \ 'id).json.prune) .get
              val seqTalks = (gjson \ "talks").as[List[JsValue]]
              seqTalks.foreach(ajsValue => {
                val generateCreated = (__ \ 'created \ 'date).json.put(JsNumber((new java.util.Date).getTime))
                val addMongoIdAndDate: Reads[JsObject] = __.json.update((generateId and generateCreated).reduce)

                val res = ajsValue.transform(addMongoIdAndDate andThen (__.json.update((__ \ 'status).json.put(JsNumber(3)))) andThen (__ \ 'loading).json.prune).get
                talks.insert(res ++ Json.obj(("speaker" -> insertedUser))).map(lastError => {
                  resOk(Messages("talk.creationsuccess.message"), res)
                })
              })
            }
            Ok(Messages("registration.creationsuccess.message", speaker \ "id"))
          }
          ).recover {
            case e: DatabaseException => {
              e.code match {
                case Some(11000) => BadRequest(Messages("globals.emailexists.message"))
                case _ => BadRequest(Messages("globals.serverInternalError.message"))
              }
            }
          }

        }
      }.getOrElse(Future.successful(BadRequest(Messages("globals.serverInternalError.message"))))
    }
  }
  def createSpeaker = Action.async {
    implicit request => {
      request.body.asJson.map {
        json => {
          val userJson = json.transform((__.json.update((__ \ 'actif).json.put(JsNumber(1)))) andThen (__ \ 'admin).json.prune andThen (__ \ 'reviewer).json.prune).get
          collection.insert(userJson).map(_ => {
            Ok(Messages("registration.creationsuccess.message", json \ "id"))
          }
          ).recover {
            case e: DatabaseException => {
              e.code match {
                case Some(11000) => BadRequest(Messages("globals.emailexists.message"))
                case _ => BadRequest(Messages("globals.serverInternalError.message"))
              }
            }
          }

        }
      }.getOrElse(Future.successful(BadRequest(Messages("globals.serverInternalError.message"))))
    }
  }
  def register = Action.async {
    implicit request => {
      request.body.asJson.map {
        json => {
          val userJson = json.transform(generatActivationCode andThen (__ \ 'admin).json.prune andThen (__ \ 'reviewer).json.prune).get
          collection.insert(userJson).map(_ => {
            val messageBody = Messages("registration.email.body", (json \ "fname").as[String], (json \ "id").as[String], (userJson \ "activationCode").as[String])
            MailUtil.send((userJson \ "id").as[String], Messages("registration.email.subject"),
              messageBody,
              (userJson \ "fname").as[String])
            Ok(Messages("registration.creationsuccess.message", json \ "id"))
          }
          ).recover {
            case e: DatabaseException => {
              e.code match {
                case Some(11000) => BadRequest(Messages("globals.emailexists.message"))
                case _ => BadRequest(Messages("globals.serverInternalError.message"))
              }
            }
          }

        }
      }.getOrElse(Future.successful(BadRequest(Messages("globals.serverInternalError.message"))))
    }
  }

  def activateAccount(email: String, activationCode: String) = Action.async {
    implicit request => {
      collection.update(
        Json.obj(("id" -> email)
          , ("activationCode" -> activationCode)
        )
        , Json.obj("$set" -> Json.obj("actif" -> 1))
      )
        .map(lastError => {
        lastError.get("n").map {
          value =>
            val linesUpdated = value match {
              case linesUpdate: BSONInteger => linesUpdate.value
              case _ => 0;
            }
            if (linesUpdated > 0) {
              Redirect("/", 302)
            }
            else {
              Ok("Invalid activation code ! ")
            }
        }.get
      })
    }
  }

  def reviewerActivation(email: String, activationCode: String) = Action.async {
    implicit request => {
      val cursor: Cursor[JsObject] = collection.find(Json.obj(("id" -> email)
        , ("activationCode" -> activationCode))).cursor[JsObject]
      cursor.headOption.map(value => {
        value.map(content => {
          Redirect("/revProfile", 302).withSession(("user", content.toString()), ("rev", "true"))
        }).getOrElse(BadRequest(Messages("globals.serverInternalError.message")))
      })
    }
  }

  def createReviewer(email: String) = Action.async {
    implicit request => {
      val userJson = Json.obj(("id" -> email), ("reviewer" -> true)).transform(generatActivationCode).get
      collection.insert(userJson).map(_ => {
        val messageBody = Messages("reviewers.activation.email.body", email, (userJson \ "activationCode").as[String])
        MailUtil.send((userJson \ "id").as[String], Messages("reviewers.activation.email.subject"),
          messageBody,
          "")
        Ok(Messages("registration.creationsuccess.message", userJson \ "id"))
      }
      ).recover {
        case e: DatabaseException => {
          e.code match {
            case Some(11000) => BadRequest(Messages("globals.emailexists.message"))
            case _ => BadRequest(Messages("globals.serverInternalError.message"))
          }
        }
      }
    }
  }

  val bodyParser = BodyParser(rh => Iteratee.fold[Array[Byte], Array[Byte]](Array[Byte]())((c, a) => c ++ a).map(Right(_)))

  def upload = Action.async(parse.maxLength(maxLength = 1024000, bodyParser)) {
    rq => {
      rq.body match {
        case Right(multiPartBody) => {

          val fileName = "" + System.nanoTime()
          val url = WS.url(Play.current.configuration.getString("upload.server").get + "uploadPictures.php?name=" + fileName)
          rq.headers.keys.foldLeft[WSRequestHolder](url)((h, a) => h.withHeaders(a -> {
            rq.headers.get(a).get
          })).post(multiPartBody).map(r => {
            Ok(Json.obj("files" -> Json.arr(Json.obj("name" -> (Play.current.configuration.getString("upload.server").get + "cfp/" + fileName + ".png")))))
          })
        }
        case Left(multiPartBody) => Future.successful(BadRequest("Max size exceeded"))
        case _ => Future.successful(BadRequest("Other"))
      }
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


  def login = Action.async {
    implicit request => {
      request.body.asJson.map {
        json => {
          val cursor: Cursor[JsObject] = collection.find(Json.obj(("id" -> json \ "id"), ("password" -> json \ "password"), ("actif" -> 1))).cursor[JsObject]
          cursor.headOption.map(value => {
            value.map(content => {
              val sessionUser = content.transform((__ \ 'password).json.prune andThen (__ \ 'cpassword).json.prune
                andThen (__ \ 'activationCode).json.prune andThen (__ \ 'id).json.prune)
              Ok(sessionUser.get).withSession(("user", sessionUser.get.toString()))
            }).getOrElse(BadRequest(Messages("1globals.serverInternalError.message")))
          })
        }
      }.getOrElse(Future.successful(BadRequest(Messages("globals.serverInternalError.message"))))
    }
  }


  def speaker(email: String) = Action.async {
    val cursor: Cursor[JsObject] = collection.find(Json.obj(("id" -> email)),
      Json.obj(("fname" -> 1), ("lname" -> 1), ("image" -> 1), ("_id" -> 1), ("bio" -> 1), ("twitter" -> 1))).cursor[JsObject]
    cursor.headOption.map(value => {
      value.map(content => {
        val sessionUser = content.transform((__ \ 'id).json.prune)
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
          cursor.headOption.map {
            _.map {
              Ok(_)
            }
              .getOrElse(BadRequest(Messages("globals.serverInternalError.message")))
          }

        }
      }.getOrElse(Future.successful(BadRequest(Messages("globals.serverInternalError.message"))))

    }
  }

  def forgetPassword(username: String) = Action.async {
    implicit request => {
      var passwordString = CFPUtil.randomString()
      val newPassword = Json.obj(("$set" -> Json.obj(("password" -> passwordString))))
      collection.update(Json.obj(("id" -> username)), newPassword).map(_ => {
        MailUtil.send(username, Messages("fp.email.subject"),
          Messages("fp.email.body", passwordString),
          "")
        Ok(Messages("registration.save.message"))
      })
    }
  }

  def saveProfile = Action.async {
    implicit request => {
      request.body.asJson.flatMap {
        json => {
          session.get("user").map(connectedUser => {
            val newJson = json.transform((__ \ '_id).json.prune andThen (__ \ 'admin).json.prune andThen (__ \ 'reviewer).json.prune).get
            collection.update(Json.obj(("_id" -> Json.parse(connectedUser) \ "_id")), Json.obj("$set" -> newJson)).map(_ => Ok(Messages("registration.save.message")))
          })
        }
      }.getOrElse(Future.successful(BadRequest(Messages("globals.serverInternalError.message"))))
    }
  }

  def saveReviewer = ReviewAction.async {
    implicit request => {
      request.body.asJson.flatMap {
        json => {
          session.get("user").map(connectedUser => {
            val newJson = json.transform((__ \ 'admin).json.prune).get
            collection.update(Json.obj(("_id" -> Json.parse(connectedUser) \ "_id")), newJson).map(_ => Ok(Messages("registration.save.message")))
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

  def allTalks() = AdminAction.async {
    implicit request => {
      val query = Json.obj()
      talks.find(query).sort(Json.obj(("title" -> 1))).cursor[JsObject]
        .enumerate() |>>> Iteratee.foldM[JsObject, List[JsObject]](List[JsObject]())((theList, aTalk) => {

        val speakerIds = (aTalk \ "otherSpeakers") match {
          case _: JsUndefined => {
            List[JsValue](aTalk \ "speaker" \ "_id")
          }
          case jsValue => {
            jsValue.as[List[JsValue]]
              .foldLeft(List[JsValue](aTalk \ "speaker" \ "_id"))((l, e) => {
              e \ "id" match {
                case _: JsUndefined => {l}
                case speakerId => {
                  val ss=  speakerId.as[String]
                  val p = "[0-9A-F]+".r
                  ss match {
                    case p(c)=> {
                      if(ss.length == 24 )
                        l :+ Json.obj(("$oid" -> speakerId ))
                      else l
                    }
                    case _ => l
                  }

                }
              }

            })
          }
        }
        //println("speakerIds = "+ speakerIds)
        collection.find(Json.obj(("_id" -> Json.obj(("$in" -> speakerIds)))), Json.obj(("fname" -> 1),
          ("lname" -> 1),
          ("bio" -> 1),
          ("image" -> 1),
          ("twitter" -> 1))).cursor[JsObject].collect[List]().map(theSpeaker => {
          println("theSpeaker = "+theSpeaker)
          theList :+ aTalk.transform(__.json.update((__ \ 'speakers).json.put(JsArray(theSpeaker)))).get
        }).recover({case _=> println(" .......... ");theList})
      }).map(acceptedTalks => Ok(Json.toJson(Json.obj(("talks" -> acceptedTalks)))))
    }
  }

  def acceptedTalks() = Action.async {
    implicit request => {
        val query = Json.obj(("status" -> 3))
        talks.find(query).sort(Json.obj(("title" -> 1))).cursor[JsObject]
          .enumerate() |>>> Iteratee.foldM[JsObject, List[JsObject]](List[JsObject]())((theList, aTalk) => {

          val speakerIds = (aTalk \ "otherSpeakers") match {
            case _ :JsUndefined => {List[JsValue](aTalk\"speaker"\"_id")}
            case jsValue => { jsValue.as[List[JsValue]].foldLeft(List[JsValue](aTalk\"speaker"\"_id"))((l,e)=> { println(e);l :+ Json.obj(("$oid" -> e \ "id"))} ) }
          }
          println(s" ==============> speakerIds = $speakerIds")
          collection.find(Json.obj(("_id"  -> Json.obj(("$in" ->  speakerIds )))),Json.obj(("fname" -> 1),
            ("lname" -> 1),
            ("bio" -> 1),
            ("image" -> 1),
            ("twitter" -> 1))).cursor[JsObject].collect[List]().map( theSpeaker =>{
                theList :+ aTalk.transform(__.json.update((__ \ 'speakers).json.put(JsArray(theSpeaker)))).get
          })
        }).map( acceptedTalks => Ok(Json.toJson(Json.obj(("talks" -> acceptedTalks)))))
    }
  }

  def fixTalks() = AdminAction.async {
    implicit request => {
      val query = Json.obj(("status" -> 3))
      val res = talks.find(query).sort(Json.obj(("title" -> 1))).cursor[JsObject]
        .enumerate() |>>> Iteratee.fold[JsObject, List[JsObject]](List[JsObject]())((theList, aTalk) => {
        // an exception may happen here
        if (((aTalk \ "hex").as[String]).length == 24)
          theList :+ aTalk
        else
          theList
      }).map(l => {
        Ok(Json.toJson(Json.obj(("talks" -> l))))
      })
      res.recover({case _ => InternalServerError("Not a hex talk")})
    }
  }

  def fixTalksLangs() = AdminAction.async {
    implicit request => {
      val query = Json.obj(("status" -> 3))
      talks.find(query).sort(Json.obj(("title" -> 1))).cursor[JsObject]
        .enumerate() |>>> Iteratee.foldM[JsObject, List[JsObject]](List[JsObject]())((theList, aTalk) => {


        config.find(Json.obj()).cursor[JsObject].headOption.map( aconfigOption =>{
          println("aconfigOption = ")
          aconfigOption.map( aConfig => {

            val theLang = (aTalk \ "language" \ "value").as[String]
            val theType = (aTalk \ "type" \ "value").as[String]
            val theTrack = (aTalk \ "track" \ "value").as[String]
            val l = (aConfig \ "languages").as[List[JsObject]]

            l.foreach(el => {

              if((el\"value").as[String].equals(theLang)){
                talks.update(Json.obj(("_id" -> aTalk \ "_id")), Json.obj("$set" ->
                  aTalk.transform(__.json.update((__ \ 'language).json.put(el.transform((__ \ '$$hashKey).json.prune).get)) andThen (__ \ '_id).json.prune ).get
                )).map(lastError => println(lastError))
              }
              theList :+ aTalk

            })
            /*val trak = (aConfig \ "sessionTypes").as[List[JsObject]]
            trak.foreach(el => {
              if((el\"value").as[String].equals(theType)){
                talks.update(Json.obj(("_id" -> aTalk \ "_id")), Json.obj("$set" ->
                  aTalk.transform(__.json.update((__ \ 'type).json.put(el.transform((__ \ '$$hashKey).json.prune).get)) andThen (__ \ '_id).json.prune ).get
                )).map(lastError => println(lastError))
              }
              theList :+ aTalk
            })   */

            /*val trakss = (aConfig \ "tracks").as[List[JsObject]]
            trakss.foreach(el => {
              if((el\"value").as[String].equals(theTrack)){
                talks.update(Json.obj(("_id" -> aTalk \ "_id")), Json.obj("$set" ->
                  aTalk.transform(__.json.update((__ \ 'track).json.put(el.transform((__ \ '$$hashKey).json.prune).get)) andThen (__ \ '_id).json.prune ).get
                )).map(lastError => println(lastError))
              }
              theList :+ aTalk
            })  */


          }).getOrElse(theList :+ aTalk)

          theList :+ aTalk
        }).recover({case t => {println("hhaaaa lerrerrr "+t);List[JsObject]()}})
      }).map( acceptedTalks => Ok(Json.toJson(Json.obj(("talks" -> acceptedTalks)))))
    }
  }

  def acceptedSpeakers() = Action.async {
    implicit request => {
      val query = Json.obj(("accepted" -> true))
      val cursor: Cursor[JsObject] = collection.find(query,
        Json.obj(("fname" -> 1),
          ("lname" -> 1),
          ("bio" -> 1),
          ("image" -> 1),
          ("twitter" -> 1))).cursor[JsObject]

      cursor.enumerate().run(Iteratee.foldM[JsObject, List[JsObject]](List[JsObject]())((theList, aSpeaker) => {
        //println(aSpeaker)
        talks.find(Json.obj(("$or",Json.arr(Json.obj(("speaker._id" -> aSpeaker \ "_id"))
                                  ,Json.obj(("otherSpeakers.id" -> aSpeaker \ "_id" \ "$oid"))
                            )),
          ( ("status" -> 3 ))
                            )).cursor[JsObject]
          .collect[List]().map(myTalks => {
          val traks = myTalks.foldLeft[Set[JsValue]](Set[JsValue]())((aSet,aTalkx) => aSet + aTalkx \ "track" \ "value" )

          theList :+ aSpeaker.transform(__.json.update((__ \ 'talks).json.put(JsArray.apply(myTalks))) andThen
            __.json.update((__ \ 'tracks).json.put(JsArray(traks.toSeq))) ).get
        })
      })).map(
        persons =>
          Ok(Json.toJson(Json.obj(("speakers" -> persons))))
      )
    }
  }

  def resOk(message: String, data: JsValue) = {
    Ok(Json.obj("message" -> message) ++ Json.obj("data" -> data))
  }
  var openCFP:Boolean = false
  def close = AdminAction.async {
    openCFP = false;
    Future.successful(Ok("Closed"))
  }
  def open = AdminAction.async {
    openCFP = true;
    Future.successful(Ok("Opened"))
  }
  def editTalk = Action.async {
    implicit request => {
      request.body.asJson.flatMap {
        json => {
          session.get("user").map(user => {
            if(openCFP){
            val userJson = Json.parse(user)
            json \ "_id" match {
              // creation
              case _: JsUndefined => {

                val generateCreated = (__ \ 'created \ 'date).json.put(JsNumber((new java.util.Date).getTime))
                val addMongoIdAndDate: Reads[JsObject] = __.json.update((generateId and generateCreated).reduce)
                val res = json.transform(addMongoIdAndDate andThen (__ \ 'loading).json.prune).get
                talks.insert(res ++ Json.obj(("speaker" -> userJson.as[JsObject]))).map(lastError => {
                  resOk(Messages("talk.creationsuccess.message"), res)
                })
              }
              // edit
              case value: JsValue => {
                val query = Json.obj(("_id" -> value), ("speaker._id" -> userJson \ "_id"), ("status" -> 1))
                val generateUpdated = (__ \ 'updated \ 'date).json.put(JsNumber((new java.util.Date).getTime))
                val res = json.transform(__.json.update(generateUpdated) andThen (__ \ '_id).json.prune andThen ((__ \ '$$hashKey).json.prune).andThen((__ \ 'loading).json.prune).andThen((__ \ 'error).json.prune))
                talks.update(query, Json.obj(("$set" -> res.get))).map(lastError =>
                  Ok(Messages("talk.creationsuccess.message")))
              }

            }
          }
            else{
              Future.successful(BadRequest("Call for paper closed"))
            }
          })
        }
      }.getOrElse(Future.successful(BadRequest(Messages("globals.serverInternalError.message"))))
    }
  }

  def emailSpeaker(myJson: JsValue, id: JsValue) = {

    val cursor: Cursor[JsObject] = collection.find(Json.obj("_id" -> id)).cursor[JsObject]
    cursor.headOption.map(value => {
      value.map(content => {

        if ((myJson \ "status").as[Int] == 3) {

          MailUtil.send((content \ "id").as[String], Messages("talks.accepted.subject"),
            Messages("talks.accepted.body", (content \ "fname").as[String], (myJson \ "title").as[String]),
            (content \ "fname").as[String])
        }
        if ((myJson \ "status").as[Int] == 4) {
          MailUtil.send((content \ "id").as[String], Messages("talks.rejected.subject"),
            Messages("talks.rejected.body", (content \ "fname").as[String], (myJson \ "title").as[String]),
            (content \ "fname").as[String])
        }
      })
    })
  }

  def adminEditTalk(email:Boolean,acceptSpeaker:Boolean) = AdminAction.async {
    implicit request => {
      request.body.asJson.flatMap {
        myJson => {
          session.get("user").map(user => {
            val userJson = Json.parse(user)

            val query = Json.obj(("_id" -> myJson \ "_id"))
            val generateUpdated = (__ \ 'updated \ 'date).json.put(JsNumber((new java.util.Date).getTime))
            val generateUpdatedBy = (__ \ 'updated \ 'by).json.put(userJson \ "_id")
            val res = myJson.transform(__.json.update(generateUpdated) andThen __.json.update(generateUpdatedBy) andThen (__ \ '_id).json.prune
              andThen ((__ \ '$$hashKey).json.prune).andThen((__ \ 'loading).json.prune).andThen((__ \ 'error).json.prune))
            talks.update(query, Json.obj(("$set" -> res.get))).map(lastError => {
              if(email) emailSpeaker(myJson, res.get \ "speaker" \ "_id")
              if(acceptSpeaker)  collection.update(Json.obj("_id" -> res.get \ "speaker" \ "_id"), Json.obj("$set" -> Json.obj("accepted" -> true)))
              //println("==> " + (res.get \ "otherSpeakers"))
              (res.get \ "otherSpeakers") match {
                case otherS:JsUndefined =>{}
                case otherS:JsValue => {
              (res.get \ "otherSpeakers").as[List[JsObject]].map {
                os: JsObject => {
                  (os \ "id") match {
                    case v: JsUndefined => {}
                    case v: JsValue => {
                      if (!v.as[String].eq("")) {
                        collection.update(Json.obj("_id" -> Json.obj(("$oid" -> os \ "id"))), Json.obj("$set" -> Json.obj("accepted" -> true)))
                        if(email) emailSpeaker(myJson, Json.obj(("$oid" -> os \ "id")))
                      }
                    }
                  }
                }
              }    //--
              }
              }
              Ok(Messages("talk.creationsuccess.message"))
            })
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

  def saveConfig = AdminAction.async {
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
            resOk(Messages("registration.configSaved.message"), res))
        }
      }.getOrElse(Future.successful(BadRequest(Messages("globals.serverInternalError.message"))))
    }
  }
}