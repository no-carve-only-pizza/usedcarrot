> **참고:** 이 저장소는 Sepolia ETH + MetaMask **온체인 제출본**이다. 결제·송금 서술은 [`WEB3_OVERRIDE.md`](WEB3_OVERRIDE.md)와 [`PROJECT_REPORT.md`](PROJECT_REPORT.md)를 따른다. 아래 본문에 남아 있는 CarrotCoin/모의지갑 문구는 초안 이력이다.

# UsedCarrot AI 구현 지시서

이 문서는 AI에게 그대로 전달하여 중고거래 플랫폼을 구현시키기 위한 개발 지시서이다. 설명용 산출물이 아니라 실제 구현 기준이므로, 아래 명세를 최대한 그대로 따른다.

## 1. 구현 목표

중고거래 플랫폼 `UsedCarrot`을 Tiny Second-hand Shopping Platform 웹 애플리케이션으로 구현한다.

이 프로젝트는 기능을 무작정 많이 넣는 것보다, 아래 최소 요구사항을 보안 약점이 적은 방식으로 완성하는 것을 우선한다. 추가 기능은 최소 요구사항이 정상 동작하고 보안 검수가 끝난 뒤 구현한다.

핵심 목표는 다음과 같다.

- 사용자는 회원가입, 로그인, 로그아웃을 할 수 있다.
- 사용자는 상품을 등록, 조회, 수정, 삭제할 수 있다.
- 사용자는 상품을 검색하고 상세 페이지를 볼 수 있다.
- 사용자는 판매자와 1:1 채팅을 할 수 있다.
- 사용자는 상품 또는 사용자를 신고할 수 있다.
- 일정 횟수 이상 신고된 상품은 숨김 처리된다.
- 일정 횟수 이상 신고된 사용자는 제한 또는 정지 처리된다.
- 관리자는 사용자, 상품, 신고, 감사 로그를 관리할 수 있다.
- 시큐어코딩 수업용 프로젝트이므로 인증, 인가, 입력값 검증, 파일 업로드 보안, XSS 방어, SQL Injection 방어, 민감정보 로그 제외를 반드시 구현한다.

### 1.1 최소 요구사항

아래 기능은 반드시 구현한다.

| 영역 | 최소 기능 | 구현 기준 |
| --- | --- | --- |
| 사용자 관리 | 회원가입 | 이메일, 비밀번호, 닉네임, 지역을 입력받고 중복 이메일/닉네임을 막는다. |
| 사용자 관리 | 로그인/로그아웃 | Spring Security 세션 기반 인증을 사용한다. |
| 사용자 관리 | 사용자 조회 | 상품 상세, 채팅, 관리자 화면에서 사용자 닉네임과 상태를 조회할 수 있다. |
| 사용자 관리 | 마이페이지 | 본인 프로필 조회, 소개글 수정, 비밀번호 변경을 제공한다. |
| 상품 관리 | 상품 등록 | 로그인 사용자가 상품명, 설명, 가격, 지역, 카테고리, 이미지를 등록한다. |
| 상품 관리 | 등록 상품 관리 | 판매자는 본인이 등록한 상품 목록을 확인하고 수정/삭제할 수 있다. |
| 상품 관리 | 상품 조회 | 전체 상품 목록과 상품 상세 페이지를 제공한다. |
| 사용자 소통 | 1:1 채팅 | 상품 구매자와 판매자 사이의 1:1 채팅을 제공한다. |
| 사용자 소통 | 전체 채팅 | 최소 구현에서는 제외한다. 시간이 남을 때 선택 기능으로 구현한다. |
| 악성 사용자 필터링 | 신고 | 상품 또는 사용자를 신고할 수 있다. |
| 악성 사용자 필터링 | 불량 상품 삭제/숨김 | 신고 누적 또는 관리자 조치로 상품을 숨김/삭제 상태로 변경한다. |
| 악성 사용자 필터링 | 불량 사용자 제한 | 신고 누적 또는 관리자 조치로 사용자를 제한/정지 상태로 변경한다. |
| 검색 | 상품 검색 | 상품명/설명 키워드 검색을 제공한다. |
| 관리자 | 전체 요소 관리 | 관리자가 사용자, 상품, 신고, 감사 로그를 조회/처리한다. |

### 1.2 선택 기능

아래 기능은 최소 요구사항 구현 후 시간이 남을 때 추가한다.

- 관심 상품
- CarrotCoin 모의지갑 상품 구매
- 실시간 WebSocket 채팅
- 알림 기능
- 고급 검색 및 정렬
- 실제 가상자산 네트워크 연동

단, 이미 이 문서에는 관심 상품과 CarrotCoin 모의지갑까지 구현 가능한 상세 명세를 포함한다. 시간이 부족하면 `Favorite`, `Wallet` 영역은 후순위로 미룬다.

### 1.3 CarrotCoin 모의지갑 구현 원칙

CarrotCoin은 실제 가상화폐가 아니라 시큐어코딩 학습용 모의 포인트이다. 실제 블록체인, 실제 코인, 외부 결제 API, 개인키, 지갑 주소 연동은 구현하지 않는다.

- CarrotCoin 잔액은 애플리케이션 DB 안에서만 관리한다.
- 회원가입 시 사용자별 모의지갑을 자동 생성한다.
- 신규 사용자의 초기 잔액은 `0` CarrotCoin이며, 관리자가 사용자별로 제한된 금액을 지급한다.
- 상품 가격 1원은 1 CarrotCoin으로 간주한다.
- 상품 구매 시 구매자 지갑 잔액을 차감하고 판매자 지갑 잔액을 증가시킨다.
- 거래 내역에는 실제 블록체인 트랜잭션 해시가 아니라 UUID 기반 `transactionHash`를 저장한다.
- 사용자는 타인의 지갑 잔액과 거래 내역을 조회할 수 없다.
- 잔액 부족, 본인 상품 구매, 중복 요청, 음수 잔액, 임의 금액 변조를 반드시 차단한다.
- 보고서에는 "실제 가상자산 결제 서비스가 아니라 DB 기반 모의지갑 시스템"이라고 명시한다.

