# Decodable Quick Start

This project provides a framework for quickly getting started with Decodable development using a local Kafka cluster. It includes:

- Decodable resources defined using declarative syntax
- Docker resources for creating a local Kafka cluster for source and sink streams
- Example Flink project for custom pipeline development

## Project Structure

```
decodable-quick-start/
├── README.md                           # Project documentation
├── decodable/                          # Decodable declarative resources
│   ├── connections/                    # Connection definitions
│   ├── streams/                        # Stream definitions
│   ├── pipelines/                      # Pipeline definitions
│   └── custom/                         # Custom pipeline implementations
│       ├── src/
│       │   ├── main/
│       │   │   ├── java/               # Java source code
│       │   │   └── resources/          # Configuration files
│       │   └── test/
│       │       ├── java/               # Test source code
│       │       └── resources/          # Test resources
│       └── pom.xml                     # Maven build configuration
└── docker/                             # Docker related files
    ├── Dockerfile                      # Main Dockerfile for the example project
    └── docker-compose.yml              # Docker compose for local development
```

## Prerequisites:

* Running Docker Daemon
* Decodable CLI installed and logged in.

## Getting Started

1. Clone this repository
2. Copy `.env.template` to `.env` in  `docker/` directories and fill in your credentials
3. Run `deploy.sh` in the project root

## Components

### Decodable Resources
Contains declarative definitions for Decodable resources including connections, streams, and pipelines.

### Docker Resources
Provides Docker configuration for local development environment.
