# UsedCarrot 개발 보고서

| 항목 | 내용 |
| --- | --- |
| 프로젝트 | Tiny Second-hand Shopping Platform — **UsedCarrot** |
| 작성 | 박도현 (WHS 4기 01반) |
| 제출 파일명 | `[WHS][secure-coding][01반]박도현(3206).pdf` |
| GitHub | https://github.com/no-carve-only-pizza/usedcarrot |
| 로컬 실행 | `http://localhost:8081` (`local` 프로필) |
| 기한 | 2026-07-23 23:59 LMS |

---

## 1. 과제 요약

### 1.1 목적

중고거래 플랫폼의 최소 요구사항을 만족하는 웹 애플리케이션을 만들고,  
**요구사항 분석 → 시스템 설계 → 구현 → 체크리스트/테스트 → 유지보수**까지 전 과정을 기록한다.  
개발 중 발견한 보안 약점과 수정 내용도 함께 정리한다.

AI(Cursor 등)를 활용해 구현·검토했고, 결제·인가·업로드 등 보안 경계는 코드와 실제 화면으로 다시 확인했다.

### 1.2 기술 스택

| 구분 | 기술 |
| --- | --- |
| Backend | Java 21, Spring Boot 3, Gradle |
| View | Thymeleaf (서버 렌더링) |
| Security | Spring Security, BCrypt, CSRF, 세션 |
| Persistence | Spring Data JPA, H2 |
| 결제 | Ethereum **Sepolia** + **MetaMask** + **Web3j** RPC 검증 |
| Test | JUnit 5, Spring Boot Test, MockMvc |

### 1.3 핵심 설계 결정

슬라이드의 “유저 간 송금”을 DB 포인트(CarrotCoin)가 아니라 **테스트넷 ETH 실제 전송**으로 구현했다.

| 결정 | 내용 |
| --- | --- |
| 결제 | MetaMask로 판매자 주소에 Sepolia ETH 전송 → 서버가 영수증 검증 후 `SOLD` |
| 서버 역할 | ETH를 보관하지 않음. `wallets`/`wallet_transactions`는 **결제 기록·감사**용 |
| 채팅 | 전체 공개 채팅 제외, 상품별 1:1만 구현 (악용·스팸 위험) |
| H2 콘솔 | 기본 비활성 |

---

## 2. 요구사항 분석

### 2.1 최소 요구사항 대응표

| 슬라이드 요구 | 구현 | 패키지/경로 |
| --- | --- | --- |
| 가입·로그인·마이페이지 | 이메일 가입, 세션 로그인, 프로필/비번/지갑/탈퇴 | `auth`, `user` |
| 상품 등록·조회·관리 | CRUD, 이미지, 내 상품, 검색·필터 | `product` |
| 사용자 소통 | 상품별 1:1 채팅 | `chat` |
| 악성 유저·상품 차단 | 신고 + 관리자 승인 후 누적 제재 | `report` |
| 유저 간 송금 | Sepolia ETH + MetaMask | `wallet`, `crypto` |
| 상품 검색 | 키워드·카테고리·지역·상태 | `/products` |
| 관리자 | 사용자/상품/신고/결제내역/감사로그 | `/admin/**` |

### 2.2 범위 조정 근거

- **전체 채팅 제외**: 최소 요구의 핵심은 거래 상대와의 소통이고, 전체 채팅은 스팸·악용 면이 크다.
- **온체인 결제 채택**: “송금”을 모의 잔액 차감이 아니라 체인 위 전송으로 맞춰, 서버 잔액 조작·다계정 인플레이션 문제를 구조적으로 줄였다.
- **신고 ≠ 즉시 제재**: 다계정 허위 신고로 자동 숨김/정지가 되지 않도록, 관리자 `RESOLVED`만 누적한다.

---

## 3. 시스템 설계

### 3.1 아키텍처

```mermaid
flowchart LR
  U["Browser + MetaMask"] --> C["Spring MVC / Thymeleaf"]
  C --> Sec["Spring Security"]
  C --> S["Service"]
  S --> DB["JPA / H2"]
  S --> E["EthereumService / Web3j"]
  E --> RPC["Sepolia RPC"]
  S --> A["AuditLogger"]
```

컨트롤러는 화면·입력만 받고, **권한·가격·결제 검증은 서비스 계층**에서 처리한다.  
구매 금액은 클라이언트 입력이 아니라 서버에 저장된 wei 가격과 온체인 `value`를 대조한다.

