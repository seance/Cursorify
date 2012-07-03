package controllers

import play.api.libs.iteratee.Enumerator
import play.api.libs.iteratee.Iteratee
import play.api.libs.concurrent.Akka
import play.api.libs.json.JsBoolean
import play.api.libs.json.JsString
import play.api.libs.json.JsValue
import play.api.libs.json.JsArray
import play.api.libs.iteratee.PushEnumerator
import play.api.mvc._
import play.api._
import akka.actor.Props
import akka.actor.Actor
import akka.util.duration._

object Application extends Controller {
  
  import Play.current
  
  lazy val cursorify = {
    val actor = Akka.system.actorOf(Props[Cursorify])
    Akka.system.scheduler.schedule(1 second, 1 second, actor, PushUpdates())
    actor
  }
  
  def index = Action {
    Ok(views.html.index("Your new application is ready."))
  }
  
  def websocket = WebSocket.using[JsValue] { request =>
    
    val cid = java.util.UUID.randomUUID().toString()
    val out = Enumerator.imperative[JsValue]()
    
    val in = Iteratee.foreach[JsValue] { e => (e \ "op") match {
        case JsString("join") => cursorify ! Join(cid, e \ "handle", out)
        case JsString("publish") => cursorify ! Publish(cid, e \ "value")
        case JsString("subscribe") => cursorify ! Subscribe(cid, e \ "value")
        case JsString("update") => cursorify ! Update(cid, e \ "trail")
        case JsString("quit") => cursorify ! Quit(cid)
        case _ => cursorify ! Quit(cid)
      }
    }.mapDone( _ => cursorify ! Quit(cid))

    (in, out)
  }
}

class Cursorify extends Actor {
  
  import play.api.libs.json.Json
  import play.api.libs.json.Reads
  
  var clients = Map[String, Client]()
  
  def receive = {
    
    case Join(cid, handle, out) =>
      clients = clients + (cid -> new Client(handle, out, true, true, JsArray()))
      
    case Publish(cid, value) =>
      clients.get(cid).map(_.pub = value.as[Boolean])
      
    case Subscribe(cid, value) =>
      clients.get(cid).map(_.sub = value.as[Boolean])
      
    case Update(cid, trail) =>
      clients.get(cid).map(_.trail = trail)
      
    case Quit(cid) =>
      clients = clients - cid
      
    case PushUpdates() =>
      val data = clients.foldLeft(Seq[Map[String, JsValue]]()) { case (seq, (cid, client)) => 
        seq :+ Map[String, JsValue]("cid" -> JsString(cid), "trail" -> client.trail)
      }
      
      for ((cid, client) <- clients if client.sub) {
        client.out.push(Json.toJson(data))
      }
  }
}

class Client(val handle: JsValue,
    val out: PushEnumerator[JsValue],
    var pub: Boolean,
    var sub: Boolean,
    var trail: JsValue)

case class Join(cid: String, handle: JsValue, out: PushEnumerator[JsValue])
case class Publish(cid: String, value: JsValue)
case class Subscribe(cid: String, value: JsValue)
case class Update(cid: String, trail: JsValue)
case class Quit(cid: String)
case class PushUpdates()
