ALTER TABLE repuesto ADD COLUMN sku VARCHAR(40);
@@
ALTER TABLE repuesto ADD COLUMN costo_promedio TEXT NOT NULL DEFAULT '0';
@@
CREATE UNIQUE INDEX uk_repuesto_sku ON repuesto(sku COLLATE NOCASE);
@@
CREATE TABLE proveedor (
    id INTEGER PRIMARY KEY,
    nombre VARCHAR(120) NOT NULL,
    ruc VARCHAR(11) NOT NULL UNIQUE,
    telefono VARCHAR(24)
);
@@
CREATE TABLE compra (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    proveedor_id BIGINT NOT NULL REFERENCES proveedor(id),
    fecha VARCHAR(10) NOT NULL,
    tipo_comprobante VARCHAR(20) NOT NULL CHECK (tipo_comprobante IN ('FACTURA','BOLETA','OTRO')),
    numero_comprobante VARCHAR(50) NOT NULL,
    estado VARCHAR(20) NOT NULL CHECK (estado IN ('BORRADOR','CONFIRMADA','ANULADA')),
    total TEXT NOT NULL,
    motivo_anulacion VARCHAR(500),
    UNIQUE (proveedor_id, tipo_comprobante, numero_comprobante)
);
@@
CREATE TABLE compra_detalle (
    id INTEGER PRIMARY KEY,
    compra_id BIGINT NOT NULL REFERENCES compra(id),
    repuesto_id BIGINT NOT NULL REFERENCES repuesto(id),
    cantidad INTEGER NOT NULL CHECK (cantidad > 0),
    costo_unitario TEXT NOT NULL,
    precio_venta_sugerido TEXT,
    UNIQUE (compra_id, repuesto_id)
);
@@
CREATE INDEX ix_compra_fecha ON compra(fecha);
@@
CREATE INDEX ix_compra_detalle_repuesto ON compra_detalle(repuesto_id);
@@
CREATE TABLE movimiento_inventario (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    repuesto_id BIGINT NOT NULL REFERENCES repuesto(id),
    tipo VARCHAR(25) NOT NULL CHECK (tipo IN ('ENTRADA_COMPRA','SALIDA_ORDEN','DEVOLUCION_ORDEN','AJUSTE')),
    cantidad INTEGER NOT NULL CHECK (cantidad BETWEEN -2147483647 AND 2147483647),
    saldo_resultante INTEGER NOT NULL CHECK (saldo_resultante BETWEEN 0 AND 2147483647),
    costo_unitario TEXT NOT NULL,
    origen_tipo VARCHAR(20) NOT NULL CHECK (origen_tipo IN ('SALDO_INICIAL','COMPRA','ORDEN','AJUSTE_MANUAL')),
    origen_id BIGINT,
    fecha VARCHAR(24) NOT NULL,
    usuario VARCHAR(30) NOT NULL,
    motivo VARCHAR(500) NOT NULL,
    CHECK (
        (tipo = 'ENTRADA_COMPRA' AND cantidad > 0 AND origen_tipo = 'COMPRA' AND origen_id IS NOT NULL)
        OR (tipo = 'SALIDA_ORDEN' AND cantidad < 0 AND origen_tipo = 'ORDEN' AND origen_id IS NOT NULL)
        OR (tipo = 'DEVOLUCION_ORDEN' AND cantidad > 0 AND origen_tipo = 'ORDEN' AND origen_id IS NOT NULL)
        OR (tipo = 'AJUSTE' AND (
            (origen_tipo = 'COMPRA' AND cantidad < 0 AND origen_id IS NOT NULL)
            OR (origen_tipo = 'AJUSTE_MANUAL' AND cantidad <> 0 AND origen_id IS NULL)
            OR (origen_tipo = 'SALDO_INICIAL' AND cantidad >= 0 AND origen_id IS NULL)
        ))
    )
);
@@
CREATE INDEX ix_movimiento_repuesto_id ON movimiento_inventario(repuesto_id, id DESC);
@@
CREATE INDEX ix_movimiento_repuesto_fecha ON movimiento_inventario(repuesto_id, fecha);
@@
CREATE UNIQUE INDEX uk_movimiento_saldo_inicial ON movimiento_inventario(repuesto_id)
    WHERE origen_tipo = 'SALDO_INICIAL';
@@
INSERT INTO movimiento_inventario
    (repuesto_id, tipo, cantidad, saldo_resultante, costo_unitario, origen_tipo, fecha, usuario, motivo)
    SELECT id, 'AJUSTE', stock, stock, '0', 'SALDO_INICIAL',
           strftime('%Y-%m-%dT%H:%M:%fZ', 'now'), 'sistema', 'Saldo inicial migrado' FROM repuesto;
@@
CREATE TRIGGER movimiento_validar_saldo BEFORE INSERT ON movimiento_inventario
FOR EACH ROW WHEN EXISTS (SELECT 1 FROM movimiento_inventario WHERE id = NEW.id)
 OR NEW.saldo_resultante <> COALESCE((
    SELECT saldo_resultante FROM movimiento_inventario
    WHERE repuesto_id = NEW.repuesto_id ORDER BY id DESC LIMIT 1
), 0) + NEW.cantidad
BEGIN
    SELECT RAISE(ABORT, 'El saldo del movimiento no coincide con el kardex');
END;
@@
CREATE TRIGGER movimiento_aplicar_saldo AFTER INSERT ON movimiento_inventario
FOR EACH ROW
BEGIN
    UPDATE repuesto SET stock = NEW.saldo_resultante WHERE id = NEW.repuesto_id;
END;
@@
CREATE TRIGGER repuesto_stock_inicial BEFORE INSERT ON repuesto
FOR EACH ROW WHEN NEW.stock <> 0
 OR EXISTS (SELECT 1 FROM movimiento_inventario WHERE repuesto_id = NEW.id)
BEGIN
    SELECT RAISE(ABORT, 'El stock inicial de un repuesto nuevo debe ser cero');
END;
@@
CREATE TRIGGER repuesto_stock_controlado BEFORE UPDATE OF stock ON repuesto
FOR EACH ROW WHEN NEW.stock <> COALESCE((
    SELECT saldo_resultante FROM movimiento_inventario WHERE repuesto_id = NEW.id ORDER BY id DESC LIMIT 1
), 0)
BEGIN
    SELECT RAISE(ABORT, 'El stock solo se cambia con un movimiento de inventario');
END;
@@
CREATE TRIGGER movimiento_sin_edicion BEFORE UPDATE ON movimiento_inventario
FOR EACH ROW
BEGIN
    SELECT RAISE(ABORT, 'El kardex no admite modificaciones');
END;
@@
CREATE TRIGGER movimiento_sin_borrado BEFORE DELETE ON movimiento_inventario
FOR EACH ROW
BEGIN
    SELECT RAISE(ABORT, 'El kardex no admite eliminaciones');
END;
@@
