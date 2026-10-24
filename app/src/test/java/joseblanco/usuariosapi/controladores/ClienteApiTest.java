package joseblanco.usuariosapi.controladores;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

public class ClienteApiTest {

    private ServidorFalso servidor;
    private ClienteApi cliente;

    @Before
    public void iniciar() throws IOException {
        servidor = new ServidorFalso();
        cliente = new ClienteApi(servidor.url());
    }

    @After
    public void detener() throws IOException {
        servidor.close();
    }

    @Test
    public void enviaMetodoRutaTokenYCuerpoJson() throws IOException {
        servidor.codigo = 201;
        servidor.respuesta = "{}";

        cliente.enviar("POST", "/api/users", "tok.123", "{\"a\":1}");

        ServidorFalso.Peticion peticion = servidor.ultima();
        assertEquals("POST", peticion.metodo);
        assertEquals("/api/users", peticion.ruta);
        assertEquals("Bearer tok.123", peticion.cabeceras.get("authorization"));
        assertTrue(peticion.cabeceras.get("content-type").startsWith("application/json"));
        assertEquals("{\"a\":1}", peticion.cuerpo);
    }

    @Test
    public void sinTokenNoEnviaLaCabeceraAuthorization() throws IOException {
        servidor.respuesta = "[]";

        cliente.enviar("GET", "/api/users", null, null);

        ServidorFalso.Peticion peticion = servidor.ultima();
        assertEquals("GET", peticion.metodo);
        assertNull(peticion.cabeceras.get("authorization"));
        assertEquals("", peticion.cuerpo);
    }

    @Test
    public void usaPutYDeleteSinProblemas() throws IOException {
        servidor.respuesta = "{}";
        cliente.enviar("PUT", "/api/users/abc", "t", "{\"name\":\"Ana\"}");
        assertEquals("PUT", servidor.ultima().metodo);

        servidor.codigo = 204;
        servidor.respuesta = "";
        cliente.enviar("DELETE", "/api/users/abc", "t", null);
        assertEquals("DELETE", servidor.ultima().metodo);
        assertEquals("/api/users/abc", servidor.ultima().ruta);
    }

    @Test
    public void devuelveCodigoYCuerpoDeUnErrorSinLanzarExcepcion() throws IOException {
        servidor.codigo = 404;
        servidor.respuesta = "{\"status\":404,\"message\":\"no existe\"}";

        RespuestaApi respuesta = cliente.enviar("GET", "/api/users/x", "t", null);

        assertEquals(404, respuesta.getCodigo());
        assertEquals("{\"status\":404,\"message\":\"no existe\"}", respuesta.getCuerpo());
        assertFalse(respuesta.esExitosa());
    }

    @Test
    public void unaRespuesta204TieneCuerpoVacioYEsExitosa() throws IOException {
        servidor.codigo = 204;
        servidor.respuesta = "";

        RespuestaApi respuesta = cliente.enviar("DELETE", "/api/users/x", "t", null);

        assertEquals(204, respuesta.getCodigo());
        assertEquals("", respuesta.getCuerpo());
        assertTrue(respuesta.esExitosa());
    }

    @Test
    public void decodificaElCuerpoComoUtf8() throws IOException {
        servidor.respuesta = "{\"name\":\"José Ñandú\"}";

        RespuestaApi respuesta = cliente.enviar("GET", "/api/users/x", "t", null);

        assertEquals("{\"name\":\"José Ñandú\"}", respuesta.getCuerpo());
    }

    @Test
    public void enviaElCuerpoEnUtf8() throws IOException {
        servidor.respuesta = "{}";

        cliente.enviar("POST", "/api/users", "t", "{\"name\":\"José Ñandú\"}");

        assertEquals("{\"name\":\"José Ñandú\"}", servidor.ultima().cuerpo);
    }

    @Test(expected = IOException.class)
    public void lanzaIOExceptionSiNoHayServidor() throws IOException {
        new ClienteApi("http://127.0.0.1:1", 500, 500).enviar("GET", "/api/users", null, null);
    }
}