### 1.4 최종 제출 조건을 고려한 구현 산출물

구현 완료 시 아래 산출물이 있어야 한다.

- GitHub public repository
- README.md
  - 프로젝트 소개
  - 기술 스택
  - 주요 기능
  - 보안 기능
  - 환경 설정
  - 실행 방법
  - 테스트 방법
  - 기본 관리자 계정 정보
- 개발 과정 보고서용 자료
  - 요구사항 분석 내용
  - 시스템 설계 내용
  - 구현 과정
  - 체크리스트 기반 테스트 결과
  - 개발 중 발견한 보안 약점
  - 보안 약점을 어떻게 수정했는지
  - 유지보수 계획

## 2. 고정 기술 스택

AI는 아래 기술 스택으로 구현한다.

- Backend: Java 21, Spring Boot 3.x
- View: Thymeleaf
- Security: Spring Security
- Database Access: Spring Data JPA
- Database: H2 개발용, MySQL 운영 전환 가능 구조
- Build Tool: Gradle
- Validation: Jakarta Bean Validation
- Password Hashing: BCrypt
- File Upload: Spring MultipartFile
- Test: JUnit 5, Spring Boot Test, MockMvc
- CSS: 기본 CSS 또는 Bootstrap 5

프론트엔드는 별도 React 프로젝트로 만들지 않는다. 수업용 구현 속도와 보안 검증을 위해 서버 렌더링 방식인 Thymeleaf를 사용한다.

## 3. 프로젝트 구조

다음 패키지 구조를 사용한다.

```text
src/main/java/com/usedcarrot
  UsedCarrotApplication.java
  config/
    SecurityConfig.java
    WebConfig.java
  common/
    BaseTimeEntity.java
    ErrorCode.java
    GlobalExceptionHandler.java
    AuditLogger.java
  auth/
    controller/AuthController.java
    dto/LoginRequest.java
    dto/RegisterRequest.java
    service/AuthService.java
  user/
    controller/UserController.java
    controller/AdminUserController.java
    domain/User.java
    domain/UserRole.java
    domain/UserStatus.java
    dto/ProfileUpdateRequest.java
    dto/PasswordChangeRequest.java
    repository/UserRepository.java
    service/UserService.java
  product/
    controller/ProductController.java
    controller/AdminProductController.java
    domain/Product.java
    domain/ProductImage.java
    domain/ProductStatus.java
    dto/ProductCreateRequest.java
    dto/ProductUpdateRequest.java
    dto/ProductSearchCondition.java
    repository/ProductRepository.java
    repository/ProductImageRepository.java
    service/ProductService.java
    service/FileStorageService.java
  favorite/
    domain/Favorite.java
    repository/FavoriteRepository.java
    service/FavoriteService.java
  chat/
    controller/ChatController.java
    domain/ChatRoom.java
    domain/Message.java
    dto/MessageCreateRequest.java
    repository/ChatRoomRepository.java
    repository/MessageRepository.java
    service/ChatService.java
  wallet/
    controller/WalletController.java
    controller/AdminWalletController.java
    domain/Wallet.java
    domain/WalletTransaction.java
    domain/WalletTransactionStatus.java
    domain/WalletTransactionType.java
    dto/WalletTransferRequest.java
    repository/WalletRepository.java
    repository/WalletTransactionRepository.java
    service/WalletService.java
  report/
    controller/ReportController.java
    controller/AdminReportController.java
    domain/Report.java
    domain/ReportStatus.java
    domain/ReportTargetType.java
    dto/ReportCreateRequest.java
    repository/ReportRepository.java
    service/ReportService.java
  audit/
    controller/AdminAuditLogController.java
    domain/AuditLog.java
    domain/AuditEventType.java
    repository/AuditLogRepository.java
```

정적 리소스와 템플릿은 다음 구조를 사용한다.

```text
src/main/resources
  application.yml
  static/
    css/style.css
    uploads/
  templates/
    layout.html
    index.html
    auth/login.html
    auth/register.html
    users/me.html
    products/list.html
    products/detail.html
    products/form.html
    chat/list.html
    chat/room.html
    wallets/detail.html
    wallets/transactions.html
    reports/form.html
    admin/dashboard.html
    admin/users.html
    admin/products.html
    admin/reports.html
    admin/audit-logs.html
```

## 4. 사용자 역할과 권한

### 4.1 역할

- `ROLE_USER`: 일반 회원
- `ROLE_ADMIN`: 관리자

### 4.2 사용자 상태

- `ACTIVE`: 정상
- `LIMITED`: 제한됨
- `SUSPENDED`: 정지됨
- `DELETED`: 탈퇴됨

### 4.3 권한 규칙

| 기능 | 비회원 | 일반 회원 | 제한 회원 | 정지 회원 | 관리자 |
| --- | --- | --- | --- | --- | --- |
| 상품 목록 조회 | 가능 | 가능 | 가능 | 가능 | 가능 |
| 상품 상세 조회 | 가능 | 가능 | 가능 | 가능 | 가능 |
| 회원가입 | 가능 | 불가 | 불가 | 불가 | 불가 |
| 로그인 | 가능 | 가능 | 가능 | 불가 | 가능 |
| 상품 등록 | 불가 | 가능 | 불가 | 불가 | 가능 |
| 본인 상품 수정/삭제 | 불가 | 가능 | 불가 | 불가 | 가능 |
| 채팅 전송 | 불가 | 가능 | 불가 | 불가 | 가능 |
| CarrotCoin 구매 | 불가 | 가능 | 불가 | 불가 | 가능 |
| 신고 등록 | 불가 | 가능 | 가능 | 불가 | 가능 |
| 관리자 화면 | 불가 | 불가 | 불가 | 불가 | 가능 |

## 5. 페이지 명세

### 5.1 기본 페이지

경로: `GET /`

표시 내용:

