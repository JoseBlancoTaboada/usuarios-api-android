package joseblanco.usuariosapi.controladores;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class RespuestaApiTest {

    @Test
    public void elMensajeDeErrorSeTomaDelCampoMessageDelJson() {
        RespuestaApi respuesta = new RespuestaApi(409, "{\"status\":409,\"message\":\"El correo ya existe.\"}");

        assertEquals("El correo ya existe.", respuesta.mensajeDeError());
    }

    @Test
    public void sinCuerpoUsaUnMensajePorDefectoSegunElCodigo() {
        int[] codigos = {400, 401, 403, 404, 409, 500, 503};
        for (int codigo : codigos) {
            String mensaje = new RespuestaApi(codigo, "").mensajeDeError();
            assertFalse("código " + codigo, mensaje == null || mensaje.trim().isEmpty());
        }
    }

    @Test
    public void unCuerpoQueNoEsJsonNoSeMuestraAlUsuario() {
        String mensaje = new RespuestaApi(500, "<html>boom</html>").mensajeDeError();

        assertFalse(mensaje.contains("<html>"));
        assertFalse(mensaje.trim().isEmpty());
    }

    @Test
    public void unJsonSinMessageUsaElMensajePorDefecto() {
        String mensaje = new RespuestaApi(404, "{\"status\":404}").mensajeDeError();

        assertFalse(mensaje.trim().isEmpty());
    }

    @Test
    public void soloLosCodigos2xxSonExitosos() {
        assertTrue(new RespuestaApi(200, "").esExitosa());
        assertTrue(new RespuestaApi(201, "").esExitosa());
        assertTrue(new RespuestaApi(204, "").esExitosa());
        assertFalse(new RespuestaApi(301, "").esExitosa());
        assertFalse(new RespuestaApi(401, "").esExitosa());
        assertFalse(new RespuestaApi(500, "").esExitosa());
    }
}
