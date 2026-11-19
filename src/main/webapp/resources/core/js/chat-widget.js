function formatTime(value) {
  if (!value) return "";
  return new Date(value).toLocaleTimeString("es-AR", { hour: "2-digit", minute: "2-digit" });
}

function normalizeConversation(item) {
  return {
    ...item,
    postulacionId: item.postulacionId ?? item.id,
    fechaUltimoMensaje: item.fechaUltimoMensaje ?? item.hora
  };
}

function normalizeMessage(item) {
  return {
    ...item,
    texto: item.texto ?? item.contenido,
    emisorId: item.emisorId ?? item.remitenteId
  };
}

export function createChatWidget(root, http = window.fetch.bind(window)) {
  const toggle = root.querySelector("#chat-widget-toggle");
  const panel = root.querySelector("#chat-widget-panel");
  const badge = root.querySelector("#chat-widget-badge");
  const listView = root.querySelector("#chat-widget-list-view");
  const conversationView = root.querySelector("#chat-widget-conversation-view");
  const messages = root.querySelector("#chat-widget-messages");
  const title = root.querySelector("#chat-widget-title");
  const apiBase = root.dataset.apiBase;
  const userId = Number(root.dataset.userId);
  let conversations = [];
  let selected = null;
  let timer;
  let stompClient = null;
  let subscription = null;
  let pendingMessages = [];
  let historyLoaded = false;
  let renderedMessageKeys = new Set();
  let queuedMessages = [];

  async function request(url, options = {}) {
    const response = await http(url, { credentials: "same-origin", ...options });
    if (!response.ok) throw new Error(`Error de chat (${response.status})`);
    return response.status === 204 ? null : response.json();
  }

  function updateBadge() {
    const count = conversations.reduce((total, item) => total + item.noLeidos, 0);
    badge.textContent = count > 9 ? "9+" : String(count);
    badge.hidden = count === 0;
  }

  function renderError() {
    listView.replaceChildren();
    const error = document.createElement("p");
    error.className = "chat-widget-empty";
    error.textContent = "No pudimos cargar tus conversaciones. Intentá nuevamente.";
    listView.append(error);
  }

  function renderList() {
    listView.replaceChildren();
    if (!conversations.length) {
      const empty = document.createElement("p");
      empty.className = "chat-widget-empty";
      empty.textContent = "No tenés conversaciones todavía";
      listView.append(empty);
      return;
    }

    conversations.forEach(item => {
      const button = document.createElement("button");
      button.type = "button";
      button.className = `chat-widget-conversation ${item.noLeidos ? "unread" : ""}`;
      button.dataset.id = item.postulacionId;
      button.innerHTML = `<span class="chat-widget-avatar">${item.otroParticipante.slice(0, 1)}</span>
        <span class="chat-widget-conversation-data"><strong></strong><small></small><span></span></span>
        <time></time>`;
      button.querySelector("strong").textContent = item.otroParticipante;
      button.querySelector("small").textContent = item.puesto || "Postulación";
      button.querySelector("span span").textContent = item.ultimoMensaje;
      button.querySelector("time").textContent = formatTime(item.fechaUltimoMensaje);
      button.addEventListener("click", () => openConversation(item));
      listView.append(button);
    });
  }

  async function loadConversations() {
    conversations = ((await request(`${apiBase}/conversaciones`)) || []).map(normalizeConversation);
    conversations.sort((a, b) => new Date(b.fechaUltimoMensaje) - new Date(a.fechaUltimoMensaje));
    updateBadge();
    if (!selected) renderList();
  }

  function messageKey(item) {
    return item.id != null
      ? `id:${item.id}`
      : `${item.emisorId}:${item.fecha}:${item.texto}`;
  }

  function appendMessage(item) {
    const key = messageKey(item);
    if (renderedMessageKeys.has(key)) return;
    renderedMessageKeys.add(key);

    const mine = Number(item.emisorId) === userId;
    const row = document.createElement("div");
    row.className = `chat-widget-message ${mine ? "mine" : "other"}`;
    const bubble = document.createElement("div");
    bubble.className = `chat-widget-bubble ${mine ? "mine" : "other"}`;
    bubble.textContent = item.texto;
    row.append(bubble);
    if (!mine) {
      const report = document.createElement("form");
      report.method = "post";
      report.action = "/reportes";
      report.innerHTML = `<input type="hidden" name="tipo" value="MENSAJE">
        <input type="hidden" name="motivo" value="Mensaje ofensivo">
        <button type="submit" class="btn btn-link btn-sm text-danger">Reportar</button>`;
      const messageId = document.createElement("input");
      messageId.type = "hidden";
      messageId.name = "objetoId";
      messageId.value = item.id;
      const reportedUserId = document.createElement("input");
      reportedUserId.type = "hidden";
      reportedUserId.name = "usuarioReportadoId";
      reportedUserId.value = item.emisorId;
      report.prepend(reportedUserId, messageId);
      row.append(report);
    }
    messages.append(row);
    messages.scrollTop = messages.scrollHeight;
  }

  function renderMessages(items) {
    messages.replaceChildren();
    renderedMessageKeys = new Set();
    items.map(normalizeMessage)
      .sort((a, b) => new Date(a.fecha) - new Date(b.fecha))
      .forEach(appendMessage);
    historyLoaded = true;
    queuedMessages.forEach(appendMessage);
    queuedMessages = [];
  }

  function stopConversation() {
    historyLoaded = false;
    queuedMessages = [];
    pendingMessages = [];
    if (subscription) subscription.unsubscribe();
    subscription = null;
    if (stompClient) stompClient.deactivate();
    stompClient = null;
  }

  async function connectConversation(item) {
    stopConversation();
    historyLoaded = false;
    const currentClient = new window.StompJs.Client({
      brokerURL: (window.location.protocol === "https:" ? "wss://" : "ws://")
        + window.location.host + "/ws-chat"
    });
    stompClient = currentClient;
    currentClient.onConnect = async () => {
      if (stompClient !== currentClient || !selected || selected.postulacionId !== item.postulacionId) return;
      subscription = currentClient.subscribe(`/tema/chat/${item.postulacionId}`, frame => {
        const incoming = normalizeMessage(JSON.parse(frame.body));
        if (historyLoaded) appendMessage(incoming);
        else queuedMessages.push(incoming);
      });
      try {
        const items = await request(`${apiBase}/conversaciones/${item.postulacionId}/mensajes`);
        if (stompClient !== currentClient || !selected || selected.postulacionId !== item.postulacionId) return;
        renderMessages(items || []);
        item.noLeidos = 0;
        updateBadge();
        await request(`${apiBase}/conversaciones/${item.postulacionId}/leido`, { method: "POST" });
        pendingMessages.forEach(texto => currentClient.publish({
          destination: `/app/chat/${item.postulacionId}`,
          body: JSON.stringify({ texto })
        }));
        pendingMessages = [];
      } catch (error) {
        console.error("No se pudieron cargar los mensajes", error);
      }
    };
    currentClient.onStompError = error => console.error("Error en el chat", error);
    currentClient.activate();
  }

  async function openConversation(item) {
    selected = item;
    title.textContent = item.otroParticipante;
    listView.hidden = true;
    conversationView.hidden = false;
    messages.replaceChildren();
    await connectConversation(item);
  }

  function closeConversation() {
    stopConversation();
    selected = null;
    title.textContent = "Mensajes";
    conversationView.hidden = true;
    listView.hidden = false;
    renderList();
  }

  async function sendMessage(event) {
    event.preventDefault();
    const input = root.querySelector("#chat-widget-input");
    if (!selected || !input.value.trim()) return;
    const texto = input.value.trim();
    input.value = "";
    if (stompClient && stompClient.connected) {
      stompClient.publish({
        destination: `/app/chat/${selected.postulacionId}`,
        body: JSON.stringify({ texto })
      });
    } else {
      pendingMessages.push(texto);
    }
  }

  toggle.addEventListener("click", async () => {
    const isOpen = !panel.hidden;
    panel.hidden = isOpen;
    toggle.setAttribute("aria-expanded", String(!isOpen));
    if (isOpen) {
      stopConversation();
    } else {
      await loadConversations();
      if (selected) await connectConversation(selected);
    }
  });
  root.querySelector("#chat-widget-close").addEventListener("click", () => {
    panel.hidden = true;
    toggle.setAttribute("aria-expanded", "false");
    stopConversation();
  });
  root.querySelector("#chat-widget-back").addEventListener("click", closeConversation);
  root.querySelector("#chat-widget-form").addEventListener("submit", sendMessage);
  loadConversations().catch(error => {
    console.error("No se pudieron cargar las conversaciones", error);
    renderError();
  });
  timer = window.setInterval(() => loadConversations().catch(() => {}), 4000);

  return {
    destroy() {
      window.clearInterval(timer);
      stopConversation();
    },
    loadConversations
  };
}

const root = document.querySelector("#chat-widget");
if (root) createChatWidget(root);
