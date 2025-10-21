# Repository Guidelines

## Project Overview

This is a Spring Boot-based LLM proxy service that provides model management, authentication, and proxy capabilities for large language models. The application serves as a backend API for managing AI models and handling requests to various LLM providers.

## Project Structure

- **Source Code**: src/main/java/cn/tyt/llmproxy/
  - controller/ - REST API endpoints
  - service/ - Business logic layer
  - entity/ - Data models and entities
  - dto/ - Data transfer objects
  - mapper/ - MyBatis data access layer
  - config/ - Spring configuration classes
  - security/ - Authentication and authorization
  - aspect/ - AOP logging and statistics

- **Tests**: src/test/java/cn/tyt/llmproxy/
  - controller/ - API endpoint tests
  - Integration tests and database initialization

- **Configuration**: src/main/resources/
  - Application properties and database configuration

- **Docker**: Containerized deployment with MySQL database

## Build, Test, and Development Commands

### Building
`ash
./mvnw clean package -DskipTests
# or use the build script
./build.sh
`

### Testing
`ash
./mvnw test
# Run specific test class
./mvnw test -Dtest=ModelControllerTests
`

### Running Locally
`ash
./mvnw spring-boot:run
# or use the start script
./start.sh
`

### Docker Deployment
`ash
docker-compose up -d
docker-compose down
`

## Coding Style & Naming Conventions

- **Java Version**: Java 17
- **Package Structure**: cn.tyt.llmproxy
- **Naming**: 
  - Interfaces: I*Service (e.g., IUserService)
  - Implementations: *ServiceImpl (e.g., UserServiceImpl)
  - Controllers: *Controller (e.g., ModelController)
  - Entities: * (e.g., User)
  - DTOs: *DTO (e.g., UserDTO)

- **Framework**: Spring Boot 3.5.3 with MyBatis Plus
- **Dependencies**: 
  - JWT for authentication
  - LangChain4J for LLM integration
  - Caffeine for caching
  - Multiple LLM provider SDKs (Alibaba, VolcEngine)

## Testing Guidelines

- **Framework**: Spring Boot Test with JUnit
- **Test Structure**: 
  - Controller tests with @SpringBootTest
  - Database integration tests
  - Authentication test coverage
- **Test Naming**: *Tests suffix (e.g., AuthControllerTests)
- **Database**: Test database initialization in InitDB.java

## Commit & Pull Request Guidelines

- **Commit Messages**: Use conventional commit format
- **PR Requirements**:
  - Link related issues
  - Include test coverage
  - Update documentation if needed
  - Ensure builds pass
- **Branch Strategy**: Feature branches from main

## Development Workflow

1. Start MySQL database: docker-compose up mysql-db
2. Run application: ./mvnw spring-boot:run
3. Access API: http://localhost:8060
4. Run tests: ./mvnw test

## Configuration & Environment

- **Database**: MySQL 8.0 with connection pooling
- **Port**: 8060 for backend API
- **Security**: JWT-based authentication
- **Caching**: Caffeine in-memory cache
- **Logging**: Structured logging with AOP aspects

## Agent-Specific Instructions

When modifying code in this repository:
- Follow existing naming conventions and package structure
- Ensure proper authentication in new endpoints
- Add appropriate logging using existing aspects
- Test with multiple LLM provider integrations
- Maintain database schema consistency
- Update API documentation in relevant controllers
