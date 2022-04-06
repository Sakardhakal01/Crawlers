package com.sakar.crawler

import java.io.{BufferedWriter, File, FileWriter}
import java.nio.file.Paths
import java.net.URL
import java.security.MessageDigest
import akka.actor.{Actor, Status, PoisonPill}
import scala.util.{Failure, Success}
import org.jsoup.Jsoup
import scala.collection.JavaConverters._
import scala.util.hashing.MurmurHash3


object Helper {
  case class Done() {}
  case class Stop() {}
}

class Helper(url: String, depth: Int) extends Actor {

  import Helper._
  implicit val ec = context.dispatcher

  val currHost = new URL(url).getHost
  WebClient.get(url) onComplete {
    case Success(body) => self ! body
    case Failure(err) => self ! Status.Failure(err)
  }

  def hash(s: String) = {
    MurmurHash3.stringHash(s)
  }

  def getFileName(url: String): String = {
    url.replaceFirst("^(http[s]?://www\\.|http[s]?://|www\\.)","")
      .replaceAll("[\\.\\/\\?]", "_")
      .takeRight(150)
  }


  def extractLinks(content: String): Iterator[String] = {
    Jsoup.parse(content, this.url).select("a[href]").iterator().asScala.map(_.absUrl("href"))
  }

  def saveAsFile(content: String): Unit = {
    val path = "./output/" + getFileName(this.url) + ".html"
    val file = new File(path)
    val bw = new BufferedWriter(new FileWriter(file))
    bw.write(content)
    bw.close()
  }

  def receive = {
    case body: String => {
      saveAsFile(body)
      extractLinks(body)
        .filter(link => link != null && link.length > 0)
        .filter(link => !link.contains("mailto"))
        .filter(link => currHost == new URL(link).getHost)
        .foreach(context.parent ! CheckLinks.CheckUrl(_, depth))
      stop
    }

    case _: Status.Failure => stop()

    case Stop => stop()
  }

  def stop(): Unit = {
    context.parent ! Done
    self ! PoisonPill
  }
}