- 상단 네비게이션
  - 로고: UsedCarrot
  - 검색창
  - 로그인하지 않은 경우: 로그인, 회원가입
  - 로그인한 경우: 상품 등록, 채팅, 마이페이지, 로그아웃
  - 관리자 경우: 관리자 메뉴
- 최신 상품 목록
  - 상품 대표 이미지
  - 상품명
  - 가격
  - 거래 지역
  - 거래 상태
  - 등록 시간
- 카테고리 필터
- 검색 결과가 없으면 "등록된 상품이 없습니다" 표시

동작:

- 검색창 입력 후 제출하면 `/products?keyword=...`로 이동한다.
- 상품 카드를 클릭하면 `/products/{id}`로 이동한다.

보안:

- 상품명과 설명 요약은 HTML escaping 처리한다.
- 삭제, 숨김, 정지 판매자의 상품은 목록에 표시하지 않는다.

### 5.2 회원가입 페이지

경로:

- `GET /register`
- `POST /register`

입력 필드:

| 필드 | 타입 | 필수 | 검증 |
| --- | --- | --- | --- |
| email | email | 예 | 이메일 형식, 최대 100자, 중복 불가 |
| password | password | 예 | 8~64자, 영문/숫자/특수문자 포함 권장 |
| passwordConfirm | password | 예 | password와 일치 |
| nickname | text | 예 | 2~20자, 중복 불가 |
| region | text | 예 | 2~50자 |

성공:

- 비밀번호를 BCrypt로 해시한다.
- 기본 권한은 `ROLE_USER`로 저장한다.
- 기본 상태는 `ACTIVE`로 저장한다.
- 로그인 페이지로 이동하고 "회원가입이 완료되었습니다" 메시지를 표시한다.

실패:

- 이메일 중복: "이미 사용 중인 이메일입니다."
- 닉네임 중복: "이미 사용 중인 닉네임입니다."
- 비밀번호 불일치: "비밀번호가 일치하지 않습니다."
- 입력값 오류: 각 필드 아래에 오류 메시지를 표시한다.

보안:

- 비밀번호 원문을 로그에 남기지 않는다.
- 서버 측 검증을 반드시 수행한다.
- CSRF 보호를 활성화한다.

### 5.3 로그인 페이지

경로:

- `GET /login`
- `POST /login`

입력 필드:

| 필드 | 타입 | 필수 |
| --- | --- | --- |
| email | email | 예 |
| password | password | 예 |

성공:

- 홈 화면 `/`으로 이동한다.
- 감사 로그에 `LOGIN_SUCCESS`를 기록한다.

실패:

- "이메일 또는 비밀번호가 올바르지 않습니다." 표시
- 감사 로그에 `LOGIN_FAILURE`를 기록한다.
- 실패 횟수 5회 이상이면 5분간 로그인 제한을 적용할 수 있다.

보안:

- 이메일 존재 여부를 노출하지 않는다.
- 탈퇴 또는 정지 계정은 로그인할 수 없다.
- 세션 고정 공격 방지를 위해 로그인 성공 시 세션을 재발급한다.

### 5.4 마이페이지

경로:

- `GET /users/me`
- `POST /users/me/profile`
- `POST /users/me/password`

표시 내용:

- 이메일
- 닉네임
- 지역
- 소개글
- 가입일
- 내가 등록한 상품 링크
- 관심 상품 링크
- 채팅 목록 링크
- CarrotCoin 지갑 링크

수정 가능:

- 닉네임
- 지역
- 소개글
- 비밀번호

검증:

- 닉네임은 2~20자, 중복 불가
- 소개글은 최대 200자
- 비밀번호 변경 시 현재 비밀번호가 맞아야 함
- 새 비밀번호와 새 비밀번호 확인이 일치해야 함

보안:

- 본인만 접근 가능
- 이메일은 수정하지 않음
- 비밀번호 변경 성공 시 감사 로그 `PASSWORD_CHANGED` 기록

### 5.5 상품 목록 페이지

경로: `GET /products`

쿼리 파라미터:

| 이름 | 설명 | 예시 |
| --- | --- | --- |
| keyword | 검색어 | 아이폰 |
| category | 카테고리 | ELECTRONICS |
| status | 거래 상태 | ON_SALE |
| region | 지역 | 서울 |
| page | 페이지 번호 | 0 |
| size | 페이지 크기 | 20 |

표시 내용:

- 상품 카드 목록
- 각 상품의 대표 이미지
- 상품명
- 가격
- 지역
- 거래 상태
- 등록 시간
- 페이지네이션

규칙:

- 기본 정렬은 최신순
- `DELETED`, `HIDDEN`, `SOLD` 상품은 기본 목록에서 제외
- 검색어 최대 50자
- 페이지 크기 최대 50개

### 5.6 새 상품 등록 페이지

경로:

- `GET /products/new`
- `POST /products`

입력 필드:

| 필드 | 타입 | 필수 | 검증 |
| --- | --- | --- | --- |
| title | text | 예 | 2~80자 |
| price | number | 예 | 0~100000000 |
| category | select | 예 | 허용된 카테고리 |
| region | text | 예 | 2~50자 |
| description | textarea | 예 | 10~2000자 |
| images | file | 아니오 | 이미지, 최대 5장, 각 5MB 이하 |

성공:

- 상품 상태는 `ON_SALE`로 저장한다.
- 대표 이미지는 첫 번째 이미지로 사용한다.
- 상세 페이지 `/products/{id}`로 이동한다.

실패:

- 입력값 오류를 폼에 표시한다.
- 파일 검증 실패 시 저장하지 않는다.

보안:

- 로그인 사용자만 접근 가능
- `LIMITED`, `SUSPENDED` 사용자는 등록 불가
- 파일 확장자, MIME 타입, 크기 검증
- 파일명은 UUID로 변경
- 상품명과 설명은 출력 시 escape

### 5.7 상품 상세 페이지

경로: `GET /products/{id}`

표시 내용:

- 상품 이미지 목록
- 상품명
- 가격
- 설명
- 카테고리
- 거래 지역
- 거래 상태
- 판매자 닉네임
- 판매자 프로필 링크
- 조회수
- 등록일

