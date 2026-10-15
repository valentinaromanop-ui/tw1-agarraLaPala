export function normalizarTexto(texto) {
  return String(texto || "")
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .toLocaleLowerCase("es");
}

export function filtrarCargos(cargos, consulta) {
  const texto = normalizarTexto(consulta);
  return cargos.filter((cargo) => normalizarTexto(cargo).includes(texto));
}

export function obtenerRangoCoincidente(texto, consulta) {
  const consultaNormalizada = normalizarTexto(consulta);
  if (!consultaNormalizada) {
    return null;
  }

  let textoNormalizado = "";
  const posicionesOriginales = [];
  // Relaciona cada carácter sin tilde con su posición para resaltar el texto original.
  for (let indice = 0; indice < texto.length;) {
    const caracter = String.fromCodePoint(texto.codePointAt(indice));
    const normalizado = normalizarTexto(caracter);
    for (let posicion = 0; posicion < normalizado.length; posicion += 1) {
      posicionesOriginales.push({ inicio: indice, fin: indice + caracter.length });
    }
    textoNormalizado += normalizado;
    indice += caracter.length;
  }

  const inicioNormalizado = textoNormalizado.indexOf(consultaNormalizada);
  if (inicioNormalizado < 0) {
    return null;
  }
  const finalNormalizado = inicioNormalizado + consultaNormalizada.length - 1;
  return {
    inicio: posicionesOriginales[inicioNormalizado].inicio,
    fin: posicionesOriginales[finalNormalizado].fin,
  };
}

function marcarCoincidencia(boton, cargo, consulta) {
  const rango = obtenerRangoCoincidente(cargo, consulta);
  if (!rango) {
    boton.textContent = cargo;
    return;
  }
  boton.append(document.createTextNode(cargo.slice(0, rango.inicio)));
  const coincidencia = document.createElement("strong");
  coincidencia.textContent = cargo.slice(rango.inicio, rango.fin);
  boton.append(coincidencia);
  boton.append(document.createTextNode(cargo.slice(rango.fin)));
}

function obtenerOfertaIdDeTarjeta(tarjeta) {
  if (tarjeta.dataset.ofertaId) {
    return tarjeta.dataset.ofertaId;
  }
  const titulo = tarjeta.querySelector(".offer-info h3")?.textContent || "Oferta sin título";
  const empresa = tarjeta.querySelector(".offer-info > p")?.textContent || "Empresa no informada";
  return `${titulo} | ${empresa}`;
}

function normalizarOfertaId(ofertaId) {
  return String(ofertaId || "").trim().toUpperCase();
}

function rutaAplicacion(ruta) {
  const script = document.querySelector("script[src*=\"/js/busqueda.js\"]");
  if (!script) {
    return ruta;
  }
  const url = new URL(script.src, window.location.href);
  const indice = url.pathname.indexOf("/js/busqueda.js");
  const contexto = indice >= 0 ? url.pathname.slice(0, indice) : "";
  return `${contexto}${ruta}`;
}

async function enviarPostulacion(boton, ofertaId) {
  boton.disabled = true;
  try {
    const respuesta = await fetch(rutaAplicacion("/postulaciones"), {
      method: "POST",
      headers: { "Content-Type": "application/x-www-form-urlencoded" },
      body: new URLSearchParams({
        idOferta: ofertaId,
        titulo: boton.dataset.titulo || "Oferta sin título",
        empresa: boton.dataset.empresa || "Empresa no informada",
        ubicacion: boton.dataset.ubicacion || "Ubicación no informada",
        modalidad: boton.dataset.modalidad || "Modalidad no informada",
        url: boton.dataset.url || "",
      }),
    });
    const contentType = respuesta.headers.get("content-type") || "";
    if (!contentType.toLowerCase().includes("application/json")) {
      throw new Error("No pudimos procesar tu postulación, intentá de nuevo.");
    }
    const resultado = await respuesta.json();
    if (respuesta.status === 401) {
      window.location.href = rutaAplicacion("/login");
      return;
    }
    if (!respuesta.ok || resultado.ok === false) {
      throw new Error(resultado.mensaje || "No se pudo enviar la postulación");
    }
    marcarComoPostulado(boton);
  } catch (error) {
    console.error(error);
    boton.disabled = false;
    boton.textContent = error instanceof TypeError
      ? "No pudimos procesar tu postulación, intentá de nuevo."
      : error.message || "No se pudo enviar la postulación";
  }
}

function marcarComoPostulado(boton) {
  boton.textContent = "✓ Postulado";
  boton.disabled = true;
  boton.classList.add("postulado");
}

