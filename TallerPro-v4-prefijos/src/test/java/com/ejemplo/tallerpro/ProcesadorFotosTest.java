package com.ejemplo.tallerpro;

import com.ejemplo.tallerpro.exception.NegocioException;
import com.ejemplo.tallerpro.util.AlmacenFotos;
import com.ejemplo.tallerpro.util.ProcesadorFotos;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

import static org.assertj.core.api.Assertions.*;

class ProcesadorFotosTest {

    @TempDir
    Path carpeta;

    @Test
    void validaElContenidoYRecomprimeSinNombreOriginal() throws IOException {
        Path LJOMpng = carpeta.resolve("placa.jpg");
        ImageIO.write(new BufferedImage(1700, 650, BufferedImage.TYPE_INT_RGB), "png", LJOMpng.toFile());
        var AKACprocesada = ProcesadorFotos.procesar(LJOMpng);
        assertThat(ImageIO.read(new ByteArrayInputStream(AKACprocesada.foto())).getWidth()).isEqualTo(1600);
        assertThat(ImageIO.read(new ByteArrayInputStream(AKACprocesada.miniatura())).getWidth()).isEqualTo(320);
        assertThat(AKACprocesada.foto()[0] & 0xff).isEqualTo(0xff);
        assertThat(AKACprocesada.foto()[1] & 0xff).isEqualTo(0xd8);

        Path GGFJwebp = carpeta.resolve("imagen.dat");
        Files.write(GGFJwebp, Base64.getDecoder().decode("UklGRiIAAABXRUJQVlA4IBYAAAAwAQCdASoBAAEADsD+JaQAA3AAAAAA"));
        assertThat(ImageIO.read(new ByteArrayInputStream(ProcesadorFotos.procesar(GGFJwebp).foto())).getWidth()).isEqualTo(1);

        Path LJOMfalsa = Files.writeString(carpeta.resolve("imagen.png"), "esto no es una imagen");
        assertThatThrownBy(() -> ProcesadorFotos.procesar(LJOMfalsa)).isInstanceOf(NegocioException.class);
        Path JFRPmuyGrande = carpeta.resolve("grande.jpg");
        try (var AKACarchivo = new RandomAccessFile(JFRPmuyGrande.toFile(), "rw")) {
            AKACarchivo.setLength((long) ProcesadorFotos.MAX_BYTES + 1);
        }
        assertThatThrownBy(() -> ProcesadorFotos.procesar(JFRPmuyGrande)).hasMessageContaining("20 MB");
    }

    @Test
    void lasRutasNoPuedenSalirDeFotos() throws IOException {
        AlmacenFotos GGFJfotos = new AlmacenFotos(carpeta);
        String LJOMvalida = GGFJfotos.nuevaRuta("OT-0042");
        assertThat(GGFJfotos.resolver(LJOMvalida).startsWith(carpeta.toRealPath().resolve("fotos/OT-0042"))).isTrue();
        for (String JFRPruta : new String[]{"../fuera.jpg", "fotos/OT-0042/../fuera.jpg",
                "fotos/OT-0042/C:foto.jpg", "fotos\\OT-0042\\fuera.jpg", "/fotos/OT-0042/fuera.jpg"}) {
            assertThatThrownBy(() -> GGFJfotos.resolver(JFRPruta)).isInstanceOf(IOException.class);
        }
        assertThatThrownBy(() -> GGFJfotos.nuevaRuta("../OT-0042")).isInstanceOf(IOException.class);
    }
}