### 3.2 온체인 결제 시퀀스

1. 판매자·구매자: 마이페이지에서 MetaMask `personal_sign`으로 지갑 연결 (세션 nonce, 10분, 1회용)  
2. 판매자: ETH 가격으로 상품 등록 → wei 저장, 등록 시점 주소를 `payToAddress`로 고정  
3. 구매자: 상품 상세에서 MetaMask로 ETH 전송 (`PaymentMemo` calldata에 상품 ID)  
4. 브라우저: `txHash`를 `/wallet/onchain-purchases`에 POST  
5. 서버 검증 (Web3j):  
   - chainId = Sepolia  
   - receipt success  
   - confirmations ≥ 3  
   - `from` = 구매자 연결 지갑  
   - `to` = 상품 `payToAddress`  
   - `value` ≥ 상품 가격  
   - input이 해당 상품 메모와 일치  
6. 상품 `SOLD`, `wallet_transactions`에 **실제 tx hash** 기록  

### 3.3 주요 화면·권한

| 화면 | 경로 | 권한 |
| --- | --- | --- |
| 홈·목록·상세 | `/`, `/products`, `/products/{id}` | 공개 |
| 등록·수정 | `/products/new`, `.../edit` | 회원 / 소유자 |
| 채팅 | `/chat`, `/chat/{id}` | 참여자 |
| 결제 내역 | `/wallet` | 본인 |
| 신고 | `/reports/new` | 회원 |
| 마이페이지 | `/users/me` | 본인 |
| 관리자 | `/admin/**` | `ROLE_ADMIN` |

### 3.4 데이터 모델 (보안 관점)

| 테이블 | 역할 | 보안 포인트 |
| --- | --- | --- |
| `users` | 계정, `wallet_address`, `locked_until` | `password_hash`만 저장, email/nickname unique |
| `products` | 상품, `price`(wei), `pay_to_address` | 판매중 가격 변경 금지, 상태 기반 노출 |
| `product_images` | 이미지 메타 | UUID 저장명, 상태 확인 후 제공 |
| `chat_rooms` / `messages` | 1:1 채팅 | 참여자만 조회·전송 |
| `wallets` / `wallet_transactions` | 결제 원장 | tx hash unique (잔액 포인트 아님) |
| `reports` | 신고 | 24h 중복 제한, 승인 후 제재 |
| `audit_logs` | 감사 | 민감 키워드 마스킹 |

---

## 4. 구현 및 화면

캡처는 두 종류다.

- **자동 캡처** (`docs/screenshots/01`~`22`): 기능별 단독 화면  
- **직접 캡처** (`docs/screenshots/manual/`): 판매자·구매자 동시 세션, MetaMask 연결·송금 UI  

### 4.1 홈·인증

![홈](screenshots/01-home.png)

![로그인](screenshots/02-login.png)

![회원가입](screenshots/03-register.png)

로그인 후 내비에 내 상품·채팅·결제·마이가 생긴다.

![로그인 후 홈](screenshots/06-home-logged-in.png)

### 4.2 사용자·지갑 연결

회원가입 시 결제 원장(`wallets`)만 만들고, MetaMask 주소는 비어 있다.  
마이페이지에서 서명을 검증해 주소를 연결한다. 이미 다른 계정에 묶인 주소는 거부한다.

직접 캡처 — 이미 연결된 지갑 재사용 시도 시 오류:

![지갑 중복 연결 거부](screenshots/manual/m01-wallet-already-linked-error.png)

판매자·구매자 각각 지갑을 연결한 상태 (좌: seller, 우: buyer, Sepolia):

![판매자·구매자 지갑 연결](screenshots/manual/m02-seller-buyer-wallets-sidebyside.png)

자동 캡처 — 마이페이지 전체:

![마이페이지](screenshots/10-mypage.png)

### 4.3 상품

카테고리 화이트리스트, ETH→wei, 이미지 JPG/PNG만 허용.  
판매자 상태 변경은 `ON_SALE`/`RESERVED`만, `SOLD`는 온체인 확정 시에만.

![상품 목록](screenshots/04-products-list.png)

![상품 상세](screenshots/05-product-detail.png)

![상품 등록](screenshots/14-product-new.png)

![내 상품](screenshots/15-my-products.png)

직접 캡처 — 예약 상품 + 판매자 지갑 주소 + **MetaMask로 ETH 결제** 버튼:

![나이키 상품 MetaMask 결제 버튼](screenshots/manual/m04-product-metamask-pay-button.png)

