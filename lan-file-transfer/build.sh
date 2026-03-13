#!/bin/bash
cd "$(dirname "$0")"

echo "Compiling Java LAN File Transfer System..."
mkdir -p bin
javac -d bin $(find . -name "*.java")

if [ $? -eq 0 ]; then
    echo "Compilation successful. Classes are in the 'bin' directory."
else
    echo "Compilation failed."
    exit 1
fi
