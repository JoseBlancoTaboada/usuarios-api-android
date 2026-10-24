package joseblanco.usuariosapi.vistas;

import android.app.Activity;
import android.content.Intent;
import android.widget.Toast;

import androidx.annotation.StringRes;

import joseblanco.usuariosapi.R;
import joseblanco.usuariosapi.controladores.Sesion;
import joseblanco.usuariosapi.datos.ApiException;

final class Avisos {

    private Avisos() {
    }

    static void mostrar(Activity actividad, @StringRes int texto) {
        Toast.makeText(actividad, texto, Toast.LENGTH_LONG).show();
    }

    static void mostrar(Activity actividad, String texto) {
        Toast.makeText(actividad, texto, Toast.LENGTH_LONG).show();
    }

    static void error(Activity actividad, Exception error) {
        if (error instanceof ApiException) {
            ApiException api = (ApiException) error;
            if (api.getCodigo() == 401 && Sesion.estaIniciada()) {
                Sesion.cerrar();
                mostrar(actividad, R.string.sesion_vencida);
                irAlLogin(actividad);
                return;
            }
            mostrar(actividad, api.getMessage());
        } else {
            mostrar(actividad, R.string.error_inesperado);
        }
    }

    static void irAlLogin(Activity actividad) {
        actividad.startActivity(new Intent(actividad, PantallaLogin.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        actividad.finish();
    }
}
