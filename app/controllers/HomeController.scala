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

  // create a json object from a string
  val MOVIES_JSON: JsValue = Json.parse(
    """
    [{"movie_id":1,"title":"Lost Highway","genre":"Crime|Drama|Fantasy|Film-Noir|Mystery|Romance","runtime_minutes":196,"release_date":"2023-12-06"},
    {"movie_id":2,"title":"Doomsday","genre":"Action|Drama|Sci-Fi|Thriller","runtime_minutes":107,"release_date":"2023-12-19"},
    {"movie_id":3,"title":"Game Over","genre":"Crime|Drama|Thriller","runtime_minutes":194,"release_date":"2023-12-08"},
    {"movie_id":4,"title":"Broken Arrow","genre":"Drama|Romance|Western","runtime_minutes":145,"release_date":"2024-02-09"},
    {"movie_id":5,"title":"Eddy Duchin Story, The","genre":"Drama|Musical|Romance","runtime_minutes":90,"release_date":"2024-01-22"},
    {"movie_id":6,"title":"Desperadoes, The","genre":"Romance|Western","runtime_minutes":65,"release_date":"2024-02-03"},
    {"movie_id":7,"title":"As If I Didn't Exist (Elina - Som om jag inte fanns)","genre":"Children|Drama","runtime_minutes":150,"release_date":"2023-11-01"},
    {"movie_id":8,"title":"Donkey Xote","genre":"Animation","runtime_minutes":84,"release_date":"2023-11-16"},
    {"movie_id":9,"title":"Nights and Weekends","genre":"Drama","runtime_minutes":169,"release_date":"2023-11-05"},
    {"movie_id":10,"title":"May I Kill U?","genre":"Comedy|Horror|Thriller","runtime_minutes":195,"release_date":"2023-11-24"},
    {"movie_id":11,"title":"Painted Lady","genre":"Crime|Drama|Thriller","runtime_minutes":192,"release_date":"2024-01-10"},
    {"movie_id":12,"title":"Krakatoa, East of Java","genre":"Adventure|Drama","runtime_minutes":121,"release_date":"2024-02-05"},
    {"movie_id":13,"title":"But Forever in My Mind","genre":"Comedy|Drama","runtime_minutes":104,"release_date":"2023-10-11"},
    {"movie_id":14,"title":"Shadow of Angels (Schatten der Engel)","genre":"Drama","runtime_minutes":145,"release_date":"2024-01-26"},
    {"movie_id":15,"title":"Locusts, The","genre":"Drama","runtime_minutes":129,"release_date":"2024-02-17"},
    {"movie_id":16,"title":"Mr. Blandings Builds His Dream House","genre":"Comedy","runtime_minutes":146,"release_date":"2024-02-04"},
    {"movie_id":17,"title":"She's So Lovely","genre":"Drama|Romance","runtime_minutes":181,"release_date":"2023-11-21"},
    {"movie_id":18,"title":"Wolverine, The","genre":"Action|Adventure|Fantasy|Sci-Fi","runtime_minutes":167,"release_date":"2023-12-06"},
    {"movie_id":19,"title":"Prince of Egypt, The","genre":"Animation|Musical","runtime_minutes":81,"release_date":"2023-12-20"},
    {"movie_id":20,"title":"Garfield Christmas Special, A","genre":"Animation|Children|Comedy|Musical","runtime_minutes":88,"release_date":"2023-11-16"},
    {"movie_id":21,"title":"Gun Woman","genre":"Action|Thriller","runtime_minutes":167,"release_date":"2024-01-31"},
    {"movie_id":22,"title":"Tales of the Grim Sleeper","genre":"Crime|Documentary","runtime_minutes":75,"release_date":"2023-11-18"},
    {"movie_id":23,"title":"Farce of the Penguins","genre":"Comedy","runtime_minutes":157,"release_date":"2023-10-16"},
    {"movie_id":24,"title":"Infinity","genre":"Drama","runtime_minutes":138,"release_date":"2023-10-04"},
    {"movie_id":25,"title":"No Way to Treat a Lady","genre":"Crime|Drama|Thriller","runtime_minutes":60,"release_date":"2024-01-09"},
    {"movie_id":26,"title":"Cosmic Journey","genre":"Sci-Fi","runtime_minutes":88,"release_date":"2023-11-14"},
    {"movie_id":27,"title":"Falling Down","genre":"Action|Drama","runtime_minutes":111,"release_date":"2023-12-30"},
    {"movie_id":28,"title":"Charlie Chan and the Curse of the Dragon Queen","genre":"Comedy|Mystery","runtime_minutes":182,"release_date":"2023-11-05"},
    {"movie_id":29,"title":"eXistenZ","genre":"Action|Sci-Fi|Thriller","runtime_minutes":143,"release_date":"2023-11-17"},
    {"movie_id":30,"title":"It's a Wonderful Life","genre":"Drama|Fantasy|Romance","runtime_minutes":61,"release_date":"2023-11-18"}]
    """
  )

  val SHOWTIMES_JSON: JsValue = Json.parse(
    """
    [{"showtime_id":1,"movie_id":16,"date":"2024-03-20","time":"17:59","capacity":66,"reservations":0},
    {"showtime_id":2,"movie_id":30,"date":"2024-02-24","time":"13:29","capacity":87,"reservations":0},
    {"showtime_id":3,"movie_id":6,"date":"2024-02-21","time":"16:50","capacity":79,"reservations":0},
    {"showtime_id":4,"movie_id":20,"date":"2024-02-08","time":"22:28","capacity":84,"reservations":0},
    {"showtime_id":5,"movie_id":26,"date":"2024-02-11","time":"17:06","capacity":70,"reservations":0},
    {"showtime_id":6,"movie_id":15,"date":"2024-02-10","time":"16:59","capacity":90,"reservations":0},
    {"showtime_id":7,"movie_id":14,"date":"2024-03-14","time":"09:04","capacity":90,"reservations":0},
    {"showtime_id":8,"movie_id":3,"date":"2024-03-28","time":"17:01","capacity":52,"reservations":0},
    {"showtime_id":9,"movie_id":10,"date":"2024-02-20","time":"12:37","capacity":78,"reservations":0},
    {"showtime_id":10,"movie_id":25,"date":"2024-03-09","time":"16:56","capacity":56,"reservations":0},
    {"showtime_id":11,"movie_id":22,"date":"2024-03-10","time":"09:40","capacity":65,"reservations":0},
    {"showtime_id":12,"movie_id":8,"date":"2024-02-05","time":"21:41","capacity":80,"reservations":0},
    {"showtime_id":13,"movie_id":3,"date":"2024-02-27","time":"14:00","capacity":80,"reservations":0},
    {"showtime_id":14,"movie_id":13,"date":"2024-02-07","time":"16:58","capacity":89,"reservations":0},
    {"showtime_id":15,"movie_id":3,"date":"2024-03-23","time":"13:46","capacity":84,"reservations":0},
    {"showtime_id":16,"movie_id":16,"date":"2024-02-28","time":"15:11","capacity":62,"reservations":0},
    {"showtime_id":17,"movie_id":11,"date":"2024-03-04","time":"22:11","capacity":97,"reservations":0},
    {"showtime_id":18,"movie_id":12,"date":"2024-02-03","time":"14:08","capacity":79,"reservations":0},
    {"showtime_id":19,"movie_id":13,"date":"2024-03-06","time":"18:57","capacity":91,"reservations":0},
    {"showtime_id":20,"movie_id":12,"date":"2024-02-19","time":"13:33","capacity":82,"reservations":0},
    {"showtime_id":21,"movie_id":12,"date":"2024-03-10","time":"09:30","capacity":99,"reservations":0},
    {"showtime_id":22,"movie_id":13,"date":"2024-03-04","time":"19:35","capacity":95,"reservations":0},
    {"showtime_id":23,"movie_id":29,"date":"2024-02-26","time":"17:11","capacity":62,"reservations":0},
    {"showtime_id":24,"movie_id":15,"date":"2024-03-08","time":"18:17","capacity":56,"reservations":0},
    {"showtime_id":25,"movie_id":6,"date":"2024-03-02","time":"17:36","capacity":52,"reservations":0},
    {"showtime_id":26,"movie_id":2,"date":"2024-03-20","time":"20:59","capacity":54,"reservations":0},
    {"showtime_id":27,"movie_id":30,"date":"2024-03-24","time":"22:59","capacity":51,"reservations":0},
    {"showtime_id":28,"movie_id":10,"date":"2024-03-23","time":"18:37","capacity":84,"reservations":0},
    {"showtime_id":29,"movie_id":1,"date":"2024-03-21","time":"11:54","capacity":58,"reservations":0},
    {"showtime_id":30,"movie_id":24,"date":"2024-03-26","time":"21:45","capacity":63,"reservations":0},
    {"showtime_id":31,"movie_id":5,"date":"2024-02-10","time":"18:17","capacity":59,"reservations":0},
    {"showtime_id":32,"movie_id":3,"date":"2024-02-20","time":"16:51","capacity":91,"reservations":0},
    {"showtime_id":33,"movie_id":5,"date":"2024-03-03","time":"09:49","capacity":62,"reservations":0},
    {"showtime_id":34,"movie_id":10,"date":"2024-03-22","time":"19:12","capacity":67,"reservations":0},
    {"showtime_id":35,"movie_id":29,"date":"2024-03-25","time":"22:08","capacity":90,"reservations":0},
    {"showtime_id":36,"movie_id":20,"date":"2024-02-23","time":"15:58","capacity":81,"reservations":0},
    {"showtime_id":37,"movie_id":9,"date":"2024-03-05","time":"11:17","capacity":55,"reservations":0},
    {"showtime_id":38,"movie_id":30,"date":"2024-03-19","time":"17:12","capacity":59,"reservations":0},
    {"showtime_id":39,"movie_id":11,"date":"2024-03-18","time":"21:10","capacity":57,"reservations":0},
    {"showtime_id":40,"movie_id":29,"date":"2024-02-20","time":"19:48","capacity":68,"reservations":0},
    {"showtime_id":41,"movie_id":9,"date":"2024-02-17","time":"14:09","capacity":59,"reservations":0},
    {"showtime_id":42,"movie_id":4,"date":"2024-02-03","time":"17:28","capacity":98,"reservations":0},
    {"showtime_id":43,"movie_id":19,"date":"2024-02-24","time":"11:57","capacity":97,"reservations":0},
    {"showtime_id":44,"movie_id":7,"date":"2024-02-17","time":"17:58","capacity":95,"reservations":0},
    {"showtime_id":45,"movie_id":15,"date":"2024-02-26","time":"22:27","capacity":51,"reservations":0},
    {"showtime_id":46,"movie_id":29,"date":"2024-02-07","time":"19:04","capacity":80,"reservations":0},
    {"showtime_id":47,"movie_id":28,"date":"2024-03-28","time":"09:25","capacity":77,"reservations":0},
    {"showtime_id":48,"movie_id":9,"date":"2024-03-28","time":"16:23","capacity":62,"reservations":0},
    {"showtime_id":49,"movie_id":3,"date":"2024-02-02","time":"12:24","capacity":82,"reservations":0},
    {"showtime_id":50,"movie_id":19,"date":"2024-03-26","time":"13:27","capacity":81,"reservations":0}]
    """
  )

  /*
  * Load MOVIES_DATA.json content and parse it into a List[Movie]
  */
  val moviesContent: String = MOVIES_JSON.toString
  // val moviesContent: String = fromFile("public/data/MOVIES_DATA.json").mkString
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
  println("moviesJSON:", moviesJSON)
  println("moviesData:", moviesData)

  /*
  * Load SHOWTIMES_DATA.json content and parse it into a List[Showtime]
  */

  val showtimesContent: String = SHOWTIMES_JSON.toString
  // val showtimesContent: String = fromFile(
  //   "public/data/SHOWTIMES_DATA.json"
  // ).mkString
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
  println("showtimesJSON:", showtimesJSON)
  println("showtimesData:", showtimesData)

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

  // print if connection is successful
  db.run(sql"SELECT 1".as[Int]).onComplete {
    case scala.util.Success(value) => println("Connection successful.")
    case scala.util.Failure(exception) =>
      println("Connection failed.", exception)
  }

  /* 
  * Main homepage
  */
  def index = Action {
    Ok(views.html.index("Welcome to Cinema World+!"))
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
