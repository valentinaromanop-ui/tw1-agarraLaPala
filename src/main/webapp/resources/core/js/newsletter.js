const formularioNewsletter = document.querySelector(".newsletter-form");

if (formularioNewsletter) {
  formularioNewsletter.addEventListener("submit", async function (evento) {
    evento.preventDefault();
    const boton = formularioNewsletter.querySelector("button[type=submit]");
    const resultado = document.querySelector("#newsletter-resultado");
    boton.disabled = true;
    resultado.textContent = "Registrando tu suscripción...";

    try {
      const respuesta = await fetch(formularioNewsletter.action, {
        method: "POST",
        body: new URLSearchParams(new FormData(formularioNewsletter)),
      });
      if (!respuesta.ok) {
        throw new Error("No se pudo registrar la suscripción");
      }
      resultado.innerHTML = await respuesta.text();
    } catch (error) {
      resultado.textContent = "No pudimos procesar tu suscripción. Intentá nuevamente.";
    } finally {
      boton.disabled = false;
    }
  });
}