function configurarBotonPostulacion(boton, ofertaId) {
  boton.addEventListener("click", () => enviarPostulacion(boton, ofertaId));
}

let postulacionesRegistradas = [];

function actualizarBotonesPostulados() {
  document.querySelectorAll(".apply-button").forEach((boton) => {
    if (
      postulacionesRegistradas
        .map(normalizarOfertaId)
        .includes(normalizarOfertaId(boton.dataset.ofertaId))
    ) {
      marcarComoPostulado(boton);
    }
  });
}

function iniciarPostulaciones() {
  document.querySelectorAll(".apply-button").forEach((boton) => {
    if (boton.classList.contains("home-offer-redirect")) {
      boton.addEventListener("click", () => {
        window.location.href = rutaAplicacion("/vacantes/recomendadas");
      });
      return;
    }
    const tarjeta = boton.closest(".offer-card");
    configurarBotonPostulacion(
      boton,
      boton.dataset.ofertaId || obtenerOfertaIdDeTarjeta(tarjeta)
    );
  });
  fetch(rutaAplicacion("/postulaciones/ids"))
    .then((respuesta) => {
      if (respuesta.status === 401) {
        return [];
      }
      if (!respuesta.ok) {
        throw new Error("No se pudieron cargar tus postulaciones.");
      }
      return respuesta.json();
    })
    .then((ids) => {
      if (!Array.isArray(ids)) {
        throw new Error("El servidor devolvió una lista de postulaciones inválida.");
      }
      postulacionesRegistradas = ids;
      actualizarBotonesPostulados();
    })
    .catch((error) => console.error("No se pudieron actualizar las postulaciones.", error));
}

function iniciarAutocompletado(formulario) {
  const campo = formulario.querySelector("input[name=\"texto\"]");
  const lista = formulario.querySelector(".search-suggestions");
  let cargos = [];
  let indiceActivo = -1;
  let listaCargada = false;
  let errorCarga = false;

  function cerrarLista() {
    lista.hidden = true;
    campo.setAttribute("aria-expanded", "false");
    campo.removeAttribute("aria-activedescendant");
    indiceActivo = -1;
  }

  function renderizarLista() {
    const coincidencias = filtrarCargos(cargos, campo.value);
    lista.replaceChildren();
    indiceActivo = -1;
    if (!listaCargada) {
      const mensaje = document.createElement("p");
      mensaje.className = "search-suggestion-message";
      mensaje.textContent = "Cargando cargos...";
      lista.append(mensaje);
      return;
    }
    if (errorCarga) {
      const mensaje = document.createElement("p");
      mensaje.className = "search-suggestion-message";
      mensaje.textContent = "No pudimos cargar los cargos. Intentá nuevamente.";
      lista.append(mensaje);
      return;
    }
    if (coincidencias.length === 0) {
      const mensaje = document.createElement("p");
      mensaje.className = "search-suggestion-message";
      mensaje.textContent = "No hay cargos que coincidan";
      lista.append(mensaje);
      return;
    }

    coincidencias.forEach((cargo, indice) => {
      const opcion = document.createElement("button");
      opcion.type = "button";
      opcion.className = "search-suggestion";
      opcion.id = `${lista.id}-opcion-${indice}`;
      opcion.setAttribute("role", "option");
      opcion.setAttribute("aria-selected", "false");
      marcarCoincidencia(opcion, cargo, campo.value);
      opcion.addEventListener("click", () => irABusqueda(cargo));
      lista.append(opcion);
    });
  }

  function mostrarLista() {
    lista.hidden = false;
    campo.setAttribute("aria-expanded", "true");
    renderizarLista();
  }

  function seleccionarOpcion(indice) {
    const opciones = lista.querySelectorAll("[role=\"option\"]");
    if (opciones.length === 0) {
      return;
    }
    indiceActivo = (indice + opciones.length) % opciones.length;
    opciones.forEach((opcion, posicion) => {
      const seleccionada = posicion === indiceActivo;
      opcion.setAttribute("aria-selected", String(seleccionada));
      if (seleccionada) {
        campo.setAttribute("aria-activedescendant", opcion.id);
        opcion.scrollIntoView({ block: "nearest" });
      }
    });
  }

  function irABusqueda(texto) {
    const destino = new URL(formulario.action, window.location.href);
    destino.searchParams.set("texto", texto);
    window.location.assign(destino);
  }

  campo.addEventListener("focus", mostrarLista);
  campo.addEventListener("input", () => {
    if (!lista.hidden) {
      renderizarLista();
    }
  });
  campo.addEventListener("keydown", (evento) => {
    if (evento.key === "ArrowDown") {
      evento.preventDefault();
      if (lista.hidden) {
        mostrarLista();
      }
      seleccionarOpcion(indiceActivo + 1);
    } else if (evento.key === "ArrowUp" && !lista.hidden) {
      evento.preventDefault();
      seleccionarOpcion(indiceActivo < 0 ? 0 : indiceActivo - 1);
    } else if (evento.key === "Escape") {
      cerrarLista();
    } else if (evento.key === "Enter") {
      const activa = lista.querySelector("[aria-selected=\"true\"]");
      if (activa && !lista.hidden) {
        evento.preventDefault();
        irABusqueda(activa.textContent);
      }
    }
  });
  formulario.addEventListener("submit", (evento) => {
    evento.preventDefault();
    irABusqueda(campo.value);
  });
  document.addEventListener("pointerdown", (evento) => {
    if (!formulario.contains(evento.target)) {
      cerrarLista();
    }
  });

  fetch(formulario.dataset.sugerenciasUrl || "/vacantes/sugerencias")
    .then((respuesta) => {
      if (!respuesta.ok) {
        throw new Error("No se pudieron cargar los cargos");
      }
      return respuesta.json();
    })
    .then((resultado) => {
      if (!Array.isArray(resultado)) {
        throw new Error("El servidor devolvió una lista de cargos inválida.");
      }
      cargos = resultado;
      listaCargada = true;
      if (!lista.hidden) {
        renderizarLista();
      }
    })
    .catch((error) => {
      cargos = [];
      errorCarga = true;
      listaCargada = true;
      console.error("No se pudieron cargar las sugerencias de cargos.", error);
      if (!lista.hidden) {
        renderizarLista();
      }
    });
}