구매자 상세 (관심·문의·신고):

![구매자 상세](screenshots/07-product-detail-buyer.png)

### 4.4 채팅

상품별 1:1. 참여자만 조회·전송. 거래완료·숨김·삭제 상품은 신규 전송 차단.

직접 캡처 — 일반 창·시크릿 창으로 구매자·판매자 동시 대화 (`RESERVED`, 0.0008 ETH):

![1:1 채팅 양측](screenshots/manual/m03-chat-two-sessions.png)

![채팅방](screenshots/08-chat-room.png)

![채팅 목록](screenshots/09-chat-list.png)

### 4.5 MetaMask 결제 (직접 캡처)

상품 상세에서 결제 버튼을 누르면 MetaMask가 연결/전송을 요청한다.  
요청자 표시는 `localhost:8081`이다.

![MetaMask 연결 요청](screenshots/manual/m05-metamask-connect-prompt.png)

![MetaMask 연결 팝업](screenshots/manual/m06-metamask-tx.png)

![SepoliaETH 0.0008 전송 확인](screenshots/manual/m07-metamask-confirm.png)

![전송 진행](screenshots/manual/m08-after-pay.png)

서버는 이후 `txHash`를 받아 RPC로 검증한다. 단위 테스트는 공개 RPC 의존을 피하고, **지갑 미연결 등록 거부·알 수 없는 tx 거부**를 검증한다.

결제 내역 화면:

![결제 내역](screenshots/11-wallet.png)

### 4.6 신고

본인/자기신고 차단, 24시간 중복 차단. 관리자 승인 후에만 누적.

| 대상 | 승인 신고 | 처리 |
| --- | --- | --- |
| 상품 | 3회 | `HIDDEN` |
| 사용자 | 5회 / 10회 | `LIMITED` / `SUSPENDED` |

![신고 폼](screenshots/13-report-form.png)

![관심](screenshots/12-favorites.png)

### 4.7 관리자

![관리자 대시보드](screenshots/17-admin-dashboard.png)

![사용자](screenshots/18-admin-users.png)

![상품](screenshots/19-admin-products.png)

![신고](screenshots/20-admin-reports.png)

![지갑 거래](screenshots/21-admin-wallet-tx.png)

![감사 로그](screenshots/22-admin-audit-logs.png)

---

## 5. 시큐어코딩

### 5.1 인증·세션

- BCrypt, 로그인 메시지 일반화  
- 실패 5회 → 15분 계정 잠금 + IP rate limit  
- 로그인 시 세션 재발급, 로그아웃 시 무효화  
- 정지/탈퇴 계정 로그인 차단  
- CSRF, 쿠키 `HttpOnly`/`SameSite`, CSP·프레임 차단  

### 5.2 인가

- 상품 수정/삭제: 소유자 또는 관리자  
- 채팅: 참여자만  
- 결제 내역: 본인  
- `/admin/**`: `ROLE_ADMIN`만 (일반 사용자 403)  

### 5.3 입력·XSS·SQLi

- Bean Validation, enum/화이트리스트  
- Thymeleaf `th:text` (`th:utext` 미사용)  
- JPA 파라미터 바인딩  

### 5.4 파일 업로드

- JPG/PNG만 (WebP 제거)  
- 확장자·MIME·시그니처·ImageIO·크기(5MB)·개수(5)  
- UUID 파일명, 숨김/삭제 상품 이미지 직접 URL 차단  

### 5.5 온체인 결제 보안

| 위협 | 대응 |
| --- | --- |
| 예전 tx로 다른 상품 SOLD | `PaymentMemo`로 productId 바인딩 |
| 판매중 가격/지갑 변경(rug) | `payToAddress` 고정, 판매중 가격·지갑 재연결 제한 |
| 서명 재사용 | 세션 nonce + TTL + 1회 소모 |
| 미확정 tx | confirmations ≥ 3, receipt success |
| tx hash 재사용 | 동일 구매자·상품만 멱등 반환 |

### 5.6 로그

- password/token/session/secret 마스킹  
- 신뢰 프록시가 아니면 `X-Forwarded-For` 무시  

체크리스트: `docs/SECURE_CODING_CHECKLIST.md`  
하드닝 요약: `docs/SECURITY_HARDENING.md`

---

## 6. 테스트

### 6.1 자동 테스트

```bash
./gradlew test
```

**BUILD SUCCESSFUL**

