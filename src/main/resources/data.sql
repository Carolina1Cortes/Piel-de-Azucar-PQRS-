-- =====================================================================
-- APOLO - Datos de prueba para desarrollo y sustentación
-- Idempotente: MERGE ... KEY(id) permite ejecutarlo en cada arranque.
-- Ids 1..999 reservados para estos datos. Fechas ubicadas en octubre de 2026.
-- Contraseña de ambos usuarios de prueba: apolo2026
-- =====================================================================

MERGE INTO usuario (id, nombre, correo, contrasena_hash, rol, bloqueado) KEY (id) VALUES
    (1, 'Paula Andrea Mesa González', 'paula@pieldeazucar.co', '$2a$10$8z9CY/HHwdPe6jMTuMIc7OYaH46lYEQrfl1eKo3uVN3gB2HkvTpKO', 'ADMIN', FALSE),
    (2, 'Operador Uno', 'operador1@pieldeazucar.co', '$2a$10$8z9CY/HHwdPe6jMTuMIc7OYaH46lYEQrfl1eKo3uVN3gB2HkvTpKO', 'OPERADOR', FALSE),
    (3, 'Operador Bloqueado', 'operador2@pieldeazucar.co', '$2a$10$8z9CY/HHwdPe6jMTuMIc7OYaH46lYEQrfl1eKo3uVN3gB2HkvTpKO', 'OPERADOR', TRUE);

MERGE INTO cliente (id, nombre, correo, telefono) KEY (id) VALUES
    (1, 'Camila Rojas', 'camila.rojas@example.com', '3001112233'),
    (2, 'Valentina Ortiz', 'valentina.ortiz@example.com', '3012223344'),
    (3, 'Daniela Pardo', 'daniela.pardo@example.com', NULL),
    (4, 'Mariana Gómez', 'mariana.gomez@example.com', '3023334455'),
    (5, 'Laura Beltrán', 'laura.beltran@example.com', '3034445566'),
    (6, 'Natalia Prieto', 'natalia.prieto@example.com', NULL);

-- PROVISIONAL: reconstruido del contexto. Reemplazar por la tabla oficial de clasificación entregada por el equipo.
MERGE INTO tipo_consulta (id, descripcion, categoria, prioridad_por_defecto, automatizacion) KEY (id) VALUES
    (1, 'Estado de un pedido', 'PEDIDOS', 'MEDIA', 'AUTOMATICA'),
    (2, 'Modificar o cancelar un pedido', 'PEDIDOS', 'ALTA', 'ASISTIDA'),
    (3, 'Seguimiento de un envío', 'ENVIOS_ENTREGAS', 'MEDIA', 'AUTOMATICA'),
    (4, 'Pedido incompleto, dañado o no entregado', 'ENVIOS_ENTREGAS', 'ALTA', 'ATENCION_HUMANA'),
    (5, 'Modo de uso de un producto', 'PRODUCTOS_USO', 'BAJA', 'AUTOMATICA'),
    (6, 'Recomendación según tipo de piel', 'PRODUCTOS_USO', 'MEDIA', 'ASISTIDA'),
    (7, 'Enrojecimiento, irritación o alergia', 'REACCIONES', 'CRITICA', 'ATENCION_HUMANA'),
    (8, 'Molestia tras el uso de un producto', 'REACCIONES', 'CRITICA', 'ATENCION_HUMANA'),
    (9, 'Solicitud de cambio o devolución', 'CAMBIOS_DEVOLUCIONES', 'ALTA', 'ASISTIDA'),
    (10, 'Garantía por producto defectuoso', 'CAMBIOS_DEVOLUCIONES', 'ALTA', 'ATENCION_HUMANA'),
    (11, 'Métodos de pago', 'PAGOS_FACTURACION', 'BAJA', 'AUTOMATICA'),
    (12, 'Cobro duplicado o error en factura', 'PAGOS_FACTURACION', 'ALTA', 'ATENCION_HUMANA'),
    (13, 'Queja por el servicio', 'ATENCION_QUEJAS', 'ALTA', 'ATENCION_HUMANA'),
    (14, 'Información general y horarios', 'ATENCION_QUEJAS', 'BAJA', 'AUTOMATICA'),
    (15, 'Sugerencia', 'SUGERENCIAS_FELICITACIONES', 'BAJA', 'REGISTRO'),
    (16, 'Felicitación', 'SUGERENCIAS_FELICITACIONES', 'BAJA', 'REGISTRO');

