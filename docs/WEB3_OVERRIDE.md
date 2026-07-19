# UsedCarrot Web3 — 온체인 전환 메모

원본 `docs/*`는 과제용 DB 모의지갑 명세를 그대로 복사한 상태다.
이 디렉터리의 **실제 동작 기준**은 `README.md`다.

## 핵심 변경

- 결제: Sepolia ETH + MetaMask (`personal_sign` 지갑 연결, `eth_sendTransaction` 송금)
- 서버: Web3j RPC로 tx 검증 후 `SOLD`
- 가격: ETH → wei 저장 (`EthFormatter`)
- DB `wallets`/`wallet_transactions`: 온체인 결제 **내역/감사**용 (잔액 포인트 아님)
- 관리자 CC 지급 제거
- 포트: 8081 (`usedcarrot` 8080과 분리)
