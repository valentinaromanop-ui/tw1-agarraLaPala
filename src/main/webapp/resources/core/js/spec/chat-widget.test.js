import { JSDOM } from "jsdom";

const widgetMarkup = `
  <div id="chat-widget" data-api-base="/api/chat" data-user-id="7">
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
  let messages;
  let subscriptions;
  let publications;

  beforeAll(async function() {
    dom = new JSDOM("<!doctype html><body></body>", { url: "http://localhost" });
    globalThis.window = dom.window;
    globalThis.document = dom.window.document;
    ({ createChatWidget } = await import("../chat-widget.js"));
  });

  beforeEach(function() {
    document.body.innerHTML = widgetMarkup;
    calls = [];
    messages = [{ emisorId: 8, texto: "Hola", fecha: "2026-01-01T10:00:00" }];
    subscriptions = [];
    publications = [];
    window.StompJs = {
      Client: class {
        constructor() {
          this.connected = false;
        }
        activate() {
          this.connected = true;
          this.onConnect();
        }
        subscribe(destination, callback) {
          subscriptions.push(callback);
          return { unsubscribe() {} };
        }
        publish(publication) {
          publications.push(publication);
          const message = JSON.parse(publication.body);
          subscriptions.forEach(callback => callback({ body: JSON.stringify({
            id: 2, emisorId: 7, texto: message.texto, fecha: "2026-01-01T10:01:00"
          }) }));
        }
        deactivate() {
          this.connected = false;
        }
      }
    };
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
      const sent = { emisorId: 7, texto: JSON.parse(options.body).texto, fecha: "2026-01-01T10:01:00" };
      messages.push(sent);
      return Promise.resolve({ ok: true, json: async () => sent });
    }
    return Promise.resolve({ ok: true, json: async () => messages });
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
    expect(root.querySelector("#chat-widget-messages").textContent).toContain("Hola");
    expect(root.querySelector(".chat-widget-message form button").textContent).toBe("Reportar");
    root.querySelector("#chat-widget-input").value = "Nuevo";
    root.querySelector("#chat-widget-form").dispatchEvent(new window.Event("submit", { bubbles: true, cancelable: true }));
    await new Promise(resolve => setTimeout(resolve, 0));
    expect(publications[0].destination).toBe("/app/chat/1");
    expect(root.querySelector("#chat-widget-input").value).toBe("");
    expect(root.querySelector("#chat-widget-messages").textContent).toContain("Nuevo");
    widget.destroy();
  });

  it("muestra el estado vacío", async function() {
    const root = document.querySelector("#chat-widget");
    const widget = createChatWidget(root, async () => ({ ok: true, json: async () => [] }));
    await widget.loadConversations();
    expect(root.querySelector(".chat-widget-empty").textContent).toBe("No tenés conversaciones todavía");
    widget.destroy();
  });
});