버튼:

- 비회원: 로그인 안내
- 일반 회원: 관심 등록, 판매자에게 문의, 신고, CarrotCoin 구매
- 판매자 본인: 수정, 삭제, 상태 변경
- 관리자: 숨김 처리, 삭제, 판매자 제한

규칙:

- 본인 상품에는 CarrotCoin 구매 버튼을 표시하지 않는다.
- `SOLD`, `DELETED`, `HIDDEN` 상품에는 신규 채팅과 CarrotCoin 구매를 제한한다.
- 상품 조회 시 조회수를 1 증가시킨다.

### 5.8 상품 수정 페이지

경로:

- `GET /products/{id}/edit`
- `POST /products/{id}/edit`

권한:

- 판매자 본인 또는 관리자만 가능

수정 가능:

- 상품명
- 가격
- 카테고리
- 지역
- 설명
- 이미지 추가/삭제
- 거래 상태

보안:

- 서버에서 반드시 상품 소유자 확인
- URL의 상품 ID만으로 수정 허용 금지
- 권한 없으면 403 처리 및 감사 로그 `ACCESS_DENIED` 기록

### 5.9 채팅 목록 페이지

경로: `GET /chat`

표시 내용:

- 내가 참여한 채팅방 목록
- 상대방 닉네임
- 상품명
- 상품 대표 이미지
- 마지막 메시지
- 마지막 메시지 시간
- 읽지 않은 메시지 수

규칙:

- 로그인 사용자만 접근 가능
- 내 채팅방만 조회 가능

### 5.10 채팅방 페이지

경로:

- `GET /chat/{roomId}`
- `POST /chat/{roomId}/messages`

표시 내용:

- 상품 요약
- 상대방 닉네임
- 메시지 목록
- 메시지 입력창
- CarrotCoin 구매 버튼
- 사용자 신고 버튼

메시지 입력 검증:

- 1~1000자
- 빈 메시지 불가
- HTML escape 처리

규칙:

- 채팅방 참여자만 조회 가능
- 채팅방 참여자만 메시지 전송 가능
- 정지 또는 제한 사용자는 메시지 전송 불가

### 5.11 신고 페이지

경로:

- `GET /reports/new?targetType=PRODUCT&targetId=1`
- `POST /reports`

입력 필드:

| 필드 | 타입 | 필수 | 검증 |
| --- | --- | --- | --- |
| targetType | hidden/select | 예 | PRODUCT 또는 USER |
| targetId | hidden | 예 | 존재하는 대상 |
| reason | select | 예 | 허용된 신고 사유 |
| detail | textarea | 아니오 | 최대 1000자 |

신고 사유:

- `FRAUD`: 사기 의심
- `PROHIBITED_ITEM`: 금지 물품
- `ABUSE`: 욕설/비방
- `SPAM`: 스팸
- `FAKE_INFO`: 허위 정보
- `OTHER`: 기타

규칙:

- 로그인 사용자만 신고 가능
- 자기 자신 신고 불가
- 본인 상품 신고 불가
- 같은 사용자가 같은 대상을 24시간 내 중복 신고 불가
- 신고 3회 이상 누적된 상품은 자동 `HIDDEN`
- 신고 5회 이상 누적된 사용자는 자동 `LIMITED`
- 신고 10회 이상 누적된 사용자는 자동 `SUSPENDED`

보안:

- 신고 상세 내용은 escape 처리
- 신고 처리 결과는 일반 사용자에게 관리자 내부 정보까지 노출하지 않음

### 5.12 CarrotCoin 모의지갑 페이지/기능

경로:

- `GET /wallet`
- `GET /wallet/transactions`
- `POST /wallet/transfers`

표시 내용:

- 내 CarrotCoin 잔액
- 최근 거래 내역
- 거래 유형
- 거래 상태
- 거래 해시
- 관련 상품
- 상대방 닉네임
- 거래 시간

CarrotCoin 구매 입력:

| 필드 | 타입 | 필수 | 검증 |
| --- | --- | --- | --- |
| productId | hidden | 예 | 존재하는 상품 |
| idempotencyKey | hidden | 예 | UUID |

서버 처리:

- 상품 ID로 상품을 조회한다.
- 현재 로그인 사용자를 구매자로 사용한다.
- 상품 판매자를 판매자로 사용한다.
- 구매 금액은 클라이언트에서 받지 않고 서버의 상품 가격을 사용한다.
- 구매자 지갑을 조회한다.
- 판매자 지갑을 조회한다.
- 구매자 지갑 잔액이 상품 가격보다 작으면 거부한다.
- 구매자와 판매자가 같으면 거부한다.
- 상품 상태가 `ON_SALE` 또는 `RESERVED`가 아니면 거부한다.
- 같은 `idempotencyKey`가 이미 있으면 기존 거래 내역을 반환한다.
- 구매자 지갑 잔액을 상품 가격만큼 차감한다.
- 판매자 지갑 잔액을 상품 가격만큼 증가시킨다.
- `transactionHash`는 UUID로 생성한다.
- 거래 상태는 `COMPLETED`로 저장한다.
- 상품 상태는 `SOLD`로 변경한다.

거래 유형:

- `INITIAL_GRANT`: 회원가입 초기 지급
- `PRODUCT_PURCHASE`: 상품 구매
- `REFUND`: 환불
- `ADMIN_ADJUSTMENT`: 관리자 조정

거래 상태:

- `COMPLETED`: 완료
- `FAILED`: 실패
- `CANCELED`: 취소

보안:

- 구매 금액을 클라이언트에서 받지 않는다.
- 지갑 잔액이 음수가 되지 않도록 트랜잭션 안에서 검증한다.
- 구매자 지갑 차감과 판매자 지갑 증가는 하나의 DB 트랜잭션으로 처리한다.
- 중복 요청 방지를 위해 `idempotencyKey`에 unique 제약조건을 건다.
- 타인의 지갑 잔액과 거래 내역 조회 불가
- 일반 사용자가 거래 상태를 임의 변경할 수 없음
- 실제 가상화폐 개인키, 지갑 주소, 외부 API 키를 저장하지 않음

