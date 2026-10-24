package joseblanco.usuariosapi.vistas;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.ArrayList;
import java.util.List;

import joseblanco.usuariosapi.R;
import joseblanco.usuariosapi.controladores.ClienteApi;
import joseblanco.usuariosapi.controladores.SegundoPlano;
import joseblanco.usuariosapi.controladores.ServicioUsuarios;
import joseblanco.usuariosapi.controladores.Sesion;
import joseblanco.usuariosapi.datos.Usuario;

public class PantallaListado extends AppCompatActivity {

    private final List<Usuario> usuarios = new ArrayList<>();
    private ArrayAdapter<String> items;
    private ServicioUsuarios servicio;
    private ProgressBar progreso;
    private TextView txtVacio;
    private Button botonAgregar;
    private Button botonCerrarSesion;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.pantalla_listado);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        servicio = new ServicioUsuarios(new ClienteApi(getString(R.string.url_api)));
        progreso = findViewById(R.id.progreso);
        txtVacio = findViewById(R.id.txtVacio);
        botonAgregar = findViewById(R.id.botonAgregar);
        botonCerrarSesion = findViewById(R.id.botonCerrarSesion);
        ListView lista = findViewById(R.id.listaUsuarios);
        ((TextView) findViewById(R.id.txtSesion)).setText(getString(R.string.sesion_de, Sesion.getEmail()));

        items = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        lista.setAdapter(items);
        lista.setOnItemClickListener((padre, vista, posicion, id) ->
                startActivity(new Intent(this, PantallaUsuario.class)
                        .putExtra(PantallaUsuario.EXTRA_ID, usuarios.get(posicion).getId())));
        botonAgregar.setOnClickListener(v -> startActivity(new Intent(this, PantallaUsuario.class)));
        botonCerrarSesion.setOnClickListener(v -> {
            Sesion.cerrar();
            Avisos.irAlLogin(this);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!Sesion.estaIniciada()) {
            Avisos.irAlLogin(this);
            return;
        }
        cargarUsuarios();
    }

    private void cargarUsuarios() {
        mostrarProgreso(true);
        SegundoPlano.ejecutar(
                () -> servicio.listar(Sesion.getToken()),
                (lista, error) -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }
                    mostrarProgreso(false);
                    if (error != null) {
                        Avisos.error(this, error);
                        return;
                    }
                    usuarios.clear();
                    usuarios.addAll(lista);
                    items.clear();
                    for (Usuario usuario : usuarios) {
                        items.add(getString(R.string.formato_item, usuario.getName(), usuario.getEmail(),
                                usuario.getRole(), usuario.getStatus()));
                    }
                    txtVacio.setVisibility(usuarios.isEmpty() ? View.VISIBLE : View.GONE);
                });
    }

    private void mostrarProgreso(boolean mostrar) {
        progreso.setVisibility(mostrar ? View.VISIBLE : View.GONE);
        botonAgregar.setEnabled(!mostrar);
        botonCerrarSesion.setEnabled(!mostrar);
    }
}
