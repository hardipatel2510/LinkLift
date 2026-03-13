#!/bin/bash
cd "$(dirname "$0")"

echo "Starting File Client..."
java -cp bin client.FileClient
