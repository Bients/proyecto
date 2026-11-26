ALTER TABLE usuario ADD COLUMN intentos_fallidos INTEGER NOT NULL DEFAULT 0 CHECK (intentos_fallidos >= 0);
ALTER TABLE usuario ADD COLUMN bloqueado_hasta INTEGER NOT NULL DEFAULT 0 CHECK (bloqueado_hasta >= 0);

CREATE INDEX ix_orden_mecanico_fecha_estado ON orden_trabajo(mecanico_id, fecha_ingreso, estado);
CREATE INDEX ix_orden_fecha_estado ON orden_trabajo(fecha_ingreso, estado);
