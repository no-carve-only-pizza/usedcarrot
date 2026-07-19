# Security hardening (2026-07-19)

UsedCarrot(온체인 제출본) 보안 점검 후 조치.

| 이슈 | 조치 |
| --- | --- |
| 결제 tx ↔ 상품 미바인딩 | `PaymentMemo` calldata `UsedCarrot\|productId={id}` 필수 검증 |
| 판매자 가격/지갑 rug | 판매중 가격 변경 금지; `payToAddress` 등록 시 고정; 활성 상품 있으면 지갑 재연결 금지 |
| 지갑 링크 nonce | 세션 발급 + 10분 TTL + 1회 소모 |
| 계정 잠금 | 실패 5회 → 15분 `lockedUntil` |
| 컨펌 수 | 기본 3 |
| 비밀번호 | 영문+숫자+특수문자 |
| Rate limit | login/register/wallet/onchain/report POST |
| WebP | 업로드 허용 제거 (JPG/PNG + ImageIO) |
| CSP img | `'self'` + unsplash만 |

보고서 「발견한 약점 → 조치」에 위 표를 그대로 쓸 수 있다.
