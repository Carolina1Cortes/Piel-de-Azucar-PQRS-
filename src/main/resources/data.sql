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

-- Fuente: "Tabla de clasificación de PQRS" entregada por Paula Mesa (Piel de Azúcar); contrastada el 22/11/2026.
-- Ids 1 a 36: tabla oficial. Id 37: tipo técnico "Por clasificar" de la clasificación automática (E5-07), no está en la tabla.
-- palabras_clave: redistribuidas desde la lista inicial de E5-07 hacia el tipo oficial más cercano.
-- Automatización según el texto oficial de la tabla:
--   id 1: "Automática, si el sistema consulta el pedido"
--   id 11: "Automática, con información aprobada"
--   id 13: "Automática, con guía de uso"
--   id 24: "Automática o asistida" (se eligió ASISTIDA, la opción más conservadora)
--   id 29: "Asistida, con alerta de seguimiento"
--   id 35: "Respuesta automática o registro" (se eligió REGISTRO, la opción más conservadora)
MERGE INTO tipo_consulta (id, descripcion, categoria, prioridad_por_defecto, automatizacion, palabras_clave) KEY (id) VALUES
    (1, '¿Cómo puedo saber el estado de mi pedido?', 'PEDIDOS', 'MEDIA', 'AUTOMATICA', 'estado de mi pedido,estado del pedido,mi pedido,numero de pedido,estado de la orden'),
    (2, 'Mi pedido todavía no ha llegado.', 'PEDIDOS', 'ALTA', 'ASISTIDA', 'cuando llega,no llega'),
    (3, 'Recibí un producto diferente al que compré.', 'PEDIDOS', 'ALTA', 'ASISTIDA', 'equivocado'),
    (4, 'Mi pedido llegó incompleto.', 'PEDIDOS', 'ALTA', 'ASISTIDA', 'incompleto,faltante,falto,solo llegaron'),
    (5, '¿Puedo modificar mi pedido después de realizar la compra?', 'PEDIDOS', 'MEDIA', 'ASISTIDA', 'cancelar,cancelacion,modificar mi pedido,agregar al pedido'),
    (6, '¿Cuánto cuesta el envío?', 'ENVIOS_ENTREGAS', 'BAJA', 'AUTOMATICA', NULL),
    (7, '¿Cuánto tiempo tarda en llegar mi pedido?', 'ENVIOS_ENTREGAS', 'BAJA', 'AUTOMATICA', 'envio,despacho'),
    (8, 'El número de guía no funciona.', 'ENVIOS_ENTREGAS', 'MEDIA', 'ASISTIDA', 'seguimiento,rastreo,guia,tracking'),
    (9, 'La transportadora indica que entregó el pedido, pero no lo recibí.', 'ENVIOS_ENTREGAS', 'ALTA', 'ASISTIDA', 'transportadora,no llego,no recibi'),
    (10, '¿Puedo cambiar la dirección de entrega?', 'ENVIOS_ENTREGAS', 'MEDIA', 'ASISTIDA', 'cambiar la direccion,cambiar mi direccion'),
    (11, '¿Cómo debo usar el tónico Rice Glow?', 'PRODUCTOS_USO', 'BAJA', 'AUTOMATICA', 'modo de uso,como uso,como usar,como aplico,como debo aplicar,cuantas veces,rutina,instrucciones'),
    (12, '¿Puedo usar el tónico todos los días?', 'PRODUCTOS_USO', 'MEDIA', 'ASISTIDA', NULL),
    (13, '¿En qué orden debo aplicar mis productos faciales?', 'PRODUCTOS_USO', 'BAJA', 'AUTOMATICA', NULL),
    (14, '¿Qué producto me recomiendan para mi tipo de piel?', 'PRODUCTOS_USO', 'MEDIA', 'ASISTIDA', 'recomienda,recomendacion,recomendar,tipo de piel,piel grasa,piel seca,piel mixta,piel sensible'),
    (15, '¿Cómo debo conservar los productos?', 'PRODUCTOS_USO', 'BAJA', 'AUTOMATICA', NULL),
    (16, 'Mi piel se puso roja después de usar el producto.', 'REACCIONES', 'CRITICA', 'ATENCION_HUMANA', 'enroj'),
    (17, 'Siento ardor o picazón después de aplicarlo.', 'REACCIONES', 'CRITICA', 'ATENCION_HUMANA', 'ardor,me arde,picazon'),
    (18, 'Me apareció una irritación después de usar el producto.', 'REACCIONES', 'CRITICA', 'ATENCION_HUMANA', 'irrita,alerg,sarpullido,ronchas,hinchaz,inflamaci,quemadura,dermatitis,brote'),
    (19, '¿Qué debo hacer si el producto me causó una reacción?', 'REACCIONES', 'CRITICA', 'ATENCION_HUMANA', 'molestia,malestar,me hizo mal,me siento mal,resequedad,reseca,descama,tirante,me pico,me ardio'),
    (20, 'El producto llegó roto o derramado.', 'CAMBIOS_DEVOLUCIONES', 'ALTA', 'ASISTIDA', 'roto'),
    (21, 'El envase llegó dañado.', 'CAMBIOS_DEVOLUCIONES', 'ALTA', 'ASISTIDA', 'danado'),
    (22, 'Quiero solicitar un cambio de producto.', 'CAMBIOS_DEVOLUCIONES', 'MEDIA', 'ASISTIDA', 'cambio,cambiar,no me gusto'),
    (23, '¿Cómo puedo solicitar una devolución?', 'CAMBIOS_DEVOLUCIONES', 'MEDIA', 'ASISTIDA', 'devol,reembols,no me funciono'),
    (24, '¿Qué información necesito para reportar un producto defectuoso?', 'CAMBIOS_DEVOLUCIONES', 'MEDIA', 'ASISTIDA', 'garantia,defectuos,defecto,fabrica,vencid,caduc'),
    (25, 'Realicé el pago, pero no me han confirmado la compra.', 'PAGOS_FACTURACION', 'ALTA', 'ASISTIDA', NULL),
    (26, '¿Qué medios de pago aceptan?', 'PAGOS_FACTURACION', 'BAJA', 'AUTOMATICA', 'metodos de pago,medios de pago,formas de pago,como pago,pse,nequi,daviplata,contraentrega,cuotas'),
    (27, '¿Puedo solicitar la factura de mi compra?', 'PAGOS_FACTURACION', 'MEDIA', 'ASISTIDA', 'factura'),
    (28, 'Me cobraron un valor diferente al que esperaba.', 'PAGOS_FACTURACION', 'ALTA', 'ATENCION_HUMANA', 'cobr,cargo,doble cobro,dos veces,descontaron,debitaron'),
    (29, 'Llevo varios días esperando una respuesta.', 'ATENCION_QUEJAS', 'ALTA', 'ASISTIDA', NULL),
    (30, 'No me dieron una solución a mi solicitud anterior.', 'ATENCION_QUEJAS', 'ALTA', 'ATENCION_HUMANA', NULL),
    (31, 'No estoy satisfecha con la atención que recibí.', 'ATENCION_QUEJAS', 'ALTA', 'ATENCION_HUMANA', 'queja,mal trato,maltrato,groser,mala atencion,pesim,inconforme,insatisfech,irrespet,reclamo'),
    (32, 'Me dieron información diferente por dos canales de atención.', 'ATENCION_QUEJAS', 'ALTA', 'ATENCION_HUMANA', NULL),
    (33, 'Me gustaría que lanzaran un producto para otro tipo de necesidad facial.', 'SUGERENCIAS_FELICITACIONES', 'BAJA', 'REGISTRO', 'deberian,seria bueno,me gustaria que'),
    (34, 'Quisiera sugerir una mejora en el empaque.', 'SUGERENCIAS_FELICITACIONES', 'BAJA', 'REGISTRO', 'sugerenc,sugier,sugerir'),
    (35, 'Me encantó el producto y quiero compartir mi experiencia.', 'SUGERENCIAS_FELICITACIONES', 'BAJA', 'REGISTRO', 'felicit,agradezc,agradec,excelente,me encanto,muy bien'),
    (36, '¿Cómo puedo enviar un testimonio sobre mi experiencia?', 'SUGERENCIAS_FELICITACIONES', 'BAJA', 'AUTOMATICA', NULL),
    (37, 'Por clasificar manualmente', 'POR_CLASIFICAR', 'MEDIA', 'ATENCION_HUMANA', NULL);

