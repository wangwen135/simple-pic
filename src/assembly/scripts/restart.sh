#!/bin/bash

# Simple-Pic Restart Script

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# Stop the application
./stop.sh

# Wait a moment
sleep 2

# Start the application
./start.sh