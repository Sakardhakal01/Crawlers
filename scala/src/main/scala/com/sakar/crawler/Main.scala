package com.sakar.crawler

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import Crawler.{CrawlReq, CrawlResp}

object Main extends App {
  println(s"Current Time ${System.currentTimeMillis}")
  if (args.length > 2) {
    println("Not enough command line arguments. Enter input file name and nesting level")
    System.exit(0)
  }
  val urls = io.Source.fromFile(args(0)).getLines().toArray
  val depth = args(1).toInt
  val actors = ActorSystem("Crawler")
  val receiver = actors.actorOf(Props[Crawler], "Crawler")
  val main = actors.actorOf(Props[Main](
    new Main(receiver, urls, depth)), "MainActor")
}

class Main(receiver: ActorRef, urls: Array[String], depth: Integer) extends Actor {
  urls foreach(receiver ! CrawlReq(_, depth))
  def receive = {
    case CrawlResp(url, links) =>
      println(s"To Crawl: $url")
      println(s"ExtractedLinks: ${links.toList.sortWith(_.length < _.length).mkString("\n")}")
      context.stop(receiver)
      context.stop(self)
  }
}

