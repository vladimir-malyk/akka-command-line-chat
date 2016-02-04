# Command line Chat with Akka and Scala

## Agenda

Simple command-line chat demonstration.
It's meant to be played with telnet localhost 7777

## Implementation

* TCP Server creates new Visitor per TCP connection.
* Visitor lifecycle (ask name and room) implemented with context.become FSM pattern.
* Room lifecycle (initialization, broadcasting/subscribing/unsubscribing) implemented with akka.actor.FSM pattern and immutable datastructures.

## Tech

* Scala
* Akka

## TODO

* Shard Room Actors with akka-cluster-sharding
* demonstrate Actors Cake Pattern