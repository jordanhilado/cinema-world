import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.{DefaultJsonProtocol, RootJsonFormat}

import scala.concurrent.ExecutionContextExecutor

case class Movie(title: String, year: Int)

// JSON (un)marshalling support
trait JsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val movieFormat: RootJsonFormat[Movie] = jsonFormat2(Movie)
}

object Main extends App with JsonSupport {
  implicit val system: ActorSystem = ActorSystem("my-system")
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  // In-memory list of movies
  var movies: List[Movie] = List(
    Movie("Inception", 2010),
    Movie("The Shawshank Redemption", 1994),
    Movie("The Godfather", 1972)
  )

  val route =
    path("health") {
      get {
        complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, "<h1>Everything is A-OK!</h1>"))
      }
    } ~
    path("movies") {
      get {
        complete(movies)
      }
    }

  val bindingFuture = Http().newServerAt("localhost", 8080).bind(route)

  println(s"Server online at http://localhost:8080/")

  // To stop the server, press Enter in the console
  scala.io.StdIn.readLine()
  bindingFuture
    .flatMap(_.unbind())
    .onComplete(_ => system.terminate())
}