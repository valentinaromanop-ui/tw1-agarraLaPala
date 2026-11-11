function formatTime(value) {
  if (!value) return "";
  return new Date(value).toLocaleTimeString("es-AR", { hour: "2-digit", minute: "2-digit" });
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

  function renderList() {
    listView.replaceChildren();
    if (!conversations.length) {
      const empty = document.createElement("p");
      empty.className = "chat-widget-empty";
      empty.textContent = "No tenés conversaciones todavía";
      listView.append(empty);
      return;
    }

    function renderError() {
      listView.replaceChildren();
      const error = document.createElement("p");
      error.className = "chat-widget-empty";
      error.textContent = "No pudimos cargar tus conversaciones. Intentá nuevamente.";
      listView.append(error);
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
    conversations = (await request(`${apiBase}/conversaciones`)) || [];
    conversations.sort((a, b) => new Date(b.fechaUltimoMensaje) - new Date(a.fechaUltimoMensaje));
    updateBadge();
    if (!selected) renderList();
  }

  function renderMessages(items) {
    messages.replaceChildren();
    items.forEach(item => {
      const row = document.createElement("div");
      row.className = "chat-widget-message";
      const bubble = document.createElement("div");
      bubble.className = `chat-widget-bubble ${item.emisorId === userId ? "mine" : "other"}`;
      bubble.textContent = item.texto;
      row.append(bubble);
      messages.append(row);
    });
    messages.scrollTop = messages.scrollHeight;
  }

  async function openConversation(item) {
    selected = item;
    title.textContent = item.otroParticipante;
    listView.hidden = true;
    conversationView.hidden = false;
    const items = await request(`${apiBase}/conversaciones/${item.postulacionId}/mensajes`);
    renderMessages(items || []);
    item.noLeidos = 0;
    updateBadge();
    await request(`${apiBase}/conversaciones/${item.postulacionId}/leidos`, { method: "POST" });
  }

  function closeConversation() {
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
    const sent = await request(`${apiBase}/conversaciones/${selected.postulacionId}/mensajes`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ texto: input.value })
    });
    input.value = "";
    const current = await request(`${apiBase}/conversaciones/${selected.postulacionId}/mensajes`);
    renderMessages(current || [sent]);
    await loadConversations();
  }

  toggle.addEventListener("click", async () => {
    const isOpen = !panel.hidden;
    panel.hidden = isOpen;
    toggle.setAttribute("aria-expanded", String(!isOpen));
    if (!isOpen) await loadConversations();
  });
  root.querySelector("#chat-widget-close").addEventListener("click", () => {
    panel.hidden = true;
    toggle.setAttribute("aria-expanded", "false");
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
    },
    loadConversations
  };
}

const root = document.querySelector("#chat-widget");
if (root) createChatWidget(root);
