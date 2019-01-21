package de.kddc.mybench

import akka.actor.ActorSystem
import com.softwaremill.macwire._

object Application {
  def main(args: Array[String]): Unit = {
    implicit val actorSystem = ActorSystem("mybench")
    val service = wire[Service]
    service.start()
  }
}



trait User {
  val id: Long
}
case class AdminUser(id: Long) extends User
case class RegularUser(id: Long) extends User

object Foobar {



  val admins = new MyList[AdminUser]()
  val regularUsers = new MyList[RegularUser]()

  val users: MyList[User] = admins

  users.addItem(AdminUser(1))
}

class MyList[+A] {
  def addItem[B >: A](item: B): Unit = ???
  def elementAt(index: Int): A = ???
}