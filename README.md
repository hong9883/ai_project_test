# RAG 기반 채팅 시스템

전자정부 프레임워크 기반의 MSA 아키텍처를 활용한 RAG(Retrieval-Augmented Generation) 채팅 애플리케이션입니다.

## 기능

- **사용자 인증**: JWT 기반 로그인/회원가입
- **문서 관리**: PDF 파일 업로드 및 텍스트 추출
- **RAG 채팅**: 업로드된 문서를 기반으로 한 질문/응답

## 기술 스택

### Backend
- **프레임워크**: Spring Boot 3.2.0 + 전자정부 프레임워크 4.1.0
- **언어**: Java 17
- **아키텍처**: MSA (Microservices Architecture)
- **데이터베이스**: PostgreSQL 15
- **캐시**: Redis 7
- **벡터 DB**: OpenSearch 2.11.0
- **LLM**: Ollama (llama2 모델)
- **PDF 처리**: Apache PDFBox 3.0.1
- **인증**: JWT (jjwt 0.12.3)

### Frontend
- **프레임워크**: React 18.2.0
- **라우팅**: React Router 6
- **HTTP 클라이언트**: Axios

### Infrastructure
- **컨테이너**: Docker & Docker Compose
- **API Gateway**: Spring Cloud Gateway

## 시스템 아키텍처

```
┌─────────────┐
│   React     │
│  Frontend   │
└──────┬──────┘
       │
       │ HTTP
       │
┌──────▼──────────────────────────────────────┐
│         API Gateway (Port 8080)              │
└──────┬──────────────────────────────────────┘
       │
       ├─────────────┬──────────────┬──────────
       │             │              │
┌──────▼──────┐ ┌───▼────────┐ ┌──▼──────────┐
│Auth Service │ │Document    │ │Chat Service │
│  (Port      │ │Service     │ │  (Port      │
│   8081)     │ │(Port 8082) │ │   8083)     │
└──────┬──────┘ └───┬────────┘ └──┬──────────┘
       │            │              │
       ├────────────┴──────────────┤
       │                           │
┌──────▼──────┐           ┌───────▼────────┐
│  PostgreSQL │           │   OpenSearch   │
│   + Redis   │           │   + Ollama     │
└─────────────┘           └────────────────┘
```

## 마이크로서비스 구성

### 1. Auth Service (Port 8081)
- 사용자 인증 및 권한 관리
- JWT 토큰 발급 및 검증
- 회원가입/로그인 API

### 2. Document Service (Port 8082)
- PDF 파일 업로드 및 저장
- PDF 텍스트 추출
- 텍스트 청크 분할
- Ollama를 통한 임베딩 생성
- OpenSearch에 벡터 저장

### 3. Chat Service (Port 8083)
- 사용자 질문 처리
- OpenSearch에서 관련 문서 검색
- Ollama를 통한 응답 생성
- 채팅 히스토리 관리

### 4. Gateway Service (Port 8080)
- 단일 진입점 제공
- JWT 인증 필터
- 라우팅 및 로드 밸런싱

## 설치 및 실행

### 사전 요구사항
- Java 17+
- Maven 3.6+
- Node.js 16+
- Docker & Docker Compose
- NVIDIA GPU (Ollama 가속화를 위한 선택사항)

### 1. 인프라 구성

```bash
# Docker 컨테이너 실행 (OpenSearch, Ollama, PostgreSQL, Redis)
cd docker
docker-compose up -d

# Ollama llama2 모델 다운로드
./init-ollama.sh
```

### 2. 백엔드 서비스 실행

각 서비스를 별도의 터미널에서 실행합니다.

```bash
# Auth Service
cd backend/auth-service
mvn spring-boot:run

# Document Service
cd backend/document-service
mvn spring-boot:run

# Chat Service
cd backend/chat-service
mvn spring-boot:run

# Gateway Service
cd backend/gateway-service
mvn spring-boot:run
```

### 3. 프론트엔드 실행

```bash
cd frontend/react-app
npm install
npm start
```

애플리케이션이 http://localhost:3000 에서 실행됩니다.

## API 엔드포인트

### Auth Service
- `POST /api/auth/signup` - 회원가입
- `POST /api/auth/login` - 로그인
- `POST /api/auth/refresh` - 토큰 갱신

### Document Service
- `POST /api/documents/upload` - PDF 업로드
- `GET /api/documents` - 문서 목록 조회
- `GET /api/documents/{id}` - 문서 상세 조회
- `DELETE /api/documents/{id}` - 문서 삭제

### Chat Service
- `POST /api/chat` - 메시지 전송
- `GET /api/chat/history` - 채팅 히스토리 조회

## 환경 설정

### 데이터베이스 연결
각 서비스의 `application.yml` 파일에서 데이터베이스 설정을 변경할 수 있습니다.

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/rag_auth
    username: rag_user
    password: rag_password
```

### JWT Secret 키 변경
운영 환경에서는 반드시 JWT secret 키를 변경해야 합니다.

```yaml
jwt:
  secret: your-256-bit-secret-key-change-this-in-production
```

### Ollama 모델 변경
다른 Ollama 모델을 사용하려면 `application.yml`을 수정합니다.

```yaml
ollama:
  model: llama2  # 또는 mistral, codellama 등
```

## 개발 가이드

### 새로운 서비스 추가
1. `backend/` 디렉토리에 새 Spring Boot 프로젝트 생성
2. `pom.xml`에 필요한 의존성 추가
3. Gateway Service의 `application.yml`에 라우팅 규칙 추가

### 프론트엔드 컴포넌트 추가
1. `frontend/react-app/src/components/` 또는 `pages/`에 새 컴포넌트 생성
2. 필요한 경우 `services/api.js`에 API 호출 함수 추가
3. `App.js`에 라우트 추가

## 트러블슈팅

### Ollama 연결 오류
```bash
# Ollama 컨테이너 상태 확인
docker ps | grep ollama

# Ollama 로그 확인
docker logs rag-ollama
```

### OpenSearch 연결 오류
```bash
# OpenSearch 상태 확인
curl http://localhost:9200/_cluster/health

# OpenSearch 로그 확인
docker logs rag-opensearch
```

### PostgreSQL 연결 오류
각 서비스의 데이터베이스가 생성되어 있는지 확인합니다.

```sql
-- PostgreSQL에 접속하여 데이터베이스 생성
CREATE DATABASE rag_auth;
CREATE DATABASE rag_document;
CREATE DATABASE rag_chat;
```

## 보안 고려사항

1. **JWT Secret**: 운영 환경에서는 강력한 시크릿 키를 사용하고 환경 변수로 관리
2. **CORS 설정**: Gateway의 CORS 설정을 운영 환경에 맞게 조정
3. **파일 업로드**: 파일 크기 제한 및 타입 검증 강화
4. **Rate Limiting**: API 요청 제한 설정 권장
5. **HTTPS**: 운영 환경에서는 HTTPS 사용 필수

## 라이선스

이 프로젝트는 테스트 목적으로 작성되었습니다.

## 기여

버그 리포트나 기능 제안은 이슈로 등록해 주세요.
