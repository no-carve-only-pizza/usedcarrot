package com.usedcarrot.crypto;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "usedcarrot.ethereum")
public class EthereumProperties {
    /** Sepolia = 11155111 */
    private long chainId = 11155111L;
    private String chainName = "Sepolia";
    private String rpcUrl = "https://ethereum-sepolia-rpc.publicnode.com";
    private String explorerTxUrl = "https://sepolia.etherscan.io/tx/";
    private String currencySymbol = "ETH";
    private int requiredConfirmations = 1;

    public long getChainId() {
        return chainId;
    }

    public void setChainId(long chainId) {
        this.chainId = chainId;
    }

    public String getChainName() {
        return chainName;
    }

    public void setChainName(String chainName) {
        this.chainName = chainName;
    }

    public String getRpcUrl() {
        return rpcUrl;
    }

    public void setRpcUrl(String rpcUrl) {
        this.rpcUrl = rpcUrl;
    }

    public String getExplorerTxUrl() {
        return explorerTxUrl;
    }

    public void setExplorerTxUrl(String explorerTxUrl) {
        this.explorerTxUrl = explorerTxUrl;
    }

    public String getCurrencySymbol() {
        return currencySymbol;
    }

    public void setCurrencySymbol(String currencySymbol) {
        this.currencySymbol = currencySymbol;
    }

    public int getRequiredConfirmations() {
        return requiredConfirmations;
    }

    public void setRequiredConfirmations(int requiredConfirmations) {
        this.requiredConfirmations = requiredConfirmations;
    }
}
