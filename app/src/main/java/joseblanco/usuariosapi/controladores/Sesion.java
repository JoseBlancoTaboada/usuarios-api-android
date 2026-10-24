package joseblanco.usuariosapi.controladores;

public final class Sesion {

    private static String token;
    private static String email;

    private Sesion() {
    }

    public static void iniciar(String nuevoToken, String nuevoEmail) {
        token = nuevoToken;
        email = nuevoEmail;
    }

    public static void cerrar() {
        token = null;
        email = null;
    }

    public static boolean estaIniciada() {
        return token != null;
    }

    public static String getToken() {
        return token;
    }

    public static String getEmail() {
        return email;
    }
}