MERGE INTO plantilla_respuesta (id, nombre, asunto, cuerpo, palabras_clave, tipo_consulta_id, activa) KEY (id) VALUES
    (1, 'Estado de pedido', 'Estado de tu pedido en Piel de Azúcar', 'Hola {nombre}, gracias por escribirnos. Tu pedido está en proceso y te avisaremos apenas salga. Cualquier duda, estamos pendientes.', 'pedido,estado,orden', 1, TRUE),
    (2, 'Modo de uso', 'Cómo usar tu producto', 'Hola {nombre}, te compartimos el modo de uso recomendado: aplica sobre el rostro limpio y seco, y complementa con protector solar durante el día.', 'uso,aplicar,rutina,modo', 11, TRUE),
    (3, 'Solicitud de cambio o devolución', 'Tu solicitud de cambio o devolución', 'Hola {nombre}, recibimos tu solicitud. Para continuar, cuéntanos el número de pedido y envíanos una foto del producto.', 'cambio,devolucion,devolver', 22, TRUE),
    (4, 'Agradecimiento por sugerencia o felicitación', '¡Gracias por escribirnos!', 'Hola {nombre}, gracias por tu mensaje. Lo compartiremos con todo el equipo de Piel de Azúcar.', 'gracias,felicitacion,sugerencia', 35, TRUE);

