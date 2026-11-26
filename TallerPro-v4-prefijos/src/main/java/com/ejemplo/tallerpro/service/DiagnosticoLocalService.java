package com.ejemplo.tallerpro.service;

import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Diagnostico basico SIN internet ni clave de API: compara el sintoma con una base de problemas
 * mecanicos frecuentes y sugiere causas, revisiones y soluciones. Es la red de seguridad cuando
 * la IA no esta disponible.
 */
@Service
public class DiagnosticoLocalService {

    private record Caso(String titulo, List<String> palabras, List<String> causas,
                        List<String> revisiones, List<String> soluciones, String urgencia) {
    }

    private record Puntaje(Caso caso, int puntos) {
    }

    private static final List<Caso> CASOS = List.of(
            new Caso("Consumo excesivo de combustible",
                    List.of("consum", "combustible", "gasolina", "diesel", "petroleo", "galon", "rendimiento",
                            "gasta mucho", "bencina", "no rinde", "dura menos"),
                    List.of("Filtro de aire sucio o tapado.",
                            "Bujías desgastadas o bobinas en mal estado (la combustión es incompleta).",
                            "Sensor de oxígeno (sonda lambda) o sensor MAF defectuoso: la mezcla se enriquece.",
                            "Inyectores sucios o que gotean.",
                            "Neumáticos con poca presión o alineación/balanceo mal hechos.",
                            "Frenos que arrastran, termostato abierto o filtro de combustible tapado.",
                            "Aceite incorrecto o con muchos kilómetros; estilo de manejo o exceso de carga."),
                    List.of("Conectar un escáner OBD2 y leer códigos de falla y datos en vivo (sondas, MAF, mezcla).",
                            "Revisar el filtro de aire y las bujías (estado y calibración).",
                            "Medir la presión de las llantas y comprobar que las ruedas giran libres.",
                            "Probar los inyectores (caudal y estanqueidad) y la presión de combustible.",
                            "Verificar la temperatura de trabajo del motor (termostato)."),
                    List.of("Cambiar filtro de aire, bujías y, si hace falta, filtro de combustible.",
                            "Limpieza de inyectores y del cuerpo de aceleración.",
                            "Reemplazar el sensor de oxígeno o el MAF si el escáner lo indica.",
                            "Inflar las llantas a la presión del fabricante y alinear/balancear.",
                            "Hacer el mantenimiento completo (aceite y filtros) según el kilometraje."),
                    "Media: no es peligroso, pero el gasto aumenta y puede dañar el catalizador si se ignora."),

            new Caso("Frenos con ruido, vibración o pedal blando",
                    List.of("freno", "frenar", "pedal", "chirr", "rechin", "pastilla", "disco", "frena"),
                    List.of("Pastillas gastadas (el testigo metálico roza el disco).",
                            "Discos deformados o con desgaste irregular (vibración al frenar).",
                            "Aire o poca cantidad de líquido de frenos; fuga en el circuito.",
                            "Bomba, mordaza (caliper) o manguera en mal estado."),
                    List.of("Medir el espesor de pastillas y discos.",
                            "Revisar el nivel y el color del líquido de frenos y buscar fugas.",
                            "Probar el pedal con el motor encendido: debe sentirse firme."),
                    List.of("Cambiar pastillas (y rectificar o cambiar discos si es necesario).",
                            "Purgar el sistema y renovar el líquido de frenos.",
                            "Reparar la fuga o la mordaza dañada."),
                    "Alta: los frenos son seguridad. Si el pedal está blando o hay mucho ruido, no conduzcas."),

            new Caso("El vehículo no enciende o tarda en arrancar",
                    List.of("no enciende", "no arranca", "no prende", "no prende", "arranque", "tarda en encender",
                            "click", "se apaga", "no da arranque"),
                    List.of("Batería descargada o con bornes sulfatados/flojos.",
                            "Motor de arranque o solenoide defectuoso.",
                            "Alternador que no carga.",
                            "Bomba de combustible, filtro tapado o sin combustible.",
                            "Bujías, bobinas o sensor del cigüeñal (CKP) en falla."),
                    List.of("Medir la batería (≈12.6 V en reposo) y el voltaje de carga con el motor en marcha (13.8–14.7 V).",
                            "Limpiar y apretar los bornes.",
                            "Escuchar si la bomba de combustible se activa al dar contacto.",
                            "Leer códigos de falla con el escáner."),
                    List.of("Cargar o reemplazar la batería; limpiar bornes.",
                            "Reparar o cambiar el motor de arranque o el alternador.",
                            "Cambiar el filtro o la bomba de combustible.",
                            "Reemplazar el sensor o las bujías defectuosas."),
                    "Media: te puede dejar varado."),

            new Caso("Sobrecalentamiento del motor",
                    List.of("recalienta", "sobrecalent", "temperatura", "hirviendo", "vapor", "refrigerante",
                            "calienta", "radiador", "ventilador"),
                    List.of("Falta de refrigerante por fuga (mangueras, radiador, bomba de agua).",
                            "Termostato trabado.",
                            "Electroventilador que no enciende.",
                            "Radiador obstruido o tapa del radiador defectuosa.",
                            "Junta de culata dañada (humo blanco, aceite lechoso)."),
                    List.of("Con el motor frío, revisar el nivel de refrigerante y buscar fugas.",
                            "Comprobar que el ventilador enciende al calentarse.",
                            "Probar la presión del sistema y verificar el termostato.",
                            "Test de gases de combustión en el refrigerante."),
                    List.of("Reparar la fuga y rellenar con refrigerante correcto.",
                            "Cambiar termostato, bomba de agua o ventilador según corresponda.",
                            "Limpiar o reemplazar el radiador.",
                            "Si hay junta de culata dañada, reparación de motor."),
                    "Alta: apaga el motor si la aguja sube; seguir conduciendo puede fundir el motor."),

            new Caso("Humo por el escape",
                    List.of("humo", "humea", "echa humo"),
                    List.of("Humo azul: el motor quema aceite (anillos, retenes de válvulas).",
                            "Humo blanco denso: refrigerante entrando a la cámara (junta de culata).",
                            "Humo negro: exceso de combustible (inyectores, filtro de aire, sensores)."),
                    List.of("Identificar el color y el momento (en frío o en caliente).",
                            "Revisar el nivel de aceite y de refrigerante.",
                            "Prueba de compresión y de fugas en cilindros."),
                    List.of("Azul: cambiar retenes o reparar el motor, y usar el aceite adecuado.",
                            "Blanco: reparar la junta de culata.",
                            "Negro: limpiar inyectores, cambiar filtro de aire y revisar sensores."),
                    "Media a alta según el color; el blanco con pérdida de refrigerante es urgente."),

            new Caso("Vibración del volante o de la carrocería",
                    List.of("vibra", "vibracion", "tiembla", "volante", "se sacude", "bamboleo", "shimmy"),
                    List.of("Ruedas desbalanceadas, deformes o con desgaste irregular.",
                            "Alineación incorrecta.",
                            "Rótulas, terminales o bujes de la suspensión con juego.",
                            "Discos de freno deformados (si vibra al frenar).",
                            "Soportes de motor rotos o falla de encendido (vibración en ralentí)."),
                    List.of("Balancear y revisar el estado de las llantas.",
                            "Revisar el juego en rótulas, terminales y bujes.",
                            "Comprobar si vibra a cierta velocidad, al frenar o en ralentí."),
                    List.of("Balanceo y alineación.",
                            "Cambiar las piezas de suspensión o dirección con juego.",
                            "Rectificar o cambiar discos; cambiar soportes de motor si están rotos."),
                    "Media: corrige pronto, desgasta las llantas y la suspensión."),

            new Caso("Luz de check engine / testigo del motor encendido",
                    List.of("check engine", "testigo", "luz del motor", "luz de motor", "luz amarilla", "luz de falla",
                            "luz del tablero"),
                    List.of("Falla de sensores (oxígeno, MAF, temperatura).",
                            "Fallas de encendido (bujías, bobinas).",
                            "Catalizador deteriorado o fuga en el sistema EVAP (tapa de combustible floja).",
                            "Problemas de mezcla o de la válvula EGR."),
                    List.of("Leer los códigos con un escáner OBD2 (DTC) y anotarlos.",
                            "Revisar la tapa del tanque y las conexiones de los sensores.",
                            "Ver si se acompaña de tirones, pérdida de potencia o consumo alto."),
                    List.of("Reparar según el código (sensor, bujías, bobinas, EGR, fuga EVAP).",
                            "Borrar los códigos y probar en ruta para confirmar."),
                    "Media; si parpadea, es alta: indica falla de encendido que puede dañar el catalizador."),

            new Caso("Pérdida de potencia, tirones o motor irregular",
                    List.of("potencia", "jalonea", "tironea", "tiron", "no acelera", "ratea", "falla al acelerar",
                            "se ahoga", "irregular", "inestable", "ralenti", "ralenti"),
                    List.of("Bujías o bobinas desgastadas.",
                            "Filtro de combustible o de aire tapado.",
                            "Inyectores sucios o bomba de combustible débil.",
                            "Sensor MAF/TPS defectuoso o fuga de vacío.",
                            "Catalizador obstruido."),
                    List.of("Leer códigos de falla.",
                            "Medir la presión de combustible.",
                            "Revisar bujías, bobinas y mangueras de vacío."),
                    List.of("Cambiar bujías y bobinas defectuosas.",
                            "Reemplazar filtros y limpiar inyectores.",
                            "Reparar las fugas de vacío o cambiar el sensor dañado."),
                    "Media."),

            new Caso("Ruidos o golpes en la suspensión",
                    List.of("suspension", "amortiguador", "golpeteo", "hoyo", "bache", "ruido al pasar", "traquetea",
                            "tronido", "crujido", "cruje"),
                    List.of("Amortiguadores vencidos o con fuga.",
                            "Bujes, rótulas, terminales o barra estabilizadora con juego.",
                            "Resortes o bases de amortiguador dañados."),
                    List.of("Prueba de rebote y revisión visual de fugas.",
                            "Levantar el vehículo y buscar juego en rótulas y bujes."),
                    List.of("Cambiar amortiguadores (por pares) y las piezas con juego.",
                            "Alinear la dirección al terminar."),
                    "Media: afecta la estabilidad y el desgaste de llantas."),

            new Caso("Embrague o caja de cambios",
                    List.of("embrague", "clutch", "cambios", "caja", "patina", "se resbala", "no entra", "palanca"),
                    List.of("Disco o prensa de embrague desgastados (patina).",
                            "Falta de líquido o fuga en el circuito hidráulico.",
                            "Aceite de caja bajo o degradado; sincronizadores desgastados.",
                            "Cable de embrague desajustado."),
                    List.of("Probar si patina en marcha alta con carga.",
                            "Revisar el nivel de líquido y el recorrido del pedal.",
                            "Revisar el aceite de la caja."),
                    List.of("Cambiar el kit de embrague.",
                            "Reparar la fuga y purgar el circuito.",
                            "Cambiar el aceite de caja o reparar los sincronizadores."),
                    "Media."),

            new Caso("Aire acondicionado que no enfría",
                    List.of("aire acondicionado", "no enfria", "a/c", "aire no enfria", "climatizador", "sale caliente"),
                    List.of("Falta de gas refrigerante por fuga.",
                            "Compresor o su embrague eléctrico dañado.",
                            "Condensador sucio o ventilador que no funciona.",
                            "Filtro de cabina tapado o fusible/relé quemado."),
                    List.of("Medir las presiones del sistema.",
                            "Comprobar que el compresor se acopla.",
                            "Revisar fusibles, relés y el filtro de cabina."),
                    List.of("Detectar y reparar la fuga, y recargar gas.",
                            "Cambiar el compresor o el condensador dañado.",
                            "Cambiar el filtro de cabina."),
                    "Baja."),

            new Caso("Problemas de aceite (fuga, consumo o presión)",
                    List.of("aceite", "presion de aceite", "fuga de aceite", "gotea", "mancha"),
                    List.of("Retenes o empaques con fuga (tapa de válvulas, cárter, cigüeñal).",
                            "Consumo por desgaste interno del motor.",
                            "Bomba o sensor de presión de aceite defectuoso, o nivel muy bajo."),
                    List.of("Revisar el nivel con la varilla y buscar el origen de la fuga.",
                            "Medir la presión real de aceite."),
                    List.of("Cambiar el empaque o el retén que gotea.",
                            "Completar o cambiar el aceite con la viscosidad correcta.",
                            "Reemplazar la bomba o el sensor si corresponde."),
                    "Alta si se enciende la luz roja de aceite: apaga el motor de inmediato."),

            new Caso("Batería o sistema eléctrico",
                    List.of("bateria", "alternador", "se descarga", "luces tenues", "se queda sin bateria",
                            "fusible", "electrico"),
                    List.of("Batería vieja que ya no retiene carga.",
                            "Alternador o regulador que no carga.",
                            "Consumo parásito (algo que queda encendido).",
                            "Bornes flojos o sulfatados, cables en mal estado."),
                    List.of("Medir voltaje en reposo y con el motor en marcha.",
                            "Medir el consumo parásito con el vehículo apagado."),
                    List.of("Cambiar la batería o el alternador.",
                            "Limpiar y ajustar los bornes; reparar cableado.",
                            "Ubicar y corregir el consumo parásito."),
                    "Media."),

            new Caso("Dirección dura, floja o con ruido",
                    List.of("direccion", "volante duro", "volante pesado", "volante flojo", "timon", "hidraulica"),
                    List.of("Falta de líquido de dirección o fuga.",
                            "Bomba de dirección o cremallera con desgaste.",
                            "Terminales y rótulas con juego; mala alineación."),
                    List.of("Revisar el nivel de líquido y las correas.",
                            "Revisar el juego en terminales y rótulas."),
                    List.of("Rellenar o cambiar el líquido y reparar la fuga.",
                            "Cambiar bomba, cremallera o terminales.",
                            "Alinear al terminar."),
                    "Alta si el volante se pone muy duro: afecta el control del vehículo."),

            new Caso("Llantas con desgaste irregular o alineación",
                    List.of("llanta", "neumatico", "desgaste", "alineacion", "balanceo", "jala hacia", "se va hacia"),
                    List.of("Alineación o balanceo incorrectos.",
                            "Presión inadecuada.",
                            "Suspensión o dirección con piezas desgastadas."),
                    List.of("Revisar el patrón de desgaste en las llantas.",
                            "Medir la presión y el ángulo de alineación."),
                    List.of("Alinear y balancear; rotar las llantas.",
                            "Cambiar las piezas de suspensión desgastadas y las llantas muy gastadas."),
                    "Media."),

            new Caso("Ruido fuerte en el escape",
                    List.of("escape", "silenciador", "ruido fuerte", "estruendo", "catalizador"),
                    List.of("Silenciador o tubo con orificios o rotos.",
                            "Fuga en el múltiple o en las juntas.",
                            "Catalizador deteriorado."),
                    List.of("Inspeccionar el escape con el motor encendido, buscando fugas."),
                    List.of("Soldar o cambiar la sección dañada.",
                            "Cambiar las juntas o el catalizador."),
                    "Baja a media: hay riesgo de que entren gases al habitáculo.")
    );

