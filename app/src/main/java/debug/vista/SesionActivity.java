package debug.vista;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Supplier;

import debug.repositorio.ProductoRepository;
import debug.servicios.AuthService;

abstract class SesionActivity extends AppCompatActivity {
    private static final ExecutorService operaciones = Executors.newSingleThreadExecutor();

    protected ProductoRepository repositorio;
    protected AuthService auth;
    private String usuarioId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        auth = new AuthService();
        usuarioId = auth.obtenerUsuarioId();
        if (usuarioId == null) {
            abrirPantallaInicial(this, LoginActivity.class);
            return;
        }

        repositorio = new ProductoRepository(this);
        mostrarPantalla(this, obtenerLayout());
        configurarPantalla(savedInstanceState);
    }

    protected abstract int obtenerLayout();

    protected abstract void configurarPantalla(Bundle estado);

    @Override
    protected void onResume() {
        super.onResume();
        if (repositorio == null) {
            return;
        }
        if (!mismaSesion()) {
            abrirPantallaInicial(this, LoginActivity.class);
            return;
        }
        alReanudar();
    }

    protected void alReanudar() {
    }

    protected <T> void ejecutar(Supplier<T> operacion, Consumer<T> alTerminar) {
        operaciones.execute(() -> {
            T resultado;
            try {
                resultado = operacion.get();
            } catch (RuntimeException e) {
                Log.w("Chungungo", "Error en una operación de inventario", e);
                runOnUiThread(() -> {
                    if (pantallaActiva()) {
                        mensaje("No se pudo completar la operación. Inténtalo de nuevo.");
                        alTerminar.accept(null);
                    }
                });
                return;
            }

            runOnUiThread(() -> {
                if (pantallaActiva()) {
                    alTerminar.accept(resultado);
                }
            });
        });
    }

    private boolean pantallaActiva() {
        return !isFinishing() && !isDestroyed() && mismaSesion();
    }

    private boolean mismaSesion() {
        return usuarioId != null && usuarioId.equals(auth.obtenerUsuarioId());
    }

    protected void mensaje(String texto) {
        Toast.makeText(this, texto, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        if (repositorio != null) {
            operaciones.execute(repositorio::cerrar);
        }
        super.onDestroy();
    }

    static void abrirPantallaInicial(AppCompatActivity activity, Class<?> destino) {
        Intent intent = new Intent(activity, destino);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        activity.startActivity(intent);
    }

    static void mostrarPantalla(AppCompatActivity activity, int layout) {
        activity.setContentView(layout);
        View contenido = activity.findViewById(android.R.id.content);
        ViewCompat.setOnApplyWindowInsetsListener(contenido, (vista, insets) -> {
            Insets margen = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout() | WindowInsetsCompat.Type.ime());
            vista.setPadding(margen.left, margen.top, margen.right, margen.bottom);
            return WindowInsetsCompat.CONSUMED;
        });
        ViewCompat.requestApplyInsets(contenido);
    }
}
