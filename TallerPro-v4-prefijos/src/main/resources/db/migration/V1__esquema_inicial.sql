CREATE TABLE cliente (
    id INTEGER PRIMARY KEY, dni VARCHAR(8) NOT NULL UNIQUE, telefono VARCHAR(9),
    apellido VARCHAR(60) NOT NULL, nombre VARCHAR(60) NOT NULL, email VARCHAR(120)
);
CREATE TABLE vehiculo (
    id INTEGER PRIMARY KEY, placa VARCHAR(8) NOT NULL UNIQUE,
    marca VARCHAR(40) NOT NULL, modelo VARCHAR(40) NOT NULL,
    anio INTEGER NOT NULL CHECK (anio BETWEEN 1950 AND 2100),
    kilometraje INTEGER NOT NULL CHECK (kilometraje >= 0),
    cliente_id BIGINT NOT NULL REFERENCES cliente(id)
);
CREATE TABLE repuesto (
    id INTEGER PRIMARY KEY, nombre VARCHAR(80) NOT NULL UNIQUE, precio FLOAT NOT NULL,
    stock INTEGER NOT NULL CHECK (stock >= 0), stock_minimo INTEGER NOT NULL CHECK (stock_minimo >= 0)
);
CREATE TABLE usuario (
    id INTEGER PRIMARY KEY, usuario VARCHAR(30) NOT NULL UNIQUE, nombre VARCHAR(60) NOT NULL,
    clave_hash VARCHAR(100) NOT NULL, rol VARCHAR(20) NOT NULL CHECK (rol IN ('ADMIN', 'MECANICO'))
);
CREATE TABLE programa_mantenimiento (
    id INTEGER PRIMARY KEY, vehiculo_id BIGINT NOT NULL REFERENCES vehiculo(id),
    servicio VARCHAR(80) NOT NULL, cada_km INTEGER NOT NULL CHECK (cada_km >= 0),
    cada_meses INTEGER NOT NULL CHECK (cada_meses >= 0),
    ultimo_km INTEGER NOT NULL CHECK (ultimo_km >= 0), ultima_fecha VARCHAR(255) NOT NULL
);
CREATE TABLE orden_trabajo (
    id INTEGER PRIMARY KEY, vehiculo_id BIGINT NOT NULL REFERENCES vehiculo(id),
    estado VARCHAR(20) NOT NULL CHECK (estado IN ('RECIBIDO', 'DIAGNOSTICO', 'REPARACION', 'LISTO', 'ENTREGADO')),
    mecanico VARCHAR(60), problema VARCHAR(500) NOT NULL, fecha_ingreso VARCHAR(255),
    kilometraje INTEGER CHECK (kilometraje >= 0)
);
CREATE TABLE detalle_orden (
    id INTEGER PRIMARY KEY, orden_id BIGINT NOT NULL REFERENCES orden_trabajo(id),
    repuesto_id BIGINT REFERENCES repuesto(id),
    tipo VARCHAR(20) NOT NULL CHECK (tipo IN ('SERVICIO', 'REPUESTO')),
    descripcion VARCHAR(120) NOT NULL, cantidad INTEGER NOT NULL CHECK (cantidad >= 1),
    precio_unitario FLOAT NOT NULL
);
