package com.restaurante.archivos;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/** Fixture HTTP bloqueante: evita depender del selector AF_UNIX de Windows/JDK 25. */
final class ServidorArchivosPrueba implements AutoCloseable {
    private final ServerSocket server;
    private final Map<String, byte[]> disco;
    ServidorArchivosPrueba(Map<String, byte[]> disco) throws IOException {
        this.disco=disco;
        server=new ServerSocket(0, 50, InetAddress.getByName("127.0.0.1"));
        Thread.ofVirtual().start(() -> {
            while (!server.isClosed()) {
                try { Socket cliente=server.accept(); Thread.ofVirtual().start(() -> responder(cliente)); }
                catch(IOException ex) { if (!server.isClosed()) throw new UncheckedIOException(ex); }
            }
        });
    }
    int getPort() { return server.getLocalPort(); }
    private void responder(Socket socket) {
        try (socket) {
            socket.setSoTimeout(5000);
            var reader=new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.US_ASCII));
            String[] request=reader.readLine().split(" ");
            String range=null, line;
            while ((line=reader.readLine()) != null && !line.isEmpty()) if (line.toLowerCase().startsWith("range:")) range=line.substring(6).trim();
            byte[] bytes=disco.get(request[1].substring("/media/".length()));
            int estado=200, desde=0, hasta=bytes == null ? -1 : bytes.length-1;
            String contentRange="";
            if (bytes == null) { bytes=new byte[0]; estado=404; }
            else if (range != null) {
                String[] partes=range.substring(6).split("-", -1);
                desde=partes[0].isEmpty() ? Math.max(0, bytes.length-Integer.parseInt(partes[1])) : Integer.parseInt(partes[0]);
                hasta=partes[1].isEmpty() || partes[0].isEmpty() ? bytes.length-1 : Math.min(hasta,Integer.parseInt(partes[1]));
                if (desde >= bytes.length || hasta < desde) { estado=416; contentRange="Content-Range: bytes */"+bytes.length+"\r\n"; }
                else { estado=206; contentRange="Content-Range: bytes "+desde+"-"+hasta+"/"+bytes.length+"\r\n"; }
            }
            int cantidad=estado == 200 || estado == 206 ? hasta-desde+1 : 0;
            var out=socket.getOutputStream();
            out.write(("HTTP/1.1 "+estado+" Test\r\nConnection: close\r\nContent-Length: "+cantidad+"\r\n"+contentRange+"\r\n").getBytes(StandardCharsets.US_ASCII));
            if (!"HEAD".equals(request[0]) && cantidad > 0) out.write(bytes,desde,cantidad);
            out.flush();
        } catch(IOException ex) { throw new UncheckedIOException(ex); }
    }
    @Override public void close() throws IOException { server.close(); }
}
