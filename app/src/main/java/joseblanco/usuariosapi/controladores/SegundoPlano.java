package joseblanco.usuariosapi.controladores;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class SegundoPlano {

    public interface AlTerminar<T> {
        void resultado(T valor, Exception error);
    }

    private static final ExecutorService EJECUTOR = Executors.newSingleThreadExecutor();
    private static final Handler HILO_PRINCIPAL = new Handler(Looper.getMainLooper());

    private SegundoPlano() {
    }

    public static <T> void ejecutar(Callable<T> trabajo, AlTerminar<T> alTerminar) {
        EJECUTOR.execute(() -> {
            T valor = null;
            Exception error = null;
            try {
                valor = trabajo.call();
            } catch (Exception e) {
                error = e;
            }
            T valorFinal = valor;
            Exception errorFinal = error;
            HILO_PRINCIPAL.post(() -> alTerminar.resultado(valorFinal, errorFinal));
        });
    }
}
