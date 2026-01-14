# Todo List Service

## Service Description
This service exposes a REST API for managing to-do items with descriptions, due dates, and status transitions.

## Blurry Requirements Addressed
 - When a todo item is set to done, is not clear if it should become immutable after due date as well.
   - Current implementation allows the todo item to be marked to not-done.
 - Is not clear if a done todo item is allowed to have its description updated
   - Current implementation allows updating the description of a done todo item.
   - What would feel natural is to not allow updating the description of a done item after the due date is in the past.
 - The due date in the todo item creation, can be already in the past?
   - Current implementation only allow due dates in the future.
   - However, i see an use case for creating todo items for historical tracking.

## Future Improvements
 - Add indexes for dueDatetime and status fields for better query performance in the cronjob.
 - Add a batch limit to the mark as past due cronjob to avoid long-running transactions.
 - Add pagination to the list items endpoint.
 - Add Swagger/OpenAPI documentation for better API discoverability.
 - To prepare for scaling, make todo items partitionable by user/category/country.
 - To prepare for scaling, a cache layer (e.g., Redis or using app server memory) can be added for the listing and find by id endpoints.

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