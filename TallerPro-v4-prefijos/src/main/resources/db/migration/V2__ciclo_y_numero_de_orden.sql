CREATE TABLE orden_trabajo_nueva (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    numero_orden VARCHAR(23) GENERATED ALWAYS AS (printf('OT-%04d', id)) VIRTUAL NOT NULL,
    vehiculo_id BIGINT NOT NULL REFERENCES vehiculo(id),
    estado VARCHAR(20) NOT NULL CHECK (estado IN ('RECIBIDO', 'DIAGNOSTICO', 'REPARACION', 'LISTO', 'ENTREGADO', 'ANULADO')),
    mecanico VARCHAR(60), problema VARCHAR(500) NOT NULL, fecha_ingreso VARCHAR(255),
    kilometraje INTEGER CHECK (kilometraje >= 0),
    motivo_anulacion VARCHAR(500), fecha_anulacion VARCHAR(30), usuario_anulacion VARCHAR(30),
    motivo_reapertura VARCHAR(500), fecha_reapertura VARCHAR(30), usuario_reapertura VARCHAR(30)
);
INSERT INTO orden_trabajo_nueva (id, vehiculo_id, estado, mecanico, problema, fecha_ingreso, kilometraje)
    SELECT id, vehiculo_id, estado, mecanico, problema, fecha_ingreso, kilometraje FROM orden_trabajo;

CREATE TABLE detalle_orden_nuevo (
    id INTEGER PRIMARY KEY, orden_id BIGINT NOT NULL REFERENCES orden_trabajo_nueva(id),
    repuesto_id BIGINT REFERENCES repuesto(id),
    tipo VARCHAR(20) NOT NULL CHECK (tipo IN ('SERVICIO', 'REPUESTO')),
    descripcion VARCHAR(120) NOT NULL, cantidad INTEGER NOT NULL CHECK (cantidad >= 1),
    precio_unitario FLOAT NOT NULL, devuelto_en_anulacion BOOLEAN NOT NULL DEFAULT FALSE
);
INSERT INTO detalle_orden_nuevo (id, orden_id, repuesto_id, tipo, descripcion, cantidad, precio_unitario)
    SELECT id, orden_id, repuesto_id, tipo, descripcion, cantidad, precio_unitario FROM detalle_orden;

DROP TABLE detalle_orden;
DROP TABLE orden_trabajo;
ALTER TABLE orden_trabajo_nueva RENAME TO orden_trabajo;
ALTER TABLE detalle_orden_nuevo RENAME TO detalle_orden;
CREATE UNIQUE INDEX uk_orden_numero ON orden_trabajo(numero_orden);
CREATE INDEX ix_orden_vehiculo ON orden_trabajo(vehiculo_id);
CREATE INDEX ix_orden_estado ON orden_trabajo(estado);
CREATE INDEX ix_detalle_orden ON detalle_orden(orden_id);
CREATE INDEX ix_detalle_repuesto ON detalle_orden(repuesto_id);
