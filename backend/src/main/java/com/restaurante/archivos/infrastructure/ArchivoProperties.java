package com.restaurante.archivos.infrastructure;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import java.time.Duration;

@Component
@ConfigurationProperties("app.archivos")
public class ArchivoProperties {
    private boolean habilitado;
    private String host="192.168.100.77", usuario="archivosapp", clavePrivada="", knownHosts="";
    private int puerto=22;
    private String directorio="/srv/restaurante/media", lecturaUrl="http://192.168.100.77/media/";
    private Duration timeout=Duration.ofSeconds(15);
    public boolean isHabilitado() { return habilitado; }
    public void setHabilitado(boolean v) { habilitado=v; }
    public String getHost(){return host;} public void setHost(String v){host=v;}
    public String getUsuario(){return usuario;} public void setUsuario(String v){usuario=v;}
    public String getClavePrivada(){return clavePrivada;} public void setClavePrivada(String v){clavePrivada=v;}
    public String getKnownHosts(){return knownHosts;} public void setKnownHosts(String v){knownHosts=v;}
    public int getPuerto(){return puerto;} public void setPuerto(int v){puerto=v;}
    public String getDirectorio(){return directorio;} public void setDirectorio(String v){directorio=v;}
    public String getLecturaUrl(){return lecturaUrl;} public void setLecturaUrl(String v){lecturaUrl=v;}
    public Duration getTimeout(){return timeout;} public void setTimeout(Duration v){timeout=v;}
    public void comprobar() throws java.io.IOException {
        if (!habilitado || clavePrivada.isBlank() || knownHosts.isBlank()) throw new java.io.IOException("SFTP no configurado");
    }
}
