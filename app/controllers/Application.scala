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
    Akka.system.scheduler.schedule(0 milliseconds, 200 milliseconds, actor, PushUpdates())
    actor
  }
  
  def index = Action {
    Ok(views.html.index("Your new application is ready."))
  }
  
  def websocket = WebSocket.using[JsValue] { request =>
    
    val cid = java.util.UUID.randomUUID().toString()
    val out = Enumerator.imperative[JsValue]()
    
    val in = Iteratee.foreach[JsValue] { e => (e \ "op") match {
        case JsString("join") => cursorify ! Join(cid, e \ "filter", e \ "handle", out)
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
  import play.api.libs.json.Writes
  
  var clients = Map[String, Client]()
  
  def receive = {
    
    case Join(cid, filter, handle, out) =>
      if (!clients.contains(cid)) {
    	  println("Cursorify: Join: "+handle.as[String]+"/"+filter.as[String]+" ("+cid+")")
	      clients = clients + (cid -> new Client(handle.as[String], filter.as[String], out))
	      out.push(Map("op" -> "joined", "cid" -> cid))
      }
  
    case Publish(cid, value) =>
      println("Cursorify: Publish: "+value)
      clients.get(cid).map(_.pub = value.as[Boolean])
      
    case Subscribe(cid, value) =>
      println("Cursorify: Subscribe: "+value)
      clients.get(cid).map(_.sub = value.as[Boolean])
      
    case Update(cid, trail) =>
      clients.get(cid).map(c => c.trail = c.trail ++ trail.as[Seq[JsValue]])
      
    case Quit(cid) =>
      clients.get(cid).map { client =>
        println("Cursorify: Quit: "+client.handle+" ("+cid+")")
        clients = clients - cid
        for (client <- findClients(client.filter.as[String]).values)
          client.out.push(Map("op" -> "quit", "cid" -> cid))
      }
  
    case PushUpdates() =>
      for (filter <- findFilters) {
        val clients = findClients(filter)
        val updates = clients.foldLeft(Seq[Map[String, JsValue]]()) {
	        case (seq, (cid, client)) if client.pub =>
	          seq :+ Map(
	            "cid" -> JsString(cid),
	            "handle" -> JsString(client.handle),
	            "trail" -> toJson(client.trail))
	      }
	      
	      val message = Map(
	          "op" -> JsString("updates"),
	          "updates" -> toJson(updates)) 
	      
	      for (client <- clients.values if client.sub)
	        client.out.push(message)
	      
	      for (client <- clients.values)
	        client.trail = Seq()
      }
  }
  
  private def findFilters() = {
    clients.groupBy { case (cid, client) => client.filter.as[String] }.keys
  }
  
  private def findClients(filter: String) = {
    clients.filter { case (cid, client) => client.filter.as[String].equals(filter) }
  }
  
  private implicit def toJson[T: Writes](t: T): JsValue = Json.toJson(t)
}

class Client(val handle: String,
    val filter: String,
    val out: PushEnumerator[JsValue],
    var pub: Boolean = true,
    var sub: Boolean = true,
    var trail: Seq[JsValue] = Seq())

case class Join(cid: String, filter: JsValue, handle: JsValue, out: PushEnumerator[JsValue])
case class Publish(cid: String, value: JsValue)
case class Subscribe(cid: String, value: JsValue)
case class Update(cid: String, trail: JsValue)
case class Quit(cid: String)
case class PushUpdates()