MERGE INTO plantilla_respuesta (id, nombre, asunto, cuerpo, palabras_clave, tipo_consulta_id, activa) KEY (id) VALUES
    (1, 'Estado de pedido', 'Estado de tu pedido en Piel de Azúcar', 'Hola {nombre}, gracias por escribirnos. Tu pedido está en proceso y te avisaremos apenas salga. Cualquier duda, estamos atentas.', 'pedido,estado,orden', 1, TRUE),
    (2, 'Modo de uso', 'Cómo usar tu producto', 'Hola {nombre}, te compartimos el modo de uso recomendado: aplica sobre el rostro limpio y seco, y complementa con protector solar durante el día.', 'uso,aplicar,rutina,modo', 5, TRUE),
    (3, 'Solicitud de cambio o devolución', 'Tu solicitud de cambio o devolución', 'Hola {nombre}, recibimos tu solicitud. Para continuar, cuéntanos el número de pedido y envíanos una foto del producto.', 'cambio,devolucion,devolver', 9, TRUE),
    (4, 'Agradecimiento por sugerencia o felicitación', '¡Gracias por escribirnos!', 'Hola {nombre}, gracias por tu mensaje. Lo compartiremos con todo el equipo de Piel de Azúcar.', 'gracias,felicitacion,sugerencia', 16, TRUE);

MERGE INTO pqrs (id, id_externo, canal, fecha_llegada, asunto, estado, prioridad, fecha_limite, descripcion_solucion, fecha_resolucion, cliente_id, tipo_consulta_id, responsable_id, resuelta_por_id) KEY (id) VALUES
    (1, 'gmail-msg-001', 'GMAIL', '2026-10-19 09:15:00', '¿Cuándo llega mi pedido #1042?', 'PENDIENTE', 'MEDIA', '2026-11-10', NULL, NULL, 1, 3, NULL, NULL),
    (2, 'gmail-msg-002', 'GMAIL', '2026-10-16 19:40:00', 'Me salió enrojecimiento con el sérum', 'PENDIENTE', 'CRITICA', '2026-11-09', NULL, NULL, 2, 7, NULL, NULL),
    (3, 'gmail-msg-003', 'GMAIL', '2026-10-14 08:00:00', '¿Cómo uso el ácido hialurónico?', 'RESUELTA', 'BAJA', '2026-11-05', 'Respuesta automática con el modo de uso del producto.', '2026-10-14 08:02:00', 3, 5, NULL, NULL),
    (4, 'gmail-msg-004', 'GMAIL', '2026-10-13 10:00:00', 'Quiero devolver mi crema hidratante', 'EN_ATENCION', 'ALTA', '2026-11-04', NULL, NULL, 4, 9, 2, NULL),
    (5, 'gmail-msg-005', 'GMAIL', '2026-10-09 11:00:00', 'Me cobraron dos veces mi pedido', 'ESCALADA', 'ALTA', '2026-11-03', NULL, NULL, 5, 12, 2, NULL),
    (6, 'gmail-msg-006', 'GMAIL', '2026-10-08 16:00:00', 'Felicitaciones por su atención', 'RESUELTA', 'BAJA', '2026-10-30', 'Se agradeció la felicitación y se compartió con el equipo.', '2026-10-08 16:20:00', 6, 16, NULL, 2),
    (7, 'gmail-msg-007', 'GMAIL', '2026-09-28 14:30:00', 'Mi pedido llegó incompleto', 'EN_ATENCION', 'ALTA', '2026-10-20', NULL, NULL, 1, 4, 2, NULL),
    (8, 'gmail-msg-008', 'GMAIL', '2026-10-20 12:10:00', 'Queja por mal trato en la línea de atención', 'DELEGADA', 'ALTA', '2026-11-11', NULL, NULL, 2, 13, 2, NULL);

MERGE INTO mensaje (id, pqrs_id, fecha, asunto, cuerpo, direccion, automatico, autor_id) KEY (id) VALUES
    (1, 1, '2026-10-19 09:15:00', '¿Cuándo llega mi pedido #1042?', 'Hola, hice el pedido hace una semana y todavía no llega. ¿Me pueden decir en qué va?', 'ENTRANTE', FALSE, NULL),
    (2, 2, '2026-10-16 19:40:00', 'Me salió enrojecimiento con el sérum', 'Hola, desde que uso el sérum me salió enrojecimiento y ardor en las mejillas. Adjunto una foto.', 'ENTRANTE', FALSE, NULL),
    (3, 3, '2026-10-14 08:00:00', '¿Cómo uso el ácido hialurónico?', 'Buenos días, ¿cómo debo aplicar el ácido hialurónico?', 'ENTRANTE', FALSE, NULL),
    (4, 4, '2026-10-13 10:00:00', 'Quiero devolver mi crema hidratante', 'Compré la crema hidratante y no me funcionó, quisiera devolverla.', 'ENTRANTE', FALSE, NULL),
    (5, 5, '2026-10-09 11:00:00', 'Me cobraron dos veces mi pedido', 'Veo dos cargos iguales en mi tarjeta por el mismo pedido.', 'ENTRANTE', FALSE, NULL),
    (6, 6, '2026-10-08 16:00:00', 'Felicitaciones por su atención', 'Quería felicitarlas, me atendieron muy bien y mi pedido llegó perfecto.', 'ENTRANTE', FALSE, NULL),
    (7, 7, '2026-09-28 14:30:00', 'Mi pedido llegó incompleto', 'Pedí tres productos y solo llegaron dos.', 'ENTRANTE', FALSE, NULL),
    (8, 8, '2026-10-20 12:10:00', 'Queja por mal trato en la línea de atención', 'Me atendieron de forma muy grosera cuando llamé a preguntar por mi pedido.', 'ENTRANTE', FALSE, NULL),
    (9, 3, '2026-10-14 08:02:00', 'Cómo usar tu producto', 'Hola Daniela, te compartimos el modo de uso recomendado: aplica sobre el rostro limpio y seco, y complementa con protector solar durante el día.', 'SALIENTE', TRUE, NULL),
    (10, 6, '2026-10-08 16:20:00', '¡Gracias por escribirnos!', 'Hola Natalia, gracias por tu mensaje. Lo compartiremos con todo el equipo de Piel de Azúcar.', 'SALIENTE', FALSE, 2);

