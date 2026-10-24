package joseblanco.usuariosapi.controladores;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class ClienteApi {

    private static final int TIEMPO_CONEXION_POR_DEFECTO_MS = 30000;
    private static final int TIEMPO_LECTURA_POR_DEFECTO_MS = 120000;

    private final String urlBase;
    private final int tiempoConexionMs;
    private final int tiempoLecturaMs;

    public ClienteApi(String urlBase) {
        this(urlBase, TIEMPO_CONEXION_POR_DEFECTO_MS, TIEMPO_LECTURA_POR_DEFECTO_MS);
    }

    public ClienteApi(String urlBase, int tiempoConexionMs, int tiempoLecturaMs) {
        this.urlBase = urlBase;
        this.tiempoConexionMs = tiempoConexionMs;
        this.tiempoLecturaMs = tiempoLecturaMs;
    }

    public RespuestaApi enviar(String metodo, String ruta, String token, String cuerpoJson) throws IOException {
        String url = urlCompleta(ruta);
        HttpURLConnection conexion = (HttpURLConnection) new URL(url).openConnection();
        try {
            conexion.setRequestMethod(metodo);
            conexion.setConnectTimeout(tiempoConexionMs);
            conexion.setReadTimeout(tiempoLecturaMs);
            conexion.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            conexion.setRequestProperty("Accept", "application/json");
            if (token != null) {
                conexion.setRequestProperty("Authorization", "Bearer " + token);
            }
            if (cuerpoJson != null) {
                byte[] cuerpo = cuerpoJson.getBytes(StandardCharsets.UTF_8);
                conexion.setDoOutput(true);
                try (OutputStream salida = conexion.getOutputStream()) {
                    salida.write(cuerpo);
                    salida.flush();
                }
            }

            int codigo = conexion.getResponseCode();
            InputStream entrada = codigo >= 400 ? conexion.getErrorStream() : conexion.getInputStream();
            String cuerpoRespuesta = leer(entrada);
            return new RespuestaApi(codigo, cuerpoRespuesta);
        } finally {
            conexion.disconnect();
        }
    }

    private String urlCompleta(String ruta) {
        String base = urlBase == null ? "" : urlBase;
        String camino = ruta == null ? "" : ruta;
        if (base.endsWith("/") && camino.startsWith("/")) {
            return base + camino.substring(1);
        }
        if (!base.endsWith("/") && !camino.startsWith("/")) {
            return base + "/" + camino;
        }
        return base + camino;
    }

    private static String leer(InputStream entrada) throws IOException {
        if (entrada == null) {
            return "";
        }
        try (InputStream flujo = entrada) {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            byte[] bloque = new byte[4096];
            int leidos;
            while ((leidos = flujo.read(bloque)) != -1) {
                bytes.write(bloque, 0, leidos);
            }
            return new String(bytes.toByteArray(), StandardCharsets.UTF_8);
        }
    }
}
