# TallerPro 2.0 - Gestión de taller automotriz
JavaFX + Spring Boot 3.3.4 + JPA/Hibernate + Lombok + SQLite + AtlantaFX

Versión 2.0: mismo funcionamiento de la 1.0 (clientes, vehículos, órdenes de trabajo con estados,
servicios y repuestos con total automático, aviso por WhatsApp) **más** todas las mejoras propuestas,
con un diseño nuevo (barra lateral, paleta grafito + naranja, modo claro/oscuro).

## Novedades
| Mejora | Dónde está |
|---|---|
| Gráficos de ingresos por mes y servicios más pedidos | Tablero |
| Modo claro / oscuro (se recuerda) | Botón en la barra lateral |
| Colores por estado (insignias y columnas) | Tablero, Órdenes, Historial, Mantenimiento, Inventario |
| Órdenes: servicio sin cantidad, repuesto buscable del inventario, filtro por estado, columna Días (rojo > 7), menú de clic derecho | Órdenes |
| Orden en **PDF** con código **QR** de resumen | Órdenes > "Generar PDF" |
| Exportar a **Excel** (5 hojas) | Ajustes y respaldos (administrador) |
| **Recordatorios de mantenimiento** por km y/o meses + aviso por WhatsApp | Mantenimiento |
| **Historial del vehículo** (visitas, km, gasto acumulado) y alerta si baja el kilometraje | Historial / Vehículos |
| **Inventario de repuestos**: saldo de kardex, SKU opcional y alertas por mínimo | Inventario / Órdenes / Tablero |
| **Compras**: alta de proveedores, borradores, confirmación y anulación | Compras |
| **Kardex**: movimientos por repuesto con filtros de fecha y tipo | Kardex |
| **Diagnóstico con IA** (API de Claude) | Diagnóstico con IA |
| **Inicio de sesión con roles** (administrador / mecánico), contraseñas con BCrypt | Pantalla de acceso, Usuarios |
| **Copia de seguridad** manual, restauración y copia automática diaria (últimas 7) | Ajustes y respaldos |
| Pantalla de bienvenida (splash) e icono propio | Al abrir la app |
| Pruebas automatizadas (JUnit + Mockito) | `src/test` |

## Primer uso
Al abrir por primera vez no hay usuarios: la app pide **crear la cuenta de administrador** (no existen
contraseñas por defecto). Después el administrador puede crear mecánicos en *Usuarios*.

- **Administrador:** todo.
- **Mecánico:** no ve Clientes ni Usuarios, ni las copias de seguridad / exportación a Excel.

## Abrir en IntelliJ IDEA
1. File > Open y elige esta carpeta (la que contiene `pom.xml`).
2. Espera a que Maven descargue las dependencias.
3. Settings > Build, Execution, Deployment > Compiler > Annotation Processors > **Enable annotation processing**.
4. Verifica que el plugin Lombok esté instalado.

## Ejecutar
```
mvn clean javafx:run
```
Los datos se guardan en `C:\Users\TU_USUARIO\.taller-pro\tallerpro-v2.db`
(la versión 2.0 usa un archivo nuevo; los datos de pruebas de la 1.0 no se tocan ni se importan).

## Pruebas automatizadas
```
mvn test
```

## Generar la aplicación de escritorio (Windows)
- **Imagen con Java incluido** — `build-app-image.bat` genera `target\app\TallerPro\TallerPro.exe`. Para usarla en otra PC, copia toda la carpeta `TallerPro`.
- **Con ventana de consola** — `build-app-image-consola.bat` genera la variante de diagnóstico.
- **Instalador de Windows x64** — con JDK 17, Maven e Inno Setup 6, ejecuta `crear-instalador.bat` para generar `target\instalador\Setup.exe`.
- macOS / Linux: `./build-instalador.sh`

La versión que aparece en Ajustes y en el instalador se obtiene del `pom.xml`. Cierra TallerPro antes de actualizarlo o desinstalarlo. Para actualizar, ejecuta el nuevo `Setup.exe` sobre la instalación anterior. La base y las fotos se conservan en `%USERPROFILE%\.taller-pro`. Si el esquema requiere migración, TallerPro guarda una copia fechada de la base antes de aplicarla en el siguiente arranque.

