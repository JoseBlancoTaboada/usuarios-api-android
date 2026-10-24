package joseblanco.usuariosapi.vistas;

import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import joseblanco.usuariosapi.R;
import joseblanco.usuariosapi.controladores.ClienteApi;
import joseblanco.usuariosapi.controladores.SegundoPlano;
import joseblanco.usuariosapi.controladores.ServicioUsuarios;
import joseblanco.usuariosapi.controladores.Sesion;
import joseblanco.usuariosapi.datos.Usuario;

public class PantallaUsuario extends AppCompatActivity {

    public static final String EXTRA_ID = "id_usuario";

    private static final int LARGO_MINIMO_NOMBRE = 3;
    private static final int LARGO_MINIMO_CLAVE = 8;

    private ServicioUsuarios servicio;
    private EditText campoNombre;
    private EditText campoEmail;
    private EditText campoClave;
    private Spinner spinnerRol;
    private Spinner spinnerEstado;
    private Button botonGuardar;
    private Button botonEliminar;
    private Button botonCancelar;
    private ProgressBar progreso;
    private ArrayAdapter<CharSequence> adaptadorRol;
    private ArrayAdapter<CharSequence> adaptadorEstado;
    private String idUsuario;
    private Usuario cargado;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.pantalla_usuario);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        servicio = new ServicioUsuarios(new ClienteApi(getString(R.string.url_api)));
        campoNombre = findViewById(R.id.campoNombre);
        campoEmail = findViewById(R.id.campoEmail);
        campoClave = findViewById(R.id.campoClave);
        spinnerRol = findViewById(R.id.spinnerRol);
        spinnerEstado = findViewById(R.id.spinnerEstado);
        botonGuardar = findViewById(R.id.botonGuardar);
        botonEliminar = findViewById(R.id.botonEliminar);
        botonCancelar = findViewById(R.id.botonCancelar);
        progreso = findViewById(R.id.progreso);

        adaptadorRol = ArrayAdapter.createFromResource(this, R.array.roles, android.R.layout.simple_spinner_item);
        adaptadorRol.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRol.setAdapter(adaptadorRol);
        spinnerRol.setSelection(adaptadorRol.getPosition("MEMBER"));
        adaptadorEstado = ArrayAdapter.createFromResource(this, R.array.estados, android.R.layout.simple_spinner_item);
        adaptadorEstado.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerEstado.setAdapter(adaptadorEstado);

        idUsuario = getIntent().getStringExtra(EXTRA_ID);
        TextView titulo = findViewById(R.id.etiTitulo);
        if (idUsuario == null) {
            titulo.setText(R.string.titulo_nuevo);
        } else {
            titulo.setText(R.string.titulo_editar);
            campoClave.setHint(R.string.pista_clave_editar);
            findViewById(R.id.filaEstado).setVisibility(View.VISIBLE);
            botonEliminar.setVisibility(View.VISIBLE);
            cargarUsuario();
        }

        botonGuardar.setOnClickListener(v -> guardar());
        botonEliminar.setOnClickListener(v -> confirmarEliminacion());
        botonCancelar.setOnClickListener(v -> finish());
    }

    private void cargarUsuario() {
        mostrarProgreso(true);
        SegundoPlano.ejecutar(
                () -> servicio.obtener(Sesion.getToken(), idUsuario),
                (usuario, error) -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }
                    mostrarProgreso(false);
                    if (error != null) {
                        Avisos.error(this, error);
                        finish();
                        return;
                    }
                    cargado = usuario;
                    campoNombre.setText(usuario.getName());
                    campoEmail.setText(usuario.getEmail());
                    spinnerRol.setSelection(Math.max(0, adaptadorRol.getPosition(usuario.getRole())));
                    spinnerEstado.setSelection(Math.max(0, adaptadorEstado.getPosition(usuario.getStatus())));
                });
    }

    private void guardar() {
        String nombre = campoNombre.getText().toString().trim();
        String email = campoEmail.getText().toString().trim();
        String clave = campoClave.getText().toString();
        boolean esNuevo = idUsuario == null;

        if (nombre.length() < LARGO_MINIMO_NOMBRE) {
            Avisos.mostrar(this, R.string.error_nombre_corto);
            return;
        }
        if (email.isEmpty()) {
            Avisos.mostrar(this, R.string.error_falta_email);
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Avisos.mostrar(this, R.string.error_email_invalido);
            return;
        }
        if (esNuevo && clave.isEmpty()) {
            Avisos.mostrar(this, R.string.error_falta_clave);
            return;
        }
        if (!clave.isEmpty() && clave.length() < LARGO_MINIMO_CLAVE) {
            Avisos.mostrar(this, R.string.error_clave_corta);
            return;
        }

        String rol = (String) spinnerRol.getSelectedItem();
        String estado = (String) spinnerEstado.getSelectedItem();
        String token = Sesion.getToken();

        mostrarProgreso(true);
        SegundoPlano.ejecutar(
                () -> esNuevo
                        ? servicio.crear(token, nombre, email, clave, rol)
                        : servicio.actualizar(token, idUsuario, nombre, email, clave, rol, estado),
                (usuario, error) -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }
                    mostrarProgreso(false);
                    if (error != null) {
                        Avisos.error(this, error);
                        return;
                    }
                    Avisos.mostrar(this, esNuevo ? R.string.aviso_creado : R.string.aviso_actualizado);
                    finish();
                });
    }

    private void confirmarEliminacion() {
        if (cargado == null) {
            return;
        }
        if (cargado.getEmail() != null && cargado.getEmail().equalsIgnoreCase(Sesion.getEmail())) {
            Avisos.mostrar(this, R.string.no_eliminarse);
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.boton_eliminar)
                .setMessage(getString(R.string.confirmar_eliminar_mensaje, cargado.getName()))
                .setPositiveButton(R.string.boton_eliminar, (dialogo, boton) -> eliminar())
                .setNegativeButton(R.string.boton_cancelar, null)
                .show();
    }

    private void eliminar() {
        String token = Sesion.getToken();
        mostrarProgreso(true);
        SegundoPlano.ejecutar(
                () -> {
                    servicio.eliminar(token, idUsuario);
                    return Boolean.TRUE;
                },
                (resultado, error) -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }
                    mostrarProgreso(false);
                    if (error != null) {
                        Avisos.error(this, error);
                        return;
                    }
                    Avisos.mostrar(this, R.string.aviso_eliminado);
                    finish();
                });
    }

    private void mostrarProgreso(boolean mostrar) {
        progreso.setVisibility(mostrar ? View.VISIBLE : View.GONE);
        botonGuardar.setEnabled(!mostrar);
        botonEliminar.setEnabled(!mostrar);
        botonCancelar.setEnabled(!mostrar);
    }
}