    /** Devuelve un diagnostico en texto a partir del sintoma descrito. */
    public String diagnosticar(String vehiculo, String sintomas) {
        String AKACtexto = normalizar(sintomas);
        List<Puntaje> GGFJpuntajes = new ArrayList<>();
        for (Caso LJOMcaso : CASOS) {
            int JFRPpuntos = 0;
            for (String AKACpalabra : LJOMcaso.palabras()) {
                if (AKACtexto.contains(normalizar(AKACpalabra))) {
                    JFRPpuntos++;
                }
            }
            if (JFRPpuntos > 0) {
                GGFJpuntajes.add(new Puntaje(LJOMcaso, JFRPpuntos));
            }
        }
        GGFJpuntajes.sort(Comparator.comparingInt(Puntaje::puntos).reversed());

        StringBuilder GGFJsb = new StringBuilder();
        if (vehiculo != null && !vehiculo.isBlank()) {
            GGFJsb.append("Vehículo: ").append(vehiculo).append("\n\n");
        }
        if (GGFJpuntajes.isEmpty()) {
            GGFJsb.append("No reconocí un problema concreto en la descripción. Orientación general:\n\n")
                    .append("1) Conecta un escáner OBD2 y lee los códigos de falla.\n")
                    .append("2) Revisa niveles (aceite, refrigerante, frenos), batería, llantas y fugas visibles.\n")
                    .append("3) Anota cuándo ocurre (en frío, en caliente, al frenar, al acelerar) y desde qué kilometraje.\n")
                    .append("4) Describe el síntoma con más detalle (ruido, olor, luces del tablero) y vuelve a consultar.\n");
            return GGFJsb.toString();
        }
        int LJOMmostrar = Math.min(2, GGFJpuntajes.size());
        for (int JFRPi = 0; JFRPi < LJOMmostrar; JFRPi++) {
            Caso AKACc = GGFJpuntajes.get(JFRPi).caso();
            GGFJsb.append(JFRPi == 0 ? "PROBLEMA MÁS PROBABLE: " : "TAMBIÉN PODRÍA SER: ").append(AKACc.titulo()).append("\n\n");
            GGFJsb.append("Posibles causas:\n");
            AKACc.causas().forEach(GGFJx -> GGFJsb.append("  • ").append(GGFJx).append("\n"));
            GGFJsb.append("\nCómo confirmarlo:\n");
            AKACc.revisiones().forEach(LJOMx -> GGFJsb.append("  • ").append(LJOMx).append("\n"));
            GGFJsb.append("\nSoluciones:\n");
            AKACc.soluciones().forEach(JFRPx -> GGFJsb.append("  • ").append(JFRPx).append("\n"));
            GGFJsb.append("\nUrgencia: ").append(AKACc.urgencia()).append("\n\n");
        }
        GGFJsb.append("Es una orientación general: confírmala con una inspección física del vehículo.");
        return GGFJsb.toString();
    }

    private static String normalizar(String AKACs) {
        if (AKACs == null) {
            return "";
        }
        return Normalizer.normalize(AKACs, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase();
    }
}