MERGE INTO adjunto (id, nombre_archivo, ruta, tipo_mime, mensaje_id, plantilla_id) KEY (id) VALUES
    (1, 'foto_enrojecimiento.jpg', 'adjuntos/mensajes/foto_enrojecimiento.jpg', 'image/jpeg', 2, NULL),
    (2, 'guia_modo_de_uso.pdf', 'adjuntos/plantillas/guia_modo_de_uso.pdf', 'application/pdf', NULL, 2);

MERGE INTO registro_bitacora (id, fecha, accion, motivo, notas_internas, pqrs_id, usuario_id) KEY (id) VALUES
    (1, '2026-09-29 08:40:00', 'CASO_TOMADO', NULL, NULL, 7, 2),
    (2, '2026-10-08 16:20:00', 'RESPUESTA_ENVIADA', NULL, NULL, 6, 2),
    (3, '2026-10-08 16:20:00', 'CASO_RESUELTO', NULL, 'Felicitación registrada.', 6, 2),
    (4, '2026-10-09 11:30:00', 'CASO_TOMADO', NULL, NULL, 5, 2),
    (5, '2026-10-09 15:00:00', 'ESCALADA', 'Posible cobro duplicado; requiere revisar con la pasarela de pagos.', NULL, 5, 2),
    (6, '2026-10-13 10:05:00', 'CASO_TOMADO', NULL, NULL, 4, 2),
    (7, '2026-10-14 08:02:00', 'RESPUESTA_ENVIADA', NULL, 'Respuesta automática del sistema.', 3, NULL),
    (8, '2026-10-14 08:02:00', 'CASO_RESUELTO', NULL, NULL, 3, NULL),
    (9, '2026-10-20 14:10:00', 'DELEGADA', 'Queja sensible; la atiende el operador.', NULL, 8, 1);

-- Festivos de Colombia (Ley Emiliani). VERIFICAR contra el calendario oficial antes de la entrega final.
MERGE INTO festivo (fecha, nombre) KEY (fecha) VALUES
    ('2026-01-01', 'Año Nuevo'),
    ('2026-01-12', 'Día de los Reyes Magos'),
    ('2026-03-23', 'Día de San José'),
    ('2026-04-02', 'Jueves Santo'),
    ('2026-04-03', 'Viernes Santo'),
    ('2026-05-01', 'Día del Trabajo'),
    ('2026-05-18', 'Ascensión del Señor'),
    ('2026-06-08', 'Corpus Christi'),
    ('2026-06-15', 'Sagrado Corazón'),
    ('2026-06-29', 'San Pedro y San Pablo'),
    ('2026-07-20', 'Día de la Independencia'),
    ('2026-08-07', 'Batalla de Boyacá'),
    ('2026-08-17', 'Asunción de la Virgen'),
    ('2026-10-12', 'Día de la Raza'),
    ('2026-11-02', 'Todos los Santos'),
    ('2026-11-16', 'Independencia de Cartagena'),
    ('2026-12-08', 'Inmaculada Concepción'),
    ('2026-12-25', 'Navidad'),
    ('2027-01-01', 'Año Nuevo'),
    ('2027-01-11', 'Día de los Reyes Magos'),
    ('2027-03-22', 'Día de San José'),
    ('2027-03-25', 'Jueves Santo'),
    ('2027-03-26', 'Viernes Santo'),
    ('2027-05-10', 'Ascensión del Señor'),
    ('2027-05-31', 'Corpus Christi'),
    ('2027-06-07', 'Sagrado Corazón'),
    ('2027-07-05', 'San Pedro y San Pablo'),
    ('2027-07-20', 'Día de la Independencia'),
    ('2027-08-16', 'Asunción de la Virgen'),
    ('2027-10-18', 'Día de la Raza'),
    ('2027-11-01', 'Todos los Santos'),
    ('2027-11-15', 'Independencia de Cartagena'),
    ('2027-12-08', 'Inmaculada Concepción');

