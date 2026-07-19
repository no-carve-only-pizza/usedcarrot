/**
 * Chat room: poll for new messages + AJAX send (no full refresh).
 */
(function () {
  const root = document.getElementById("chatRoom");
  if (!root) return;

  const roomId = root.dataset.roomId;
  const meId = Number(root.dataset.meId);
  const canSend = root.dataset.canSend === "true";
  const feed = document.getElementById("chatFeed");
  const form = document.getElementById("chatForm");
  const input = document.getElementById("chatInput");
  const csrf = document.querySelector('meta[name="_csrf"]');
  const csrfHeader = document.querySelector('meta[name="_csrf_header"]');
  const baseTitle = document.title;
  let lastId = Number(root.dataset.lastId || "0");
  let unseen = 0;

  function escapeHtml(s) {
    return String(s)
      .replace(/&/g, "&amp;")
      .replace(/</g, "&lt;")
      .replace(/>/g, "&gt;")
      .replace(/"/g, "&quot;");
  }

  function appendMessage(msg, mine) {
    if (document.getElementById("msg-" + msg.id)) return;
    const empty = feed.querySelector(".chat-empty");
    if (empty) empty.remove();

    const row = document.createElement("div");
    row.className = "chat-row" + (mine ? " mine" : " theirs");
    row.id = "msg-" + msg.id;
    row.innerHTML =
      '<div class="chat-bubble">' +
      (mine ? "" : '<div class="chat-name">' + escapeHtml(msg.senderNickname) + "</div>") +
      '<div class="chat-text">' + escapeHtml(msg.content) + "</div>" +
      '<div class="chat-time">' + escapeHtml(msg.createdAt) + "</div>" +
      "</div>";
    feed.appendChild(row);
    feed.scrollTop = feed.scrollHeight;
    lastId = Math.max(lastId, msg.id);
  }

  function notifyIncoming() {
    if (document.hidden) {
      unseen += 1;
      document.title = "(" + unseen + ") 새 메시지 · " + baseTitle;
    }
  }

  document.addEventListener("visibilitychange", function () {
    if (!document.hidden) {
      unseen = 0;
      document.title = baseTitle;
    }
  });

  async function poll() {
    try {
      const res = await fetch("/chat/" + roomId + "/messages.json?afterId=" + lastId, {
        headers: { Accept: "application/json" },
        credentials: "same-origin",
      });
      if (!res.ok) return;
      const list = await res.json();
      for (const msg of list) {
        const mine = Number(msg.senderId) === meId;
        appendMessage(msg, mine);
        if (!mine) notifyIncoming();
      }
    } catch (_) {
      /* ignore transient network blips */
    }
  }

  if (form && canSend) {
    form.addEventListener("submit", async function (e) {
      e.preventDefault();
      const content = (input.value || "").trim();
      if (!content) return;
      const body = new URLSearchParams();
      body.set("content", content);
      const headers = {
        "Content-Type": "application/x-www-form-urlencoded",
        Accept: "application/json",
      };
      if (csrf && csrfHeader) {
        headers[csrfHeader.content] = csrf.content;
      }
      const btn = form.querySelector("button[type=submit]");
      if (btn) btn.disabled = true;
      try {
        const res = await fetch("/chat/" + roomId + "/messages.json", {
          method: "POST",
          headers,
          body,
          credentials: "same-origin",
        });
        const data = await res.json();
        if (!res.ok) {
          alert(data.error || "전송 실패");
          return;
        }
        input.value = "";
        appendMessage(data, true);
      } catch (err) {
        alert("전송에 실패했습니다.");
      } finally {
        if (btn) btn.disabled = false;
        input.focus();
      }
    });
  }

  feed.scrollTop = feed.scrollHeight;
  setInterval(poll, 2000);
  poll();
})();
