# SwiftRoute Service

A simple SwiftRoute built with Spring Boot and MongoDB.

This repository provides a minimal service to create short URLs and redirect to the original long URLs. The project is implemented in Java (Java 17) using Spring Boot and Spring Data MongoDB.

## Features

- Create a short URL for a long URL
- Redirect short URLs to the original long URL
- Validation for input URLs
- Uses MongoDB as the persistence layer

## Project layout

- `Url shortner/` — main application module (contains `pom.xml` and source under `src/`)

## Requirements

- Java 17+
- Maven 3.6+
- A running MongoDB instance (local or remote)

## Configuration

The application reads MongoDB connection and other settings from Spring Boot properties. You can provide them via `src/main/resources/application.properties` or with environment variables.

Key properties to set:

- `spring.data.mongodb.uri` — MongoDB connection URI (e.g. `mongodb://localhost:27017/urlshortener`)
- `app.base-url` — (optional) the base URL used when returning full shortened URLs (e.g. `http://localhost:8080`)

## Build

Build the project with Maven:

```bash
mvn clean package
```

## Run

Run with Maven:

```bash
mvn spring-boot:run
```

Or run the packaged jar:

```bash
java -jar target/url-shortener-1.0.0.jar
```

The application runs by default on port 8080.

## API

Typical endpoints (project may use slightly different paths — check source under `Url shortner/src`):

- Create a short URL
  - Method: POST
  - Path: `/api/url/shorten`
  - Body: `{ "longUrl": "https://example.com/very/long/path" }`
  - Returns: shortened URL details (JSON)

- Redirect to original URL
  - Method: GET
  - Path: `/{shortId}`
  - Behavior: HTTP 302 redirect to the original long URL

Example curl to create a short URL:

```bash
curl -X POST -H "Content-Type: application/json" \
  -d '{"longUrl":"https://example.com/some/long/path"}' \
  http://localhost:8080/api/url/shorten
```

## Tests

Run tests with:

```bash
mvn test
```

## Development notes

- The Maven `pom.xml` in `SwiftRoute/` shows this is a Spring Boot application using `spring-boot-starter-web` and `spring-boot-starter-data-mongodb`.
- If you use Lombok in your IDE, enable annotation processing.

## Contributing

Contributions are welcome. Please open an issue or submit a pull request with a clear description of changes.

