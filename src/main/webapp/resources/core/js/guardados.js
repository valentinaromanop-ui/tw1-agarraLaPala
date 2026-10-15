function rutaAplicacion(ruta) {
  const script = document.querySelector("script[src*=\"/js/guardados.js\"]");
  if (!script) {
    return ruta;
  }
  const url = new URL(script.src, window.location.href);
  const indice = url.pathname.indexOf("/js/guardados.js");
  const contexto = indice >= 0 ? url.pathname.slice(0, indice) : "";
  return `${contexto}${ruta}`;
}

function aplicarEstadoGuardado(boton, guardado) {
  boton.classList.toggle("is-saved", guardado);
  boton.dataset.guardado = String(guardado);
  boton.setAttribute("aria-pressed", String(guardado));
  const descripcion = guardado ? "Quitar de guardados" : "Guardar empleo";
  boton.setAttribute("aria-label", descripcion);
  boton.title = descripcion;
  const icono = boton.querySelector("i");
  if (icono) {
    icono.className = guardado ? "bi bi-bookmark-fill" : "bi bi-bookmark";
  }
}

function datosOferta(boton) {
  const tarjeta = boton.closest(".offer-card");
  return {
    idOferta: boton.dataset.ofertaId || tarjeta?.dataset.ofertaId || "",
    titulo: boton.dataset.titulo || tarjeta?.querySelector(".offer-info h2, .offer-info h3")?.textContent.trim() || "",
    empresa: boton.dataset.empresa || tarjeta?.querySelector(".offer-info > p")?.textContent.trim() || "",
    ubicacion: boton.dataset.ubicacion || "",
    modalidad: boton.dataset.modalidad || "",
    url: boton.dataset.url || "",
  };
}

function tokenCsrf() {
  const meta = document.querySelector("meta[name='_csrf']");
  const input = document.querySelector("input[name='_csrf']");
  return meta?.content || input?.value || "";
}

async function respuestaJson(respuesta) {
  const contenido = await respuesta.text();
  try {
    return contenido ? JSON.parse(contenido) : {};
  } catch {
    throw new Error(
      `El servidor devolvió una respuesta inválida (${respuesta.status}).`
    );
  }
}

function mostrarAviso(mensaje) {
  let aviso = document.querySelector(".saved-jobs-notice");
  if (!aviso) {
    aviso = document.createElement("p");
    aviso.className = "saved-jobs-notice";
    aviso.setAttribute("role", "status");
    document.body.append(aviso);
  }
  aviso.textContent = mensaje;
  window.clearTimeout(aviso.timeoutId);
  aviso.timeoutId = window.setTimeout(() => aviso.remove(), 3500);
}

function mostrarEstadoVacio() {
  const lista = document.querySelector("#empleos-guardados-lista");
  if (lista) {
    lista.innerHTML = `
      <p class="empty-vacancies" role="status">Todavía no guardaste empleos</p>
      <a class="primary-action" href="${rutaAplicacion("/vacantes/recomendadas")}">Ver vacantes</a>
    `;
  }
}

async function alternarGuardado(boton) {
  const datos = datosOferta(boton);
  if (!datos.idOferta) {
    return;
  }
  const estabaGuardado = boton.dataset.guardado === "true";
  const csrf = tokenCsrf();
  const headers = { "Content-Type": "application/x-www-form-urlencoded" };
  if (csrf) {
    headers["X-CSRF-TOKEN"] = csrf;
  }
  boton.disabled = true;
  try {
    const respuesta = await fetch(
      rutaAplicacion(estabaGuardado ? "/empleos-guardados/quitar" : "/empleos-guardados"),
      {
        method: "POST",
        headers,
        body: new URLSearchParams(datos),
      }
    );
    if (respuesta.status === 401) {
      window.location.href = rutaAplicacion("/login");
      return;
    }
    const resultado = await respuestaJson(respuesta);
    if (!respuesta.ok || resultado.ok === false) {
      throw new Error(resultado.mensaje || "No se pudo actualizar el empleo guardado.");
    }
    const nuevoEstado = !estabaGuardado;
    if (nuevoEstado) {
      idsGuardados.add(datos.idOferta);
    } else {
      idsGuardados.delete(datos.idOferta);
    }
    aplicarEstadoGuardado(boton, nuevoEstado);
    if (estabaGuardado && document.body.classList.contains("saved-jobs-page")) {
      const tarjeta = boton.closest(".offer-card");
      if (tarjeta) {
        tarjeta.classList.add("is-removing");
        window.setTimeout(() => {
          tarjeta.remove();
          const lista = document.querySelector("#empleos-guardados-lista");
          if (lista && !lista.querySelector(".offer-card")) {
            mostrarEstadoVacio();
          }
        }, 220);
      }
    }
  } catch (error) {
    aplicarEstadoGuardado(boton, estabaGuardado);
    mostrarAviso("No se pudo actualizar el empleo guardado. Intentá nuevamente.");
    console.error(error);
  } finally {
    boton.disabled = false;
  }
}

async function cargarIdsGuardados() {
  const respuesta = await fetch(rutaAplicacion("/empleos-guardados/ids"));
  if (respuesta.status === 401) {
    return [];
  }
  if (!respuesta.ok) {
    throw new Error("No se pudieron cargar los empleos guardados.");
  }
  return respuesta.json();
}

let idsGuardados = new Set();

function sincronizarBotones() {
  document.querySelectorAll(".save-job-button").forEach((boton) => {
    const datos = datosOferta(boton);
    aplicarEstadoGuardado(boton, idsGuardados.has(datos.idOferta));
  });
}

document.addEventListener("click", (evento) => {
  const boton = evento.target.closest(".save-job-button");
  if (boton && !boton.disabled) {
    alternarGuardado(boton);
  }
});

document.addEventListener("guardados:actualizar", sincronizarBotones);

async function iniciarGuardados() {
  document.querySelectorAll(".offer-card").forEach((tarjeta) => {
    if (tarjeta.querySelector(".save-job-button")) {
      return;
    }
    const titulo = tarjeta.querySelector(".offer-info h2, .offer-info h3")?.textContent.trim() || "Oferta sin título";
    const empresa = tarjeta.querySelector(".offer-info > p")?.textContent.trim() || "Empresa no informada";
    const boton = document.createElement("button");
    boton.type = "button";
    boton.className = "save-job-button";
    boton.dataset.ofertaId = `DEMO:${titulo}|${empresa}`;
    boton.dataset.titulo = titulo;
    boton.dataset.empresa = empresa;
    aplicarEstadoGuardado(boton, false);
    boton.innerHTML = "<i class=\"bi bi-bookmark\" aria-hidden=\"true\"></i>";
    tarjeta.prepend(boton);
  });
  const botones = [...document.querySelectorAll(".save-job-button")];
  if (botones.length === 0) {
    return;
  }
  try {
    idsGuardados = new Set((await cargarIdsGuardados()).map((id) => String(id)));
    sincronizarBotones();
  } catch (error) {
    console.error(error);
  }
}

iniciarGuardados();
