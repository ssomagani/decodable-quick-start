# Tests Project

This is a Java project for testing Decodable pipelines and components.

## Project Structure

```
tests/
├── src/
│   ├── main/
│   │   └── java/        # Main source code
│   └── test/
│       └── java/        # Test source code
├── pom.xml              # Maven project configuration
└── README.md           # This file
```

## Requirements

- Java 11 or higher
- Maven 3.6 or higher

## Building the Project

To build the project, run:

```bash
mvn clean install
```

## Running Tests

To run the tests, use:

```bash
mvn test
```

## Adding New Tests

1. Create your test class in the `src/test/java` directory
2. Follow the package structure `com.decodable.tests`
3. Use JUnit 4 for writing tests
4. Run tests using Maven as described above 