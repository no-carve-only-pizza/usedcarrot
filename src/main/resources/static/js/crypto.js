/**
 * UsedCarrot Web3 helpers — MetaMask personal_sign + native ETH payment on configured chain.
 *
 * MetaMask는 personal_sign UI에 "현재 선택된 계정"만 보여 준다.
 * 사이트에서 Account 2를 골라도 확장이 Account 1이면 서명은 1로 나간다.
 * → 고른 주소가 활성 계정이 될 때까지 accountsChanged를 기다린다.
 */
window.UsedCarrotWeb3 = (function () {
  function shortAddr(addr) {
    return addr.slice(0, 6) + "…" + addr.slice(-4);
  }

  function removeEl(id) {
    const el = document.getElementById(id);
    if (el) el.remove();
  }

  function pickAccount(accounts) {
    if (accounts.length === 1) {
      return Promise.resolve(accounts[0]);
    }
    return new Promise(function (resolve, reject) {
      removeEl("mmAccountPicker");
      const overlay = document.createElement("div");
      overlay.id = "mmAccountPicker";
      overlay.className = "mm-picker-overlay";
      overlay.innerHTML =
        '<div class="mm-picker" role="dialog" aria-modal="true">' +
        "<h3>연결할 MetaMask 계정</h3>" +
        "<p>쓸 주소를 고르세요. 이어서 MetaMask에서 <strong>같은 계정으로 전환</strong>해야 합니다.</p>" +
        '<div class="mm-picker-list"></div>' +
        '<button type="button" class="mm-picker-cancel ghost">취소</button>' +
        "</div>";
      const list = overlay.querySelector(".mm-picker-list");
      accounts.forEach(function (addr, i) {
        const btn = document.createElement("button");
        btn.type = "button";
        btn.className = "mm-picker-item";
        btn.textContent = "Account " + (i + 1) + "  ·  " + shortAddr(addr);
        btn.title = addr;
        btn.addEventListener("click", function () {
          overlay.remove();
          resolve(addr);
        });
        list.appendChild(btn);
      });
      overlay.querySelector(".mm-picker-cancel").addEventListener("click", function () {
        overlay.remove();
        reject(new Error("계정 선택을 취소했습니다."));
      });
      document.body.appendChild(overlay);
    });
  }

  /** MetaMask 활성 계정이 wanted가 될 때까지 대기 */
  function waitUntilActive(wanted) {
    const target = wanted.toLowerCase();
    return window.ethereum.request({ method: "eth_accounts" }).then(function (accs) {
      if (accs[0] && accs[0].toLowerCase() === target) {
        return wanted;
      }
      return new Promise(function (resolve, reject) {
        removeEl("mmSwitchHint");
        const overlay = document.createElement("div");
        overlay.id = "mmSwitchHint";
        overlay.className = "mm-picker-overlay";
        overlay.innerHTML =
          '<div class="mm-picker" role="dialog" aria-modal="true">' +
          "<h3>MetaMask에서 계정 전환</h3>" +
          "<p>서명 창에 Account 1이 뜨는 건, 확장 프로그램이 아직 그 계정을 쓰고 있어서입니다.</p>" +
          "<ol class=\"steps\">" +
          "<li>MetaMask 확장 아이콘을 엽니다.</li>" +
          "<li>위쪽 계정 메뉴에서 <strong>" + shortAddr(wanted) + "</strong> 로 바꿉니다.</li>" +
          "<li>이 창은 자동으로 닫힙니다. 그다음 서명 요청이 뜹니다.</li>" +
          "</ol>" +
          '<p class="wallet-addr">' + wanted + "</p>" +
          '<button type="button" class="mm-picker-cancel ghost">취소</button>' +
          "</div>";
        document.body.appendChild(overlay);

        let done = false;
        function finish(ok, err) {
          if (done) return;
          done = true;
          clearTimeout(timer);
          window.ethereum.removeListener("accountsChanged", onChange);
          overlay.remove();
          if (ok) resolve(wanted);
          else reject(err || new Error("취소했습니다."));
        }

        function onChange(accs) {
          if (accs[0] && accs[0].toLowerCase() === target) {
            finish(true);
          }
        }

        const timer = setTimeout(function () {
          finish(false, new Error("시간 초과. MetaMask에서 " + shortAddr(wanted) + " 로 전환 후 다시 시도하세요."));
        }, 90000);

        overlay.querySelector(".mm-picker-cancel").addEventListener("click", function () {
          finish(false, new Error("계정 전환을 취소했습니다."));
        });
        window.ethereum.on("accountsChanged", onChange);

        // 이미 바꿨을 수도 있음
        window.ethereum.request({ method: "eth_accounts" }).then(function (now) {
          if (now[0] && now[0].toLowerCase() === target) finish(true);
        });
      });
    });
  }

  async function ensureWallet(expectedChainId) {
    if (!window.ethereum) {
      throw new Error(
        "MetaMask를 찾을 수 없습니다.\n\n" +
        "1) Chrome 또는 Brave를 쓰세요 (Cursor 내장 브라우저/Safari는 안 됩니다)\n" +
        "2) MetaMask 확장 프로그램을 설치·잠금 해제하세요\n" +
        "3) 이 탭을 새로고침한 뒤 다시 눌러주세요\n\n" +
        "설치: https://metamask.io/download/"
      );
    }

    try {
      await window.ethereum.request({
        method: "wallet_requestPermissions",
        params: [{ eth_accounts: {} }],
      });
    } catch (e) {
      if (e && (e.code === 4001 || e.code === "ACTION_REJECTED")) {
        throw new Error("MetaMask 연결을 거부했습니다.");
      }
    }

    let accounts = await window.ethereum.request({ method: "eth_requestAccounts" });
    if (!accounts || !accounts.length) {
      accounts = await window.ethereum.request({ method: "eth_accounts" });
    }
    if (!accounts || !accounts.length) {
      throw new Error("지갑 계정을 선택해주세요. MetaMask에서 쓸 계정을 체크했는지 확인하세요.");
    }

    const chainIdHex = await window.ethereum.request({ method: "eth_chainId" });
    const chainId = parseInt(chainIdHex, 16);
    if (Number(expectedChainId) !== chainId) {
      const hex = "0x" + Number(expectedChainId).toString(16);
      try {
        await window.ethereum.request({
          method: "wallet_switchEthereumChain",
          params: [{ chainId: hex }],
        });
      } catch (e) {
        throw new Error("네트워크를 Sepolia(체인 ID " + expectedChainId + ")로 전환해주세요.");
      }
    }

    const chosen = await pickAccount(accounts);
    await waitUntilActive(chosen);
    return chosen;
  }

  async function linkWallet(formId, chainId) {
    const form = document.getElementById(formId);
    const message = form.message.value;
    if (!message) {
      throw new Error("연결 메시지가 없습니다. 페이지를 새로고침해주세요.");
    }
    const address = await ensureWallet(chainId);
    const signature = await window.ethereum.request({
      method: "personal_sign",
      params: [message, address],
    });
    form.address.value = address;
    form.signature.value = signature;
    form.submit();
  }

  async function payProduct(opts) {
    const address = await ensureWallet(opts.chainId);
    if (opts.buyerWallet && address.toLowerCase() !== opts.buyerWallet.toLowerCase()) {
      throw new Error(
        "연결된 구매자 지갑과 다릅니다.\n" +
        "연결됨: " + shortAddr(opts.buyerWallet) + "\n" +
        "선택함: " + shortAddr(address) + "\n" +
        "마이페이지에서 지갑을 다시 연결하거나, MetaMask에서 같은 계정을 고르세요."
      );
    }
    const txHash = await window.ethereum.request({
      method: "eth_sendTransaction",
      params: [{
        from: address,
        to: opts.sellerWallet,
        value: "0x" + BigInt(opts.priceWei).toString(16),
        data: opts.paymentData || "0x",
      }],
    });
    const form = document.getElementById(opts.formId);
    form.productId.value = opts.productId;
    form.txHash.value = txHash;
    form.submit();
  }

  document.addEventListener("DOMContentLoaded", function () {
    const payBtn = document.getElementById("payBtn");
    if (payBtn) {
      payBtn.addEventListener("click", async function () {
        payBtn.disabled = true;
        try {
          await payProduct({
            formId: "onchainPurchaseForm",
            productId: payBtn.dataset.productId,
            priceWei: payBtn.dataset.priceWei,
            sellerWallet: payBtn.dataset.seller,
            buyerWallet: payBtn.dataset.buyer,
            chainId: payBtn.dataset.chainId,
            paymentData: payBtn.dataset.paymentData,
          });
        } catch (e) {
          alert(e.message || e);
          payBtn.disabled = false;
        }
      });
    }
    const linkBtn = document.getElementById("linkWalletBtn");
    if (linkBtn) {
      linkBtn.addEventListener("click", async function () {
        linkBtn.disabled = true;
        try {
          await linkWallet("walletLinkForm", linkBtn.dataset.chainId);
        } catch (e) {
          alert(e.message || e);
          linkBtn.disabled = false;
        }
      });
    }
  });

  return { linkWallet, payProduct };
})();
