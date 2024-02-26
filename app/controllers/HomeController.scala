package controllers

import javax.inject._
import play.api.mvc._
import io.circe.parser._
import io.circe.Decoder
import play.api.libs.json._
import scala.io.Source.fromFile
import slick.jdbc.H2Profile.api._
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.libs.functional.syntax._
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.concurrent.Future

case class Movie(
    movie_id: Int,
    title: String,
    genre: String,
    runtime_minutes: Int,
    release_date: String
)
case class Showtime(
    showtime_id: Int,
    movie_id: Int,
    date: String,
    time: String,
    capacity: Int,
    reservations: Int
)
case class MovieDetails(
    movie_id: Int,
    title: String,
    genre: String,
    runtime_minutes: Int,
    release_date: String,
    showtimes: List[Showtime]
)
case class Reservation(
    reservation_id: String,
    showtime_id: Int,
    name: String,
    email: String,
    seats: Int,
    timestamp: String
)

@Singleton
class HomeController @Inject() (cc: ControllerComponents)(implicit
    assetsFinder: AssetsFinder
) extends AbstractController(cc) {

  /*
  * Load MOVIES_DATA.json content and parse it into a List[Movie]
  */
  val moviesContent: String = fromFile("public/data/MOVIES_DATA.json").mkString
  val moviesJSON: JsValue = Json.parse(moviesContent)
  implicit val movieDecoder: Decoder[Movie] = Decoder.forProduct5(
    "movie_id",
    "title",
    "genre",
    "runtime_minutes",
    "release_date"
  )(Movie.apply)
  val moviesData: List[Movie] = decode[List[Movie]](moviesContent) match {
    case Right(value) => value
    case Left(error) =>
      throw new Exception(s"Error parsing Movies JSON: $error")
  }
  // println("moviesData:", moviesData)

  /*
  * Load SHOWTIMES_DATA.json content and parse it into a List[Showtime]
  */
  val showtimesContent: String = fromFile(
    "public/data/SHOWTIMES_DATA.json"
  ).mkString
  val showtimesJSON: JsValue = Json.parse(showtimesContent)
  implicit val showtimeDecoder: Decoder[Showtime] = Decoder.forProduct6(
    "showtime_id",
    "movie_id",
    "date",
    "time",
    "capacity",
    "reservations"
  )(Showtime.apply)
  val showtimesData: List[Showtime] =
    decode[List[Showtime]](showtimesContent) match {
      case Right(value) => value
      case Left(error) =>
        throw new Exception(s"Error parsing Showtimes JSON: $error")
    }
  // println("showtimesData:", showtimesData)

  /*
  * Define JSON Writes for Reservation type
  */
  implicit val reservationWrites: Writes[Reservation] = (
    (JsPath \ "reservation_id").write[String] and
      (JsPath \ "showtime_id").write[Int] and
      (JsPath \ "name").write[String] and
      (JsPath \ "email").write[String] and
      (JsPath \ "seats").write[Int] and
      (JsPath \ "timestamp").write[String]
  )(unlift(Reservation.unapply))

  /*
  * Movies table definition
  * movie_id: A unique identifier for the movie
  * title: The title of the movie
  * genre: The genre of the movie
  * runtime_minutes: The runtime of the movie in minutes
  * release_date: The release date of the movie in "YYYY-MM-DD" format
  */
  class Movies(tag: Tag)
      extends Table[(Int, String, String, Int, String)](tag, "movies") {
    def movie_id = column[Int]("movie_id", O.PrimaryKey)
    def title = column[String]("title")
    def genre = column[String]("genre")
    def runtime_minutes = column[Int]("runtime_minutes")
    def release_date = column[String]("release_date")
    def * = (movie_id, title, genre, runtime_minutes, release_date)
  }
  val movies_table = TableQuery[Movies]

  /*
  * Showtimes table definition
  * showtime_id: A unique identifier for the showtime
  * movie_id: A foreign key to the movie_id in the movies table
  * date: The date of the showtime in "YYYY-MM-DD" format
  * time: The time of the showtime in "HH:MM" format
  * capacity: The total number of seats available for the showtime
  * reservations: The total number of seats reserved for the showtime
  */
  class Showtimes(tag: Tag)
      extends Table[(Int, Int, String, String, Int, Int)](tag, "showtimes") {
    def showtime_id = column[Int]("showtime_id", O.PrimaryKey)
    def movie_id = column[Int]("movie_id")
    def date = column[String]("date")
    def time = column[String]("time")
    def capacity = column[Int]("capacity")
    def reservations = column[Int]("reservations")
    def movie_fk = foreignKey("movie_fk", movie_id, movies_table)(_.movie_id)
    def * = (showtime_id, movie_id, date, time, capacity, reservations)
  }
  val showtimes_table = TableQuery[Showtimes]

  /*
  * Reservations table definition
  * reservation_id: A unique identifier for the reservation
  * showtime_id: A foreign key to the showtime_id in the showtimes table
  * name: The name of the person making the reservation
  * email: The email of the person making the reservation
  * seats: The number of seats reserved
  * timestamp: The timestamp of the reservation in "YYYY-MM-DDTHH:MM:SS" format
  */
  class Reservations(tag: Tag)
      extends Table[(String, Int, String, String, Int, String)](
        tag,
        "reservations"
      ) {
    def reservation_id = column[String]("reservation_id", O.PrimaryKey)
    def showtime_id = column[Int]("showtime_id")
    def name = column[String]("name")
    def email = column[String]("email")
    def seats = column[Int]("seats")
    def timestamp = column[String]("timestamp")
    def showtime_fk =
      foreignKey("showtime_fk", showtime_id, showtimes_table)(_.showtime_id)
    def * = (reservation_id, showtime_id, name, email, seats, timestamp)
  }
  val reservations_table = TableQuery[Reservations]

  val db = Database.forConfig("cinemaWorldDB")

  /* 
  * Main homepage
  */
  def index = Action {
    Ok(views.html.index("Your new application is ready."))
  }

  /* 
  * Initialize the movies, showtimes, and reservations tables
  */
  def setup = Action {
    val setup = DBIO.seq(
      (movies_table.schema).create,
      movies_table ++= moviesData.map(movie =>
        (
          movie.movie_id,
          movie.title,
          movie.genre,
          movie.runtime_minutes,
          movie.release_date
        )
      ),
      (showtimes_table.schema).create,
      showtimes_table ++= showtimesData.map(showtime =>
        (
          showtime.showtime_id,
          showtime.movie_id,
          showtime.date,
          showtime.time,
          showtime.capacity,
          showtime.reservations
        )
      ),
      (reservations_table.schema).create
    )

    val setupFuture = db.run(setup)

    Ok("Database setup complete.")
  }

  /* 
  * Route for returning a list of all the movies
  */
  def movies = Action.async { implicit request =>
    // Ok(views.html.movies(moviesData))
    // Ok(moviesJSON)

    implicit val movieWrites: Writes[Movie] = (
      (JsPath \ "movie_id").write[Int] and
        (JsPath \ "title").write[String] and
        (JsPath \ "genre").write[String] and
        (JsPath \ "runtime_minutes").write[Int] and
        (JsPath \ "release_date").write[String]
    )(unlift(Movie.unapply))

    for {
      dbMovies <- db.run(movies_table.result)
    } yield {
      val movies = dbMovies
        .map(movie =>
          Movie(
            movie._1,
            movie._2,
            movie._3,
            movie._4,
            movie._5
          )
        )
        .toList
      Ok(Json.toJson(movies))
    }
  }

  /* 
  * Route for returning a list of all the showtimes
  */
  def showtimes = Action.async { implicit request =>
    // Ok(views.html.showtimes(showtimesData))
    // Ok(showtimesJSON)

    implicit val showtimeWrites: Writes[Showtime] = (
      (JsPath \ "showtime_id").write[Int] and
        (JsPath \ "movie_id").write[Int] and
        (JsPath \ "date").write[String] and
        (JsPath \ "time").write[String] and
        (JsPath \ "capacity").write[Int] and
        (JsPath \ "reservations").write[Int]
    )(unlift(Showtime.unapply))

    for {
      dbShowtimes <- db.run(showtimes_table.result)
    } yield {
      val showtimes = dbShowtimes
        .map(showtime =>
          Showtime(
            showtime._1,
            showtime._2,
            showtime._3,
            showtime._4,
            showtime._5,
            showtime._6
          )
        )
        .toList
      Ok(Json.toJson(showtimes))
    }
  }

  /* 
  * Route for returning a movie's details
  */
  def movie(movie_id: Int) = Action.async { implicit request =>
    // val movie = moviesJSON
    //   .as[JsArray]
    //   .value
    //   .filter(movie => (movie \ "id").as[Int] == movie_id)
    // val showtimes = showtimesJSON
    //   .as[JsArray]
    //   .value
    //   .filter(showtime => (showtime \ "movie_id").as[Int] == movie_id)
    // val movieDetailsJSON =
    //   movie(0).as[JsObject] + ("showtimes" -> JsArray(showtimes))

    // implicit val movieDetailsDecoder: Decoder[MovieDetails] =
    //   Decoder.forProduct6(
    //     "movie_id",
    //     "title",
    //     "genre",
    //     "runtime_minutes",
    //     "release_date",
    //     "showtimes"
    //   )(MovieDetails.apply)
    // val movieData = decode[MovieDetails](movieDetailsJSON.toString) match {
    //   case Right(value) => value
    //   case Left(error) =>
    //     throw new Exception(s"Error parsing MovieDetails JSON: $error")
    // }

    // Ok(views.html.movie(movieData))
    // Ok(movieDetailsJSON)

    implicit val showtimeWrites: Writes[Showtime] = (
      (JsPath \ "showtime_id").write[Int] and
        (JsPath \ "movie_id").write[Int] and
        (JsPath \ "date").write[String] and
        (JsPath \ "time").write[String] and
        (JsPath \ "capacity").write[Int] and
        (JsPath \ "reservations").write[Int]
    )(unlift(Showtime.unapply))

    implicit val movieDetailsWrites: Writes[MovieDetails] = (
      (JsPath \ "movie_id").write[Int] and
        (JsPath \ "title").write[String] and
        (JsPath \ "genre").write[String] and
        (JsPath \ "runtime_minutes").write[Int] and
        (JsPath \ "release_date").write[String] and
        (JsPath \ "showtimes").write[Seq[Showtime]]
    )(unlift(MovieDetails.unapply))

    val movie = db.run(movies_table.filter(_.movie_id === movie_id).result.head)
    val showtimes = db.run(
      showtimes_table.filter(_.movie_id === movie_id).result
    )

    for {
      movie <- movie
      showtimes <- showtimes
    } yield {
      val movieDetails = MovieDetails(
        movie._1,
        movie._2,
        movie._3,
        movie._4,
        movie._5,
        showtimes
          .map(showtime =>
            Showtime(
              showtime._1,
              showtime._2,
              showtime._3,
              showtime._4,
              showtime._5,
              showtime._6
            )
          )
          .toList
      )
      Ok(Json.toJson(movieDetails))
    }
  }

  /* 
  * Route for reserving seats for a showtime
  */
  def reserve(showtime_id: Int, name: String, email: String, seats: Int) =
    Action.async { implicit request =>
      db.run(
        showtimes_table
          .filter(_.showtime_id === showtime_id)
          .result
          .headOption
      ).flatMap {
        case Some(showtime) =>
          db.run(
            showtimes_table.filter(_.showtime_id === showtime_id).result.head
          ).flatMap { reservations =>
            db.run(
              showtimes_table
                .filter(_.showtime_id === showtime_id)
                .map(_.capacity)
                .result
            ).flatMap { capacity =>
              if (seats <= 0) {
                Future.successful(BadRequest("Invalid number of seats."))
              } else {
                val reservation_id = java.util.UUID.randomUUID.toString
                val timestamp = java.time.LocalDateTime.now().toString
                if (
                  java.time.LocalDateTime
                    .parse(s"${reservations._3}T${reservations._4}")
                    .isBefore(java.time.LocalDateTime.now())
                ) {
                  Future.successful(BadRequest("Showtime has already passed."))
                } else {
                  if (reservations._6 + seats <= capacity.head) {
                    db.run(
                      showtimes_table
                        .filter(_.showtime_id === showtime_id)
                        .map(_.reservations)
                        .update(reservations._6 + seats)
                    ).flatMap { _ =>
                      db.run(
                        reservations_table += (
                          reservation_id,
                          showtime_id,
                          name,
                          email,
                          seats,
                          timestamp
                        )
                      ).flatMap { _ =>
                        val reservationData = Reservation(
                          reservation_id,
                          showtime_id,
                          name,
                          email,
                          seats,
                          timestamp
                        )
                        Future.successful(Ok(Json.toJson(reservationData)))
                      }
                    }
                  } else {
                    Future.successful(BadRequest("Not enough seats available."))
                  }
                }
              }
            }
          }
        case None => Future.successful(BadRequest("Showtime not found."))
      }

      // val showtime = showtimesJSON
      //   .as[JsArray]
      //   .value
      //   .filter(showtime => (showtime \ "showtime_id").as[Int] == showtime_id)
      // val updatedShowtime =
      //   showtime(0).as[JsObject] + ("reservations" -> JsNumber(
      //     (showtime(0) \ "reservations").as[Int] + seats
      //   ))
      // val reservationJSON = Json.obj(
      //   "reservation_id" -> java.util.UUID.randomUUID.toString,
      //   "showtime_id" -> showtime_id,
      //   "name" -> name,
      //   "email" -> email,
      //   "seats" -> seats,
      //   "timestamp" -> java.time.LocalDateTime.now().toString
      // )

      // implicit val movieDetailsDecoder: Decoder[MovieDetails] =
      //   Decoder.forProduct6(
      //     "movie_id",
      //     "title",
      //     "genre",
      //     "runtime_minutes",
      //     "release_date",
      //     "showtimes"
      //   )(MovieDetails.apply)
      // val movieData = decode[MovieDetails](movieDetailsJSON.toString) match {
      //   case Right(value) => value
      //   case Left(error) =>
      //     throw new Exception(s"Error parsing MovieDetails JSON: $error")
      // }

      // implicit val reservationDecoder: Decoder[Reservation] =
      //   Decoder.forProduct6(
      //     "reservation_id",
      //     "showtime_id",
      //     "name",
      //     "email",
      //     "seats",
      //     "timestamp"
      //   )(Reservation.apply)
      // val reservationData = decode[Reservation](reservationJSON.toString) match {
      //   case Right(value) => value
      //   case Left(error) =>
      //     throw new Exception(s"Error parsing Reservation JSON: $error")
      // }

      // Ok(views.html.reserve(reservationData))
      // Ok(reservationJSON)
    }

  /* 
  * Route for cancelling a reservation
  */
  def cancel(reservation_id: String) = Action.async { implicit request =>
    db.run(
      reservations_table
        .filter(_.reservation_id === reservation_id)
        .result
        .headOption
    ).flatMap {
      case Some(reservation) =>
        db.run(
          showtimes_table
            .filter(_.showtime_id === reservation._2)
            .result
            .head
        ).flatMap { showtime =>
          val timestamp = java.time.LocalDateTime.parse(reservation._6)
          if (
            java.time.LocalDateTime
              .parse(s"${showtime._3}T${showtime._4}")
              .isBefore(java.time.LocalDateTime.now())
          ) {
            Future.successful(BadRequest("Showtime has already passed."))
          } else {
            db.run(
              showtimes_table
                .filter(_.showtime_id === reservation._2)
                .map(_.reservations)
                .update(showtime._6 - reservation._5)
            ).flatMap { _ =>
              db.run(
                reservations_table
                  .filter(_.reservation_id === reservation_id)
                  .delete
              ).flatMap { _ =>
                if (
                  java.time.LocalDateTime
                    .parse(s"${showtime._3}T${showtime._4}")
                    .isBefore(
                      java.time.LocalDateTime.now().plusDays(1)
                    )
                ) {
                  Future.successful(
                    BadRequest(
                      "Reservation cancelled. A penalty fee of $5.00 has been charged."
                    )
                  )
                } else {
                  Future.successful(Ok("Reservation cancelled."))
                }
              }
            }
          }
        }
      case None => Future.successful(BadRequest("Reservation not found."))
    }
  }
}
