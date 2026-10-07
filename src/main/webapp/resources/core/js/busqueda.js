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

async function enviarPostulacion(boton, ofertaId) {
  boton.disabled = true;
  try {
    const respuesta = await fetch("/postularse", {
      method: "POST",
      headers: { "Content-Type": "application/x-www-form-urlencoded" },
      body: new URLSearchParams({ ofertaId }),
    });
    const resultado = await respuesta.json();
    if (!respuesta.ok) {
      throw new Error(resultado.mensaje || "No se pudo enviar la postulación");
    }
    boton.textContent = resultado.mensaje;
  } catch (error) {
    boton.disabled = false;
    boton.textContent = error.message;
  }
}

function configurarBotonPostulacion(boton, ofertaId) {
  boton.addEventListener("click", () => enviarPostulacion(boton, ofertaId));
}

function iniciarPostulaciones() {
  document.querySelectorAll(".apply-button").forEach((boton) => {
    const tarjeta = boton.closest(".offer-card");
    configurarBotonPostulacion(boton, boton.dataset.ofertaId || obtenerOfertaIdDeTarjeta(tarjeta));
  });
}

function iniciarAutocompletado(formulario) {
  const campo = formulario.querySelector("input[name=\"texto\"]");
  const lista = formulario.querySelector(".search-suggestions");
  let cargos = [];
  let indiceActivo = -1;
  let listaCargada = false;

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
      cargos = resultado;
      listaCargada = true;
      if (!lista.hidden) {
        renderizarLista();
      }
    })
    .catch(() => {
      cargos = [];
      listaCargada = true;
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
    botonPostularse.dataset.ofertaId = oferta.url || oferta.id || `${oferta.titulo || "Oferta sin título"} | ${oferta.empresa || "Empresa no informada"}`;
    configurarBotonPostulacion(botonPostularse, botonPostularse.dataset.ofertaId);
    tarjeta.append(botonPostularse);
    return tarjeta;
  }

  function mostrarOfertas() {
    tarjetas.replaceChildren(...ofertas.slice(0, cantidadVisible).map(crearTarjeta));
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