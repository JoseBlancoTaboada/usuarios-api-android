package joseblanco.usuariosapi.controladores;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

class ServidorFalso implements AutoCloseable {

    static class Peticion {
        String metodo;
        String ruta;
        final Map<String, String> cabeceras = new HashMap<>();
        String cuerpo = "";
    }

    volatile int codigo = 200;
    volatile String respuesta = "";

    private final ServerSocket servidor;
    private final List<Peticion> peticiones = new CopyOnWriteArrayList<>();

    ServidorFalso() throws IOException {
        servidor = new ServerSocket(0, 5, InetAddress.getLoopbackAddress());
        Thread hilo = new Thread(this::atender);
        hilo.setDaemon(true);
        hilo.start();
    }

    String url() {
        return "http://127.0.0.1:" + servidor.getLocalPort();
    }

    Peticion ultima() {
        return peticiones.get(peticiones.size() - 1);
    }

    int cantidadDePeticiones() {
        return peticiones.size();
    }

    @Override
    public void close() throws IOException {
        servidor.close();
    }

    private void atender() {
        while (!servidor.isClosed()) {
            try (Socket cliente = servidor.accept()) {
                InputStream entrada = cliente.getInputStream();
                String[] lineas = leerCabecera(entrada).split("\r\n");
                Peticion peticion = new Peticion();
                String[] primera = lineas[0].split(" ");
                peticion.metodo = primera[0];
                peticion.ruta = primera[1];
                int largo = 0;
                for (int i = 1; i < lineas.length; i++) {
                    int dosPuntos = lineas[i].indexOf(':');
                    if (dosPuntos < 0) {
                        continue;
                    }
                    String clave = lineas[i].substring(0, dosPuntos).trim().toLowerCase(Locale.ROOT);
                    String valor = lineas[i].substring(dosPuntos + 1).trim();
                    peticion.cabeceras.put(clave, valor);
                    if (clave.equals("content-length")) {
                        largo = Integer.parseInt(valor);
                    }
                }
                byte[] cuerpo = new byte[largo];
                int leidos = 0;
                while (leidos < largo) {
                    int n = entrada.read(cuerpo, leidos, largo - leidos);
                    if (n == -1) {
                        break;
                    }
                    leidos += n;
                }
                peticion.cuerpo = new String(cuerpo, 0, leidos, StandardCharsets.UTF_8);
                peticiones.add(peticion);

                byte[] bytes = respuesta.getBytes(StandardCharsets.UTF_8);
                String encabezado = "HTTP/1.1 " + codigo + " Respuesta\r\n"
                        + "Content-Type: application/json; charset=UTF-8\r\n"
                        + "Content-Length: " + bytes.length + "\r\n"
                        + "Connection: close\r\n\r\n";
                OutputStream salida = cliente.getOutputStream();
                salida.write(encabezado.getBytes(StandardCharsets.US_ASCII));
                salida.write(bytes);
                salida.flush();
            } catch (IOException e) {
                // el servidor se cierra al terminar la prueba
            }
        }
    }

    private static String leerCabecera(InputStream entrada) throws IOException {
        StringBuilder cabecera = new StringBuilder();
        int b;
        while ((b = entrada.read()) != -1) {
            cabecera.append((char) b);
            int n = cabecera.length();
            if (n >= 4 && cabecera.substring(n - 4).equals("\r\n\r\n")) {
                break;
            }
        }
        return cabecera.toString();
    }
}
