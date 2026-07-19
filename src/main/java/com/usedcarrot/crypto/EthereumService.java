package com.usedcarrot.crypto;

import com.usedcarrot.common.AppException;
import com.usedcarrot.common.ErrorCode;
import java.math.BigInteger;
import org.springframework.stereotype.Service;
import org.web3j.crypto.Keys;
import org.web3j.crypto.Sign;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.EthBlockNumber;
import org.web3j.protocol.core.methods.response.EthChainId;
import org.web3j.protocol.core.methods.response.EthGetTransactionReceipt;
import org.web3j.protocol.core.methods.response.EthTransaction;
import org.web3j.protocol.core.methods.response.Transaction;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.http.HttpService;
import org.web3j.utils.Numeric;

@Service
public class EthereumService {
    private final EthereumProperties properties;
    private final Web3j web3j;

    public EthereumService(EthereumProperties properties) {
        this.properties = properties;
        this.web3j = Web3j.build(new HttpService(properties.getRpcUrl()));
    }

    public EthereumProperties properties() {
        return properties;
    }

    public void assertConfiguredChain() {
        try {
            EthChainId chainId = web3j.ethChainId().send();
            if (chainId.hasError() || chainId.getChainId() == null) {
                throw new AppException(ErrorCode.INVALID_STATE, "이더리움 RPC 체인 조회에 실패했습니다.");
            }
            if (chainId.getChainId().longValue() != properties.getChainId()) {
                throw new AppException(ErrorCode.INVALID_STATE,
                    "설정된 체인과 RPC 체인이 다릅니다. expected=" + properties.getChainId());
            }
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            throw new AppException(ErrorCode.INVALID_STATE, "이더리움 RPC에 연결할 수 없습니다.");
        }
    }

    public OnChainPayment loadPayment(String txHash) {
        assertConfiguredChain();
        String normalizedHash = normalizeTxHash(txHash);
        try {
            EthTransaction ethTransaction = web3j.ethGetTransactionByHash(normalizedHash).send();
            Transaction tx = ethTransaction.getTransaction()
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "온체인 트랜잭션을 찾을 수 없습니다."));
            EthGetTransactionReceipt receiptResponse = web3j.ethGetTransactionReceipt(normalizedHash).send();
            TransactionReceipt receipt = receiptResponse.getTransactionReceipt()
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_STATE, "아직 확정되지 않은 트랜잭션입니다. 잠시 후 다시 시도하세요."));
            if (!receipt.isStatusOK()) {
                throw new AppException(ErrorCode.INVALID_STATE, "실패한 온체인 트랜잭션입니다.");
            }
            EthBlockNumber latest = web3j.ethBlockNumber().send();
            BigInteger txBlock = receipt.getBlockNumber();
            BigInteger confirmations = latest.getBlockNumber().subtract(txBlock).add(BigInteger.ONE);
            if (confirmations.intValue() < properties.getRequiredConfirmations()) {
                throw new AppException(ErrorCode.INVALID_STATE, "컨펌 수가 부족합니다. 잠시 후 다시 시도하세요.");
            }
            if (tx.getTo() == null || tx.getTo().isBlank()) {
                throw new AppException(ErrorCode.INVALID_STATE, "컨트랙트 생성 트랜잭션은 결제에 사용할 수 없습니다.");
            }
            return new OnChainPayment(
                normalizedHash,
                Keys.toChecksumAddress(tx.getFrom()),
                Keys.toChecksumAddress(tx.getTo()),
                tx.getValue(),
                tx.getInput(),
                confirmations.intValue()
            );
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            throw new AppException(ErrorCode.INVALID_STATE, "온체인 트랜잭션 검증에 실패했습니다.");
        }
    }

    public String recoverAddress(String message, String signatureHex) {
        try {
            byte[] signature = Numeric.hexStringToByteArray(signatureHex);
            if (signature.length != 65) {
                throw new AppException(ErrorCode.BAD_REQUEST, "서명 형식이 올바르지 않습니다.");
            }
            byte v = signature[64];
            if (v < 27) {
                v += 27;
            }
            Sign.SignatureData signatureData = new Sign.SignatureData(
                v,
                java.util.Arrays.copyOfRange(signature, 0, 32),
                java.util.Arrays.copyOfRange(signature, 32, 64)
            );
            BigInteger publicKey = Sign.signedPrefixedMessageToKey(message.getBytes(java.nio.charset.StandardCharsets.UTF_8), signatureData);
            return Keys.toChecksumAddress("0x" + Keys.getAddress(publicKey));
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            throw new AppException(ErrorCode.BAD_REQUEST, "지갑 서명을 검증할 수 없습니다.");
        }
    }

    public String normalizeAddress(String address) {
        if (address == null || !address.matches("(?i)^0x[0-9a-f]{40}$")) {
            throw new AppException(ErrorCode.BAD_REQUEST, "이더리움 주소 형식이 올바르지 않습니다.");
        }
        return Keys.toChecksumAddress(address);
    }

    public String normalizeTxHash(String txHash) {
        if (txHash == null || !txHash.matches("(?i)^0x[0-9a-f]{64}$")) {
            throw new AppException(ErrorCode.BAD_REQUEST, "트랜잭션 해시 형식이 올바르지 않습니다.");
        }
        return txHash.toLowerCase();
    }

    public record OnChainPayment(String txHash, String from, String to, BigInteger valueWei,
                                 String input, int confirmations) {
    }
}
