const contadorNotificaciones = document.getElementById("notifications-count");

if (contadorNotificaciones) {
  fetch(contadorNotificaciones.dataset.contadorUrl, {
    credentials: "same-origin",
    headers: { Accept: "application/json" },
  })
    .then((response) => {
      if (!response.ok) {
        throw new Error(`No se pudo consultar el contador (${response.status}).`);
      }
      return response.json();
    })
    .then((resultado) => {
      if (!Number.isInteger(resultado.count) || resultado.count < 0) {
        throw new Error("El contador de notificaciones devolvió un valor inválido.");
      }
      contadorNotificaciones.textContent = resultado.count;
      contadorNotificaciones.hidden = resultado.count === 0;
    })
    .catch((error) => console.error("Error al actualizar las notificaciones:", error));
}
