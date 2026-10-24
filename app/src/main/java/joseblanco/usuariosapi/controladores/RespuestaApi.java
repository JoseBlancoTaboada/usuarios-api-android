package joseblanco.usuariosapi.controladores;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class RespuestaApi {

    private final int codigo;
    private final String cuerpo;

    public RespuestaApi(int codigo, String cuerpo) {
        this.codigo = codigo;
        this.cuerpo = cuerpo == null ? "" : cuerpo;
    }

    public int getCodigo() {
        return codigo;
    }

    public String getCuerpo() {
        return cuerpo;
    }

    public boolean esExitosa() {
        return codigo >= 200 && codigo <= 299;
    }

    public String mensajeDeError() {
        String mensajeDelServidor = leerMensajeDelServidor();
        if (mensajeDelServidor != null) {
            return mensajeDelServidor;
        }
        return mensajePorDefecto();
    }

    private String leerMensajeDelServidor() {
        if (cuerpo.trim().isEmpty()) {
            return null;
        }
        try {
            JsonElement elemento = JsonParser.parseString(cuerpo);
            if (elemento == null || !elemento.isJsonObject()) {
                return null;
            }
            JsonObject objeto = elemento.getAsJsonObject();
            JsonElement mensaje = objeto.get("message");
            if (mensaje == null || !mensaje.isJsonPrimitive()) {
                return null;
            }
            String texto = mensaje.getAsString().trim();
            return texto.isEmpty() ? null : texto;
        } catch (RuntimeException e) {
            return null;
        }
    }

    private String mensajePorDefecto() {
        switch (codigo) {
            case 400:
                return "Los datos enviados no son válidos.";
            case 401:
                return "Tu sesión no es válida o venció. Inicia sesión de nuevo.";
            case 403:
                return "No tienes permisos para hacer esta operación.";
            case 404:
                return "No se encontró lo que buscabas.";
            case 409:
                return "Ya existe un registro con esos datos.";
            case 500:
                return "El servidor tuvo un problema. Inténtalo más tarde.";
            default:
                return "Ocurrió un error al comunicarse con el servidor (código " + codigo + ").";
        }
    }
}
