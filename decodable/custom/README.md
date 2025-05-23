# Decodable Java Custom Pipeline Project

This is an Java project demonstrates how to develop and test custom pipelines using Decodable's SDK.

## Project Structure

```
custom/
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

To build this project alone, you can simply run:

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

Standalone
1. Create your pipeline class in `src/main/java/co/decodable/example/`
2. Write tests in `src/test/java/co/decodable/example/`
3. Build and test locally
4. Deploy to Decodable using the CLI or web interface

Together with all Decodable resources
1. `cd ..`
2. `./deploy.sh`

## DataStreamJob

The example pipeline demonstrates:
- Reading from a Decodable stream that was created using the declarative resource definition in `decodable/streams`
- Processing data using Flink operators
- Writing to a Decodable stream which was also created using the declarative resource definition in `decodable/streams`


