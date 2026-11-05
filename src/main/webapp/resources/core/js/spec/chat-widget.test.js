import { JSDOM } from "jsdom";
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const script = fs.readFileSync(
  path.join(path.dirname(fileURLToPath(import.meta.url)), "..", "chat-widget.js"),
  "utf8"
);

function cargarWidget(fetchMock, html, timers) {
  const dom = new JSDOM(html, { runScripts: "outside-only", url: "http://localhost" });
  dom.window.fetch = fetchMock;
  dom.window.eval(script);
  if (timers) {
    dom.window.setTimeout = function(callback) {
      timers.push(callback);
      return timers.length;
    };
    dom.window.clearTimeout = function() {};
  }
  return dom;
}

describe("Widget de chat", function() {
  it("no se renderiza sin sesión", function() {
    const fetchMock = jasmine.createSpy("fetch");
    const dom = cargarWidget(fetchMock, "<body></body>");

    expect(dom.window.document.querySelector("#chat-widget")).toBeNull();
    expect(fetchMock).not.toHaveBeenCalled();
    dom.window.close();
  });

  it("muestra el estado vacío y alterna el panel", async function() {
    const fetchMock = jasmine.createSpy("fetch").and.callFake(function(url) {
      const body = url.endsWith("no-leidos") ? { cantidad: 0 } : [];
      return Promise.resolve({ ok: true, json: () => Promise.resolve(body) });
    });
    const dom = cargarWidget(fetchMock,
      "<body><div id=\"chat-widget\" data-user-id=\"7\">" +
      "<button id=\"chat-widget-toggle\"></button><span id=\"chat-widget-badge\"></span>" +
      "<section id=\"chat-widget-panel\" hidden><header><button id=\"chat-widget-back\"></button>" +
      "<h2 id=\"chat-widget-title\"></h2><button id=\"chat-widget-close\"></button></header>" +
      "<div id=\"chat-widget-content\"></div></section></div>");

    expect(dom.window.document.querySelector("#chat-widget-panel").hidden).toBeTrue();
    await new Promise((resolve) => setTimeout(resolve, 0));

    expect(dom.window.document.querySelector("#chat-widget-content").textContent)
      .toContain("No tenés conversaciones todavía");
    dom.window.document.querySelector("#chat-widget-toggle").click();

    expect(dom.window.document.querySelector("#chat-widget-panel").hidden).toBeFalse();
    dom.window.document.querySelector("#chat-widget-close").click();

    expect(dom.window.document.querySelector("#chat-widget-panel").hidden).toBeTrue();
    dom.window.close();
  });

  it("la burbuja abre el panel y la x lo cierra", async function() {
    const fetchMock = jasmine.createSpy("fetch").and.callFake(function(url) {
      const body = url.endsWith("no-leidos") ? { cantidad: 0 } : [];
      return Promise.resolve({ ok: true, json: () => Promise.resolve(body) });
    });
    const dom = cargarWidget(fetchMock,
      "<body><div id=\"chat-widget\" data-user-id=\"7\">" +
      "<button id=\"chat-widget-toggle\"></button><span id=\"chat-widget-badge\"></span>" +
      "<section id=\"chat-widget-panel\" hidden><header><button id=\"chat-widget-back\"></button>" +
      "<h2 id=\"chat-widget-title\"></h2><button id=\"chat-widget-close\"></button></header>" +
      "<div id=\"chat-widget-content\"></div></section></div>");

    dom.window.document.querySelector("#chat-widget-toggle").click();
    expect(dom.window.document.querySelector("#chat-widget-panel").hidden).toBeFalse();
    dom.window.document.querySelector("#chat-widget-close").click();
    expect(dom.window.document.querySelector("#chat-widget-panel").hidden).toBeTrue();
    dom.window.close();
  });

  it("abre desde postulantes la conversación aceptada", async function() {
    const fetchMock = jasmine.createSpy("fetch").and.callFake(function(url) {
      if (url.endsWith("no-leidos")) {
        return Promise.resolve({ ok: true, json: () => Promise.resolve({ cantidad: 0 }) });
      }
      if (url.endsWith("/conversaciones/42/mensajes")) {
        return Promise.resolve({ ok: true, json: () => Promise.resolve([]) });
      }
      return Promise.resolve({ ok: true, json: () => Promise.resolve([]) });
    });
    const dom = cargarWidget(fetchMock,
      "<body><button class=\"chat-open-conversation\" data-conversation-id=\"42\"></button>" +
      "<div id=\"chat-widget\" data-user-id=\"7\"><button id=\"chat-widget-toggle\"></button>" +
      "<span id=\"chat-widget-badge\"></span><section id=\"chat-widget-panel\" hidden>" +
      "<header><button id=\"chat-widget-back\"></button><h2 id=\"chat-widget-title\"></h2>" +
      "<button id=\"chat-widget-close\"></button></header><div id=\"chat-widget-content\"></div>" +
      "</section></div>");

    await new Promise((resolve) => setTimeout(resolve, 0));
    dom.window.document.querySelector(".chat-open-conversation").click();
    await new Promise((resolve) => setTimeout(resolve, 0));

    expect(dom.window.document.querySelector("#chat-widget-panel").hidden).toBeFalse();
    expect(fetchMock.calls.allArgs().some((args) =>
      args[0] === "/api/chat/conversaciones/42/mensajes"
    )).toBeTrue();
    expect(fetchMock.calls.allArgs().some((args) =>
      args[0] === "/api/chat/conversaciones/42/leido" &&
      args[1].method === "POST"
    )).toBeTrue();
    dom.window.close();
  });

  it("conserva el texto y el foco durante el polling", async function() {
    const timers = [];
    let historyCalls = 0;
    const fetchMock = jasmine.createSpy("fetch").and.callFake(function(url) {
      if (url.endsWith("/conversaciones/42/mensajes")) {
        historyCalls += 1;
        const messages = historyCalls === 1 ? [] : [{
          id: 8,
          contenido: "Mensaje nuevo",
          remitenteId: 9
        }];
        return Promise.resolve({ ok: true, json: () => Promise.resolve(messages) });
      }
      if (url.endsWith("no-leidos")) {
        return Promise.resolve({ ok: true, json: () => Promise.resolve({ cantidad: 0 }) });
      }
      if (url.endsWith("/leido")) {
        return Promise.resolve({ ok: true, json: () => Promise.resolve({ cantidad: 0 }) });
      }
      return Promise.resolve({ ok: true, json: () => Promise.resolve([]) });
    });
    const dom = cargarWidget(fetchMock,
      "<body><div id=\"chat-widget\" data-user-id=\"7\">" +
      "<button id=\"chat-widget-toggle\"></button><span id=\"chat-widget-badge\"></span>" +
      "<section id=\"chat-widget-panel\" hidden><header><button id=\"chat-widget-back\"></button>" +
      "<h2 id=\"chat-widget-title\"></h2><button id=\"chat-widget-close\"></button></header>" +
      "<div id=\"chat-widget-content\"></div></section></div>", timers);

    await dom.window.openChatConversation(42);
    const input = dom.window.document.querySelector("input[name=\"contenido\"]");
    input.value = "Texto en progreso";
    input.focus();
    const focusedInput = dom.window.document.activeElement;

    const poll = timers[timers.length - 1];
    await poll();

    expect(input.value).toBe("Texto en progreso");
    expect(dom.window.document.activeElement).toBe(focusedInput);
    expect(dom.window.document.querySelectorAll(".chat-widget-message").length).toBe(1);
    dom.window.close();
  });
});
