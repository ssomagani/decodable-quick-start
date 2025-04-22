# Flink Example Project

This project demonstrates how to develop and test custom pipelines using Decodable's SDK.

## Project Structure

```
flink/
├── src/
│   ├── main/
│   │   ├── java/                   # Java source code
│   │   └── resources/              # Configuration files
│   └── test/
│       ├── java/                   # Test source code
│       └── resources/              # Test resources
└── pom.xml                         # Maven build configuration
```

## Building the Project

To build the project, run:

```bash
mvn clean package
```

This will create a JAR file in the `target/` directory that can be deployed to Decodable.

## Running Tests

To run the tests, use:

```bash
mvn test
```

## Development

1. Create your pipeline class in `src/main/java/co/decodable/example/`
2. Write tests in `src/test/java/co/decodable/example/`
3. Build and test locally
4. Deploy to Decodable using the CLI or web interface

## Example Pipeline

The example pipeline demonstrates:
- Reading from a Decodable stream
- Processing data using Flink operators
- Writing to a Decodable stream

See `ExamplePipeline.java` for implementation details. 

