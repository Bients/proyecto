ALTER TABLE orden_trabajo ADD COLUMN cliente_nombre VARCHAR(121);
@@
ALTER TABLE orden_trabajo ADD COLUMN cliente_dni VARCHAR(8);
@@
ALTER TABLE orden_trabajo ADD COLUMN cliente_telefono VARCHAR(9);
@@
ALTER TABLE orden_trabajo ADD COLUMN cliente_correo VARCHAR(120);
@@
ALTER TABLE orden_trabajo ADD COLUMN vehiculo_placa VARCHAR(8);
@@
ALTER TABLE orden_trabajo ADD COLUMN vehiculo_marca VARCHAR(40);
@@
ALTER TABLE orden_trabajo ADD COLUMN vehiculo_modelo VARCHAR(40);
@@
ALTER TABLE orden_trabajo ADD COLUMN vehiculo_anio INTEGER;
@@
UPDATE orden_trabajo
SET (cliente_nombre, cliente_dni, cliente_telefono, cliente_correo, vehiculo_placa, vehiculo_marca, vehiculo_modelo, vehiculo_anio) =
    (SELECT c.nombre || ' ' || c.apellido, c.dni, c.telefono, c.email, v.placa, v.marca, v.modelo, v.anio
     FROM vehiculo v JOIN cliente c ON c.id = v.cliente_id WHERE v.id = orden_trabajo.vehiculo_id);
@@
CREATE TABLE detalle_orden_nuevo (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    orden_id BIGINT NOT NULL REFERENCES orden_trabajo(id),
    repuesto_id BIGINT REFERENCES repuesto(id),
    tipo VARCHAR(20) NOT NULL CHECK (tipo IN ('SERVICIO', 'REPUESTO', 'REPUESTO_EXTERNO')),
    descripcion VARCHAR(120) NOT NULL,
    cantidad INTEGER NOT NULL CHECK (cantidad >= 1),
    precio_unitario FLOAT NOT NULL,
    precio_unitario_exacto TEXT,
    costo_unitario TEXT,
    devuelto_en_anulacion BOOLEAN NOT NULL DEFAULT FALSE,
    retirado BOOLEAN NOT NULL DEFAULT FALSE,
    CHECK ((tipo = 'REPUESTO' AND repuesto_id IS NOT NULL) OR (tipo <> 'REPUESTO' AND repuesto_id IS NULL))
);
@@
INSERT INTO detalle_orden_nuevo
    (id, orden_id, repuesto_id, tipo, descripcion, cantidad, precio_unitario, devuelto_en_anulacion)
SELECT id, orden_id, CASE WHEN tipo = 'REPUESTO' THEN repuesto_id ELSE NULL END,
       CASE WHEN tipo = 'REPUESTO' AND repuesto_id IS NULL THEN 'REPUESTO_EXTERNO' ELSE tipo END,
       descripcion, cantidad, precio_unitario, devuelto_en_anulacion
FROM detalle_orden;
@@
DROP TABLE detalle_orden;
@@
ALTER TABLE detalle_orden_nuevo RENAME TO detalle_orden;
@@
CREATE INDEX ix_detalle_orden ON detalle_orden(orden_id, retirado);
@@
CREATE INDEX ix_detalle_repuesto ON detalle_orden(repuesto_id);
@@
ALTER TABLE movimiento_inventario ADD COLUMN detalle_orden_id BIGINT REFERENCES detalle_orden(id);
@@
CREATE INDEX ix_movimiento_orden_detalle ON movimiento_inventario(origen_tipo, origen_id, detalle_orden_id, id);
@@
CREATE TRIGGER movimiento_validar_detalle_orden BEFORE INSERT ON movimiento_inventario
FOR EACH ROW WHEN
    (NEW.origen_tipo = 'ORDEN' AND NOT EXISTS (
        SELECT 1 FROM detalle_orden d WHERE d.id = NEW.detalle_orden_id AND d.orden_id = NEW.origen_id
          AND d.repuesto_id = NEW.repuesto_id AND d.tipo = 'REPUESTO' AND d.retirado = 0
    ))
    OR (NEW.origen_tipo <> 'ORDEN' AND NEW.detalle_orden_id IS NOT NULL)
BEGIN
    SELECT RAISE(ABORT, 'El movimiento debe referenciar la partida activa de la orden');
END;
@@