MERGE INTO pqrs (id, id_externo, radicado, canal, fecha_llegada, asunto, estado, prioridad, fecha_limite, descripcion_solucion, fecha_resolucion, cliente_id, tipo_consulta_id, responsable_id, resuelta_por_id) KEY (id) VALUES
    (1, 'gmail-msg-001', 'PQRS-2026-000001', 'GMAIL', '2026-10-19 09:15:00', '¿Cuándo llega mi pedido #1042?', 'PENDIENTE', 'ALTA', '2026-11-10', NULL, NULL, 1, 2, NULL, NULL),
    (2, 'gmail-msg-002', 'PQRS-2026-000002', 'GMAIL', '2026-10-16 19:40:00', 'Me salió enrojecimiento con el sérum', 'PENDIENTE', 'CRITICA', '2026-11-09', NULL, NULL, 2, 16, NULL, NULL),
    (3, 'gmail-msg-003', 'PQRS-2026-000003', 'GMAIL', '2026-10-14 08:00:00', '¿Cómo uso el ácido hialurónico?', 'RESUELTA', 'BAJA', '2026-11-05', 'Respuesta automática con el modo de uso del producto.', '2026-10-14 08:02:00', 3, 11, NULL, NULL),
    (4, 'gmail-msg-004', 'PQRS-2026-000004', 'GMAIL', '2026-10-13 10:00:00', 'Quiero devolver mi crema hidratante', 'EN_ATENCION', 'MEDIA', '2026-11-04', NULL, NULL, 4, 23, 2, NULL),
    (5, 'gmail-msg-005', 'PQRS-2026-000005', 'GMAIL', '2026-10-09 11:00:00', 'Me cobraron dos veces mi pedido', 'ESCALADA', 'ALTA', '2026-11-03', NULL, NULL, 5, 28, 2, NULL),
    (6, 'gmail-msg-006', 'PQRS-2026-000006', 'GMAIL', '2026-10-08 16:00:00', 'Felicitaciones por su atención', 'RESUELTA', 'BAJA', '2026-10-30', 'Se agradeció la felicitación y se compartió con el equipo.', '2026-10-08 16:20:00', 6, 35, NULL, 2),
    (7, 'gmail-msg-007', 'PQRS-2026-000007', 'GMAIL', '2026-09-28 14:30:00', 'Mi pedido llegó incompleto', 'EN_ATENCION', 'ALTA', '2026-10-20', NULL, NULL, 1, 4, 2, NULL),
    (8, 'gmail-msg-008', 'PQRS-2026-000008', 'GMAIL', '2026-10-20 12:10:00', 'Queja por mal trato en la línea de atención', 'DELEGADA', 'ALTA', '2026-11-11', NULL, NULL, 2, 31, 2, NULL);

