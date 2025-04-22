#!/bin/bash

# Exit on any error
set -e

# Kafka connection details from kafka-source.yaml
BOOTSTRAP_SERVERS="2.tcp.ngrok.io:15495"
TOPIC="test_topic"

# Check if kcat is installed
if ! command -v kcat &> /dev/null; then
    echo "❌ kcat is not installed. Please install it first."
    echo "   On macOS: brew install kcat"
    echo "   On Ubuntu: apt-get install kcat"
    exit 1
fi

# Create a sample JSON message
MESSAGE='{"id": 1, "value": "test message", "timestamp": "'$(date -u +"%Y-%m-%dT%H:%M:%SZ")'"}'

echo "🚀 Producing message to topic: $TOPIC"
echo "📝 Message: $MESSAGE"

# Produce the message using kcat
echo "$MESSAGE" | kcat -P -b "$BOOTSTRAP_SERVERS" -t "$TOPIC"

echo "✅ Message produced successfully!" 