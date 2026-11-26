CREATE TABLE orden_foto (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    orden_id INTEGER NOT NULL REFERENCES orden_trabajo(id),
    fase VARCHAR(16) NOT NULL CHECK (fase IN ('INGRESO','DIAGNOSTICO','REPUESTOS','ENTREGA')),
    ruta_relativa VARCHAR(140) NOT NULL UNIQUE, nota VARCHAR(500) NOT NULL,
    fecha VARCHAR(30) NOT NULL, usuario VARCHAR(30) NOT NULL, usuario_id INTEGER REFERENCES usuario(id) ON DELETE SET NULL
);
CREATE INDEX ix_orden_foto_fase ON orden_foto(orden_id, fase, id);
