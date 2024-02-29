import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.http.Status
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.api.libs.json._

/** Functional tests start a Play application internally, available as `app`.
  */
class FunctionalSpec extends PlaySpec with GuiceOneAppPerSuite {

  "Routes" should {

    "send 404 on a bad request" in {
      route(app, FakeRequest(GET, "/boum")).map(status(_)) mustBe Some(
        NOT_FOUND
      )
    }

    "send 200 on a good request" in {
      route(app, FakeRequest(GET, "/")).map(status(_)) mustBe Some(OK)
    }

  }

  "HomeController" should {

    "render the index page" in {
      val home = route(app, FakeRequest(GET, "/")).get

      status(home) mustBe Status.OK
      contentType(home) mustBe Some("text/html")
      contentAsString(home) must include("Welcome to Cinema World+!")
    }

    "be able to setup the cinemaWorldDB" in {
      val setup = route(app, FakeRequest(GET, "/setup")).get

      status(setup) mustBe Status.OK
      contentType(setup) mustBe Some("text/plain")
      contentAsString(setup) must include("Database setup complete.")
    }

    "be able to query movies from the movies_table" in {
      val movies = route(app, FakeRequest(GET, "/movies")).get

      status(movies) mustBe Status.OK
      contentType(movies) mustBe Some("application/json")
      contentAsString(movies) must include(
        "{\"movie_id\":1,\"title\":\"Lost Highway\",\"genre\":\"Crime|Drama|Fantasy|Film-Noir|Mystery|Romance\",\"runtime_minutes\":196,\"release_date\":\"2023-12-06\"}"
      )
    }

    "be able to query showtimes from the showtimes_table" in {
      val showtimes = route(app, FakeRequest(GET, "/showtimes")).get

      status(showtimes) mustBe Status.OK
      contentType(showtimes) mustBe Some("application/json")
      contentAsString(showtimes) must include(
        "{\"showtime_id\":1,\"movie_id\":16,\"date\":\"2024-03-20\",\"time\":\"17:59\",\"capacity\":66,\"reservations\":"
      )
    }

    "be able to query a specific movie from the /movie?movie_id=X endpoint" in {
      val movie = route(app, FakeRequest(GET, "/movie?movie_id=1")).get

      status(movie) mustBe Status.OK
      contentType(movie) mustBe Some("application/json")
      contentAsString(movie) must include(
        "{\"movie_id\":1,\"title\":\"Lost Highway\",\"genre\":\"Crime|Drama|Fantasy|Film-Noir|Mystery|Romance\",\"runtime_minutes\":196,\"release_date\":\"2023-12-06\",\"showtimes\":[{\"showtime_id\":29,\"movie_id\":1,\"date\":\"2024-03-21\",\"time\":\"11:54\",\"capacity\":58,\"reservations\":0}]}"
      )
    }

    "be able to book a reservation at the /reserve route" in {
      val reservation = route(
        app,
        FakeRequest(
          POST,
          "/reserve?showtime_id=1&name=jordan&email=jordan@mail.com&seats=1"
        )
      ).get

      status(reservation) mustBe Status.OK
      contentType(reservation) mustBe Some("application/json")
      contentAsString(reservation) must include(
        "\"showtime_id\":1,\"name\":\"jordan\",\"email\":\"jordan@mail.com\",\"seats\":1,\"timestamp\""
      )
    }

    "not be able to book a reservation at the /reserve route for showtimes that are old" in {
      val reservation = route(
        app,
        FakeRequest(
          POST,
          "/reserve?showtime_id=2&name=jordan&email=jordan@mail.com&seats=1"
        )
      ).get

      status(reservation) mustBe Status.BAD_REQUEST
      contentType(reservation) mustBe Some("text/plain")
      contentAsString(reservation) must include(
        "Showtime has already passed."
      )
    }

    "not be able to book a reservation at the /reserve route if there are not enough seats" in {
      val reservation = route(
        app,
        FakeRequest(
          POST,
          "/reserve?showtime_id=1&name=jordan&email=jordan@mail.com&seats=150"
        )
      ).get

      status(reservation) mustBe Status.BAD_REQUEST
      contentType(reservation) mustBe Some("text/plain")
      contentAsString(reservation) must include(
        "Not enough seats available."
      )
    }

    "not be able to book a reservation at the /reserve route if the showtime_id does not exist" in {
      val reservation = route(
        app,
        FakeRequest(
          POST,
          "/reserve?showtime_id=0&name=jordan&email=jordan@mail.com&seats=150"
        )
      ).get

      status(reservation) mustBe Status.BAD_REQUEST
      contentType(reservation) mustBe Some("text/plain")
      contentAsString(reservation) must include(
        "Showtime not found."
      )
    }

    "be able to cancel a reservation at the /cancel route" in {
      val cancel = route(
        app,
        FakeRequest(
          GET,
          "/cancel?reservation_id=2c318d19-a995-46a9-ac5c-b8d6b0da5911"
        )
      ).get

      // status should return a badrequest
      status(cancel) mustBe Status.BAD_REQUEST
      contentType(cancel) mustBe Some("text/plain")
      contentAsString(cancel) must include("Reservation not found.")
    }
  }
}
