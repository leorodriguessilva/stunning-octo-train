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

### Prerequisites
- Java 21 or higher

### Installing Java via asdf tool
If you are using [asdf](https://asdf-vm.com/) to manage your development environment, you can set up the required Java 
version by running in the root directory of the project:

```bash
asdf install
```

### Build the Service
```bash
./gradlew clean build
```

### Run the Automatic Tests
```bash
./gradlew test
```

### Run Locally with Docker
```bash
docker compose up --build
```

The service will be available at `http://localhost:8080`.

## API Overview
- `POST /items` - Create a new item
- `PUT /items/{id}/description` - Update an item's description
- `PUT /items/{id}/done` - Mark an item as done
- `PUT /items/{id}/not-done` - Mark an item as not done
- `GET /items/{id}` - Get a single item
- `GET /items?includeAll=true` - Get all items (default is only not done)

### Create an item
```bash
curl -X POST http://localhost:8080/items \
  -H "Content-Type: application/json" \
  -d '{
    "description": "Write documentation",
    "dueDatetime": "2026-01-25T00:00:00Z"
  }'
```

### Update an item's description
```bash
curl -X PUT http://localhost:8080/items/1/description \
  -H "Content-Type: application/json" \
  -d '{
    "description": "Update docs"
  }'
```

### Mark an item done
```bash
curl -X PUT http://localhost:8080/items/1/done
```

### Mark an item not done
```bash
curl -X PUT http://localhost:8080/items/1/not-done
```

### Get an item by id
```bash
curl http://localhost:8080/items/1
```

### List items
```bash
curl http://localhost:8080/items
```

### List all items (including done/past due)
```bash
curl http://localhost:8080/items?includeAll=true
```