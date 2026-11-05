(function () {
  "use strict";

  const root = document.getElementById("chat-widget");
  if (!root) return;

  const userId = Number(root.dataset.userId);
  const baseUrl = (root.dataset.baseUrl || "/").replace(/\/+$/, "");
  const toggle = document.getElementById("chat-widget-toggle");
  const panel = document.getElementById("chat-widget-panel");
  const close = document.getElementById("chat-widget-close");
  const back = document.getElementById("chat-widget-back");
  const badge = document.getElementById("chat-widget-badge");
  const title = document.getElementById("chat-widget-title");
  const content = document.getElementById("chat-widget-content");
  let selectedId = null;
  let conversations = [];
  let pollTimer = null;
  let consecutiveErrors = 0;
  let pollingStopped = false;
  let currentMessages = [];

  panel.hidden = true;
  toggle.setAttribute("aria-expanded", "false");

  function apiUrl(path) {
    return baseUrl + "/" + path.replace(/^\/+/, "");
  }

  async function request(path, options) {
    const requestOptions = options || {};
    const url = apiUrl(path);
    const method = requestOptions.method || "GET";
    const response = await fetch(url, requestOptions);
    if (!response.ok) {
      const error = new Error("No se pudo actualizar el chat (" + response.status + ")");
      error.status = response.status;
      error.path = path;
      error.method = method;
      console.error("Error en el chat", {
        status: response.status,
        method: method,
        path: path
      });
      throw error;
    }
    consecutiveErrors = 0;
    return response.json();
  }

  function showError(error) {
    content.innerHTML = "<p class=\"chat-widget-error\">" +
      escapeHtml(error.message || "No se pudo actualizar el chat") + "</p>";
  }

  function handleError(error) {
    consecutiveErrors += 1;
    if (!panel.hidden) showError(error);
    if (consecutiveErrors >= 3) {
      pollingStopped = true;
      if (!panel.hidden) {
        content.innerHTML += "<p class=\"chat-widget-error\">El chat se pausó. Volvé a abrir la página para reintentar.</p>";
      }
    }
  }

  function updateBadge(count) {
    badge.hidden = !count;
    badge.textContent = count > 9 ? "9+" : String(count);
  }

  function formatTime(value) {
    if (!value) return "";
    return new Date(value).toLocaleTimeString("es-AR", { hour: "2-digit", minute: "2-digit" });
  }

  function renderList() {
    title.textContent = "Mensajes";
    back.hidden = true;
    if (!conversations.length) {
      content.innerHTML = "<p class=\"chat-widget-empty\">No tenés conversaciones todavía</p>";
      return;
    }
    content.innerHTML = conversations.map(function (conversation) {
      const unread = conversation.noLeidos > 0;
      return "<button class=\"chat-widget-conversation " + (unread ? "unread" : "") +
        "\" data-conversation-id=\"" + conversation.id + "\">" +
        "<span class=\"chat-widget-conversation-top\"><span class=\"chat-widget-name\">" +
        escapeHtml(conversation.otroParticipante) + (unread ? "<span class=\"chat-widget-unread-dot\"></span>" : "") +
        "</span><span class=\"chat-widget-time\">" + formatTime(conversation.hora) + "</span></span>" +
        "<span class=\"chat-widget-job\">" + escapeHtml(conversation.puesto || "Postulación") + "</span>" +
        "<span class=\"chat-widget-preview\">" + escapeHtml(conversation.ultimoMensaje || "Sin mensajes") + "</span></button>";
    }).join("");
    content.querySelectorAll("[data-conversation-id]").forEach(function (item) {
      item.addEventListener("click", function () {
        openConversation(Number(item.dataset.conversationId)).catch(handleError);
      });
    });
  }

  function messageKey(message, index) {
    return message.id === undefined || message.id === null
      ? "index:" + index
      : "id:" + message.id;
  }

  function appendMessage(message) {
    const messagesContainer = content.querySelector(".chat-widget-messages");
    const element = document.createElement("div");
    element.className = "chat-widget-message" +
      (message.remitenteId === userId ? " mine" : "");
    element.textContent = message.contenido;
    messagesContainer.appendChild(element);
  }

  function createConversationPanel(messages) {
    content.innerHTML = "<div class=\"chat-widget-messages\"></div>" +
      "<form class=\"chat-widget-composer\" id=\"chat-widget-form\">" +
      "<input name=\"contenido\" maxlength=\"2000\" placeholder=\"Escribí un mensaje…\" autocomplete=\"off\" required>" +
      "<button type=\"submit\">Enviar</button></form>";
    content.querySelector("form").addEventListener("submit", function (event) {
      sendMessage(event).catch(handleError);
    });
    currentMessages = [];
    messages.forEach(function (message) {
      appendMessage(message);
      currentMessages.push(message);
    });
    content.querySelector("input").focus();
  }

  function updateMessages(messages) {
    const knownKeys = new Set(currentMessages.map(messageKey));
    const newMessages = messages.filter(function (message, index) {
      return !knownKeys.has(messageKey(message, index));
    });
    if (!newMessages.length) return;

    const messagesContainer = content.querySelector(".chat-widget-messages");
    const atBottom = messagesContainer.scrollHeight -
      messagesContainer.scrollTop - messagesContainer.clientHeight <= 4;
    newMessages.forEach(function (message) {
      appendMessage(message);
      currentMessages.push(message);
    });
    if (atBottom) messagesContainer.scrollTop = messagesContainer.scrollHeight;
  }

  async function openConversation(id) {
    selectedId = id;
    const conversation = conversations.find(function (item) { return item.id === id; });
    title.textContent = conversation ? conversation.otroParticipante : "Conversación";
    back.hidden = false;
    const messages = await request("/api/chat/conversaciones/" + id + "/mensajes");
    createConversationPanel(messages);
    await request("/api/chat/conversaciones/" + id + "/leido", { method: "POST" });
    await refreshBadge();
  }

  async function sendMessage(event) {
    event.preventDefault();
    const input = event.target.elements.contenido;
    const contenido = input.value.trim();
    if (!contenido || selectedId === null) return;
    const body = new URLSearchParams({ contenido: contenido });
    const message = await request(
      "/api/chat/conversaciones/" + selectedId + "/mensajes",
      { method: "POST", body: body }
    );
    currentMessages.push(message);
    appendMessage(message);
    input.value = "";
    input.focus();
  }

  async function refreshBadge() {
    const result = await request("/api/chat/no-leidos");
    updateBadge(result.cantidad);
  }

  async function refreshConversations() {
    conversations = await request("/api/chat/conversaciones");
    if (selectedId === null) renderList();
    await refreshBadge();
  }

  async function refreshOpenConversation() {
    if (selectedId === null) {
      await refreshConversations();
      return;
    }
    const messages = await request(
      "/api/chat/conversaciones/" + selectedId + "/mensajes"
    );
    updateMessages(messages);
    await request("/api/chat/conversaciones/" + selectedId + "/leido", { method: "POST" });
    await refreshBadge();
  }

  function schedulePoll() {
    if (pollingStopped) return;
    window.clearTimeout(pollTimer);
    pollTimer = window.setTimeout(async function () {
      try {
        if (panel.hidden) {
          await refreshBadge();
        } else {
          await refreshOpenConversation();
        }
      } catch (error) {
        handleError(error);
      }
      schedulePoll();
    }, panel.hidden ? 15000 : 4000);
  }

  window.openChatConversation = async function (conversationId) {
    panel.hidden = false;
    toggle.setAttribute("aria-expanded", "true");
    selectedId = null;
    await refreshConversations();
    await openConversation(Number(conversationId));
    schedulePoll();
  };

  function escapeHtml(value) {
    return String(value).replace(/[&<>"']/g, function (character) {
      return { "&": "&amp;", "<": "&lt;", ">": "&gt;", "\"": "&quot;", "'": "&#039;" }[character];
    });
  }

  toggle.addEventListener("click", async function () {
    const isOpen = !panel.hidden;
    panel.hidden = isOpen;
    toggle.setAttribute("aria-expanded", String(!isOpen));
    if (!isOpen) {
      selectedId = null;
      try {
        await refreshConversations();
      } catch (error) {
        handleError(error);
      }
    }
    schedulePoll();
  });

  close.addEventListener("click", function () {
    panel.hidden = true;
    selectedId = null;
    toggle.setAttribute("aria-expanded", "false");
    schedulePoll();
  });

  back.addEventListener("click", function () {
    selectedId = null;
    renderList();
    schedulePoll();
  });

  document.querySelectorAll(".chat-open-conversation").forEach(function (item) {
    item.addEventListener("click", function () {
      window.openChatConversation(item.dataset.conversationId).catch(handleError);
    });
  });

  refreshConversations().catch(handleError);
  schedulePoll();
  window.addEventListener("beforeunload", function () {
    window.clearTimeout(pollTimer);
  });
})();
