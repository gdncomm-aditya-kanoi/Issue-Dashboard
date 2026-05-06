# Issue Dashboard

A Spring Boot microservice that receives issue data from Power Automate and stores them in MongoDB.

## Architecture

- **Controller**: `TeamsWebhookController` - Handles HTTP requests from Power Automate
- **Service**: `IssueService` - Processes and transforms message data
- **Repository**: `IssueRepository` - MongoDB data access layer
- **Entity**: `Issue` - MongoDB document model

## API Endpoint

### POST /api/webhook

Receives issue data from Power Automate and stores it in MongoDB.

**Request Body:**
```json
{
  "text": "Issue description",
  "user": "username",
  "category": "bug|feature|question",
  "severity": "low|medium|high|critical"
}
```

**Response:**
```json
{
  "id": "generated-id",
  "text": "Issue description",
  "username": "username",
  "category": "bug",
  "severity": "high",
  "timestamp": "2026-05-06T14:30:00",
  "processed": false,
  "source": "Power Automate"
}
```

## Configuration

```properties
server.port=8888
spring.data.mongodb.uri=mongodb://localhost:27017/issue_dashboard
spring.data.mongodb.database=issue_dashboard
```

## Running the Application

```bash
mvn clean install
mvn spring-boot:run
```

The application will start on port 8888.