## Asistente de IA
Es un chat: puedes preguntar lo que quieras (diagnósticos, soluciones, repuestos, costos, consejos) y hacer
preguntas de seguimiento. Responde con causas probables, cómo confirmarlas, soluciones, urgencia y prevención.

**Para usar la IA completa** necesitas una clave de API de Claude:
1. Entra a console.anthropic.com, crea una cuenta de API y agrega saldo (Billing).
2. En *API Keys* crea una clave (empieza con `sk-ant-`) y cópiala completa.
3. En la app: *Ajustes y respaldos > Asistente de IA* > pega la clave > **Guardar** > **Probar conexión**.

> Una suscripción de Claude.ai (Free, Pro...) **no** incluye clave de API: son cuentas distintas.
> El error `invalid x-api-key` significa que la clave está mal copiada, es de otra cuenta o fue eliminada.

**Sin clave o sin internet** la pantalla no se queda vacía: responde el **modo básico**, una base local de
problemas mecánicos frecuentes (consumo de combustible, frenos, sobrecalentamiento, no arranca, humo,
vibraciones, batería, aire acondicionado, etc.) con causas, revisiones y soluciones, y te explica por qué no
pudo usar la IA. El modo básico solo cubre temas de mecánica; las preguntas generales requieren la clave.

También se acepta la variable de entorno `ANTHROPIC_API_KEY`. El modelo se cambia en
`application.properties` (`taller.ia.modelo`). La IA es una orientación: confírmala con una inspección física.

## Lista de pruebas manuales (guion para la sustentación)
1. Primer uso: crear el administrador e iniciar sesión.
2. Clientes: registrar un cliente (DNI de 8 dígitos, teléfono de 9).
3. Vehículos: registrar un vehículo con su propietario y kilometraje.
4. Inventario: registrar un repuesto con mínimo 2. En Compras, crear un proveedor, añadir 5 unidades y confirmar. Verificar saldo 5 en Inventario y la entrada en Kardex.
5. Órdenes: crear una orden, guardar, agregar un servicio y un repuesto del inventario; verificar el total y que el stock baja.
6. Quitar el repuesto de la orden: el stock vuelve. Doble clic en una fila del detalle para editarla (el stock se ajusta); filtrar por estado; clic derecho en una orden para ver las transiciones y acciones disponibles. Anular una orden abierta con motivo y marcar solo las líneas de repuestos no instalados. Comprobar que la orden sigue visible y que solo esas unidades vuelven al stock. Una orden entregada requiere reapertura por un administrador con motivo.
7. Generar el PDF de la orden y escanear el QR.
8. Mantenimiento: programar "Cambio de aceite cada 5000 km / 6 meses" y comprobar el estado (Al día / Próximo / Vencido).
9. Vehículos: bajar el kilometraje de un vehículo → debe pedir confirmación.
10. Historial: elegir el vehículo y ver visitas, km y gasto.
11. Modo oscuro y cerrar sesión; entrar como mecánico (usuario creado en *Usuarios*) y comprobar que no ve Clientes.
12. Ajustes: crear copia de seguridad, exportar a Excel, abrir el archivo.
13. Cerrar y abrir la app: los datos siguen ahí.

## Migraciones de SQLite

Las migraciones se ejecutan antes de iniciar JPA y se registran con `PRAGMA user_version`.
`V1__esquema_inicial.sql` crea las tablas de una base nueva. Una base del TallerPro 2.0 anterior
se reconoce como versión 1. `V2__ciclo_y_numero_de_orden.sql` conserva los IDs y las líneas,
habilita la anulación con devolución selectiva y genera el número visible a partir del ID.
Si la base ya contiene datos, se crea primero una copia consistente `respaldos/antes-de-migrar-*.db`.
`V3__busquedas_y_mecanico.sql` añade los índices de búsqueda y el vínculo opcional del mecánico
con Usuarios. Si el texto histórico coincide con un solo nombre o login, se asigna esa cuenta.
Si hay varias coincidencias o ninguna, se conserva el texto histórico.
`V4__compras_y_kardex.sql` registra el saldo previo de cada repuesto como un movimiento inicial.
El costo promedio de apertura es cero porque el esquema anterior solo guardaba el precio de venta.
SQLite actualiza el stock al insertar un movimiento y protege el saldo y el historial de cambios directos.
`V5__trazabilidad_de_ordenes.sql` vincula los movimientos nuevos a su partida
y registra una copia de los datos del vehículo y del cliente. Los datos de órdenes anteriores se
copian según el propietario conocido al migrar. Los costos y las referencias de partidas que no
existían quedan desconocidos. Un repuesto histórico sin catálogo se clasifica como externo.
La migración V6 convierte los importes de venta a céntimos enteros con redondeo de mitad hacia
arriba y añade pagos, descuentos y autorizaciones de entrega con saldo. El historial anterior
no contiene cobros registrados: TallerPro no genera abonos históricos al migrar.
Si falla una migración, SQLite revierte los cambios y el arranque muestra el error.

