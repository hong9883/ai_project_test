#!/bin/bash

echo "=========================================="
echo "RAG Chat System - 전체 시스템 시작"
echo "=========================================="

# Docker 인프라 시작
echo ""
echo "1. Docker 인프라 시작 중..."
cd docker
docker-compose up -d

echo ""
echo "Docker 서비스 준비 대기 중 (30초)..."
sleep 30

echo ""
echo "2. Ollama 모델 확인/다운로드 중..."
docker exec rag-ollama ollama pull llama2

echo ""
echo "=========================================="
echo "인프라 준비 완료!"
echo "=========================================="
echo ""
echo "다음 단계:"
echo "1. 각 백엔드 서비스를 별도 터미널에서 실행:"
echo "   - cd backend/auth-service && mvn spring-boot:run"
echo "   - cd backend/document-service && mvn spring-boot:run"
echo "   - cd backend/chat-service && mvn spring-boot:run"
echo "   - cd backend/gateway-service && mvn spring-boot:run"
echo ""
echo "2. 프론트엔드 실행:"
echo "   - cd frontend/react-app && npm install && npm start"
echo ""
echo "=========================================="
echo "접속 URL: http://localhost:3000"
echo "=========================================="
