package models


import anorm._
import anorm.SqlParser._
import javax.inject.{Inject, Singleton}
import play.api.db.DBApi

case class FCMember(
     id: Int,
     name: String,
     rank: String,
     rankIcon: String,
     avatar: String,
     feastMatches: Int,
     server: String,
     var beigetreten: String,
     var ausgetreten: String,
     var notizen: String
     )

@Singleton
class MemberDAO @Inject() (dbapi: DBApi){

  private val db = dbapi.database("default")

  private var memberList = List()

  val memberParser = int("ID") ~
    str("Name") ~
    str("Rank") ~
    str("RankIcon") ~
    str("Avatar") ~
    int("FeastMatches") ~
    str("Server") ~
    str("Beigetreten") ~
    str("Ausgetreten") ~
    str("Notizen") map {
    case id ~ name ~ rank ~ ricon ~ avatar ~ feast ~ server
        ~ beitritt ~ austritt ~ notizen
    =>  FCMember(
          id, name, rank, ricon, avatar,
          feast, server, beitritt, austritt, notizen
        )
  }

  def sqlMemberOn(query: String, member: FCMember) = {
    db.withConnection { implicit c =>
      SQL(query).on(
        'id -> member.id,
        'name -> member.name,
        'rank -> member.rank,
        'ricon -> member.rankIcon,
        'avatar -> member.avatar,
        'feast -> member.feastMatches,
        'server -> member.server,
        'beitritt -> member.beigetreten,
        'austritt -> member.ausgetreten,
        'notizen -> member.notizen
      ).execute()
    }
  }

  def list = {
    val sqlQuery = "SELECT * FROM members"
    db.withConnection { implicit  c =>
      SQL(sqlQuery).executeQuery().as(memberParser *)
    }
  }

  def getMember(id: Int) = {
    val sqlQuery = "SELECT * FROM members WHERE ID={id}"
    db.withConnection { implicit c =>
      SQL(sqlQuery).on('id -> id).executeQuery().as(memberParser.singleOpt)
    }
  }

  def create(member: FCMember) = {
    val sqlQuery = """
          INSERT INTO members (Avatar, FeastMatches, ID, Name, Rank, RankIcon, Server, Beigetreten, Ausgetreten, Notizen)
          VALUES
          ({avatar},{feast}, {id}, {name}, {rank}, {ricon}, {server},
          {beitritt}, {austritt}, {notizen})
       """
    sqlMemberOn(sqlQuery, member)

  }

  def remove(id: Int) = {
    val sqlQuery = "DELETE FROM members where ID={id}"
    db.withConnection { implicit  c =>
      SQL(sqlQuery).on('id -> id).execute()
    }
  }

  def update(member: FCMember) = {
    val sqlQuery =
      """
        UPDATE members SET
        Avatar={avatar},
        FeastMatches={feast},
        Name={name},
        Rank={rank},
        RankIcon={ricon},
        Server={server},
        Beigetreten={beitritt},
        Ausgetreten={austritt},
        Notizen={notizen}
        WHERE ID={id}
      """
    sqlMemberOn(sqlQuery, member)
  }

}
