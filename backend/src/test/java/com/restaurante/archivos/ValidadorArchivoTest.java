package com.restaurante.archivos;

import com.restaurante.archivos.application.ValidadorArchivo;
import com.restaurante.shared.domain.exception.BusinessRuleException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import static org.junit.jupiter.api.Assertions.*;

class ValidadorArchivoTest {
    @TempDir Path temp;
    private final ValidadorArchivo validador=new ValidadorArchivo();
    @Test void detectaContenidoAunqueLaExtensionMienta() throws Exception {
        Path archivo=temp.resolve("foto.exe");
        ImageIO.write(new BufferedImage(8, 8, BufferedImage.TYPE_INT_RGB), "png", archivo.toFile());
        var v=validador.validar(archivo);
        assertEquals(TipoArchivo.IMAGEN, v.tipo()); assertEquals("image/png", v.mime());
    }
    @Test void rechazaHtmlRenombradoComoFoto() throws Exception {
        Path archivo=Files.writeString(temp.resolve("foto.jpg"), "<html><script>alert(1)</script></html>");
        assertThrows(BusinessRuleException.class, () -> validador.validar(archivo));
    }
    @Test void rechazaArchivoVacio() throws Exception {
        Path archivo=Files.createFile(temp.resolve("vacio"));
        assertThrows(BusinessRuleException.class, () -> validador.validar(archivo));
    }
    @Test void rechazaJpegTruncado() throws Exception {
        Path archivo=Files.write(temp.resolve("truncado.jpg"), new byte[]{(byte)255,(byte)216,(byte)255,(byte)224,0,16,74,70,73,70,0});
        assertThrows(BusinessRuleException.class, () -> validador.validar(archivo));
    }
}