## 6. 관리자 페이지 명세

### 6.1 관리자 대시보드

경로: `GET /admin`

표시 내용:

- 전체 사용자 수
- 활성 사용자 수
- 제한/정지 사용자 수
- 전체 상품 수
- 숨김 상품 수
- 미처리 신고 수
- 최근 감사 로그 20개

권한:

- `ROLE_ADMIN`만 접근 가능

### 6.2 사용자 관리

경로:

- `GET /admin/users`
- `POST /admin/users/{id}/status`

기능:

- 사용자 목록 조회
- 이메일/닉네임 검색
- 상태별 필터
- 사용자 상태 변경
  - `ACTIVE`
  - `LIMITED`
  - `SUSPENDED`

주의:

- 관리자는 사용자 비밀번호를 볼 수 없다.
- 관리자는 본인 계정을 정지할 수 없다.
- 상태 변경 시 감사 로그 `USER_STATUS_CHANGED` 기록

### 6.3 상품 관리

경로:

- `GET /admin/products`
- `POST /admin/products/{id}/status`

기능:

- 전체 상품 조회
- 신고된 상품 우선 조회
- 상품 상태 변경
  - `ON_SALE`
  - `RESERVED`
  - `SOLD`
  - `HIDDEN`
  - `DELETED`

주의:

- 상품 삭제는 물리 삭제가 아니라 상태를 `DELETED`로 변경한다.
- 상품 숨김/삭제 시 감사 로그 `PRODUCT_STATUS_CHANGED` 기록

### 6.4 신고 관리

경로:

- `GET /admin/reports`
- `GET /admin/reports/{id}`
- `POST /admin/reports/{id}/process`

기능:

- 신고 목록 조회
- 신고 대상 유형별 필터
- 처리 상태별 필터
- 신고 상세 확인
- 처리 상태 변경
  - `RECEIVED`
  - `IN_PROGRESS`
  - `RESOLVED`
  - `REJECTED`

처리 시 입력:

- 처리 결과 메모
- 상품 숨김 여부
- 사용자 제한 여부

### 6.5 감사 로그 관리

경로: `GET /admin/audit-logs`

표시 내용:

- 발생 시간
- 이벤트 유형
- 사용자 ID
- IP 주소
- 결과
- 상세 내용

필터:

- 이벤트 유형
- 사용자 ID
- 날짜 범위

민감정보 금지:

- 비밀번호
- 세션 ID
- 인증 토큰
- 실제 가상자산 개인키
- 실제 지갑 주소

## 7. 데이터베이스 상세 명세

### 7.1 users

```sql
CREATE TABLE users (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  email VARCHAR(100) NOT NULL UNIQUE,
  password_hash VARCHAR(255) NOT NULL,
  nickname VARCHAR(20) NOT NULL UNIQUE,
  region VARCHAR(50) NOT NULL,
  bio VARCHAR(200),
  role VARCHAR(20) NOT NULL,
  status VARCHAR(20) NOT NULL,
  login_fail_count INT NOT NULL DEFAULT 0,
  locked_until TIMESTAMP NULL,
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP NOT NULL
);
```

### 7.2 products

```sql
CREATE TABLE products (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  seller_id BIGINT NOT NULL,
  title VARCHAR(80) NOT NULL,
  description VARCHAR(2000) NOT NULL,
  price INT NOT NULL,
  category VARCHAR(50) NOT NULL,
  region VARCHAR(50) NOT NULL,
  status VARCHAR(20) NOT NULL,
  view_count INT NOT NULL DEFAULT 0,
  report_count INT NOT NULL DEFAULT 0,
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP NOT NULL,
  CONSTRAINT fk_products_seller FOREIGN KEY (seller_id) REFERENCES users(id)
);
```

### 7.3 product_images

```sql
CREATE TABLE product_images (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  product_id BIGINT NOT NULL,
  original_file_name VARCHAR(255) NOT NULL,
  stored_file_name VARCHAR(255) NOT NULL,
  mime_type VARCHAR(100) NOT NULL,
  file_size BIGINT NOT NULL,
  path VARCHAR(500) NOT NULL,
  created_at TIMESTAMP NOT NULL,
  CONSTRAINT fk_product_images_product FOREIGN KEY (product_id) REFERENCES products(id)
);
```

### 7.4 favorites

```sql
CREATE TABLE favorites (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  product_id BIGINT NOT NULL,
  created_at TIMESTAMP NOT NULL,
  CONSTRAINT uq_favorites_user_product UNIQUE (user_id, product_id),
  CONSTRAINT fk_favorites_user FOREIGN KEY (user_id) REFERENCES users(id),
  CONSTRAINT fk_favorites_product FOREIGN KEY (product_id) REFERENCES products(id)
);
```

### 7.5 chat_rooms

```sql
CREATE TABLE chat_rooms (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  product_id BIGINT NOT NULL,
  buyer_id BIGINT NOT NULL,
  seller_id BIGINT NOT NULL,
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP NOT NULL,
  CONSTRAINT uq_chat_room UNIQUE (product_id, buyer_id, seller_id),
  CONSTRAINT fk_chat_rooms_product FOREIGN KEY (product_id) REFERENCES products(id),
  CONSTRAINT fk_chat_rooms_buyer FOREIGN KEY (buyer_id) REFERENCES users(id),
  CONSTRAINT fk_chat_rooms_seller FOREIGN KEY (seller_id) REFERENCES users(id)
);
```

### 7.6 messages

```sql
CREATE TABLE messages (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  chat_room_id BIGINT NOT NULL,
  sender_id BIGINT NOT NULL,
  content VARCHAR(1000) NOT NULL,
  is_read BOOLEAN NOT NULL DEFAULT FALSE,
  created_at TIMESTAMP NOT NULL,
  CONSTRAINT fk_messages_chat_room FOREIGN KEY (chat_room_id) REFERENCES chat_rooms(id),
  CONSTRAINT fk_messages_sender FOREIGN KEY (sender_id) REFERENCES users(id)
);
```

