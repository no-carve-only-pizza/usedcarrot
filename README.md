# UsedCarrot

UsedCarrot은 Spring Boot 3, Java 21, Thymeleaf, Spring Security, JPA, H2 기반의 중고거래 플랫폼입니다.

공개 저장소: <https://github.com/no-carve-only-pizza/usedcarrot>

## 주요 기능

- 회원가입, 로그인, 로그아웃, 마이페이지, 비밀번호 변경
- 상품 등록, 목록, 상세, 수정, 삭제, 검색
- 이미지 업로드 보안 검증: 확장자, MIME, 파일 시그니처, 크기, 개수 제한
- 관심 상품 등록/취소
- 상품별 1:1 채팅
- 상품/사용자 신고와 신고 누적 제재
- 관리자 사용자/상품/신고/감사 로그 관리
- DB 내부 `Wallet`, `WalletTransaction` 기반 CarrotCoin 모의지갑

## 보안 기능

- BCrypt 비밀번호 해시 저장
- Spring Security 세션 인증, CSRF 보호, 세션 고정 방어
- `/admin/**` 관리자 권한 제한
- 상품, 채팅방, 지갑, 거래 내역의 서버 측 권한 검증
- DTO Bean Validation 기반 입력값 검증
- Thymeleaf `th:text` 중심 출력으로 XSS 방어
- Spring Data JPA 파라미터 바인딩으로 SQL Injection 방어
- 비밀번호, 토큰, 세션 ID 등 민감정보 감사 로그 제외

## CarrotCoin 안내

CarrotCoin은 실제 가상자산 결제 서비스가 아니라 DB 기반 모의지갑 시스템입니다. 실제 블록체인, 개인키, 지갑 주소, 외부 결제 API는 사용하지 않습니다. 신규 지갑은 0 CC로 생성되며 관리자가 필요한 금액을 지급합니다. 상품 구매 시 서버에 저장된 상품 가격으로 구매자 지갑 차감과 판매자 지갑 증가가 하나의 DB 트랜잭션에서 처리됩니다.

## 실행 방법

Java 21이 필요합니다.

로컬 실행에서는 관리자 정보를 환경변수로 지정하고 `local` 프로필을 사용합니다. 비밀번호는 12자 이상이어야 합니다.

```bash
export USEDCARROT_ADMIN_EMAIL='admin@example.com'
export USEDCARROT_ADMIN_PASSWORD='change-this-to-a-strong-password'
./gradlew bootRun --args='--spring.profiles.active=local'
```

운영 환경에서는 `local` 프로필을 사용하지 말고 HTTPS에서 실행합니다. 기본 설정은 세션 쿠키에 `Secure`, `HttpOnly`, `SameSite=Strict`를 적용합니다.

접속 URL:

- 애플리케이션: http://localhost:8080
- H2 콘솔: 기본 비활성화. 로컬 디버깅이 필요할 때만 `spring.h2.console.enabled=true`로 실행하세요.

H2 기본 설정:

- JDBC URL: `jdbc:h2:file:./data/usedcarrot;MODE=MySQL;DATABASE_TO_LOWER=TRUE`
- Username: `sa`
- Password: 없음

## 테스트 방법

```bash
./gradlew test
```

현재 테스트는 기본 계정 미생성, 0 CC 지갑 생성, 관리자 CC 지급, CarrotCoin 구매, 신고 관리자 승인, 로그인 IP 제한, 프록시 IP 위조 방어와 상품 경로 인증을 확인합니다.

## 계정과 CC 지급

소스에 포함된 기본 계정은 없습니다. 관리자는 위 환경변수로 최초 1회 생성하며 일반 사용자는 회원가입합니다. 관리자는 사용자 관리 화면에서 사용자별로 최대 1,000,000 CC를 지급할 수 있습니다.
