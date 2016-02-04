package org.example.chat

import akka.actor._

import scala.collection.mutable

/**
  * Room Supervisor
  *
  * Manages Rooms lifecycle and message delivery
  */
class RoomSupervisor extends Actor with ActorLogging {

  /**
    * Supervised rooms.
    * Room id -> Room actor ref
    */
  val rooms: mutable.HashMap[String, ActorRef] = mutable.HashMap.empty[String, ActorRef]

  override def receive = {
    /**
      * Create new Room if not exists, route message
      */
    case msg: Room.Command.Subscribe =>
      rooms.get(msg.id) match {
        case Some(room) =>
          room ! msg
        case None =>
          val room = context.actorOf(Room.props())
          rooms.put(msg.id, room)
          room ! msg
      }

    /**
      * Route message if Room exists
      */
    case msg =>
      val id = Room.idExtractor(msg)
      rooms.get(id) match {
        case Some(room) =>
          room ! msg
        case None =>
      }
  }

}

object RoomSupervisor {

  def props() = Props(new RoomSupervisor)

}