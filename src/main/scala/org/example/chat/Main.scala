package org.example.chat

import akka.actor._

/**
  * Initialization
  */
object Main {

  def main(args: Array[String]): Unit = {

    val system = ActorSystem("server")

    val rooms = system.actorOf(RoomSupervisor.props(), "rooms")

    val server = system.actorOf(Server.props("localhost", 1980), "front")

  }

}
