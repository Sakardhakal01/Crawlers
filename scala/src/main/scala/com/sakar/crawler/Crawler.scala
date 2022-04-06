package com.sakar.crawler

import akka.actor.{Actor, ActorRef, Props}
import Crawler.{CrawlReq, CrawlResp}
import CheckLinks.Result
import scala.collection.mutable
import akka.actor.{Actor, ActorRef, Props, ReceiveTimeout}
import Helper.Done
import CheckLinks.{CheckUrl, Result}
import scala.concurrent.duration.Duration
import java.util.concurrent.Executors
import com.ning.http.client.{AsyncCompletionHandler, AsyncHttpClient, AsyncHttpClientConfig, Response}
import scala.concurrent.{Future, Promise}

object Crawler {
  case class CrawlReq(url: String, depth: Integer) {}
  case class CrawlResp(url: String, links: Set[String]) {}
}

class Crawler extends Actor {
  val clients: mutable.Map[String, Set[ActorRef]] = collection.mutable.Map[String, Set[ActorRef]]()
  val controllers: mutable.Map[String, ActorRef] = mutable.Map[String, ActorRef]()

  def receive = {
    case CrawlReq(url, depth) =>
      val controller = controllers get url
      if (controller.isEmpty) {
        controllers += (url -> context.actorOf(Props[CheckLinks](new CheckLinks(url, depth))))
        clients += (url -> Set.empty[ActorRef])
      }
      clients(url) += sender

    case Result(url, links) =>
      context.stop(controllers(url))
      clients(url) foreach (_ ! CrawlResp(url, links))
      clients -= url
      controllers -= url
  }
}


object CheckLinks {
  case class CheckUrl(url: String, depth: Int){}
  case class Result(url: String, links: Set[String]) {}
}

class CheckLinks(root: String, originalDepth: Integer) extends Actor {
  var cache = Set.empty[String]
  var children = Set.empty[ActorRef]

  self ! CheckUrl(root, originalDepth)
  context.setReceiveTimeout(Duration.create("10 second"))

  def receive = {
    case CheckUrl(url, depth) =>
      if (!cache(url) && depth >= 0)
        children += context.actorOf(Props[Helper](new Helper(url, depth-1)))
      cache += url

    case Done =>
      children -= sender
      if (children.isEmpty) context.parent ! Result(root, cache)

    case ReceiveTimeout => children foreach (_ ! Helper.Stop)

  }
}


object WebClient {
  val config = new AsyncHttpClientConfig.Builder()
  val client = new AsyncHttpClient(config
    .setFollowRedirect(true)
    .setExecutorService(Executors.newWorkStealingPool(64))
    .build())

  def get(url: String): Future[String] = {
    val promise = Promise[String]()
    val request = client.prepareGet(url).build()
    client.executeRequest(request, new AsyncCompletionHandler[Response]() {
      override def onCompleted(response: Response): Response = {
        promise.success(response.getResponseBody)
        response
      }
      override def onThrowable(t: Throwable): Unit = {
        promise.failure(t)
      }
    })
    promise.future
  }
}


