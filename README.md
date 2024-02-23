# cinema-world

### Overview:
You are tasked with creating a small backend service for a fictional cinema chain named "Cinema World+”. This service will manage movie listings, showtimes, and reservations. Your goal is to design and implement a set of RESTful endpoints, a simple database schema, and the logic to interact with this database, all using Scala.

The requirements listed below are also the suggested order of implementation.  We recommend that you do what you can for each requirement within the estimated time window.  You are welcome to spend more time if desired but the limited time is accounted for during the evaluation.  Completion of one requirement is not a prerequisite to moving on to the next.  Accomplish what you can and then move on.

**You are not required to use the stub code in this repository as your basis.  It is only here for those who want a jumping off point.**

### Requirements:
1. API Design & Implementation (1 hour):
   - Design RESTful endpoints to perform the following operations:
     - List all movies currently showing.
     - Get details of a specific movie by ID (including showtimes).
     - Book tickets for a specific showtime.
   - Implement these endpoints in Scala using a framework of your choice
   - GET endpoints can run off static in-memory data.  POST endpoint can simply log the action.
2. Database (1 hour):
   - Design a simple relational database schema to store movies, showtimes, and reservations.
   - Implement database access from your API using a Scala library (e.g., Slick, Doobie).
   - Include basic instructions on how to set up the database for testing your application.
   - Even if the database is not fully wired in, progress to the next requirement after an hour.
3. Business Logic (30 minutes):
   - Ensure that reservations cannot exceed the seating capacity of a showtime.
   - Implement a simple cancellation functionality with a basic penalty calculation: if a booking is canceled less than 24 hours before the showtime, a penalty fee applies.
4. Documentation & Testing (30 min):
   - Provide a README file with:
     - Instructions on how to run your application.
     - An overview of your API endpoints and how to use them.
   - Include unit tests for your business logic and integration tests for your endpoints.

### Bonus (Optional):
- Dockerize your application for easy setup and deployment.


### Evaluation Criteria:
- Correctness and completeness of the implemented features.
- Code quality, including readability, structure, and adherence to Scala best practices.
- Design of RESTful API and database schema.
- Quality of documentation, including clarity of instructions and API documentation.
- Test coverage and quality of tests.

### Submission Guidelines:
- Provide your code in a clean, organized repository on GitHub or a similar platform.
- Ensure all dependencies are properly managed for easy setup.
- Submit your repository link along with any necessary setup instructions via the provided submission form.
---
### How to run
`sbt run`

http://localhost:8080/movies