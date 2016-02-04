package org.example.chat

import akka.actor._

import scala.collection.immutable

/**
  * Chat room
  * Demonstrates akka.actor.FSM solution
  */
class Room extends FSM[Room.State, Room.Data] with ActorLogging {

  import Room._

  /**
    * Initial state and data
    */
  startWith(State.Initial, Data.Initial)

  /**
    * State "Initial" handler
    * @return
    */
  when(State.Initial) {

    /**
      * Setup Room id.
      * Subscribe Visitor.
      */
    case Event(msg@Command.Subscribe(_, _, _), _) =>
      msg.sender ! Visitor.Message.Out(s"ROOM[${msg.id}]> Welcome, ${msg.name}!")
      goto(State.Active) using Data.Active(msg.id, immutable.HashMap(msg.sender -> msg.name))

  }

  /**
    * State "Active" handler
    * @return
    */
  when(State.Active) {

    /**
      * Subscribe new Visitor and notify subscribed Visitors.
      */
    case Event(msg@Command.Subscribe(_, _, _), stateData: Data.Active) =>
      for ((visitor: ActorRef, name: String) <- stateData.visitors) {
        visitor ! Visitor.Message.Out(s"ROOM[${stateData.id}] ${msg.name} joined room.")
      }
      msg.sender ! Visitor.Message.Out(s"ROOM[${stateData.id}] Welcome, ${msg.name}!")
      stay using stateData.copy(
        visitors = stateData.visitors + (msg.sender -> msg.name)
      )

    /**
      * Broadcast received message.
      */
    case Event(msg@Command.Message(_, _, message), stateData: Data.Active) =>
      stateData.visitors.get(msg.sender) match {
        case Some(senderName) =>
          for ((visitor, name) <- stateData.visitors if visitor != msg.sender) {
            visitor ! Visitor.Message.Out(s"[$senderName] $message")
          }
        case None =>
      }
      stay

    /**
      * Unsubscribe Visitor and notify subscribed Visitors.
      */
    case Event(msg@Command.Leave(_, _), stateData: Data.Active) =>
      stateData.visitors.get(msg.sender) match {
        case Some(senderName) =>
          for ((visitor, name) <- stateData.visitors if visitor != msg.sender) {
            visitor ! Visitor.Message.Out(s"ROOM[${stateData.id}] Visitor $senderName left room.")
          }
        case None =>
      }
      stay using stateData.copy(
        visitors = stateData.visitors - sender
      )

  }

  /**
    * Default handler
    */
  whenUnhandled {
    case Event(e, s) =>
      log.warning("received unhandled request {} in state {}/{}", e, stateName, s)
      stay
  }

  initialize()

}

object Room {

  def props() = Props(new Room())

  /**
    * Extract destination Room id from message
    */
  val idExtractor: PartialFunction[Any, String] = {
    case msg: Command => msg.id
  }

  /**
    * Subscribed Visitors hashmap
    * Visitor ref -> Visitor name
    */
  type Visitors = immutable.HashMap[ActorRef, String]

  /**
    * Room commands
    */
  sealed trait Command {

    /**
      * Sender ref
      * TODO substitute sender when RoomSupervisor proxying
      * @return
      */
    def sender: ActorRef

    /**
      * Destination Room Id
      * @return
      */
    def id: String
  }

  object Command {

    /**
      * Subscribe
      *
      * @param sender Visitor ref
      * @param id Room id
      * @param name Visitor name
      */
    final case class Subscribe(sender: ActorRef, id: String, name: String) extends Command

    /**
      * Leave Room
      *
      * @param sender Visitor ref
      * @param id Room id
      */
    final case class Leave(sender: ActorRef, id: String) extends Command

    /**
      * Chat Message
      *
      * @param sender Visitor ref
      * @param id Room id
      * @param message Chat message
      */
    final case class Message(sender: ActorRef, id: String, message: String) extends Command

  }

  /**
    * FSM Data
    */
  sealed trait Data

  object Data {

    /**
      * Initial State
      */
    case object Initial extends Data

    /**
      * Active State
      *
      * @param id Room Id
      * @param visitors Subscribed visitors
      */
    final case class Active(
                             id: String,
                             visitors: Visitors
                           ) extends Data

  }

  /**
    * FSM States
    */
  sealed trait State

  object State {

    /**
      * Initial
      */
    case object Initial extends State

    /**
      * Active
      */
    case object Active extends State

  }

}