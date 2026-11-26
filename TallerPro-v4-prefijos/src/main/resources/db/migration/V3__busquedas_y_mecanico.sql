ALTER TABLE orden_trabajo ADD COLUMN mecanico_id BIGINT REFERENCES usuario(id) ON DELETE SET NULL;

UPDATE orden_trabajo
SET mecanico_id = (
    SELECT min(u.id)
    FROM usuario u
    WHERE trim(u.nombre) = trim(orden_trabajo.mecanico) COLLATE NOCASE
       OR u.usuario = trim(orden_trabajo.mecanico) COLLATE NOCASE
    HAVING count(*) = 1
)
WHERE trim(coalesce(mecanico, '')) <> '';

CREATE INDEX ix_cliente_dni_busqueda ON cliente(dni);
CREATE INDEX ix_vehiculo_placa_busqueda ON vehiculo(replace(replace(upper(placa), '-', ''), ' ', ''));
CREATE INDEX ix_repuesto_nombre_busqueda ON repuesto(nombre COLLATE NOCASE);
CREATE INDEX ix_orden_fecha ON orden_trabajo(fecha_ingreso);
CREATE INDEX ix_orden_estado_fecha ON orden_trabajo(estado, fecha_ingreso);
CREATE INDEX ix_orden_mecanico_fecha ON orden_trabajo(mecanico_id, fecha_ingreso);
