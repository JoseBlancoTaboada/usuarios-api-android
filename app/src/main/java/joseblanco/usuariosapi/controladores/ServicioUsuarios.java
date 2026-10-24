package joseblanco.usuariosapi.controladores;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import joseblanco.usuariosapi.datos.ApiException;
import joseblanco.usuariosapi.datos.Usuario;

public class ServicioUsuarios {

    private static final String MENSAJE_RESPUESTA_INESPERADA = "El servidor respondió algo inesperado";
    private static final String RUTA_USUARIOS = "/api/users";
    private static final String RUTA_LOGIN = "/api/auth/login";

    private static final Gson GSON = new Gson();
    private static final Type LISTA_DE_USUARIOS = new TypeToken<List<Usuario>>() {
    }.getType();

    private final ClienteApi cliente;

    public ServicioUsuarios(ClienteApi cliente) {
        this.cliente = cliente;
    }

    public String iniciarSesion(String email, String password) throws ApiException {
        JsonObject cuerpo = new JsonObject();
        cuerpo.addProperty("email", email);
        cuerpo.addProperty("password", password);

        RespuestaApi respuesta = enviar("POST", RUTA_LOGIN, null, cuerpo);
        JsonObject json = leerObjeto(respuesta);
        JsonElement token = json.get("accessToken");
        if (token == null || !token.isJsonPrimitive() || token.getAsString().trim().isEmpty()) {
            throw new ApiException(respuesta.getCodigo(), MENSAJE_RESPUESTA_INESPERADA);
        }
        return token.getAsString();
    }

    public List<Usuario> listar(String token) throws ApiException {
        RespuestaApi respuesta = enviar("GET", RUTA_USUARIOS, token, null);
        verificar(respuesta);

        JsonElement elemento = parsear(respuesta.getCuerpo());
        if (elemento == null || !elemento.isJsonArray()) {
            throw new ApiException(respuesta.getCodigo(), MENSAJE_RESPUESTA_INESPERADA);
        }
        try {
            List<Usuario> usuarios = GSON.fromJson(elemento, LISTA_DE_USUARIOS);
            return usuarios == null ? new ArrayList<Usuario>() : usuarios;
        } catch (RuntimeException e) {
            throw new ApiException(respuesta.getCodigo(), MENSAJE_RESPUESTA_INESPERADA);
        }
    }

    public Usuario obtener(String token, String id) throws ApiException {
        RespuestaApi respuesta = enviar("GET", rutaDeUsuario(id), token, null);
        return leerUsuario(respuesta);
    }

    public Usuario crear(String token, String nombre, String email, String password, String rol)
            throws ApiException {
        JsonObject cuerpo = new JsonObject();
        cuerpo.addProperty("id", UUID.randomUUID().toString());
        cuerpo.addProperty("name", nombre);
        cuerpo.addProperty("email", email);
        cuerpo.addProperty("password", password);
        cuerpo.addProperty("role", rol);

        RespuestaApi respuesta = enviar("POST", RUTA_USUARIOS, token, cuerpo);
        return leerUsuario(respuesta);
    }

    public Usuario actualizar(String token, String id, String nombre, String email, String password,
            String rol, String estado) throws ApiException {
        JsonObject cuerpo = new JsonObject();
        cuerpo.addProperty("name", nombre);
        cuerpo.addProperty("email", email);
        cuerpo.addProperty("role", rol);
        cuerpo.addProperty("status", estado);
        if (password != null && !password.isEmpty()) {
            cuerpo.addProperty("password", password);
        }

        RespuestaApi respuesta = enviar("PUT", rutaDeUsuario(id), token, cuerpo);
        return leerUsuario(respuesta);
    }

    public void eliminar(String token, String id) throws ApiException {
        RespuestaApi respuesta = enviar("DELETE", rutaDeUsuario(id), token, null);
        verificar(respuesta);
    }

    private RespuestaApi enviar(String metodo, String ruta, String token, JsonObject cuerpo) throws ApiException {
        String cuerpoJson = cuerpo == null ? null : cuerpo.toString();
        try {
            return cliente.enviar(metodo, ruta, token, cuerpoJson);
        } catch (IOException e) {
            String detalle = e.getMessage();
            if (detalle == null || detalle.trim().isEmpty()) {
                detalle = e.getClass().getSimpleName();
            }
            throw new ApiException(0, "No se pudo conectar con el servidor: " + detalle);
        }
    }

    private static String rutaDeUsuario(String id) {
        return RUTA_USUARIOS + "/" + id;
    }

    private static void verificar(RespuestaApi respuesta) throws ApiException {
        if (!respuesta.esExitosa()) {
            throw new ApiException(respuesta.getCodigo(), respuesta.mensajeDeError());
        }
    }

    private static JsonElement parsear(String cuerpo) {
        if (cuerpo == null || cuerpo.trim().isEmpty()) {
            return null;
        }
        try {
            return JsonParser.parseString(cuerpo);
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static Usuario leerUsuario(RespuestaApi respuesta) throws ApiException {
        verificar(respuesta);

        JsonElement elemento = parsear(respuesta.getCuerpo());
        if (elemento == null || !elemento.isJsonObject()) {
            throw new ApiException(respuesta.getCodigo(), MENSAJE_RESPUESTA_INESPERADA);
        }
        try {
            Usuario usuario = GSON.fromJson(elemento, Usuario.class);
            if (usuario == null) {
                throw new ApiException(respuesta.getCodigo(), MENSAJE_RESPUESTA_INESPERADA);
            }
            return usuario;
        } catch (RuntimeException e) {
            throw new ApiException(respuesta.getCodigo(), MENSAJE_RESPUESTA_INESPERADA);
        }
    }

    private static JsonObject leerObjeto(RespuestaApi respuesta) throws ApiException {
        verificar(respuesta);

        JsonElement elemento = parsear(respuesta.getCuerpo());
        if (elemento == null || !elemento.isJsonObject()) {
            throw new ApiException(respuesta.getCodigo(), MENSAJE_RESPUESTA_INESPERADA);
        }
        return elemento.getAsJsonObject();
    }
}