### 7.7 wallets

```sql
CREATE TABLE wallets (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL UNIQUE,
  balance BIGINT NOT NULL DEFAULT 0,
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP NOT NULL,
  CONSTRAINT fk_wallets_user FOREIGN KEY (user_id) REFERENCES users(id),
  CONSTRAINT chk_wallets_balance CHECK (balance >= 0)
);
```

### 7.8 wallet_transactions

```sql
CREATE TABLE wallet_transactions (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  product_id BIGINT,
  buyer_id BIGINT,
  seller_id BIGINT,
  from_wallet_id BIGINT,
  to_wallet_id BIGINT,
  amount BIGINT NOT NULL,
  type VARCHAR(30) NOT NULL,
  status VARCHAR(20) NOT NULL,
  transaction_hash VARCHAR(100) NOT NULL UNIQUE,
  idempotency_key VARCHAR(100) NOT NULL UNIQUE,
  failure_reason VARCHAR(500),
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP NOT NULL,
  CONSTRAINT fk_wallet_tx_product FOREIGN KEY (product_id) REFERENCES products(id),
  CONSTRAINT fk_wallet_tx_buyer FOREIGN KEY (buyer_id) REFERENCES users(id),
  CONSTRAINT fk_wallet_tx_seller FOREIGN KEY (seller_id) REFERENCES users(id),
  CONSTRAINT fk_wallet_tx_from_wallet FOREIGN KEY (from_wallet_id) REFERENCES wallets(id),
  CONSTRAINT fk_wallet_tx_to_wallet FOREIGN KEY (to_wallet_id) REFERENCES wallets(id),
  CONSTRAINT chk_wallet_tx_amount CHECK (amount > 0)
);
```

### 7.9 reports

```sql
CREATE TABLE reports (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  reporter_id BIGINT NOT NULL,
  target_type VARCHAR(20) NOT NULL,
  target_id BIGINT NOT NULL,
  reason VARCHAR(50) NOT NULL,
  detail VARCHAR(1000),
  status VARCHAR(20) NOT NULL,
  admin_memo VARCHAR(1000),
  handled_by BIGINT,
  handled_at TIMESTAMP,
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP NOT NULL,
  CONSTRAINT fk_reports_reporter FOREIGN KEY (reporter_id) REFERENCES users(id),
  CONSTRAINT fk_reports_handler FOREIGN KEY (handled_by) REFERENCES users(id)
);
```

### 7.10 audit_logs

```sql
CREATE TABLE audit_logs (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT,
  event_type VARCHAR(50) NOT NULL,
  ip_address VARCHAR(50),
  result VARCHAR(20) NOT NULL,
  detail VARCHAR(1000),
  created_at TIMESTAMP NOT NULL,
  CONSTRAINT fk_audit_logs_user FOREIGN KEY (user_id) REFERENCES users(id)
);
```

## 8. Enum 명세

```java
enum UserRole {
    ROLE_USER, ROLE_ADMIN
}

enum UserStatus {
    ACTIVE, LIMITED, SUSPENDED, DELETED
}

enum ProductStatus {
    ON_SALE, RESERVED, SOLD, HIDDEN, DELETED
}

enum WalletTransactionType {
    INITIAL_GRANT, PRODUCT_PURCHASE, REFUND, ADMIN_ADJUSTMENT
}

enum WalletTransactionStatus {
    COMPLETED, CANCELED, FAILED
}

enum ReportTargetType {
    PRODUCT, USER
}

enum ReportStatus {
    RECEIVED, IN_PROGRESS, RESOLVED, REJECTED
}

enum AuditEventType {
    REGISTER,
    LOGIN_SUCCESS,
    LOGIN_FAILURE,
    LOGOUT,
    PASSWORD_CHANGED,
    PRODUCT_CREATED,
    PRODUCT_UPDATED,
    PRODUCT_DELETED,
    PRODUCT_STATUS_CHANGED,
    MESSAGE_SENT,
    WALLET_CREATED,
    WALLET_INITIAL_GRANTED,
    CARROTCOIN_TRANSFER_COMPLETED,
    CARROTCOIN_TRANSFER_FAILED,
    CARROTCOIN_REFUNDED,
    WALLET_ADMIN_ADJUSTED,
    REPORT_CREATED,
    REPORT_PROCESSED,
    USER_STATUS_CHANGED,
    ACCESS_DENIED,
    FILE_UPLOAD_REJECTED
}
```

## 9. API 상세 명세

### 9.1 회원가입

`POST /register`

요청:

```json
{
  "email": "user@example.com",
  "password": "Password123!",
  "passwordConfirm": "Password123!",
  "nickname": "당근유저",
  "region": "서울 강남구"
}
```

처리:

- 이메일 형식 검증
- 비밀번호와 비밀번호 확인 일치 검증
- 이메일 중복 검증
- 닉네임 중복 검증
- 비밀번호 BCrypt 해시
- 사용자 저장

### 9.2 상품 등록

`POST /products`

요청 타입: `multipart/form-data`

필드:

- title
- price
- category
- region
- description
- images

처리:

- 로그인 확인
- 사용자 상태 확인
- 입력값 검증
- 파일 검증
- 상품 저장
- 이미지 저장
- 감사 로그 기록

### 9.3 채팅방 생성

`POST /chat-rooms`

요청:

```json
{
  "productId": 1
}
```

처리:

- 로그인 확인
- 상품 존재 확인
- 상품 판매자와 현재 사용자가 같은지 확인
- 기존 채팅방 존재 시 기존 채팅방 반환
- 없으면 새 채팅방 생성

### 9.4 메시지 전송

`POST /chat/{roomId}/messages`

요청:

```json
{
  "content": "아직 판매 중인가요?"
}
```

처리:

- 채팅방 참여자 확인
- 사용자 상태 확인
- 메시지 길이 검증
- 메시지 저장
- 감사 로그 기록

### 9.5 신고 등록

`POST /reports`

요청:

