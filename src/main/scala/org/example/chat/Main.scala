package org.example.chat

import akka.actor.{ActorIdentity, ActorPath, ActorSystem, Identify, Props}
import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings}
import akka.pattern.ask
import akka.persistence.journal.leveldb.{SharedLeveldbJournal, SharedLeveldbStore}
import akka.util.Timeout
import com.typesafe.config.ConfigFactory

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

/**
  * Initialization
  */
object Main {

  def main(args: Array[String]): Unit = {

    /**
      * Run cluster nodes
      * See application.conf akka.cluster.seed-nodes for proper ports
      *
      * Front uses fancy 1981 and 1982 ports, see Front Service section
      *
      * Based on https://github.com/typesafehub/activator-akka-cluster-sharding-scala#master
      */
    runNodes(Seq("2551", "2552"))

  }

  /**
    * Run shards
    *
    * @param ports Ports
    */
  def runNodes(ports: Seq[String]): Unit = {

    ports foreach { port =>

      // Override the configuration of the port
      val config = ConfigFactory.parseString("akka.remote.netty.tcp.port=" + port).
        withFallback(ConfigFactory.load())

      // Create an Akka system
      val system = ActorSystem("ClusterSystem", config)

      // Where to start shared journal
      // Start the shared journal one one node (don't crash this SPOF)
      // This will not be needed with a distributed journal
      if (port == "2551")
        system.actorOf(Props[SharedLeveldbStore], "store")

      /**
        * Attach shared journal
        */
      attachSharedJournal(
        system,
        path = ActorPath.fromString("akka.tcp://ClusterSystem@127.0.0.1:2551/user/store")
      )

      /**
        * Room shard region
        */
      val roomRegion = ClusterSharding(system).start(
        typeName = Room.shardName,
        entityProps = Room.props(),
        settings = ClusterShardingSettings(system),
        extractEntityId = Room.idExtractor,
        extractShardId = Room.shardResolver)

      /**
        * Front Service with fancy year-like tcp-port
        */
      val frontPort = port.toInt - 2550 + 1980
      val server = system.actorOf(Server.props("localhost", frontPort), "front")

    }

  }

  /**
    * Attach sharding coordinator state journal
    *
    * Distributed journal is required as storage for the state of the sharding coordinator
    *
    * @param system Actor system
    * @param path Distributed journal path
    */
  def attachSharedJournal(system: ActorSystem, path: ActorPath): Unit = {

    // register the shared journal
    implicit val timeout = Timeout(15.seconds)
    val f = system.actorSelection(path) ? Identify(None)
    f.onSuccess {
      case ActorIdentity(_, Some(ref)) => SharedLeveldbJournal.setStore(ref, system)
      case _ =>
        system.log.error("Shared journal not started at {}", path)
        system.terminate()
    }
    f.onFailure {
      case _ =>
        system.log.error("Lookup of shared journal at {} timed out", path)
        system.terminate()
    }

  }

}
