#!/bin/bash
cd "$(dirname "$0")"

echo "Starting File Server..."
# Run the server in the current directory so it creates shared_files here
java -cp bin server.FileServer
