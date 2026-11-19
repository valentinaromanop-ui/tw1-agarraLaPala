package com.tallerwebi.presentacion;

import com.tallerwebi.dominio.LimiteReportesAlcanzadoException;
import com.tallerwebi.dominio.ServicioReporte;
import com.tallerwebi.dominio.TipoReporte;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ControladorReporte {

  private final ServicioReporte servicio;

  @Autowired
  public ControladorReporte(ServicioReporte servicioReporte) {
    this.servicio = servicioReporte;
  }

  @PostMapping("/reportes")
  public String reportar(
    @RequestParam("tipo") TipoReporte tipo,
    @RequestParam("objetoId") Long objetoId,
    @RequestParam("usuarioReportadoId") Long usuarioReportadoId,
    @RequestParam("motivo") String motivo,
    @RequestParam(value = "detalle", required = false) String detalle,
    HttpSession sesion,
    HttpServletRequest req,
    RedirectAttributes ra
  ) {
    Long yo = (Long) sesion.getAttribute("USUARIO_ID");
    if (yo == null) {
      return "redirect:/login";
    }
    try {
      servicio.reportar(tipo, objetoId, usuarioReportadoId, yo, motivo, detalle);
      ra.addFlashAttribute("exito", "Reporte enviado");
    } catch (LimiteReportesAlcanzadoException e) {
      ra.addFlashAttribute("error", e.getMessage());
    }
    String ref = req.getHeader("Referer");
    return "redirect:" + (ref != null ? ref : "/home");
  }
}