```json
{
  "targetType": "PRODUCT",
  "targetId": 1,
  "reason": "FRAUD",
  "detail": "입금 유도 후 답장이 없습니다."
}
```

처리:

- 로그인 확인
- 대상 존재 확인
- 자기 자신 또는 본인 상품 신고 여부 확인
- 24시간 내 중복 신고 확인
- 신고 저장
- 대상 신고 수 증가
- 상품 신고 3회 이상이면 상품 `HIDDEN`
- 사용자 신고 5회 이상이면 사용자 `LIMITED`
- 사용자 신고 10회 이상이면 사용자 `SUSPENDED`
- 감사 로그 기록

### 9.6 CarrotCoin 모의지갑 상품 구매

`POST /wallet/transfers`

요청:

```json
{
  "productId": 1,
  "idempotencyKey": "c6153b2e-4bc8-4dc7-8a6e-15b46e2d2f99"
}
```

처리:

- 로그인 확인
- 사용자 상태 확인
- 상품 존재 확인
- 상품 상태 확인
- 판매자와 구매자가 같은지 확인
- 구매자 지갑과 판매자 지갑 조회
- 금액은 클라이언트 입력값이 아니라 상품 가격으로 설정
- 구매자 잔액이 상품 가격보다 크거나 같은지 확인
- 같은 idempotencyKey가 있으면 기존 거래 내역 반환
- 하나의 DB 트랜잭션 안에서 구매자 잔액 차감과 판매자 잔액 증가 처리
- `WalletTransaction`을 `PRODUCT_PURCHASE`, `COMPLETED` 상태로 저장
- `transactionHash`는 UUID로 생성
- 상품 상태를 `SOLD`로 변경
- 감사 로그 기록

## 10. 보안 구현 체크리스트

### 10.1 인증

- Spring Security 사용
- 비밀번호 BCrypt 저장
- 로그인 실패 메시지 일반화
- 정지/탈퇴 계정 로그인 차단
- 로그인 성공 시 세션 재발급
- 로그아웃 시 세션 무효화

### 10.2 인가

- 상품 수정/삭제 시 판매자 본인 확인
- 채팅방 조회 시 참여자 확인
- 메시지 전송 시 참여자 확인
- 지갑 조회 시 지갑 소유자/관리자 확인
- 거래 내역 조회 시 구매자/판매자/관리자 확인
- 관리자 URL `/admin/**`는 `ROLE_ADMIN`만 접근

### 10.3 입력값 검증

- DTO에 `@NotBlank`, `@Size`, `@Email`, `@Min`, `@Max` 사용
- 서버 측 검증 필수
- 컨트롤러에서 `BindingResult`로 오류 처리
- 허용되지 않는 enum 값 거부

### 10.4 XSS 방어

- Thymeleaf의 `th:text` 사용
- 사용자 입력값 출력 시 `th:utext` 사용 금지
- 상품명, 설명, 닉네임, 메시지, 신고 상세는 HTML escape

### 10.5 SQL Injection 방어

- JPA Repository 또는 QueryDSL 사용
- 문자열 연결 방식의 JPQL/SQL 생성 금지
- 검색 조건은 파라미터 바인딩 사용

### 10.6 파일 업로드 보안

- 허용 확장자: `.jpg`, `.jpeg`, `.png`, `.webp`
- 허용 MIME: `image/jpeg`, `image/png`, `image/webp`
- 파일 크기: 5MB 이하
- 상품당 최대 5장
- 저장 파일명은 UUID 사용
- 업로드 실패 로그 기록
- 업로드 경로는 application.yml에서 관리
- 업로드 파일은 실행 경로에 두지 않음

### 10.7 로그 보안

로그에 남기면 안 되는 값:

- 비밀번호
- 비밀번호 확인
- 세션 ID 전체
- CSRF 토큰
- 실제 가상자산 개인키
- 실제 지갑 주소
- 외부 결제 또는 거래소 API 키

반드시 남겨야 하는 이벤트:

- 회원가입
- 로그인 성공
- 로그인 실패
- 비밀번호 변경
- 상품 등록/수정/삭제
- 권한 오류
- 신고 등록
- 신고 처리
- 사용자 상태 변경
- 상품 상태 변경
- CarrotCoin 구매 성공/실패
- 지갑 관리자 조정

## 11. 테스트 케이스

### 11.1 인증 테스트

- 정상 회원가입 성공
- 중복 이메일 회원가입 실패
- 중복 닉네임 회원가입 실패
- 비밀번호 불일치 실패
- 정상 로그인 성공
- 틀린 비밀번호 로그인 실패
- 정지 계정 로그인 실패
- 로그아웃 후 인증 필요 페이지 접근 실패

### 11.2 상품 테스트

- 로그인 사용자의 상품 등록 성공
- 비회원 상품 등록 실패
- 필수값 누락 상품 등록 실패
- 잘못된 가격 상품 등록 실패
- 상품 목록 조회 성공
- 상품 상세 조회 성공
- 판매자 본인 상품 수정 성공
- 타인 상품 수정 실패
- 판매자 본인 상품 삭제 성공
- 삭제 상품 목록 미노출

### 11.3 파일 업로드 테스트

- jpg 업로드 성공
- png 업로드 성공
- exe 업로드 실패
- 확장자만 jpg인 비이미지 파일 실패
- 5MB 초과 파일 실패
- 5장 초과 업로드 실패

### 11.4 채팅 테스트

- 상품 구매자의 채팅방 생성 성공
- 판매자가 본인 상품에 채팅방 생성 실패
- 채팅 참여자 메시지 전송 성공
- 비참여자 채팅방 조회 실패
- 비참여자 메시지 전송 실패
- 스크립트 포함 메시지 저장 후 화면에서 실행되지 않음

### 11.5 신고 테스트

- 상품 신고 성공
- 사용자 신고 성공
- 본인 상품 신고 실패
- 자기 자신 신고 실패
- 24시간 내 중복 신고 실패
- 상품 신고 3회 누적 시 숨김 처리
- 사용자 신고 5회 누적 시 제한 처리
- 사용자 신고 10회 누적 시 정지 처리

