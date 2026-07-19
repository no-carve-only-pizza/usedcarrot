package com.usedcarrot.crypto;

import java.nio.charset.StandardCharsets;
import org.web3j.utils.Numeric;

/**
 * Native ETH transfer calldata that binds a payment to one product listing.
 * MetaMask eth_sendTransaction.data = hex(UTF-8 "UsedCarrot|productId={id}")
 */
public final class PaymentMemo {
    private PaymentMemo() {
    }

    public static String plaintext(long productId) {
        return "UsedCarrot|productId=" + productId;
    }

    public static String calldataHex(long productId) {
        return Numeric.toHexString(plaintext(productId).getBytes(StandardCharsets.UTF_8));
    }

    public static boolean matches(String inputHex, long productId) {
        if (inputHex == null || inputHex.isBlank() || "0x".equalsIgnoreCase(inputHex)) {
            return false;
        }
        try {
            byte[] raw = Numeric.hexStringToByteArray(inputHex);
            String decoded = new String(raw, StandardCharsets.UTF_8);
            return plaintext(productId).equals(decoded);
        } catch (Exception e) {
            return false;
        }
    }
}
