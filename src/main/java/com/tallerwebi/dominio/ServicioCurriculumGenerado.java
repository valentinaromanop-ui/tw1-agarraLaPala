package com.tallerwebi.dominio;

import java.io.IOException;

public interface ServicioCurriculumGenerado {
  CurriculumGenerado subirOriginal(Long usuarioId, String nombreArchivo, byte[] archivo)
    throws IOException;
  CurriculumGenerado generarAts(Long usuarioId);
  CurriculumGenerado obtener(Long usuarioId);
}
