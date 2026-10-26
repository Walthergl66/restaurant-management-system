package com.restaurante.archivos.application;

import com.restaurante.archivos.TipoArchivo;
import com.restaurante.shared.domain.exception.BusinessRuleException;
import org.apache.tika.Tika;
import org.springframework.stereotype.Component;
import javax.imageio.ImageIO;
import java.io.IOException;
import java.nio.file.*;
import java.util.Map;

@Component
public class ValidadorArchivo {
    public static final long MAX_IMAGEN=10L*1024*1024, MAX_VIDEO=100L*1024*1024;
    private static final Map<String,String> EXTENSIONES=Map.of(
            "image/jpeg","jpg", "image/png","png", "image/webp","webp",
            "video/mp4","mp4", "application/mp4","mp4", "video/webm","webm");
    public record Validado(TipoArchivo tipo, String mime, String extension, long tamano) {}

    public Validado validar(Path archivo) throws IOException {
        long tamano=Files.size(archivo);
        if (tamano == 0 || tamano > MAX_VIDEO) throw new BusinessRuleException("El archivo está vacío o supera 100 MB");
        String mime=new Tika().detect(archivo);
        String extension=EXTENSIONES.get(mime);
        if (extension == null) throw new BusinessRuleException("Formato no permitido. Usa JPG, PNG, WebP, MP4 o WebM");
        TipoArchivo tipo=mime.startsWith("image/") ? TipoArchivo.IMAGEN : TipoArchivo.VIDEO;
        if (tipo == TipoArchivo.IMAGEN) {
            if (tamano > MAX_IMAGEN) throw new BusinessRuleException("La imagen supera 10 MB");
            if (!"webp".equals(extension)) comprobarImagen(archivo);
        }
        return new Validado(tipo, "application/mp4".equals(mime) ? "video/mp4" : mime, extension, tamano);
    }
    private void comprobarImagen(Path archivo) throws IOException {
        try (var input=ImageIO.createImageInputStream(archivo.toFile())) {
            var lectores=ImageIO.getImageReaders(input);
            if (!lectores.hasNext()) throw new BusinessRuleException("La imagen no se puede leer");
            var lector=lectores.next();
            try {
                lector.setInput(input);
                long pixeles=(long)lector.getWidth(0)*lector.getHeight(0);
                if (pixeles <= 0 || pixeles > 25_000_000) throw new BusinessRuleException("La imagen supera 25 megapíxeles");
                if (lector.read(0) == null) throw new BusinessRuleException("La imagen está dañada");
            } catch (javax.imageio.IIOException ex) { throw new BusinessRuleException("La imagen está dañada"); }
            finally { lector.dispose(); }
        }
    }
}
