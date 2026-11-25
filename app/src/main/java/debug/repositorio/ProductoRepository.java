package debug.repositorio;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

import debug.modelo.Producto;
import debug.servicios.SyncWorker;
import debug.vistaModelo.ProductoDAO;

public class ProductoRepository {
    private final WorkManager workManager;
    private final ProductoDAO productoDAO;
    private final String usuarioId;

    public ProductoRepository(Context context) {
        FirebaseUser usuario = FirebaseAuth.getInstance().getCurrentUser();
        if (usuario == null) {
            throw new IllegalStateException("No hay una sesión activa");
        }

        usuarioId = usuario.getUid();
        Context aplicacion = context.getApplicationContext();
        productoDAO = new ProductoDAO(aplicacion, usuarioId);
        workManager = WorkManager.getInstance(aplicacion);
    }

    public boolean agregarProducto(Producto producto) {
        verificarSesion();
        boolean resultado = productoDAO.agregarProducto(producto);
        if (resultado) {
            programar(ExistingWorkPolicy.APPEND_OR_REPLACE);
        }
        return resultado;
    }

    public ArrayList<Producto> listarProductos() {
        verificarSesion();
        return productoDAO.listarProductos();
    }

    public Producto buscarProducto(int id) {
        verificarSesion();
        return productoDAO.buscarProducto(id);
    }

    public boolean actualizarProducto(Producto producto) {
        verificarSesion();
        boolean resultado = productoDAO.actualizarProducto(producto);
        if (resultado) {
            programar(ExistingWorkPolicy.APPEND_OR_REPLACE);
        }
        return resultado;
    }

    public boolean eliminarProducto(int id) {
        verificarSesion();
        boolean resultado = productoDAO.eliminarProducto(id);
        if (resultado) {
            programar(ExistingWorkPolicy.APPEND_OR_REPLACE);
        }
        return resultado;
    }

    public void sincronizar() {
        verificarSesion();
        programar(ExistingWorkPolicy.KEEP);
    }

    public LiveData<List<WorkInfo>> observarSincronizacion() {
        return workManager.getWorkInfosForUniqueWorkLiveData(nombreTrabajo());
    }

    private void programar(ExistingWorkPolicy politica) {
        Constraints restricciones = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();
        Data datos = new Data.Builder()
                .putString(SyncWorker.USUARIO_ID, usuarioId)
                .build();
        OneTimeWorkRequest solicitud = new OneTimeWorkRequest.Builder(SyncWorker.class)
                .setConstraints(restricciones)
                .setInputData(datos)
                .build();
        workManager.enqueueUniqueWork(nombreTrabajo(), politica, solicitud);
    }

    private String nombreTrabajo() {
        return "sincronizar_productos_" + usuarioId;
    }

    private void verificarSesion() {
        FirebaseUser usuario = FirebaseAuth.getInstance().getCurrentUser();
        if (usuario == null || !usuarioId.equals(usuario.getUid())) {
            throw new IllegalStateException("La sesión ha cambiado");
        }
    }

    public void cerrar() {
        productoDAO.cerrar();
    }
}
