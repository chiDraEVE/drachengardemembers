package controllers

import models._

import javax.inject.Inject

import scala.concurrent.Future
import scala.concurrent.duration._
import play.api.mvc._
import play.api.libs.ws._
import play.api.libs.json._
import play.api.http.HttpEntity
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl._
import akka.util.ByteString

import java.util.Calendar
import java.text.SimpleDateFormat

import scala.concurrent.ExecutionContext
import play.api.libs.functional.syntax._

import scala.collection.mutable.ListBuffer
import anorm._
import play.api.db.DBApi

case class Member(
       id: Int,
       name: String,
       rank: String,
       rankIcon: String,
       avatar: String,
       feastMatches: Int,
       server: String
                 )

class DrachengardeController @Inject()
(ws: WSClient,
 val cc: ControllerComponents,
 implicit val ec: ExecutionContext,
 dbapi: DBApi,
 memberDAO: MemberDAO) extends AbstractController(cc) {

  val url = "https://xivapi.com/freecompany/9228438586435602713/?key=701939a9e82b4070bf4c722c&data=FCM&extended=1"

  private val db = dbapi.database("default")

  implicit val memberReads: Reads[Member] = (
    (__ \ "ID").read[Int] and
      (__ \ "Name").read[String] and
      (__ \ "Rank").read[String] and
      (__ \ "RankIcon").read[String] and
      (__ \ "Avatar").read[String] and
      (__ \ "FeastMatches").read[Int] and
      (__ \ "Server").read[String]
  )(Member.apply _)


  def ff14 = Action.async {
    ws.url(url).get().map {
      response => Ok(response.body)
    }
  }


  def members = Action.async {
    ws.url(url).get().map {
      response => {
        var members = (response.json \ "FreeCompanyMembers" \ "data").as[JsArray].value
//        var members = (response.json \ "FreeCompanyMembers" \ "data").validate[Member]
//        Ok((response.json \ "FreeCompanyMembers" \ "data").as[JsArray])
//        val member = members.last.validate[Member]
//        members.value
//        var output = ""
//        for (member <- members) {
//          output = output + "Mitglied: " + member.toString() + " | "
//        }
        var membersList = new ListBuffer[Member]
        members.foreach(member => {
          membersList += member.validate[Member].get}
        )
//        Ok(membersList.toString())
//        Ok(checkForMembers(membersList).toString())
        membersList = checkForMembers(membersList)
        Ok(views.html.members(membersList))
      }
    }
  }

  def member(id: Int) = Action.async {
    ws.url(url).get().map {
      response => {
        val members = (response.json \ "FreeCompanyMembers" \ "data").as[JsArray]
        Ok(members(id))
      }
    }
  }

  def getMembers() = Action{
    Ok(memberDAO.list.toString())
  }
  def getMember(id: Int) = Action{
    Ok(memberDAO.getMember(id).toString())
  }



  def checkForMembers(membersListFromREST: ListBuffer[Member]) = {
//    var out: String = ""

    def updateMember(memDB: FCMember, memRest: Member): Unit = {
      if ( memDB.name != memRest.name
          || memDB.rank != memRest.rank
          || memDB.rankIcon != memRest.rankIcon
          || memDB.avatar != memRest.avatar
          || memDB.server != memRest.server
          || memDB.feastMatches != memRest.feastMatches)
        memberDAO.update(newMember(memRest, memDB.beigetreten, memDB.ausgetreten, memDB.notizen))
    }

    def dismissMember(member: FCMember) = {
      member.ausgetreten = Calendar.getInstance().getTime.toString
      memberDAO.update(member)
    }

    def newMember(member: Member,
                  eintritt: String,
                  austritt: String,
                  notizen: String
                 ): FCMember = new FCMember(
      member.id,
      member.name,
      member.rank,
      member.rankIcon,
      member.avatar,
      member.feastMatches,
      member.server,
      eintritt,
      austritt,
      notizen
    )

    // temporary List of Members where items will be removed,
    // if the member exists both at REST and DB. So I get a list
    // of members, who are in the database, but not found over REST API
    var crossingMembersOut = new ListBuffer[Int]
    memberDAO.list.foreach( member =>
      crossingMembersOut += member.id
    )

    membersListFromREST.foreach(member =>
      memberDAO.getMember(member.id) match {
        case Some(mem) => {
          crossingMembersOut - mem.id
          updateMember(mem, member)
        }
        case None => {
//          out += member.name + " is a new Member | "
          val eintritt = Calendar.getInstance().getTime.toString
          memberDAO.create(newMember(member, eintritt, "NULL", "NULL"))
        }
      }
    )
    crossingMembersOut.foreach(_ => dismissMember(_))
    membersListFromREST
  }

  def writeBalduin = Action {
    db.withConnection { implicit connection =>
      SQL("INSERT INTO `members` " +
        "(`Avatar`, `FeastMatches`, `ID`, `Name`, `Rank`, `RankIcon`, `Server`, `Beigetreten`, `Ausgetreten`, `Notizen`) " +
        "VALUES ('https://img2.finalfantasyxiv.com/f/42dcb0d69435e30ee7a657d95cafc068_7206469080400ed57a5373d0a9c55c59fc0_96x96.jpg', " +
        "'0', '8628470', 'Balduin Drachengarde', 'Drachengeneral', " +
        "'https://img.finalfantasyxiv.com/lds/h/Z/W5a6yeRyN2eYiaV-AGU7mJKEhs.png', 'Lich', NULL, NULL, NULL)")
        .execute()
    }
    Ok("Balduin in Datenbank eingetragen")
  }

  def deleteBalduin = Action {
    db.withConnection { implicit connection =>
      SQL("DELETE FROM members WHERE name='Balduin Drachengarde'")
        .execute()
    }
    Ok("Balduin aus Datenbank gelöscht")
  }
}
