package joseblanco.usuariosapi.controladores;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

import joseblanco.usuariosapi.datos.ApiException;
import joseblanco.usuariosapi.datos.Usuario;

public class ServicioUsuariosTest {

    private static final String USUARIO_JSON =
            "{\"id\":\"id-1\",\"name\":\"Ana Gómez\",\"email\":\"ana@ejemplo.com\",\"role\":\"MEMBER\",\"status\":\"ACTIVE\"}";

    private ServidorFalso servidor;
    private ServicioUsuarios servicio;

    @Before
    public void iniciar() throws IOException {
        servidor = new ServidorFalso();
        servicio = new ServicioUsuarios(new ClienteApi(servidor.url()));
    }

    @After
    public void detener() throws IOException {
        servidor.close();
    }

    private JsonObject cuerpoEnviado() {
        return JsonParser.parseString(servidor.ultima().cuerpo).getAsJsonObject();
    }

    @Test
    public void iniciarSesionDevuelveElTokenYEnviaEmailYPassword() throws ApiException {
        servidor.respuesta = "{\"accessToken\":\"abc.def.ghi\",\"tokenType\":\"Bearer\",\"expiresIn\":900}";

        String token = servicio.iniciarSesion("ana@ejemplo.com", "clave1234");

        assertEquals("abc.def.ghi", token);
        assertEquals("POST", servidor.ultima().metodo);
        assertEquals("/api/auth/login", servidor.ultima().ruta);
        assertEquals("ana@ejemplo.com", cuerpoEnviado().get("email").getAsString());
        assertEquals("clave1234", cuerpoEnviado().get("password").getAsString());
    }

    @Test
    public void iniciarSesionConCredencialesMalasLanzaApiExceptionConElMensajeDelServidor() {
        servidor.codigo = 401;
        servidor.respuesta = "{\"status\":401,\"message\":\"Correo o contraseña incorrectos.\"}";

        try {
            servicio.iniciarSesion("ana@ejemplo.com", "incorrecta1");
            fail("Debía lanzar ApiException");
        } catch (ApiException e) {
            assertEquals(401, e.getCodigo());
            assertEquals("Correo o contraseña incorrectos.", e.getMessage());
        }
    }

    @Test
    public void listarDevuelveLosUsuariosYEnviaElToken() throws ApiException {
        servidor.respuesta = "[" + USUARIO_JSON + ",{\"id\":\"id-2\",\"name\":\"Beto\",\"email\":\"beto@ejemplo.com\",\"role\":\"ADMIN\",\"status\":\"PENDING\"}]";

        List<Usuario> usuarios = servicio.listar("tk");

        assertEquals(2, usuarios.size());
        assertEquals("Beto", usuarios.get(1).getName());
        assertEquals("ADMIN", usuarios.get(1).getRole());
        assertEquals("PENDING", usuarios.get(1).getStatus());
        assertEquals("GET", servidor.ultima().metodo);
        assertEquals("/api/users", servidor.ultima().ruta);
        assertEquals("Bearer tk", servidor.ultima().cabeceras.get("authorization"));
    }

    @Test
    public void listarConUnaListaVaciaDevuelveUnaListaVacia() throws ApiException {
        servidor.respuesta = "[]";

        assertTrue(servicio.listar("tk").isEmpty());
    }

    @Test
    public void obtenerPideElUsuarioPorSuIdEnLaRuta() throws ApiException {
        servidor.respuesta = USUARIO_JSON;

        Usuario usuario = servicio.obtener("tk", "id-1");

        assertEquals("GET", servidor.ultima().metodo);
        assertEquals("/api/users/id-1", servidor.ultima().ruta);
        assertEquals("id-1", usuario.getId());
        assertEquals("Ana Gómez", usuario.getName());
        assertEquals("ana@ejemplo.com", usuario.getEmail());
    }

    @Test
    public void obtenerUnIdInexistenteLanzaApiException404() {
        servidor.codigo = 404;
        servidor.respuesta = "{\"status\":404,\"message\":\"The user was not found.\"}";

        try {
            servicio.obtener("tk", "no-existe");
            fail("Debía lanzar ApiException");
        } catch (ApiException e) {
            assertEquals(404, e.getCodigo());
            assertEquals("The user was not found.", e.getMessage());
        }
    }

