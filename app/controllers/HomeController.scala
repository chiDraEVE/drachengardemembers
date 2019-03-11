package controllers

import javax.inject._
import play.api.libs.json.{JsObject, Json, Writes}
import play.api.libs.ws._
import play.api.mvc._

import scala.collection.mutable.ListBuffer
import scala.concurrent.{ExecutionContext, Future}

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class HomeController @Inject()(ws: WSClient, val cc: ControllerComponents, implicit val ec: ExecutionContext) extends AbstractController(cc) {

  /**
   * Create an Action to render an HTML page with a welcome message.
   * The configuration in the `routes` file means that this method
   * will be called when the application receives a `GET` request with
   * a path of `/`.
   */
  def index = Action {
    Ok(views.html.index("Your new application is ready."))
  }

  case class Member(id: Int, name: String)

  implicit val writesMember = Writes[Member] {
    case Member(id, name) =>
      Json.obj(
        "id" -> id,
        "name" -> name
      )
  }

  def ff14 = Action{
    val url = "https://xivapi.com/freecompany/9228438586435602713/?key=701939a9e82b4070bf4c722c&data=FCM&extended=1"
    val res = ws.url(url)
    val response = res.withHttpHeaders("Content-Type" -> "application/json").get()
    implicit val memberReads = Json.reads[Member]

    val result = response.map { r =>
      (r.json \ "FreeCompanyMember" \ "data").validate[Member]
    }

    Ok("ff14")
  }

}
