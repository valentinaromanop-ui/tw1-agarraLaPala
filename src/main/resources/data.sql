INSERT INTO Usuario(id, email, password, rol, activo) VALUES(null, 'test@unlam.edu.ar', 'test', 'ADMIN', true);

INSERT INTO Skill (id, nombre) VALUES (1, 'java'), (2, 'sql'), (3, 'php'), (4, 'python');
INSERT INTO Vacante (id, titulo, empresa, descripcion, ubicacion, modalidad, fecha_publicacion, activa) VALUES
(1, 'Desarrollador Java', 'Empresa A', 'Desarrollo de aplicaciones backend con Java y SQL.', 'Argentina', 'REMOTE', '2026-09-28 10:00:00', true);
INSERT INTO Vacante (id, titulo, empresa, descripcion, ubicacion, modalidad, fecha_publicacion, activa) VALUES
(2, 'Desarrollador PHP', 'Empresa B', 'Mantenimiento de aplicaciones PHP con SQL.', 'Buenos Aires', 'HIBRIDO', '2026-09-29 09:00:00', true);
INSERT INTO Vacante (id, titulo, empresa, descripcion, ubicacion, modalidad, fecha_publicacion, activa) VALUES
(3, 'Desarrollador Python', 'Empresa C', 'Desarrollo de servicios con Python.', 'Buenos Aires', 'PRESENCIAL', '2026-09-29 10:00:00', true);
INSERT INTO Vacante (id, titulo, empresa, descripcion, ubicacion, modalidad, fecha_publicacion, activa) VALUES
(4, 'Java', 'Empresa A', 'Oferta inactiva', 'Argentina', 'REMOTE', '2026-09-29 11:00:00', false);
INSERT INTO Vacante_skill (vacante_id, skill_id) VALUES (1, 1);
INSERT INTO Vacante_skill (vacante_id, skill_id) VALUES (1, 2);
INSERT INTO Vacante_skill (vacante_id, skill_id) VALUES (2, 2);
INSERT INTO Vacante_skill (vacante_id, skill_id) VALUES (2, 3);
INSERT INTO Vacante_skill (vacante_id, skill_id) VALUES (3, 4);
INSERT INTO Vacante_skill (vacante_id, skill_id) VALUES (4, 1);
