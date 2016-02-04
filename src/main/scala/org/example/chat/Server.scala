package org.example.chat

import java.net.InetSocketAddress

import akka.actor.{Actor, ActorLogging, Props}
import akka.io.Tcp._
import akka.io._

/**
  * Main TCP socket server
  */
class Server(address: String, port: Int) extends Actor with ActorLogging {

  override def preStart() {
    log.info("Starting org.example.chat.Server")

    import context.system
    val opts = List(SO.KeepAlive(on = true), SO.TcpNoDelay(on = true))
    IO(Tcp) ! Bind(self, new InetSocketAddress(address, port), options = opts)
  }

  def receive = {

    case b@Bound(localAddress) =>
      log.info("org.example.chat.Server bound at " + localAddress)

    case CommandFailed(_: Bind) =>
      log.info("Command failed org.example.chat.Server")
      context stop self

    case c@Connected(remote, local) =>
      log.info("New incoming connection on org.example.chat.Server")
      val visitor = context.actorOf(Visitor.props(sender))
      visitor ! Visitor.Message.Greeting
      sender ! Register(visitor)

  }

}

object Server {

  def props(address: String, port: Int) = Props(new Server(address, port))

}