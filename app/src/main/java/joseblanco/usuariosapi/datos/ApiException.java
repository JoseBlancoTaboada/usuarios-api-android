package joseblanco.usuariosapi.datos;

public class ApiException extends Exception {

    private static final long serialVersionUID = 1L;

    private final int codigo;

    public ApiException(int codigo, String mensaje) {
        super(mensaje == null ? "Ocurrió un error al comunicarse con el servidor" : mensaje);
        this.codigo = codigo;
    }

    public int getCodigo() {
        return codigo;
    }
}
