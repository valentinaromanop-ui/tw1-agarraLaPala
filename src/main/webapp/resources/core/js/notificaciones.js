const contadorNotificaciones = document.getElementById("notifications-count");

if (contadorNotificaciones) {
  const actualizarContador = async () => {
    const response = await fetch(contadorNotificaciones.dataset.contadorUrl, {
      credentials: "same-origin",
      headers: { Accept: "application/json" },
    });
    if (!response.ok) {
      throw new Error(`No se pudo consultar el contador (${response.status}).`);
    }
    const resultado = await response.json();
    if (!Number.isInteger(resultado.count) || resultado.count < 0) {
      throw new Error("El contador de notificaciones devolvió un valor inválido.");
    }
    contadorNotificaciones.textContent = resultado.count;
    contadorNotificaciones.hidden = resultado.count === 0;
  };

  actualizarContador().catch((error) =>
    console.error("Error al actualizar las notificaciones:", error),
  );

  const dropdown = document.getElementById("notifications-dropdown-toggle")?.parentElement;
  dropdown?.addEventListener("show.bs.dropdown", async () => {
    contadorNotificaciones.textContent = "0";
    contadorNotificaciones.hidden = true;
    try {
      const response = await fetch(
        document.getElementById("notifications-dropdown-toggle").dataset.marcarLeidasUrl,
        { method: "POST", credentials: "same-origin" },
      );
      if (!response.ok) {
        throw new Error(`No se pudieron marcar las notificaciones (${response.status}).`);
      }
    } catch (error) {
      console.error("Error al marcar las notificaciones como leídas:", error);
      actualizarContador().catch((actualizacionError) =>
        console.error("Error al restaurar el contador:", actualizacionError),
      );
    }
  });
}
