-- Esquema emitido por Hibernate 6.5.3 del TallerPro anterior, sin migraciones.
CREATE TABLE cliente (dni varchar(8) not null, id integer, telefono varchar(9), apellido varchar(60) not null, nombre varchar(60) not null, email varchar(120), primary key (id));
CREATE TABLE detalle_orden (cantidad integer not null check (cantidad>=1), precio_unitario float not null, id integer, orden_id bigint not null, repuesto_id bigint, tipo varchar(20) not null check (tipo in ('SERVICIO','REPUESTO')), descripcion varchar(120) not null, primary key (id));
CREATE TABLE orden_trabajo (kilometraje integer check (kilometraje>=0), id integer, vehiculo_id bigint not null, estado varchar(20) not null check (estado in ('RECIBIDO','DIAGNOSTICO','REPARACION','LISTO','ENTREGADO')), mecanico varchar(60), problema varchar(500) not null, fecha_ingreso varchar(255), primary key (id));
CREATE TABLE programa_mantenimiento (cada_km integer not null check (cada_km>=0), cada_meses integer not null check (cada_meses>=0), ultimo_km integer not null check (ultimo_km>=0), id integer, vehiculo_id bigint not null, servicio varchar(80) not null, ultima_fecha varchar(255) not null, primary key (id));
CREATE TABLE repuesto (precio float not null, stock integer not null check (stock>=0), stock_minimo integer not null check (stock_minimo>=0), id integer, nombre varchar(80) not null, primary key (id));
CREATE TABLE usuario (id integer, rol varchar(20) not null check (rol in ('ADMIN','MECANICO')), usuario varchar(30) not null, nombre varchar(60) not null, clave_hash varchar(100) not null, primary key (id));
CREATE TABLE vehiculo (anio integer not null check ((anio<=2100) and (anio>=1950)), kilometraje integer not null check (kilometraje>=0), cliente_id bigint not null, id integer, placa varchar(8) not null, marca varchar(40) not null, modelo varchar(40) not null, primary key (id));

INSERT INTO cliente (id,dni,apellido,nombre) VALUES (11,'12345678','Pérez','Ana');
INSERT INTO vehiculo (id,cliente_id,placa,marca,modelo,anio,kilometraje) VALUES (21,11,'ABC123','Toyota','Yaris',2020,100);
INSERT INTO repuesto (id,nombre,precio,stock,stock_minimo) VALUES (31,'Filtro',20,5,1);
INSERT INTO usuario (id,rol,usuario,nombre,clave_hash) VALUES (1,'ADMIN','operador','Operador','hash-legado');
INSERT INTO programa_mantenimiento (id,vehiculo_id,servicio,cada_km,cada_meses,ultimo_km,ultima_fecha)
    VALUES (51,21,'Aceite',5000,6,100,'2025-01-02');
INSERT INTO orden_trabajo (id,vehiculo_id,estado,problema,fecha_ingreso,kilometraje)
    VALUES (41,21,'REPARACION','Revisión','2025-01-02',100);
INSERT INTO detalle_orden (id,orden_id,repuesto_id,tipo,descripcion,cantidad,precio_unitario)
    VALUES (7,41,31,'REPUESTO','Filtro sin usar',2,20), (8,41,31,'REPUESTO','Filtro instalado',1,20);
