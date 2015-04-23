package com.textocat.example.client

import com.mashape.unirest.http.Unirest
import com.mashape.unirest.request.{HttpRequest, HttpRequestWithBody}
import org.json.JSONArray

import scala.annotation.tailrec

/**
 * Textocat Entity Recognition Example in Scala
 */
object TextocatConsoleScalaClient {
  val authToken = "<YOUR_TOKEN_HERE>"
  val requestUrl = "http://api.textocat.com/api/entity/"

  sealed trait Command {
    def method: String => HttpRequest
  }

  case object QUEUE extends Command {
    override def method = Unirest.post
    override def toString = "queue"
  }

  case object REQUEST extends Command {
    override def method = Unirest.get
    override def toString = "request"
  }

  case object RETRIEVE extends Command {
    override def method = Unirest.get
    override def toString = "retrieve"
  }

  def prebuild(command: Command): HttpRequest = command.method(requestUrl + command)
    .queryString("auth_token", authToken)
    .header("Content-Type", "application/json")
    .header("Accept", "application/json")

  def queue(text: String) = {
    val inputDocument = "[{\"text\": \"" + text + "\"}]"
    val response = (prebuild(QUEUE).asInstanceOf[HttpRequestWithBody]).body(inputDocument).asJson
    response.getBody.getObject.getString("batchId")
  }

  @tailrec
  def waitUntilCompleted(batchId: String): Unit = {
    Thread.sleep(1000);
    val response = prebuild(REQUEST).queryString("batch_id", batchId).asJson
    if (response.getBody.getObject.getString("status") != "FINISHED") waitUntilCompleted(batchId)
  }

  def retrieve(batchId: String): JSONArray = {
    val response = prebuild(RETRIEVE).queryString("batch_id", batchId).asJson
    response.getBody.getObject.getJSONArray("documents")
  }

  def main(args: Array[String]) {
    val text = "Председатель совета директоров ОАО «МДМ Банк» Олег Вьюгин — о том," +
      " чему приведет обмен санкциями между Россией и Западом в следующем году. Беседовала Светлана Сухова."
    val batchId = queue(text)
    waitUntilCompleted(batchId)
    val documents = retrieve(batchId)
    // output processed documents
    for (i <- 0 until documents.length) {
      val entities = documents.getJSONObject(i).getJSONArray("entities")
      for (j <- 0 until entities.length) {
        val entity = entities.getJSONObject(j)
        System.out.println(entity.getString("span") + ":" + entity.getString("category"))
      }
    }
  }
}
