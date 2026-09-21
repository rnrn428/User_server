# 🍄 Yes AI Do (EcoSphere) — User & Auth Microservice

<div align="center">
  <img src="https://img.shields.io/badge/Java-21-007396?style=for-the-badge&logo=openjdk&logoColor=white"/>
  <img src="https://img.shields.io/badge/Spring%20Boot-3.x-6DB33F?style=for-the-badge&logo=springboot&logoColor=white"/>
  <img src="https://img.shields.io/badge/Spring%20Security-6DB33F?style=for-the-badge&logo=springsecurity&logoColor=white"/>
  <img src="https://img.shields.io/badge/Spring%20Cloud-OpenFeign-6DB33F?style=for-the-badge&logo=spring&logoColor=white"/>
  <img src="https://img.shields.io/badge/PostgreSQL-4169E1?style=for-the-badge&logo=postgresql&logoColor=white"/>
  <img src="https://img.shields.io/badge/Redis-DC382D?style=for-the-badge&logo=redis&logoColor=white"/>
  <img src="https://img.shields.io/badge/SonarQube-Passed-4E9BCD?style=for-the-badge&logo=sonarqube&logoColor=white"/>
</div>

<br/>

> **NHN Academy AIoT 3기 최종 프로젝트 [Yes AI Do]**  
> IoT 센서 시계열 데이터와 AI 생육 분석(Vision + LLM)을 활용한 **스마트 버섯 재배 자동화 플랫폼의 인증/회원 마이크로서비스**입니다.  
> 🔗 **전체 팀 프로젝트 조직:** [nhnacademy-aiot3-yes-ai-do](https://github.com/nhnacademy-aiot3-yes-ai-do)

---

## 📌 서비스 개요 (Service Overview)

`User_server`는 전체 마이크로서비스 생태계의 신원 보증(Identity Provider) 및 보안 관문을 담당합니다.  
사용자 가입부터 소셜 연동, 토큰 라이프사이클 관리, 휴면 계정 정책, 고객 문의 처리까지 회원 도메인 전체를 캡슐화하여 제공합니다.

---

## 🛠 핵심 기능 및 기술적 구현 (Key Implementations)

### 1. 🔐 인증 & 인가 파이프라인 (Authentication & JWT)
* **Google OAuth2 + 자체 로그인 통합**: 구글 소셜 로그인 및 일반 이메일/비밀번호 로그인을 하나의 인증 체계로 통합 지원
* **Dual Token Strategy (Access / Refresh Token)**:
  * Stateless한 인증을 위해 Access Token(단기) 발급
  * 보안 강화를 위해 Refresh Token(장기)은 **Redis**에 암호화 저장 및 만료 시간(TTL) 자동 관리

### 2. ⚡ Redis 기반 토큰 블랙리스트 & 세션 최적화
* **즉시 무효화 (Logout & Blacklist)**: JWT의 취약점인 '로그아웃 후에도 만료 전까지 유효한 문제'를 해결하기 위해 Redis Blacklist 기법 도입
* **인메모리 캐싱**: 잦은 토큰 검증 요청에 대해 DB I/O를 발생시키지 않고 Redis에서 즉각 처리하여 성능 오버헤드 최소화

### 3. 🛡️ 계정 라이프사이클 & 보안 정책
* **휴면 계정 자동 전환 스케줄러 (`@Scheduled`)**: 개인정보보호 정책에 따라 장기 미접속 회원을 자동으로 휴면 상태(`DORMANT`)로 격리 및 본인 인증 후 복구 처리
* **회원 탈퇴 (Soft Delete)**: 연관된 재배 데이터 및 센서 이력 보존을 위해 `is_deleted` 플래그 기반 연쇄 비활성화 처리

### 4. 📧 이메일 인증 시스템
* `Spring Boot Starter Mail`을 활용한 가입/비밀번호 재설정 시 6자리 보안 인증 코드 발송
* 인증 코드 유효 시간(3분) 검증 및 재발송 제한 로직 구현

### 5. 📡 MSA 서비스 간 통신 (OpenFeign)
* `Spring Cloud OpenFeign`을 통해 재배(Cultivation) 및 게이트웨이(Gateway) 서비스와의 내부 통신 연동

### 6. 🧼 코드 품질 관리 (SonarQube)
* SonarQube 정적 코드 분석기를 도입하여 코드 스멜(Code Smell), 보안 취약점, 버그를 리팩토링하고 **Quality Gate**를 안정적으로 통과

---

## 📂 패키지 구조 (Package Structure)

```
site.yesaido.user_server
├── domain
│   ├── email             # 이메일 발송 및 인증 코드 검증 도메인
│   ├── inquiry           # 1:1 고객 문의 도메인 (CRUD, REST Docs)
│   └── user              # 회원 및 인증 도메인
│       ├── controller    # 회원가입, 로그인, OAuth2, 프로필, 탈퇴 API
│       ├── entity        # User, UserStatus(ACTIVE, DORMANT, DELETED)
│       ├── repository    # Spring Data JPA Repository
│       ├── scheduler     # 휴면 계정 자동 판별 스케줄러
│       └── service       # JWT 발급, 비즈니스 검증, 패스워드 암호화
└── global
    ├── config            # SecurityConfig, RedisConfig, SwaggerConfig
    ├── exception         # GlobalExceptionHandler, Custom Exceptions
    ├── jwt               # JwtProvider, JwtValidator, TokenBlacklist
    └── oauth             # OAuth2UserInfo, OAuth2SuccessHandler
```

---

## ⚙️ 실행 환경 (Requirements)

* **Java**: 21
* **Framework**: Spring Boot 3.x
* **Database**: PostgreSQL, Redis
* **Build Tool**: Maven

---

## 👨‍💻 Contributor

* **이재웅 (JaeUng)** — [GitHub Profile (@rnrn428)](https://github.com/rnrn428)
  * 인증/인가(OAuth2, JWT), Redis 토큰 및 세션 관리, 휴면 계정 정책, SonarQube 리팩토링 주도
