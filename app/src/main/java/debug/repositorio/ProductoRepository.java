package debug.repositorio;

import android.content.Context;

import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;

import debug.modelo.Producto;
import debug.vistaModelo.ProductoDAO;
import debug.servicios.SyncWorker;

public class ProductoRepository {

    private final Context context;
    private final ProductoDAO productoDAO;
    private final String usuarioId;

    public ProductoRepository(Context context) {

        this.context = context.getApplicationContext();

        FirebaseUser usuario =
                FirebaseAuth.getInstance().getCurrentUser();

        if (usuario == null) {
            throw new IllegalStateException("No hay una sesión activa");
        }

        usuarioId = usuario.getUid();

        productoDAO = new ProductoDAO(this.context, usuarioId);

        sincronizar();
    }

    // Crear producto
    public boolean agregarProducto(Producto producto) {

        verificarSesion();

        boolean resultado = productoDAO.agregarProducto(producto);

        if (resultado) {
            sincronizar();
        }

        return resultado;
    }

    // Listar productos
    public ArrayList<Producto> listarProductos() {

        verificarSesion();

        return productoDAO.listarProductos();
    }

    // Actualizar producto
    public boolean actualizarProducto(Producto producto) {

        verificarSesion();

        boolean resultado = productoDAO.actualizarProducto(producto);

        if (resultado) {
            sincronizar();
        }

        return resultado;
    }

    // Eliminar producto
    public boolean eliminarProducto(int id) {

        verificarSesion();

        boolean resultado = productoDAO.eliminarProducto(id);

        if (resultado) {
            sincronizar();
        }

        return resultado;
    }

    // Programar sincronización con Firebase
    public void sincronizar() {

        verificarSesion();

        Constraints restricciones = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        Data datos = new Data.Builder()
                .putString("usuarioId", usuarioId)
                .build();

        OneTimeWorkRequest solicitud =
                new OneTimeWorkRequest.Builder(SyncWorker.class)
                        .setConstraints(restricciones)
                        .setInputData(datos)
                        .build();

        WorkManager.getInstance(context).enqueueUniqueWork(
                "sincronizar_productos_" + usuarioId,
                ExistingWorkPolicy.APPEND_OR_REPLACE,
                solicitud
        );
    }

    private void verificarSesion() {

        FirebaseUser usuario =
                FirebaseAuth.getInstance().getCurrentUser();

        if (usuario == null ||
                !usuarioId.equals(usuario.getUid())) {

            throw new IllegalStateException("La sesión ha cambiado");
        }
    }

    public void cerrar() {
        productoDAO.cerrar();
    }
}