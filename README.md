# Transaction Management System
Developed for TabCorp - March 2025

## Overview

The Transaction Management System is a reactive Spring Boot application designed to process and analyze transaction data efficiently. It provides a scalable architecture for high-throughput transaction processing with real-time analytics capabilities.

### Architecture

The system follows a layered architecture:

- **REST API Layer**: Provides endpoints for querying transaction analytics and authentication
- **Service Layer**: Contains the business logic for transaction processing and validation
- **Repository Layer**: Handles data access using reactive repositories
- **Messaging Layer**: Integrates with Kafka for asynchronous transaction processing

### Data Flow

1. Transactions are received via Kafka messages
2. The system validates each transaction (customer validity, product status, quantities)
3. Valid transactions are persisted to the database
4. The analytics engine processes transaction data to provide customer and product insights
5. Results are cached using Redis for improved performance

## Features

- **Transaction Processing**
  - JSON and BSON format support
  - Batch processing capabilities
  - Comprehensive validation
  - Error handling with detailed feedback

- **Real-time Analytics**
  - Total cost per customer
  - Total cost per product
  - Transaction history

- **Caching System**
  - Redis-based caching
  - Automatic cache invalidation
  - Improved query performance

- **Security**
  - Token-based authentication
  - Role-based access control
  - Mock authentication system for development

- **Reactive Programming**
  - Non-blocking I/O operations
  - Event-driven architecture
  - Efficient resource utilization

## Technologies

- **Spring Boot**: Application framework
- **Spring WebFlux**: Reactive web framework
- **Project Reactor**: Reactive programming library
- **Spring Security**: Authentication and authorization
- **R2DBC**: Reactive database connectivity
- **H2 Database**: In-memory database for development
- **Redis**: Distributed caching
- **Kafka**: Message broker for asynchronous processing
- **MapStruct**: Object mapping
- **Lombok**: Reduces boilerplate code
- **JUnit 5**: Testing framework
- **Circuit Breaker**: Fallback configuration in case of failure 
## Setup Instructions

### Prerequisites

- JDK 21 
- Gradle
- Docker (optional, for running Redis and Kafka)

### Local Development Setup

1. **Clone the repository**
   ```bash
   git clone https://github.com/yourusername/transaction-management.git
   cd transaction-management
   ```

2. **Run Redis and Kafka containers (optional)**
   ```bash
   docker-compose up -d
   ```

3. **Build the application**
   ```bash
   ./gradlew clean build
   ```

4. **Run the application**
   ```bash
   ./gradlew bootRun
   ```

   The application will start on port 8080 by default.

### Configuration

The application can be configured by modifying the `src/main/resources/application.yml` file:

## Authentication

The system uses a mock authentication controller for development purposes.

### Obtaining Authentication Tokens

To obtain a token, send a POST request to the auth endpoint:

```bash
curl -X POST http://localhost:8080/api/auth/token \
  -H "Content-Type: application/json" \
  -d '{"username": "user", "password": "password"}'
```

Response:
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "expiresIn": 3600
}
```

### Using Authentication Tokens

Include the token in subsequent requests using the Authorization header:

```bash
curl -X GET http://localhost:8080/api/transactions/analytics/customer \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..."
```

### CI/CD
The project is configured with github actions on commit and pull requests 
```
.github/workflows/github-actions.yml
```

## API Endpoints

### Transaction Analytics

#### Get Transaction Cost Per Customer

```bash
curl -X GET http://localhost:8080/api/transactions/analytics/customer \
  -H "Authorization: Bearer your-token-here"
```

Response:
```json
[
  {
    "customerId": 1,
    "firstName": "John",
    "lastName": "Doe",
    "totalCost": 125.50
  },
  {
    "customerId": 2,
    "firstName": "Jane",
    "lastName": "Smith",
    "totalCost": 75.25
  }
]
```

#### Get Transaction Cost Per Product

```bash
curl -X GET http://localhost:8080/api/transactions/analytics/product \
  -H "Authorization: Bearer your-token-here"
```

Response:
```json
[
  {
    "productCode": "PRODUCT_001",
    "status": "ACTIVE",
    "totalCost": 250.75
  },
  {
    "productCode": "PRODUCT_002",
    "status": "ACTIVE",
    "totalCost": 180.00
  }
]
```

### Transaction Processing

Transactions are processed through Kafka messages

## Kafka Integration

The system integrates with Kafka for asynchronous transaction processing.

### Kafka Topics

- `transaction-json-input`: For JSON formatted transactions
- `transaction-bson-input`: For BSON formatted transactions
- `transaction-errors`: For transaction processing errors

### Message Format

JSON messages should follow this format:

```json
{
  "customerId": 1,
  "productCode": "PRODUCT_001",
  "quantity": 3,
  "transactionTime": "2023-07-15T10:30:00",
  "dataFormat": "JSON",
  "jsonData": "{\"metadata\": {\"channel\": \"web\", \"region\": \"US\"}}"
}
```

### Producing Messages

You can use any Kafka producer to send messages to the input topics. Here's an example using the Kafka CLI:

```bash
kafka-console-producer.sh --broker-list localhost:9092 --topic transaction-json-input
```

## Testing

Run the tests with:

```bash
./gradlew test
```

### Unit Tests

Unit tests validate individual components in isolation.

### Integration Tests

Integration tests validate the interaction between components with the database.

## Troubleshooting

### Common Issues

- **Connection Refused to Redis**: Ensure Redis is running on the configured host and port
- **Authentication Failed**: Check that you're using a valid token in your requests
- **Missing Table Errors**: The schema is auto-generated on startup; ensure the application has proper permissions

### Logs

Logs are stored in `logs/transaction-management.log` by default.

### Further enhancements
1. The database can be migrated to mongodb for more data for larger size, external db offers lot more features than in memory
2. There can be more tests added
3. Values related to security can be stored in aws secret manager or azure key vault
4. Gatling/scala performance tests can be added to test volume as it is a high volume processing application
