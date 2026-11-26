ALTER TABLE repuesto ADD COLUMN precio_centimos INTEGER NOT NULL DEFAULT 0 CHECK (typeof(precio_centimos) = 'integer' AND precio_centimos >= 0);
@@
UPDATE repuesto SET precio_centimos = centimos_v6(precio);
@@
ALTER TABLE detalle_orden ADD COLUMN precio_unitario_centimos INTEGER NOT NULL DEFAULT 0 CHECK (typeof(precio_unitario_centimos) = 'integer' AND precio_unitario_centimos >= 0);
@@
UPDATE detalle_orden SET precio_unitario_centimos = centimos_v6(COALESCE(precio_unitario_exacto, precio_unitario));
@@
ALTER TABLE compra ADD COLUMN total_centimos INTEGER NOT NULL DEFAULT 0 CHECK (typeof(total_centimos) = 'integer' AND total_centimos >= 0);
@@
UPDATE compra SET total_centimos = centimos_v6(total);
@@
ALTER TABLE compra_detalle ADD COLUMN precio_venta_centimos INTEGER CHECK (precio_venta_centimos IS NULL OR (typeof(precio_venta_centimos) = 'integer' AND precio_venta_centimos >= 0));
@@
UPDATE compra_detalle SET precio_venta_centimos = centimos_v6(precio_venta_sugerido) WHERE precio_venta_sugerido IS NOT NULL;
@@
ALTER TABLE orden_trabajo ADD COLUMN descuento_centimos INTEGER NOT NULL DEFAULT 0 CHECK (typeof(descuento_centimos) = 'integer' AND descuento_centimos >= 0);
@@
CREATE TABLE entrega_con_saldo (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    orden_id INTEGER NOT NULL REFERENCES orden_trabajo(id),
    saldo_centimos INTEGER NOT NULL CHECK (typeof(saldo_centimos) = 'integer' AND saldo_centimos > 0),
    fecha VARCHAR(24) NOT NULL,
    usuario VARCHAR(30) NOT NULL,
    motivo VARCHAR(500) NOT NULL CHECK (length(trim(motivo)) BETWEEN 1 AND 500)
);
@@
CREATE INDEX ix_entrega_con_saldo_orden ON entrega_con_saldo(orden_id, id);
@@
CREATE TABLE pago (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    orden_id INTEGER NOT NULL REFERENCES orden_trabajo(id),
    monto_centimos INTEGER NOT NULL CHECK (typeof(monto_centimos) = 'integer' AND monto_centimos > 0),
    metodo VARCHAR(20) NOT NULL CHECK (metodo IN ('EFECTIVO','YAPE','PLIN','TRANSFERENCIA','TARJETA')),
    fecha VARCHAR(24) NOT NULL,
    nota VARCHAR(500),
    usuario VARCHAR(30) NOT NULL,
    anulado BOOLEAN NOT NULL DEFAULT FALSE CHECK (anulado IN (0, 1)),
    motivo_anulacion VARCHAR(500), fecha_anulacion VARCHAR(24), usuario_anulacion VARCHAR(30),
    CHECK ((anulado = 0 AND motivo_anulacion IS NULL AND fecha_anulacion IS NULL AND usuario_anulacion IS NULL)
        OR (anulado = 1 AND motivo_anulacion IS NOT NULL AND length(trim(motivo_anulacion)) BETWEEN 1 AND 500
            AND fecha_anulacion IS NOT NULL AND usuario_anulacion IS NOT NULL))
);
@@
CREATE INDEX ix_pago_orden_activo ON pago(orden_id, anulado);
@@
CREATE INDEX ix_pago_fecha ON pago(fecha);
@@
CREATE INDEX ix_pago_fecha_anulacion ON pago(fecha_anulacion);
@@
CREATE TRIGGER pago_no_reemplazar BEFORE INSERT ON pago FOR EACH ROW
WHEN EXISTS (SELECT 1 FROM pago WHERE id = NEW.id)
BEGIN
    SELECT RAISE(ABORT, 'El pago no puede reemplazarse');
END;
@@
CREATE TRIGGER pago_sin_borrado BEFORE DELETE ON pago FOR EACH ROW
BEGIN
    SELECT RAISE(ABORT, 'El pago no puede borrarse');
END;
@@
CREATE TRIGGER pago_solo_anulacion BEFORE UPDATE ON pago FOR EACH ROW
WHEN OLD.anulado <> 0 OR NEW.anulado <> 1 OR OLD.id IS NOT NEW.id OR OLD.orden_id IS NOT NEW.orden_id
    OR OLD.monto_centimos IS NOT NEW.monto_centimos OR OLD.metodo IS NOT NEW.metodo OR OLD.fecha IS NOT NEW.fecha
    OR OLD.nota IS NOT NEW.nota OR OLD.usuario IS NOT NEW.usuario
BEGIN
    SELECT RAISE(ABORT, 'El pago solo admite una anulación');
END;
@@
