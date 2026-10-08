ALTER TABLE curriculum_generado
  MODIFY COLUMN archivoOriginal LONGBLOB NOT NULL;

ALTER TABLE curriculum_generado
  MODIFY COLUMN textoOriginal LONGTEXT NOT NULL,
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
