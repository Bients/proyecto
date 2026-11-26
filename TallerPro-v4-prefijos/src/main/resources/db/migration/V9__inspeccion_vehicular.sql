ALTER TABLE vehiculo ADD COLUMN tipo_carroceria VARCHAR(20) NOT NULL DEFAULT 'SEDAN';
ALTER TABLE orden_trabajo ADD COLUMN recepcion_cerrada INTEGER NOT NULL DEFAULT 0 CHECK (recepcion_cerrada IN (0,1));
ALTER TABLE orden_trabajo ADD COLUMN cierre_inspeccion VARCHAR(30);
ALTER TABLE orden_trabajo ADD COLUMN carroceria_cierre VARCHAR(20);
UPDATE orden_trabajo SET recepcion_cerrada = 1 WHERE estado <> 'RECIBIDO';

CREATE TABLE hallazgo_vehiculo (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    vehiculo_id INTEGER NOT NULL REFERENCES vehiculo(id),
    orden_origen_id INTEGER REFERENCES orden_trabajo(id),
    orden_seguimiento_id INTEGER REFERENCES orden_trabajo(id),
    detalle_orden_id INTEGER UNIQUE REFERENCES detalle_orden(id),
    foto_id INTEGER REFERENCES orden_foto(id),
    zona VARCHAR(50) NOT NULL,
    capa VARCHAR(25) NOT NULL CHECK (capa IN ('CARROCERIA','CHASIS_Y_BAJOS','MECANICA','ELECTRICO_INTERIOR')),
    vista VARCHAR(16) NOT NULL CHECK (vista IN ('FRONTAL','TRASERA','IZQUIERDA','DERECHA','SUPERIOR','BAJOS')),
    x REAL, y REAL,
    tipo VARCHAR(20) NOT NULL CHECK (tipo IN ('RAYON','ABOLLADURA','GOLPE','FUGA','RUIDO','FALLA','PIEZA_FALTANTE','DESGASTE','OTRO')),
    gravedad VARCHAR(10) NOT NULL CHECK (gravedad IN ('LEVE','MEDIA','GRAVE')),
    origen VARCHAR(16) NOT NULL CHECK (origen IN ('PREEXISTENTE','DETECTADO')),
    estado VARCHAR(16) NOT NULL CHECK (estado IN ('ABIERTO','EN_REPARACION','REPARADO','DESCARTADO')),
    nota VARCHAR(500) NOT NULL CHECK (length(nota) <= 500),
    usuario VARCHAR(30) NOT NULL,
    usuario_actualizacion VARCHAR(30) NOT NULL,
    fecha_registro VARCHAR(30) NOT NULL,
    fecha_actualizacion VARCHAR(30) NOT NULL,
    motivo_descarte VARCHAR(500),
    CHECK ((x IS NULL AND y IS NULL) OR (x IS NOT NULL AND y IS NOT NULL AND x BETWEEN 0 AND 1 AND y BETWEEN 0 AND 1)),
    CHECK (detalle_orden_id IS NULL OR orden_seguimiento_id IS NOT NULL),
    CHECK (estado <> 'REPARADO' OR detalle_orden_id IS NOT NULL),
    CHECK (estado <> 'DESCARTADO' OR (motivo_descarte IS NOT NULL AND length(trim(motivo_descarte)) BETWEEN 1 AND 500))
);
CREATE INDEX ix_hallazgo_vehiculo_estado ON hallazgo_vehiculo(vehiculo_id, estado, id);
CREATE INDEX ix_hallazgo_orden_origen ON hallazgo_vehiculo(orden_origen_id);
CREATE INDEX ix_hallazgo_orden_seguimiento ON hallazgo_vehiculo(orden_seguimiento_id, estado);
CREATE INDEX ix_hallazgo_foto ON hallazgo_vehiculo(foto_id);
CREATE INDEX ix_vehiculo_cliente_placa ON vehiculo(cliente_id, placa);
CREATE TABLE hallazgo_evento (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    hallazgo_id INTEGER NOT NULL REFERENCES hallazgo_vehiculo(id),
    orden_id INTEGER REFERENCES orden_trabajo(id),
    detalle_orden_id INTEGER REFERENCES detalle_orden(id),
    accion VARCHAR(35) NOT NULL,
    estado VARCHAR(16) NOT NULL,
    usuario VARCHAR(30) NOT NULL,
    fecha VARCHAR(30) NOT NULL,
    nota VARCHAR(500) NOT NULL,
    zona VARCHAR(50),
    vista VARCHAR(16),
    x REAL, y REAL,
    tipo VARCHAR(20),
    gravedad VARCHAR(10)
);
CREATE INDEX ix_hallazgo_evento_historial ON hallazgo_evento(hallazgo_id, id);
CREATE INDEX ix_hallazgo_evento_orden ON hallazgo_evento(orden_id, hallazgo_id);
CREATE INDEX ix_hallazgo_evento_cierre ON hallazgo_evento(orden_id, fecha, accion);
