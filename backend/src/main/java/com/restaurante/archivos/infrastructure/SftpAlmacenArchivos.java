package com.restaurante.archivos.infrastructure;

import com.restaurante.shared.domain.exception.AlmacenamientoNoDisponible;
import com.restaurante.archivos.application.AlmacenArchivos;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.sftp.SFTPClient;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.nio.file.Path;

@Component
public class SftpAlmacenArchivos implements AlmacenArchivos {
    private final ArchivoProperties config;
    public SftpAlmacenArchivos(ArchivoProperties config) { this.config=config; }
    private SSHClient conectar() throws IOException {
        config.comprobar();
        SSHClient ssh=new SSHClient();
        try {
            ssh.setConnectTimeout(Math.toIntExact(config.getTimeout().toMillis()));
            ssh.setTimeout(Math.toIntExact(config.getTimeout().toMillis()));
            ssh.loadKnownHosts(Path.of(config.getKnownHosts()).toFile());
            ssh.connect(config.getHost(), config.getPuerto());
            ssh.authPublickey(config.getUsuario(), config.getClavePrivada());
            return ssh;
        } catch (IOException | RuntimeException ex) { ssh.close(); throw ex; }
    }
    private String ruta(String clave) {
        if (!clave.matches("(imagenes|videos)/[a-f0-9-]+\\.(jpg|png|webp|mp4|webm)")) throw new IllegalArgumentException("Clave inválida");
        return config.getDirectorio().replaceAll("/+$", "") + "/" + clave;
    }
    @Override public void guardar(Path archivo, String clave) {
        String destino=ruta(clave), temporal=destino+".part";
        try (SSHClient ssh=conectar(); SFTPClient sftp=ssh.newSFTPClient()) {
            try {
                sftp.put(archivo.toString(), temporal);
                sftp.chmod(temporal, 0640);
                sftp.rename(temporal, destino);
            } catch (IOException ex) {
                try { sftp.rm(temporal); } catch (IOException ignored) { }
                throw ex;
            }
        } catch (IOException ex) { throw new AlmacenamientoNoDisponible(ex); }
    }
    @Override public void eliminar(String clave) {
        try (SSHClient ssh=conectar(); SFTPClient sftp=ssh.newSFTPClient()) {
            String ruta=ruta(clave);
            if (sftp.statExistence(ruta) != null) sftp.rm(ruta);
        } catch (IOException ex) { throw new AlmacenamientoNoDisponible(ex); }
    }
}
