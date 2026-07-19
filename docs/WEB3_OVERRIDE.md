# 결제·문서 기준 (온체인)

이 저장소의 **실제 동작 기준**은 `README.md`와 `docs/PROJECT_REPORT.md`다.

초기 `SRS.md` / `SystemDesign.md` / `AI_IMPLEMENTATION_SPEC.md`에는  
과거 DB CarrotCoin 모의지갑 문구가 남아 있을 수 있다.  
**송금·결제 관련 요구는 아래 온체인 규칙으로 대체**한다.

## 결제

- Sepolia ETH + MetaMask (`personal_sign` 연결, `eth_sendTransaction` 송금)
- 서버: Web3j RPC로 tx 검증 후 `SOLD`
- 가격: ETH → wei 저장 (`EthFormatter`)
- DB `wallets` / `wallet_transactions`: 온체인 결제 **내역·감사**용 (포인트 잔액 아님)
- 관리자 CC 지급: 없음
- 기본 포트: **8081**

## 관련 코드

- `com.usedcarrot.crypto.*`
- `WalletService.confirmOnChainPurchase`
- `docs/SECURITY_HARDENING.md`
