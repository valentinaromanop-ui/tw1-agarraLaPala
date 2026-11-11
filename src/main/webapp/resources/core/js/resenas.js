document.querySelectorAll("[data-review-comment]").forEach((comentario) => {
  const contador = comentario.parentElement.querySelector(".review-character-count");
  if (!contador) {
    return;
  }
  const actualizarContador = () => {
    contador.textContent = `${comentario.value.length}/2000 caracteres`;
  };
  comentario.addEventListener("input", actualizarContador);
  actualizarContador();
});

document.querySelectorAll("form[data-confirm]").forEach((formulario) => {
  formulario.addEventListener("submit", (evento) => {
    if (!window.confirm(formulario.dataset.confirm)) {
      evento.preventDefault();
    }
  });
});
