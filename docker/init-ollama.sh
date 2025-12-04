#!/bin/bash

echo "Waiting for Ollama to be ready..."
sleep 10

echo "Pulling llama2 model..."
docker exec rag-ollama ollama pull llama2

echo "Ollama setup complete!"