    @Test
    public void crearEnviaUnIdUuidYTodosLosDatos() throws ApiException {
        servidor.codigo = 201;
        servidor.respuesta = USUARIO_JSON;

        Usuario creado = servicio.crear("tk", "Ana Gómez", "ana@ejemplo.com", "clave1234", "MEMBER");

        assertEquals("POST", servidor.ultima().metodo);
        assertEquals("/api/users", servidor.ultima().ruta);
        JsonObject cuerpo = cuerpoEnviado();
        assertTrue(cuerpo.get("id").getAsString()
                .matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"));
        assertEquals("Ana Gómez", cuerpo.get("name").getAsString());
        assertEquals("ana@ejemplo.com", cuerpo.get("email").getAsString());
        assertEquals("clave1234", cuerpo.get("password").getAsString());
        assertEquals("MEMBER", cuerpo.get("role").getAsString());
        assertEquals("id-1", creado.getId());
    }

    @Test
    public void cadaCrearGeneraUnIdDistinto() throws ApiException {
        servidor.codigo = 201;
        servidor.respuesta = USUARIO_JSON;

        servicio.crear("tk", "Ana", "a@ejemplo.com", "clave1234", "MEMBER");
        String primero = cuerpoEnviado().get("id").getAsString();
        servicio.crear("tk", "Beto", "b@ejemplo.com", "clave1234", "MEMBER");
        String segundo = cuerpoEnviado().get("id").getAsString();

        assertFalse(primero.equals(segundo));
    }

    @Test
    public void crearConUnCorreoRepetidoLanzaApiException409() {
        servidor.codigo = 409;
        servidor.respuesta = "{\"status\":409,\"message\":\"A user with that email already exists.\"}";

        try {
            servicio.crear("tk", "Ana", "ana@ejemplo.com", "clave1234", "MEMBER");
            fail("Debía lanzar ApiException");
        } catch (ApiException e) {
            assertEquals(409, e.getCodigo());
            assertEquals("A user with that email already exists.", e.getMessage());
        }
    }

    @Test
    public void actualizarSinClaveNoEnviaElCampoPassword() throws ApiException {
        servidor.respuesta = USUARIO_JSON;

        servicio.actualizar("tk", "id-1", "Ana Gómez", "ana@ejemplo.com", "", "MEMBER", "ACTIVE");

        assertEquals("PUT", servidor.ultima().metodo);
        assertEquals("/api/users/id-1", servidor.ultima().ruta);
        JsonObject cuerpo = cuerpoEnviado();
        assertFalse(cuerpo.has("password"));
        assertEquals("Ana Gómez", cuerpo.get("name").getAsString());
        assertEquals("ana@ejemplo.com", cuerpo.get("email").getAsString());
        assertEquals("MEMBER", cuerpo.get("role").getAsString());
        assertEquals("ACTIVE", cuerpo.get("status").getAsString());
        assertFalse(cuerpo.has("id"));
    }

    @Test
    public void actualizarConClaveNulaTampocoLaEnvia() throws ApiException {
        servidor.respuesta = USUARIO_JSON;

        servicio.actualizar("tk", "id-1", "Ana", "ana@ejemplo.com", null, "MEMBER", "ACTIVE");

        assertFalse(cuerpoEnviado().has("password"));
    }

    @Test
    public void actualizarConClaveLaEnvia() throws ApiException {
        servidor.respuesta = USUARIO_JSON;

        servicio.actualizar("tk", "id-1", "Ana", "ana@ejemplo.com", "NuevaClave1", "MEMBER", "ACTIVE");

        assertEquals("NuevaClave1", cuerpoEnviado().get("password").getAsString());
    }

    @Test
    public void eliminarUsaDeleteYAceptaUnaRespuesta204() throws ApiException {
        servidor.codigo = 204;
        servidor.respuesta = "";

        servicio.eliminar("tk", "id-1");

        assertEquals("DELETE", servidor.ultima().metodo);
        assertEquals("/api/users/id-1", servidor.ultima().ruta);
        assertEquals("Bearer tk", servidor.ultima().cabeceras.get("authorization"));
    }

    @Test
    public void eliminarUnIdInexistenteLanzaApiException404() {
        servidor.codigo = 404;
        servidor.respuesta = "{\"status\":404,\"message\":\"The user was not found.\"}";

        try {
            servicio.eliminar("tk", "no-existe");
            fail("Debía lanzar ApiException");
        } catch (ApiException e) {
            assertEquals(404, e.getCodigo());
        }
    }

    @Test
    public void unTokenVencidoLanzaApiException401() {
        servidor.codigo = 401;
        servidor.respuesta = "{\"status\":401,\"message\":\"Autenticacion requerida o token invalido.\"}";

        try {
            servicio.listar("vencido");
            fail("Debía lanzar ApiException");
        } catch (ApiException e) {
            assertEquals(401, e.getCodigo());
        }
    }

    @Test
    public void sinConexionLanzaApiExceptionConCodigoCero() {
        ServicioUsuarios sinServidor = new ServicioUsuarios(new ClienteApi("http://127.0.0.1:1", 500, 500));

        try {
            sinServidor.listar("tk");
            fail("Debía lanzar ApiException");
        } catch (ApiException e) {
            assertEquals(0, e.getCodigo());
            assertTrue(e.getMessage().toLowerCase().contains("conectar"));
        }
    }

    @Test
    public void unCuerpoQueNoEsJsonLanzaApiExceptionEnVezDeFallarConOtraExcepcion() {
        servidor.respuesta = "<html>no soy json</html>";

        try {
            servicio.listar("tk");
            fail("Debía lanzar ApiException");
        } catch (ApiException e) {
            assertEquals(200, e.getCodigo());
            assertFalse(e.getMessage().contains("<html>"));
        }
    }
}