## Compras e inventario

- Crea el proveedor desde su selector y registra una partida por repuesto. Guardar un borrador
  conserva el documento. Confirmarlo registra las entradas y recalcula el promedio ponderado.
- Activa «Aplicar precios sugeridos al confirmar» si deseas actualizar los precios de venta.
- Anular una compra confirmada registra salidas al costo promedio vigente. La anulación se rechaza
  si alguna partida deja saldo negativo. El kardex mantiene las entradas originales.
- Los repuestos nuevos comienzan con stock cero. Los ajustes manuales son exclusivos de ADMIN y
  requieren una cantidad con signo y un motivo.

## Repuestos y trazabilidad en órdenes

- Agregar repuestos de inventario descuenta al instante. Editar la cantidad registra la diferencia
  y retirar la partida devuelve sus unidades, conservándola para el historial de movimientos.
- Un repuesto externo tiene su propio costo y precio y queda fuera del kardex. Si falta saldo,
  el formulario ofrece preparar una partida externa antes de guardar.
- Los precios y costos de las piezas quedan registrados al agregarlas. La ganancia estimada solo
  está disponible para el administrador y requiere que todas las partidas tengan un costo conocido.
  En órdenes anuladas no se muestra ganancia.
- **Trazabilidad** muestra los movimientos de la orden y permite filtrar por la partida seleccionada.
  Los movimientos anteriores a esta función siguen disponibles como históricos sin partida.

## Cobros y cierre de caja

Los totales se calculan a partir de las partidas. El descuento se registra como importe fijo en
soles. Guarda un cambio de descuento antes de abrir Pagos. Si una modificación deja el subtotal\npor debajo del descuento, reduce primero el descuento.

**Pagos** acepta adelantos y abonos con distintos métodos. Cuando los abonos superan lo exigible,
la ficha muestra el excedente como saldo a favor. Una anulación exige motivo y queda registrada.
El cierre de caja atribuye cada cobro y cada anulación a la fecha en que ocurrió y muestra el neto
por método. Para entregar con saldo pendiente, un administrador debe autorizar y registrar el motivo.

## Fotos de la orden y respaldo

La pestaña **Fotos** permite seleccionar o arrastrar varias imágenes de ingreso, diagnóstico,
repuestos o entrega. Admite JPG, PNG y WebP por contenido, hasta 20 MB y 25 megapíxeles.
Las imágenes se recomprimen como JPEG. Se guardan una vista de hasta 1600 px y una miniatura
de hasta 320 px con nombres generados bajo fotos/OT-XXXX dentro del directorio de datos.

El autor y el administrador pueden anotar o retirar fotos de órdenes abiertas. Puedes exportar
las imágenes de una orden por fase a una carpeta o ZIP, o marcar **Incluir fotos en PDF**.
El aviso por WhatsApp funciona como antes.

La migración V7 introduce orden_foto. En Ajustes, los nuevos respaldos ZIP guardan la base,
las fotos y las miniaturas. Se conserva la restauración de archivos .db anteriores cuando ese
archivo contiene toda la información necesaria. Antes de restaurar se valida la copia y se
guarda un respaldo de los datos actuales. Si faltan archivos del estado anterior, se conservan
los que quedan en una carpeta de recuperación.

## Búsqueda y selección

