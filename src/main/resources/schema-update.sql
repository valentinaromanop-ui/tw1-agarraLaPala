ALTER TABLE curriculum_generado
  MODIFY COLUMN nombreArchivoOriginal VARCHAR(255) NULL,
  MODIFY COLUMN archivoOriginal LONGBLOB NULL;

ALTER TABLE curriculum_generado
  MODIFY COLUMN textoOriginal LONGTEXT NULL,
  MODIFY COLUMN contenidoAts LONGTEXT NULL;

SET @agregar_pdf_ats = IF(
  (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'curriculum_generado'
      AND column_name = 'archivoAtsPdf'
  ) = 0,
  'ALTER TABLE curriculum_generado ADD COLUMN archivoAtsPdf LONGBLOB NULL',
  'SELECT 1'
);
PREPARE migracion_pdf_ats FROM @agregar_pdf_ats;
EXECUTE migracion_pdf_ats;
DEALLOCATE PREPARE migracion_pdf_ats;

ALTER TABLE curriculum_generado
  MODIFY COLUMN archivoAtsPdf LONGBLOB NULL;

SET @agregar_ats_gratis_usado = IF(
  (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'curriculum_generado'
      AND column_name = 'atsGratisUsado'
  ) = 0,
  'ALTER TABLE curriculum_generado ADD COLUMN atsGratisUsado BOOLEAN NOT NULL DEFAULT FALSE',
  'SELECT 1'
);
PREPARE migracion_ats_gratis_usado FROM @agregar_ats_gratis_usado;
EXECUTE migracion_ats_gratis_usado;
DEALLOCATE PREPARE migracion_ats_gratis_usado;

UPDATE curriculum_generado
SET atsGratisUsado = TRUE
WHERE contenidoAts IS NOT NULL OR archivoAtsPdf IS NOT NULL;

SET @actualizar_foto_perfil = IF(
  (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'Usuario'
      AND column_name = 'foto_perfil'
  ) = 0,
  'ALTER TABLE Usuario ADD COLUMN foto_perfil LONGBLOB NULL',
  'ALTER TABLE Usuario MODIFY COLUMN foto_perfil LONGBLOB NULL'
);
PREPARE migracion_foto_perfil FROM @actualizar_foto_perfil;
EXECUTE migracion_foto_perfil;
DEALLOCATE PREPARE migracion_foto_perfil;

SET @copiar_id_oferta = IF(
  (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'Postulacion'
      AND column_name = 'oferta_id'
  ) > 0,
  'UPDATE Postulacion SET id_oferta = oferta_id WHERE id_oferta IS NULL',
  'SELECT 1'
);
PREPARE migracion_id_oferta FROM @copiar_id_oferta;
EXECUTE migracion_id_oferta;
DEALLOCATE PREPARE migracion_id_oferta;

SET @permitir_oferta_id_nulo = IF(
  (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'Postulacion'
      AND column_name = 'oferta_id'
  ) > 0,
  (
    SELECT CONCAT(
      'ALTER TABLE Postulacion MODIFY COLUMN oferta_id ',
      column_type,
      IF(character_set_name IS NULL, '', CONCAT(' CHARACTER SET ', character_set_name)),
      IF(collation_name IS NULL, '', CONCAT(' COLLATE ', collation_name)),
      ' NULL'
    )
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'Postulacion'
      AND column_name = 'oferta_id'
  ),
  'SELECT 1'
);
PREPARE migracion_oferta_id_nula FROM @permitir_oferta_id_nulo;
EXECUTE migracion_oferta_id_nula;
DEALLOCATE PREPARE migracion_oferta_id_nula;

SET @agregar_fecha_decision_postulacion = IF(
  (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'Postulacion'
      AND column_name = 'fecha_decision'
  ) = 0,
  'ALTER TABLE Postulacion ADD COLUMN fecha_decision DATETIME NULL',
  'SELECT 1'
);
PREPARE migracion_fecha_decision_postulacion FROM @agregar_fecha_decision_postulacion;
EXECUTE migracion_fecha_decision_postulacion;
DEALLOCATE PREPARE migracion_fecha_decision_postulacion;

SET @migrar_estado_postulacion = IF(
  (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'Postulacion'
      AND column_name = 'estado'
  ) > 0,
  'UPDATE Postulacion SET estado = ''PENDIENTE''',
  'SELECT 1'
);
PREPARE migracion_estado_postulacion FROM @migrar_estado_postulacion;
EXECUTE migracion_estado_postulacion;
DEALLOCATE PREPARE migracion_estado_postulacion;

SET @definir_estado_postulacion_default = IF(
  (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'Postulacion'
      AND column_name = 'estado'
  ) > 0,
  'ALTER TABLE Postulacion MODIFY COLUMN estado VARCHAR(20) NOT NULL DEFAULT ''PENDIENTE''',
  'SELECT 1'
);
PREPARE migracion_estado_postulacion_default FROM @definir_estado_postulacion_default;
EXECUTE migracion_estado_postulacion_default;
DEALLOCATE PREPARE migracion_estado_postulacion_default;