function iniciarResultados() {
  const resultados = document.querySelector("#resultados-vacantes");
  if (!resultados) {
    return;
  }

  const estado = document.querySelector("#estado-resultados");
  const tarjetas = document.querySelector("#tarjetas-vacantes");
  const formularioFiltros = document.querySelector("#filtros-busqueda");
  const botonVerMas = document.querySelector("#ver-mas");
  const formularioBusqueda = document.querySelector(".search-autocomplete input[name=\"texto\"]");
  let ofertas = [];
  let cantidadVisible = 6;

  function parametrosActuales() {
    const parametros = new URLSearchParams(window.location.search);
    parametros.set("texto", formularioBusqueda.value);
    for (const campo of formularioFiltros.elements) {
      if (campo.name && !campo.disabled) {
        if (campo.value) {
          parametros.set(campo.name, campo.value);
        } else {
          parametros.delete(campo.name);
        }
      }
    }
    return parametros;
  }

  function actualizarUrl(parametros) {
    const consulta = parametros.toString();
    const nuevaUrl = consulta ? `${window.location.pathname}?${consulta}` : window.location.pathname;
    window.history.replaceState({}, "", nuevaUrl);
  }

  function crearTarjeta(oferta) {
    const tarjeta = document.createElement("article");
    tarjeta.className = "offer-card search-result-card";
    tarjeta.dataset.ofertaId = oferta.fuente && oferta.id
      ? `${oferta.fuente}:${oferta.id}`
      : oferta.url || `${oferta.titulo || "Oferta sin título"} | ${oferta.empresa || "Empresa no informada"}`;
    const botonGuardar = document.createElement("button");
    botonGuardar.type = "button";
    botonGuardar.className = "save-job-button";
    botonGuardar.dataset.ofertaId = tarjeta.dataset.ofertaId;
    botonGuardar.dataset.titulo = oferta.titulo || "Oferta sin título";
    botonGuardar.dataset.empresa = oferta.empresa || "Empresa no informada";
    botonGuardar.dataset.ubicacion = oferta.ubicacion || "";
    botonGuardar.dataset.modalidad = oferta.modalidad || "";
    botonGuardar.dataset.url = oferta.url || "";
    botonGuardar.dataset.guardado = "false";
    botonGuardar.setAttribute("aria-label", "Guardar empleo");
    botonGuardar.setAttribute("aria-pressed", "false");
    botonGuardar.innerHTML = "<i class=\"bi bi-bookmark\" aria-hidden=\"true\"></i>";
    tarjeta.append(botonGuardar);
    const informacion = document.createElement("div");
    informacion.className = "offer-info";
    const titulo = document.createElement("h3");
    titulo.textContent = oferta.titulo || "Cargo sin título";
    const empresa = document.createElement("p");
    empresa.textContent = oferta.empresa || "Empresa no informada";
    const detalles = document.createElement("p");
    detalles.className = "result-details";
    const fecha = oferta.fechaPublicacion
      ? new Intl.DateTimeFormat("es-AR", { dateStyle: "medium", timeStyle: "short" })
        .format(new Date(oferta.fechaPublicacion))
      : "Sin fecha informada";
    detalles.textContent = `${oferta.ubicacion || "Ubicación no informada"} · ${oferta.modalidad || "Modalidad no informada"} · ${fecha}`;
    const skills = document.createElement("p");
    skills.className = "result-skills";
    skills.textContent = `Skills: ${(oferta.skills || []).join(", ") || "No informadas"}`;
    informacion.append(titulo, empresa, detalles, skills);
    tarjeta.append(informacion);
    if (oferta.url) {
      const enlace = document.createElement("a");
      enlace.className = "result-link";
      enlace.href = oferta.url;
      enlace.target = "_blank";
      enlace.rel = "noopener noreferrer";
      enlace.textContent = "Ver oferta";
      tarjeta.append(enlace);
    }
    const botonPostularse = document.createElement("button");
    botonPostularse.type = "button";
    botonPostularse.className = "apply-button";
    botonPostularse.textContent = "Postularme";
    botonPostularse.dataset.ofertaId = oferta.fuente && oferta.id
      ? `${oferta.fuente}:${oferta.id}`
      : oferta.url || oferta.id || `${oferta.titulo || "Oferta sin título"} | ${oferta.empresa || "Empresa no informada"}`;
    botonPostularse.dataset.titulo = oferta.titulo || "Oferta sin título";
    botonPostularse.dataset.empresa = oferta.empresa || "Empresa no informada";
    botonPostularse.dataset.ubicacion = oferta.ubicacion || "Ubicación no informada";
    botonPostularse.dataset.modalidad = oferta.modalidad || "Modalidad no informada";
    botonPostularse.dataset.url = oferta.url || "";
    configurarBotonPostulacion(botonPostularse, botonPostularse.dataset.ofertaId);
    actualizarBotonesPostulados();
    tarjeta.append(botonPostularse);
    return tarjeta;
  }

  function mostrarOfertas() {
    tarjetas.replaceChildren(...ofertas.slice(0, cantidadVisible).map(crearTarjeta));
    document.dispatchEvent(new CustomEvent("guardados:actualizar"));
    botonVerMas.hidden = cantidadVisible >= ofertas.length;
  }

  async function cargarOfertas(guardarEnUrl) {
    const parametros = parametrosActuales();
    if (guardarEnUrl) {
      actualizarUrl(parametros);
    }
    estado.textContent = "Cargando...";
    resultados.setAttribute("aria-busy", "true");
    tarjetas.replaceChildren();
    botonVerMas.hidden = true;
    try {
      const respuesta = await fetch(`${resultados.dataset.endpoint}?${parametros.toString()}`);
      if (!respuesta.ok) {
        throw new Error("La búsqueda falló");
      }
      ofertas = await respuesta.json();
      cantidadVisible = 6;
      if (ofertas.length === 0) {
        estado.replaceChildren(document.createTextNode(
          "No encontramos ofertas con esos filtros. Probá limpiar los filtros. "
        ));
        const limpiar = document.createElement("button");
        limpiar.type = "button";
        limpiar.className = "empty-results-action";
        limpiar.textContent = "Limpiar filtros";
        limpiar.addEventListener("click", () => formularioFiltros.reset());
        estado.append(limpiar);
      } else {
        estado.textContent = `${ofertas.length} ofertas encontradas`;
        mostrarOfertas();
      }
    } catch {
      estado.textContent = "No pudimos cargar las ofertas. Intentá nuevamente.";
    } finally {
      resultados.setAttribute("aria-busy", "false");
    }
  }

  for (const campo of formularioFiltros.elements) {
    if (campo.name && !campo.disabled) {
      const valorInicial = new URLSearchParams(window.location.search).get(campo.name);
      if (valorInicial !== null) {
        campo.value = valorInicial;
      }
      campo.addEventListener("change", () => cargarOfertas(true));
    }
  }
  formularioFiltros.addEventListener("reset", (evento) => {
    evento.preventDefault();
    for (const campo of formularioFiltros.elements) {
      if (campo.name && !campo.disabled) {
        campo.value = "";
      }
    }
    cargarOfertas(true);
  });
  botonVerMas.addEventListener("click", () => {
    cantidadVisible += 6;
    mostrarOfertas();
  });
  window.addEventListener("popstate", () => cargarOfertas(false));
  cargarOfertas(false);
}

if (typeof document !== "undefined") {
  document.querySelectorAll(".search-autocomplete").forEach(iniciarAutocompletado);
  iniciarResultados();
  iniciarPostulaciones();
}