Usa Ctrl+F para enfocar la búsqueda de la pantalla. En los selectores, escribe para buscar,
usa las flechas para recorrer las coincidencias, Enter para elegir y Esc para cerrar.
El botón + Nuevo abre el formulario de alta cuando está disponible en ese selector.
Las placas nuevas se guardan en mayúsculas y sin espacios ni guiones. Las placas históricas
conservan su presentación y se pueden encontrar con o sin separadores. El código visible de
un repuesto es su SKU. Si no tiene SKU, se muestra # seguido de su ID de catálogo.

## Estructura
```
src/main/java/com/ejemplo/tallerpro/
  Launcher, JavaFxApplication (splash, login, ventana principal), TallerProApplication
  model/       Cliente, Vehiculo, OrdenTrabajo, DetalleOrden, Repuesto, ProgramaMantenimiento, Usuario, enums
  repository/  repositorios Spring Data JPA
  service/     reglas de negocio + PdfService, ReporteService (Excel), RespaldoService, IaService
  controller/  un controlador por vista
  util/        Ui, Tema, Preferencias, Rutas, Restauracion, Navegador, SesionActual
src/main/resources/  application.properties, css/app.css, img/icono.png, view/*.fxml
src/test/java/       pruebas JUnit + Mockito
icono/               TallerPro.ico y PNG del icono
```

## Acceso y operación diaria

- ADMIN gestiona respaldos, cobros, compras, ajustes de inventario, usuarios y la clave de IA.
  MECÁNICO consulta y modifica órdenes asignadas a su cuenta, registra partidas y fotos y
  consulta la trazabilidad sin costos. Las cifras de cobranza se reservan a ADMIN.
  Las órdenes nuevas que recibe un mecánico se le
  asignan automáticamente. ADMIN asigna las órdenes anteriores sin responsable asociado.
- Las nuevas contraseñas requieren 10 caracteres como mínimo. Cinco intentos consecutivos
  fallidos bloquean la cuenta durante 15 minutos, incluso si se reinicia TallerPro. Las
  contraseñas existentes siguen funcionando. Las operaciones registran el usuario en los
  datos de negocio o en actividad.log, según la operación.
- El tablero muestra las órdenes ingresadas hoy por estado. Con Nueva orden, selecciona
  la placa y escribe el motivo. El kilometraje y la asignación están en Más detalles.
- La restauración comprueba la integridad, la versión y los archivos asociados antes del
  reemplazo. Conserva una copia anterior fechada y mantiene la restauración pendiente si falla.


## Inspección del vehículo

Busca al cliente en **Inspección** y elige una placa. Si tiene un solo vehículo disponible,
la ficha se abre directamente. En Órdenes, **Inspección** abre la misma ficha con la visita
seleccionada. Usa las capas y vistas para escoger una zona y registrar un hallazgo. Puedes
acercar con Ctrl + rueda y arrastrar el fondo para desplazar la vista ampliada.

Los hallazgos se quedan en el vehículo. Al salir de Recibido se protege la descripción de
los preexistentes de esa visita. Al convertir un hallazgo en partida se aplica el inventario
normal. Si se retira la partida o se anula la visita, el hallazgo vuelve a Abierto. La
entrega marca como reparadas las partidas vinculadas y conserva los datos de inspección
que aparecen al reimprimir el PDF de esa visita.

La migración V9 conserva los registros y asigna inicialmente carrocería Sedán a los
vehículos existentes. Las fotos de inspección siguen siendo fotos de la orden y viajan
en el mismo ZIP de respaldo. El recorrido del hallazgo se conserva en la base respaldada.


## Autocompletar un cliente por DNI

En **Clientes → Nuevo**, escribe los ocho dígitos. Si el DNI ya está en TallerPro se
abre su ficha y se muestran sus vehículos. Para completar nombres y apellidos de un DNI
nuevo, activa **Consulta externa de DNI** en **Ajustes y respaldos** con un token de
[Decolecta](https://decolecta.gitbook.io/docs/servicios/integrations-2). Al activarla,
TallerPro envía al proveedor únicamente el DNI consultado. El teléfono, el correo y el
guardado de la ficha se mantienen en el formulario del taller. Sin token o si falla la
consulta, puedes registrar al cliente manualmente.
