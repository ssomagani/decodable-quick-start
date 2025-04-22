# Decodable Quick Start

This project provides a framework for quickly getting started with Decodable development. It includes:

- Decodable resources defined using declarative syntax
- Docker resources for local development
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

## Getting Started

Prerequisites - (a) Docker Daemon running, (b) Decodable CLI installed and logged in.

1. Clone this repository
2. Copy `.env.template` to `.env` in both `decodable/` and `docker/` directories and fill in your credentials
3. Follow the instructions in each component's README for specific setup steps

### Quick Start
1. Run `deploy.sh` in the project root

## Components

### Decodable Resources
Contains declarative definitions for Decodable resources including connections, streams, and pipelines.

### Docker Resources
Provides Docker configuration for local development environment.
