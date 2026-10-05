const buscador = document.getElementById("buscador-skills");
const opciones = document.querySelectorAll(".skill-option");
const botonesSkills = document.querySelectorAll(".skill-button");
const skillsSeleccionadas = document.getElementById("skills-seleccionadas");
const buscadorIdiomas = document.getElementById("buscador-idiomas");
const opcionesIdiomas = document.querySelectorAll(".language-option");
const botonesIdiomas = document.querySelectorAll(".language-button");
const idiomasSeleccionados = document.getElementById("idiomas-seleccionados");
const telefono = document.getElementById("telefono");
const todosLosDias = document.getElementById("todos-los-dias");

function actualizarSkillsOcultas() {
  skillsSeleccionadas.innerHTML = "";
  botonesSkills.forEach((boton) => {
    if (boton.classList.contains("selected")) {
      const skillOculta = document.createElement("input");
      skillOculta.type = "hidden";
      skillOculta.name = "skillIds";
      skillOculta.value = boton.dataset.skillId;
      skillsSeleccionadas.appendChild(skillOculta);
    }
  });
}

botonesSkills.forEach((boton) => {
  boton.addEventListener("click", () => {
    const seleccionado = boton.classList.toggle("selected");
    boton.setAttribute("aria-pressed", seleccionado);
    actualizarSkillsOcultas();
  });
});

actualizarSkillsOcultas();

function actualizarIdiomasOcultos() {
  idiomasSeleccionados.innerHTML = "";
  botonesIdiomas.forEach((boton) => {
    if (boton.classList.contains("selected")) {
      const nivel = boton.parentElement.querySelector(".language-level");
      const idiomaOculto = document.createElement("input");
      idiomaOculto.type = "hidden";
      idiomaOculto.name = `idiomas[${boton.dataset.language}]`;
      idiomaOculto.value = nivel.value;
      idiomasSeleccionados.appendChild(idiomaOculto);
    }
  });
}

botonesIdiomas.forEach((boton) => {
  boton.addEventListener("click", () => {
    const seleccionado = boton.classList.toggle("selected");
    const nivel = boton.parentElement.querySelector(".language-level");
    nivel.classList.toggle("d-none", !seleccionado);
    boton.setAttribute("aria-pressed", seleccionado);
    actualizarIdiomasOcultos();
  });
});

document.querySelectorAll(".language-level").forEach((nivel) => {
  nivel.addEventListener("change", actualizarIdiomasOcultos);
});

actualizarIdiomasOcultos();

buscador.addEventListener("input", (event) => {
  const texto = event.target.value.toLowerCase();
  opciones.forEach((opcion) => {
    const nombre = opcion.querySelector(".skill-button").textContent.toLowerCase();
    opcion.hidden = !nombre.includes(texto);
  });
});

buscadorIdiomas.addEventListener("input", (event) => {
  const texto = event.target.value.toLowerCase();
  opcionesIdiomas.forEach((opcion) => {
    const nombre = opcion.querySelector(".language-button").textContent.toLowerCase();
    opcion.hidden = !nombre.includes(texto);
  });
});

telefono.addEventListener("input", (event) => {
  event.target.value = event.target.value.replace(/\D/g, "");
});

todosLosDias.addEventListener("click", () => {
  const dias = document.querySelectorAll('input[id^="dia-"]');
  const todosSeleccionados = Array.from(dias).every((dia) => dia.checked);
  dias.forEach((dia) => {
    dia.checked = !todosSeleccionados;
  });
});