# UsedCarrot

WHS 시큐어코딩 과제용 **중고거래 플랫폼**입니다.  
결제는 DB 포인트가 아니라 **Ethereum Sepolia + MetaMask** 온체인 전송이며, 서버는 ETH를 보관하지 않고 RPC로 영수증을 검증한 뒤 상품을 `SOLD` 처리합니다.

- GitHub: https://github.com/no-carve-only-pizza/usedcarrot  
- 보고서: [`docs/PROJECT_REPORT.md`](docs/PROJECT_REPORT.md)  
- 포트: **8081** (`local` 프로필)

## 주요 기능

- 회원가입 / 로그인 / 마이페이지 (MetaMask 지갑 연결)
- 상품 등록·검색·상세·수정·삭제, 관심 상품
- 상품별 1:1 채팅
- Sepolia ETH 결제 (MetaMask → 서버 Web3j 검증)
- 신고 + 관리자 승인 후 제재
- 관리자: 사용자·상품·신고·결제내역·감사로그

## 기술 스택

Java 21, Spring Boot 3, Thymeleaf, Spring Security, JPA, H2, Web3j

## 결제 흐름

1. 판매자·구매자: 마이페이지에서 MetaMask `personal_sign`으로 지갑 연결  
2. 판매자: ETH 가격으로 상품 등록 (서버는 wei + `payToAddress` 저장)  
3. 구매자: 상품 상세에서 MetaMask로 판매자 주소에 ETH 전송  
4. 브라우저가 `txHash`를 서버에 제출  
5. 서버가 Sepolia RPC로 검증 (`from`/`to`/`value`/상품 메모/컨펌)  
6. 상품 `SOLD`, 결제 내역에 실제 tx hash 기록  

## 실행

Java 21, MetaMask, Sepolia ETH(faucet) 필요.

```bash
export USEDCARROT_ADMIN_EMAIL='admin@example.com'
export USEDCARROT_ADMIN_PASSWORD='ChangeThisAdmin1!'
./gradlew bootRun --args='--spring.profiles.active=local'
```

- 앱: http://localhost:8081  
- 선택 RPC: `USEDCARROT_ETH_RPC_URL` (기본 public Sepolia RPC)

## 데모 계정 (`local`)

첫 실행 시 생성됩니다. **구매·등록 전에 MetaMask 연결**이 필요합니다.

| 계정 | 이메일 | 비밀번호 |
| --- | --- | --- |
| 판매자 | `seller@demo.local` | `DemoUser1234!` |
| 판매자2 | `seller2@demo.local` | `DemoUser1234!` |
| 구매자 | `buyer@demo.local` | `DemoUser1234!` |
| 관리자 | 환경변수 `USEDCARROT_ADMIN_EMAIL` | 환경변수 `USEDCARROT_ADMIN_PASSWORD` |

Sepolia faucet 예: https://sepoliafaucet.com/

## 테스트

```bash
./gradlew test
```

온체인 확정은 공개 RPC에 의존하므로, 단위 테스트는 지갑 미연결 등록 거부·잘못된 tx 해시 거부를 검증합니다.

## 보안 문서

- `docs/SECURITY_HARDENING.md`
- `docs/SECURITY_FIX_LOG.md`
- `docs/SECURE_CODING_CHECKLIST.md`

## DB / 업로드

- H2 파일: `./data/` (gitignore)
- 업로드: `./uploads/` (gitignore)
- H2 콘솔: 기본 비활성
