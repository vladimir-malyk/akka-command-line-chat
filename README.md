# Command line Chat with Akka and Scala

## Agenda

Cluster sharded command-line chat demonstration.

http://www.slideshare.net/vladimirmalyk/reactive-programming-with-akka-and-scala

It's meant to be played with
* telnet localhost 1981
* telnet localhost 1982

## Implementation

* Two cluster actor systems, running on the same JVM.
* Two TCP Servers are listening for incoming connections (localhost:1981 and localhost:1981).
* Each TCP Server creates new Visitor per TCP connection.
* Visitor lifecycle (ask name and room) is implemented with context.become FSM pattern.
* Room lifecycle (initialization, broadcasting/subscribing/unsubscribing) is implemented with akka.actor.FSM pattern and immutable data structures.

## Tech

* Scala
* Akka

## TODO

* demonstrate Actors Cake Pattern