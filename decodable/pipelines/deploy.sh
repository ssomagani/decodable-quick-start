#!/bin/bash

# Exit on any error
set -e

# Get the absolute path to the project root
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"

# Configuration
PIPELINE_DEF="$PROJECT_ROOT/decodable/pipelines/pipelines.yaml"
CUSTOM_DIR="$PROJECT_ROOT/decodable/custom"
JAR_FILE="$CUSTOM_DIR/target/flink-example-1.0-SNAPSHOT.jar"

echo "🚀 Starting deployment process..."
echo "📂 Using project root: $PROJECT_ROOT"
echo "📂 Custom directory: $CUSTOM_DIR"
echo "📦 JAR file path: $JAR_FILE"

# Check if decodable CLI is installed
if ! command -v decodable &> /dev/null; then
    echo "❌ Decodable CLI is not installed. Please install it first."
    exit 1
fi

# Build the JAR
echo "📦 Building JAR file..."
cd "$CUSTOM_DIR"
mvn clean package
cd "$(dirname "${BASH_SOURCE[0]}")"

# Check if build was successful
if [ ! -f "$JAR_FILE" ]; then
    echo "❌ Build failed: JAR file not found at $JAR_FILE"
    echo "🔍 Checking if JAR exists with a different name..."
    JAR_FILES=$(find "$CUSTOM_DIR/target" -name "*.jar" | grep -v "original-")
    if [ -n "$JAR_FILES" ]; then
        echo "✅ Found JAR files:"
        echo "$JAR_FILES"
        echo "Please update the JAR_FILE variable in the script to match one of these files."
    else
        echo "❌ No JAR files found in $CUSTOM_DIR/target"
    fi
    exit 1
fi

# Deploy to Decodable using declarative syntax
echo "📤 Deploying to Decodable..."
decodable apply "$PIPELINE_DEF"

echo "✅ Deployment completed successfully!"
echo "You can now activate the pipeline using: decodable pipeline activate flink-example" 