### 11.6 CarrotCoin 모의지갑 테스트

- 회원가입 시 모의지갑 자동 생성 성공
- 신규 사용자 초기 잔액 0 CarrotCoin 지갑 생성 성공
- 관리자 CC 지급 후 상품 구매 성공
- 정상 상품 구매 시 구매자 잔액 차감 성공
- 정상 상품 구매 시 판매자 잔액 증가 성공
- 본인 상품 CarrotCoin 구매 실패
- 판매완료 상품 CarrotCoin 구매 실패
- 잔액 부족 시 구매 실패
- 클라이언트 금액 변조 불가능
- 같은 idempotencyKey 재요청 시 중복 거래 생성 안 됨
- 타인 지갑 잔액 조회 실패
- 타인 거래 내역 조회 실패
- 일반 사용자의 거래 상태 임의 변경 실패

### 11.7 관리자 테스트

- 일반 사용자 관리자 페이지 접근 실패
- 관리자 사용자 목록 조회 성공
- 관리자 사용자 상태 변경 성공
- 관리자 상품 숨김 처리 성공
- 관리자 신고 처리 성공
- 관리자 감사 로그 조회 성공

### 11.8 보안 공격 테스트

- 상품명에 `<script>alert(1)</script>` 입력 후 실행되지 않아야 함
- 메시지에 `<img src=x onerror=alert(1)>` 입력 후 실행되지 않아야 함
- 검색어에 `' OR '1'='1` 입력해도 SQL 오류 또는 인증 우회 없어야 함
- 상품 수정 URL의 ID를 타인 상품으로 바꿔도 수정 실패해야 함
- 관리자 API를 일반 사용자로 호출하면 403이어야 함
- 로그에 비밀번호 원문이 없어야 함

## 12. 구현 순서

AI는 아래 순서대로 구현한다.

1. Spring Boot 프로젝트 생성
2. Gradle 의존성 추가
3. application.yml 설정
4. 공통 BaseTimeEntity 생성
5. User 엔티티와 Repository 생성
6. Spring Security 설정
7. 회원가입, 로그인, 로그아웃 구현
8. 마이페이지와 비밀번호 변경 구현
9. Product, ProductImage 엔티티 구현
10. 상품 등록, 목록, 상세, 수정, 삭제 구현
11. 파일 업로드 검증과 저장 구현
12. 상품 검색과 필터 구현
13. Favorite 구현
14. ChatRoom, Message 구현
15. 채팅방 목록, 채팅방 상세, 메시지 전송 구현
16. Report 구현
17. 신고 누적에 따른 상품 숨김/사용자 제한 구현
18. Wallet, WalletTransaction 구현
19. CarrotCoin 모의지갑 잔액 조회, 상품 구매, 거래 내역 구현
20. AuditLog 구현
21. 관리자 대시보드 구현
22. 관리자 사용자/상품/신고/로그 관리 구현
23. 테스트 코드 작성
24. XSS, SQL Injection, 권한 우회, 파일 업로드 보안 점검
25. README 실행 방법 작성
26. GitHub 공개 저장소 기준으로 불필요한 비밀값, 로컬 경로, 개인정보가 포함되지 않았는지 확인
27. 개발 중 발견한 보안 약점과 수정 내용을 `docs/SECURITY_FIX_LOG.md`에 기록
28. 최종 보고서 작성을 위해 `docs/PROJECT_REPORT_GUIDE.md`의 목차에 맞춰 근거 자료 정리

## 13. AI에게 줄 최종 프롬프트

아래 프롬프트를 AI 개발 도구에 붙여넣어 구현을 요청한다.

```text
Spring Boot 3, Java 21, Thymeleaf, Spring Security, Spring Data JPA, H2 DB를 사용해서 UsedCarrot 중고거래 플랫폼을 구현해줘.

프로젝트는 시큐어코딩 수업용이므로 기능 구현뿐 아니라 보안 요구사항을 반드시 반영해야 해.

구현해야 할 핵심 기능:
1. 회원가입, 로그인, 로그아웃
2. 마이페이지, 프로필 수정, 비밀번호 변경
3. 상품 등록, 목록, 상세, 수정, 삭제
4. 상품 검색과 카테고리/상태 필터
5. 이미지 업로드: jpg, jpeg, png, webp만 허용, 5MB 이하, 최대 5장, UUID 파일명 저장
6. 관심 상품 등록/취소
7. 상품별 1:1 채팅방 생성, 메시지 전송, 채팅 목록
8. 상품/사용자 신고
9. 신고 3회 이상 상품 자동 숨김, 사용자 신고 5회 이상 제한, 10회 이상 정지
10. CarrotCoin 모의지갑: 실제 가상자산 네트워크 연동 없이 DB 내부 지갑 잔액으로 상품 구매 구현, idempotencyKey로 중복 방지
11. 관리자 페이지: 사용자 관리, 상품 관리, 신고 처리, 감사 로그 조회
12. 감사 로그: 로그인 성공/실패, 권한 오류, 상품 변경, 신고, CarrotCoin 거래, 관리자 조치 기록

보안 요구사항:
- 비밀번호는 BCrypt로 저장
- 사용자 입력값은 서버에서 검증
- Thymeleaf에서는 사용자 입력값을 th:text로 출력하고 th:utext 사용 금지
- JPA 파라미터 바인딩을 사용하고 문자열 연결 SQL 금지
- 타인 상품 수정/삭제 불가
- 채팅방 참여자만 메시지 조회 가능
- 지갑 소유자만 잔액 조회 가능
- CarrotCoin 거래 참여자만 거래 내역 조회 가능
- /admin/** 는 ROLE_ADMIN만 접근 가능
- 비밀번호, 세션 ID, 토큰, 실제 가상자산 개인키, 외부 API 키는 로그에 남기지 말 것

패키지 구조, DB 테이블, 페이지 경로, API, 테스트 케이스는 docs/AI_IMPLEMENTATION_SPEC.md의 내용을 기준으로 구현해줘.
```