MERGE INTO mensaje (id, pqrs_id, fecha, asunto, cuerpo, direccion, automatico, autor_id) KEY (id) VALUES
    (1, 1, '2026-10-19 09:15:00', '¿Cuándo llega mi pedido #1042?', 'Hola, hice el pedido hace una semana y todavía no llega. ¿Me pueden decir en qué va?', 'ENTRANTE', FALSE, NULL),
    (2, 2, '2026-10-16 19:40:00', 'Me salió enrojecimiento con el sérum', 'Hola, desde que uso el sérum me salió enrojecimiento y ardor en las mejillas. Adjunto una foto.', 'ENTRANTE', FALSE, NULL),
    (3, 3, '2026-10-14 08:00:00', '¿Cómo uso el ácido hialurónico?', 'Buenos días, ¿cómo debo aplicar el ácido hialurónico?', 'ENTRANTE', FALSE, NULL),
    (4, 4, '2026-10-13 10:00:00', 'Quiero devolver mi crema hidratante', 'Compré la crema hidratante y no me funcionó, quisiera devolverla.', 'ENTRANTE', FALSE, NULL),
    (5, 5, '2026-10-09 11:00:00', 'Me cobraron dos veces mi pedido', 'Veo dos cargos iguales en mi tarjeta por el mismo pedido.', 'ENTRANTE', FALSE, NULL),
    (6, 6, '2026-10-08 16:00:00', 'Felicitaciones por su atención', 'Quería felicitarlos, me atendieron muy bien y mi pedido llegó perfecto.', 'ENTRANTE', FALSE, NULL),
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

-- Festivos de Colombia 2026 y 2027. Fuentes: Ley 51 de 1983 (Ley Emiliani), Ley 2578 de 2026 (Virgen de Chiquinquirá)
-- y contraste con Hacé Cuentas (CC-BY 4.0, hacecuentas.com). Fechas contrastadas el 22/11/2026.
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
    ('2026-07-13', 'Virgen de Chiquinquirá'),
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
    ('2027-05-01', 'Día del Trabajo'),
    ('2027-05-10', 'Ascensión del Señor'),
    ('2027-05-31', 'Corpus Christi'),
    ('2027-06-07', 'Sagrado Corazón'),
    ('2027-07-05', 'San Pedro y San Pablo'),
    ('2027-07-12', 'Virgen de Chiquinquirá'),
    ('2027-07-20', 'Día de la Independencia'),
    ('2027-08-07', 'Batalla de Boyacá'),
    ('2027-08-16', 'Asunción de la Virgen'),
    ('2027-10-18', 'Día de la Raza'),
    ('2027-11-01', 'Todos los Santos'),
    ('2027-11-15', 'Independencia de Cartagena'),
    ('2027-12-08', 'Inmaculada Concepción'),
    ('2027-12-25', 'Navidad');


-- E5-13: categoría de los casos de prueba que ya vienen resueltos
UPDATE pqrs SET categoria_resolucion = 'INFORMACION_BRINDADA' WHERE id = 3 AND categoria_resolucion IS NULL;
UPDATE pqrs SET categoria_resolucion = 'SOLUCIONADO' WHERE id = 6 AND categoria_resolucion IS NULL;
