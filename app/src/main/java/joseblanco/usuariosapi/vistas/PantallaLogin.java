package joseblanco.usuariosapi.vistas;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import joseblanco.usuariosapi.R;
import joseblanco.usuariosapi.controladores.ClienteApi;
import joseblanco.usuariosapi.controladores.Sesion;
import joseblanco.usuariosapi.controladores.SegundoPlano;
import joseblanco.usuariosapi.controladores.ServicioUsuarios;

public class PantallaLogin extends AppCompatActivity {

    private EditText campoEmail;
    private EditText campoClave;
    private Button botonEntrar;
    private ProgressBar progreso;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.pantalla_login);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        campoEmail = findViewById(R.id.campoEmail);
        campoClave = findViewById(R.id.campoClave);
        botonEntrar = findViewById(R.id.botonEntrar);
        progreso = findViewById(R.id.progreso);

        botonEntrar.setOnClickListener(v -> entrar());
    }

    private void entrar() {
        String email = campoEmail.getText().toString().trim();
        String clave = campoClave.getText().toString();
        if (email.isEmpty()) {
            Avisos.mostrar(this, R.string.error_falta_email);
            return;
        }
        if (clave.isEmpty()) {
            Avisos.mostrar(this, R.string.error_falta_clave);
            return;
        }

        ServicioUsuarios servicio = new ServicioUsuarios(new ClienteApi(getString(R.string.url_api)));
        mostrarProgreso(true);
        SegundoPlano.ejecutar(
                () -> servicio.iniciarSesion(email, clave),
                (token, error) -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }
                    mostrarProgreso(false);
                    if (error != null) {
                        Avisos.error(this, error);
                        return;
                    }
                    Sesion.iniciar(token, email);
                    campoClave.setText("");
                    startActivity(new Intent(this, PantallaListado.class));
                    finish();
                });
    }

    private void mostrarProgreso(boolean mostrar) {
        progreso.setVisibility(mostrar ? View.VISIBLE : View.GONE);
        botonEntrar.setEnabled(!mostrar);
    }
}
