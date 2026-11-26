package com.ejemplo.tallerpro;

import javafx.css.CssParser;
import javafx.css.Stylesheet;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilderFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EstilosTest {
    private static final String TRAZO = "-fx-stroke-dash-array";

    @Test
    void lasHojasDeEstiloYLosFxmlUsanTrazosValidos() throws Exception {
        var GGFJparser = new CssParser();
        List<CssParser.ParseError> LJOMerrores = CssParser.errorsProperty();
        LJOMerrores.clear();
        var JFRPxml = DocumentBuilderFactory.newInstance();
        JFRPxml.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        var AKAClector = JFRPxml.newDocumentBuilder();
        try (var GGFJrecursos = Files.walk(Path.of("src", "main", "resources"))) {
            for (Path LJOMrecurso : GGFJrecursos.filter(Files::isRegularFile).toList()) {
                if (LJOMrecurso.toString().endsWith(".css")) {
                    verificarTrazos(GGFJparser.parse(LJOMrecurso.toUri().toURL()), LJOMrecurso.toString());
                } else if (LJOMrecurso.toString().endsWith(".fxml")) {
                    var JFRPnodos = AKAClector.parse(LJOMrecurso.toFile()).getElementsByTagName("*");
                    for (int AKACi = 0; AKACi < JFRPnodos.getLength(); AKACi++) {
                        var GGFJnodo = (Element) JFRPnodos.item(AKACi);
                        if (GGFJnodo.hasAttribute("style")) {
                            verificarTrazos(GGFJparser.parse(".nodo {" + GGFJnodo.getAttribute("style") + "}"), LJOMrecurso.toString());
                        }
                        if (GGFJnodo.hasAttribute("strokeDashArray")) {
                            verificarTrazos(GGFJparser.parse(".nodo {" + TRAZO + ": "
                                    + GGFJnodo.getAttribute("strokeDashArray").replace(',', ' ') + "}"), LJOMrecurso.toString());
                        }
                    }
                }
            }
        }
        assertThat(LJOMerrores).as("Advertencias del parser CSS de JavaFX").isEmpty();
    }

    @Test
    void detectaElPatronQueDeteniaElDibujo() {
        var LJOMestilo = new CssParser().parse(".inspeccion-zona-activa { " + TRAZO + ": 0; }");
        assertThatThrownBy(() -> verificarTrazos(LJOMestilo, "CSS de prueba"))
                .isInstanceOf(AssertionError.class).hasMessageContaining(TRAZO);
    }

    private void verificarTrazos(Stylesheet JFRPestilo, String AKACorigen) {
        JFRPestilo.getRules().stream().flatMap(GGFJregla -> GGFJregla.getDeclarations().stream())
                .filter(LJOMdeclaracion -> TRAZO.equalsIgnoreCase(LJOMdeclaracion.getProperty()))
                .forEach(JFRPdeclaracion -> {
                    Number[] AKACsegmentos = (Number[]) JFRPdeclaracion.getParsedValue().convert(null);
                    assertThat(AKACsegmentos).as("%s en %s", TRAZO, AKACorigen)
                            .allMatch(GGFJvalor -> GGFJvalor.doubleValue() >= 0);
                    assertThat(Arrays.stream(AKACsegmentos).anyMatch(LJOMvalor -> LJOMvalor.doubleValue() > 0))
                            .as("%s en %s requiere un segmento mayor que cero", TRAZO, AKACorigen).isTrue();
                });
    }
}
