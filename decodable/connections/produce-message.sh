#!/bin/bash

# Exit on any error
set -e

# Get the NGROK_URL from environment variables
NGROK_URL=$(docker exec broker curl -s http://ngrok:4040/api/tunnels/command_line | jq -r '.["public_url"]' | awk -F[/:] '{print $4 ":" $5}' | sed 's/.$//')

echo "NGROK_URL: $NGROK_URL"
# Kafka connection details from kafka-source.yaml
BOOTSTRAP_SERVERS="$NGROK_URL"
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