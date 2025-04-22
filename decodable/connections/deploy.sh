#!/bin/bash

# Exit on any error
set -e

# Get the absolute path to the connections directory
CONNECTIONS_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo "🚀 Starting deployment of connection resources..."

# Check if decodable CLI is installed
if ! command -v decodable &> /dev/null; then
    echo "❌ Decodable CLI is not installed. Please install it first."
    exit 1
fi

# Function to process a template file and create a copy with environment variables substituted
process_template_file() {
    local template_file=$1
    local output_file="${template_file%.template}"
    
    echo "🔄 Processing template file: $(basename "$template_file")"
    
    # Check if the template file exists
    if [ ! -f "$template_file" ]; then
        echo "❌ Template file not found: $template_file"
        return 1
    fi
    
    # Create a copy of the template file
    cp "$template_file" "$output_file"
    echo "📝 Created file: $output_file"
    
    # Get all environment variables
    local env_vars=$(env | cut -d= -f1)
    
    # Replace each environment variable in the output file
    for var in $env_vars; do
        # Skip internal variables and functions
        if [[ "$var" == "_" || "$var" == "BASH_"* || "$var" == "FUNCNAME" || "$var" == "CONNECTIONS_DIR" ]]; then
            continue
        fi
        
        # Get the value of the environment variable
        local value="${!var}"
        
        # Skip empty values
        if [ -z "$value" ]; then
            continue
        fi
        
        # Replace the variable in the output file using awk
        # Format: ${VAR_NAME}
        awk -v var="$var" -v val="$value" '{gsub("\\$\\{" var "\\}", val); print}' "$output_file" > "${output_file}.new"
        mv "${output_file}.new" "$output_file"
        
        # Format: $VAR_NAME
        awk -v var="$var" -v val="$value" '{gsub("\\$" var, val); print}' "$output_file" > "${output_file}.new"
        mv "${output_file}.new" "$output_file"
    done
    
    echo "✅ Processed template file: $(basename "$template_file")"
    
    # Return the path to the processed file
    echo "$output_file"
}

# Find all template files in the current directory
TEMPLATE_FILES=$(find "$CONNECTIONS_DIR" -maxdepth 1 -name "*.yaml.template" -o -name "*.yml.template")

if [ -z "$TEMPLATE_FILES" ]; then
    echo "ℹ️ No template files found in $CONNECTIONS_DIR"
    exit 0
fi

# Process and apply each template file
for template_file in $TEMPLATE_FILES; do
    echo "📦 Processing template: $(basename "$template_file")"
    
    # Process the template file
    PROCESSED_FILE=$(process_template_file "$template_file")
    
    # Check if processing was successful
    if [ $? -ne 0 ]; then
        echo "❌ Failed to process $(basename "$template_file")"
        continue
    fi
    
    # Apply the processed file
    echo "📦 Applying resource definition: $(basename "$PROCESSED_FILE")"
    
    # Change to the connections directory before running decodable apply
    cd "$CONNECTIONS_DIR"
    decodable apply "$(basename "$PROCESSED_FILE")"
    
    if [ $? -eq 0 ]; then
        echo "✅ Successfully applied $(basename "$PROCESSED_FILE")"
        


    else
        echo "❌ Failed to apply $(basename "$PROCESSED_FILE")"
        exit 1
    fi
done

echo "✅ All connection resources deployed and activated successfully!" 