| 테스트 | 확인 |
| --- | --- |
| 가입 시 원장 0·지갑 미연결 | 통과 |
| 지갑 없이 상품 등록 | 거부 |
| 잘못된 tx hash 확정 | 거부, 상태 유지 |
| LIMITED 판매자 수정 | 거부 |
| 미승인 신고 3회 | 상품 숨김 안 됨 |
| IP rate limit / 위조 헤더 | 통과 |
| 위장 PNG 업로드 | 거부 |
| `/products/new` 비로그인 | 리다이렉트, 없는 상품 404 |

### 6.2 수동·실기 확인

| ID | 내용 | 증거 |
| --- | --- | --- |
| TC-UI-001 | 홈·목록·상세 | 01, 04, 05 |
| TC-AUTH-001 | 로그인·가입 | 02, 03 |
| TC-WAL-001 | 지갑 연결·중복 거부 | manual m01, m02 |
| TC-CHAT-001 | 양측 1:1 채팅 | manual m03 |
| TC-PAY-001 | MetaMask 연결·0.0008 ETH 전송 UI | manual m04–m08 |
| TC-ADM-001 | 관리자 메뉴 | 17–22 |
| TC-ADM-002 | 일반 사용자 `/admin` | 403 |

---

## 7. 발견한 보안 약점과 수정

상세: `docs/SECURITY_FIX_LOG.md`, `docs/SECURITY_HARDENING.md`

| 약점 | 위험 | 수정 |
| --- | --- | --- |
| H2 콘솔 공개 | 높음 | 기본 비활성 |
| 상품 소유자 미검증 / edit 노출 | 높음 | GET·POST 모두 검증 |
| 업로드 확장자만 검증 | 중간 | MIME·시그니처·디코딩 |
| 숨김 상품 이미지 URL | 중간 | 상태 확인 컨트롤러 |
| 허위 신고 자동 제재 | 높음 | 관리자 승인만 누적 |
| 공개 기본 관리자 | 치명 | 환경변수로만 생성 |
| tx↔상품 미바인딩 | 높음 | PaymentMemo |
| 판매자 rug | 높음 | payToAddress·가격 고정 |
| 지갑 서명 재사용 | 중간 | nonce |
| XFF로 감사 IP 위조 | 중간 | 신뢰 프록시만 |
| 무차별 대입 | 중간 | 잠금 + IP limit |

---

## 8. 실행 방법

저장소: https://github.com/no-carve-only-pizza/usedcarrot  

```bash
git clone https://github.com/no-carve-only-pizza/usedcarrot.git
cd usedcarrot
export USEDCARROT_ADMIN_EMAIL='admin@example.com'
export USEDCARROT_ADMIN_PASSWORD='ChangeThisAdmin1!'
./gradlew bootRun --args='--spring.profiles.active=local'
```

- 앱: http://localhost:8081  
- 선택: `USEDCARROT_ETH_RPC_URL`  
- 테스트: `./gradlew test`  
- `local` 데모: `seller@demo.local` / `buyer@demo.local` / 비밀번호 `DemoUser1234!` (MetaMask 연결 필요)  
- Sepolia faucet 예: https://sepoliafaucet.com/

환경변수 없이 관리자 계정을 소스에 하드코딩하지 않았다.

---

## 9. 유지보수

- Spring·의존성 보안 패치  
- 공개 RPC → 자체/유료 RPC, 컨펌 수 운영 조정  
- 신고 정책·관리자 2인 승인 검토  
- 채팅 읽음, 이미지 썸네일  
- 운영 시 H2→MySQL, HTTPS, 관리자 비밀번호 교체  
- 메인넷/실결제 전환 시 키 관리·법무는 별도 (현재는 Sepolia 교육용)

---

## 부록. 캡처 인덱스

### 자동 (`docs/screenshots/`)

| 파일 | 화면 |
| --- | --- |
| 01–03 | 홈, 로그인, 회원가입 |
| 04–07, 14–16 | 상품 목록·상세·등록·내상품·수정 |
| 08–09 | 채팅 |
| 10–13 | 마이, 결제, 관심, 신고 |
| 17–22 | 관리자 |

### 직접 (`docs/screenshots/manual/`)

| 파일 | 내용 |
| --- | --- |
| m01 | 지갑 중복 연결 오류 |
| m02 | 판매자·구매자 지갑 연결 (양창) |
| m03 | 1:1 채팅 양측 |
| m04 | 상품 상세 MetaMask 결제 버튼 |
| m05–m08 | MetaMask 연결·전송 UI |
