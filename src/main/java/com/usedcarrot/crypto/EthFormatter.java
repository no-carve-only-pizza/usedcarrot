package com.usedcarrot.crypto;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import org.springframework.stereotype.Component;
import org.web3j.utils.Convert;

@Component("eth")
public class EthFormatter {
    public String format(long wei) {
        return Convert.fromWei(BigDecimal.valueOf(wei), Convert.Unit.ETHER)
            .stripTrailingZeros()
            .toPlainString();
    }

    public static long toWei(BigDecimal eth) {
        if (eth == null) {
            throw new IllegalArgumentException("가격이 필요합니다.");
        }
        BigInteger wei = Convert.toWei(eth, Convert.Unit.ETHER).toBigIntegerExact();
        if (wei.compareTo(BigInteger.valueOf(Long.MAX_VALUE)) > 0) {
            throw new IllegalArgumentException("가격이 너무 큽니다.");
        }
        return wei.longValueExact();
    }

    public static BigDecimal fromWei(long wei) {
        return Convert.fromWei(BigDecimal.valueOf(wei), Convert.Unit.ETHER)
            .setScale(18, RoundingMode.DOWN)
            .stripTrailingZeros();
    }
}
