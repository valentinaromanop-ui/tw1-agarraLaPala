import { JSDOM } from "jsdom";

const widgetMarkup = `
  <div id="chat-widget" data-api-base="/api/chat">
    <button id="chat-widget-toggle"></button><span id="chat-widget-badge" hidden></span>
    <section id="chat-widget-panel" hidden>
      <button id="chat-widget-close"></button><h2 id="chat-widget-title"></h2>
      <div id="chat-widget-list-view"></div>
      <div id="chat-widget-conversation-view" hidden>
        <button id="chat-widget-back"></button><div id="chat-widget-messages"></div>
        <form id="chat-widget-form"><input id="chat-widget-input"><button>Enviar</button></form>
      </div>
    </section>
  </div>`;

describe("Chat Widget", function() {
  let createChatWidget;
  let dom;
  let calls;

  beforeAll(async function() {
    dom = new JSDOM("<!doctype html><body></body>", { url: "http://localhost" });
    globalThis.window = dom.window;
    globalThis.document = dom.window.document;
    ({ createChatWidget } = await import("../chat-widget.js"));
  });

  beforeEach(function() {
    document.body.innerHTML = widgetMarkup;
    calls = [];
  });

  function fakeFetch(url, options = {}) {
    calls.push({ url, options });
    if (url.endsWith("/conversaciones") && options.method !== "POST") {
      return Promise.resolve({
        ok: true,
        json: async () => [{ postulacionId: 1, otroParticipante: "ACME", puesto: "Dev", ultimoMensaje: "Hola", fechaUltimoMensaje: "2026-01-01T10:00:00", noLeidos: 1 }]
      });
    }
    if (url.includes("/mensajes") && options.method === "POST") {
      return Promise.resolve({ ok: true, json: async () => ({ emisorId: 7, texto: "Nuevo", fecha: "2026-01-01T10:01:00" }) });
    }
    return Promise.resolve({ ok: true, json: async () => [{ emisorId: 8, texto: "Hola", fecha: "2026-01-01T10:00:00" }] });
  }

  it("abre y cierra el panel y muestra el badge", async function() {
    const root = document.querySelector("#chat-widget");
    const widget = createChatWidget(root, fakeFetch);
    await widget.loadConversations();
    expect(root.querySelector("#chat-widget-badge").textContent).toBe("1");
    root.querySelector("#chat-widget-toggle").click();
    expect(root.querySelector("#chat-widget-panel").hidden).toBe(false);
    root.querySelector("#chat-widget-close").click();
    expect(root.querySelector("#chat-widget-panel").hidden).toBe(true);
    widget.destroy();
  });

  it("abre una conversación y envía un mensaje", async function() {
    const root = document.querySelector("#chat-widget");
    const widget = createChatWidget(root, fakeFetch);
    await widget.loadConversations();
    root.querySelector(".chat-widget-conversation").click();
    await new Promise(resolve => setTimeout(resolve, 0));
    expect(root.querySelector("#chat-widget-conversation-view").hidden).toBe(false);
    root.querySelector("#chat-widget-input").value = "Nuevo";
    root.querySelector("#chat-widget-form").dispatchEvent(new window.Event("submit", { bubbles: true, cancelable: true }));
    await new Promise(resolve => setTimeout(resolve, 0));
    expect(calls.some(call => call.options.method === "POST" && call.url.includes("/mensajes"))).toBe(true);
    widget.destroy();
  });

  it("muestra el estado vacío", async function() {
    const root = document.querySelector("#chat-widget");
    const widget = createChatWidget(root, async () => ({ ok: true, json: async () => [] }));
    await widget.loadConversations();

    expect(root.querySelector(".chat-widget-empty").textContent).toBe("No tenés conversaciones todavía");
    widget.destroy();
  });

  it("muestra un mensaje de error si falla la carga de conversaciones", async function() {
    const root = document.querySelector("#chat-widget");
    const widget = createChatWidget(root, async () => Promise.reject(new Error("network")));
    await new Promise(resolve => setTimeout(resolve, 0));

    expect(root.querySelector(".chat-widget-empty").textContent)
      .toBe("No pudimos cargar tus conversaciones. Intentá nuevamente.");
    widget.destroy();
  });
});
