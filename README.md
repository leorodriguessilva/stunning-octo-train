# Todo List Service

## Service Description
This service exposes a REST API for managing to-do items with descriptions, due dates, and status transitions. Items 
automatically become **past due** when their `due_datetime` has passed and they are still marked as **not done**. 
Past-due items are immutable via the API. 

## Tech Stack
- **Language:** Kotlin (JVM)
- **Framework:** Spring Boot (Web + Data JPA + Validation)
- **Database:** H2 (in-memory)
- **Build Tool:** Gradle
- **Testing:** JUnit 5 + Spring Boot Test + MockMvc
- **Containerization:** Docker / Docker Compose

## How-To Guide

### Build the Service
```bash
gradle clean build
```

### Run the Automatic Tests
```bash
gradle test
```

### Run Locally with Docker
```bash
docker compose up --build
```

The service will be available at `http://localhost:8080`.

## API Overview
- `POST /items` - Create a new item
- `PUT /items/{id}/description` - Update an item's description
- `POST /items/{id}/done` - Mark an item as done
- `POST /items/{id}/not-done` - Mark an item as not done
- `GET /items/{id}` - Get a single item
- `GET /items?includeAll=true` - Get all items (default is only not done)