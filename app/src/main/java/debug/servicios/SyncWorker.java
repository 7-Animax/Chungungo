package debug.servicios;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import debug.modelo.Producto;
import debug.vistaModelo.ProductoDAO;

public class SyncWorker extends Worker {
    private static final String TAG = "SyncWorker";
    public static final String USUARIO_ID = "usuarioId";

    public SyncWorker(@NonNull Context context, @NonNull WorkerParameters parametros) {
        super(context, parametros);
    }

    @NonNull
    @Override
    public Result doWork() {
        String usuarioId = getInputData().getString(USUARIO_ID);
        if (!sesionValida(usuarioId)) {
            return Result.success();
        }

        ProductoDAO dao = new ProductoDAO(getApplicationContext(), usuarioId);
        ProductoService servicio = new ProductoService();
        try {
            // Las escrituras de un intento anterior pueden seguir activas en el SDK.
            Tasks.await(servicio.esperarEscriturasPendientes(), 30, TimeUnit.SECONDS);
            for (ProductoDAO.Cambio cambio : dao.obtenerPendientes()) {
                if (!sesionValida(usuarioId)) {
                    return Result.success();
                }
                Task<Void> operacion = cambio.eliminado
                        ? servicio.eliminarProducto(cambio.producto)
                        : servicio.guardarProducto(cambio.producto);
                Tasks.await(operacion, 30, TimeUnit.SECONDS);
                if (!sesionValida(usuarioId)) {
                    return Result.success();
                }
                dao.confirmarCambio(cambio);
            }

            QuerySnapshot resultado = Tasks.await(
                    servicio.listarProductos(usuarioId), 30, TimeUnit.SECONDS);
            if (!sesionValida(usuarioId)) {
                return Result.success();
            }
            if (resultado.getMetadata().hasPendingWrites()) {
                return Result.retry();
            }

            ArrayList<Producto> productos = new ArrayList<>();
            for (DocumentSnapshot documento : resultado.getDocuments()) {
                // Las versiones anteriores dejaban documentos con borrado lógico.
                if (Boolean.TRUE.equals(documento.getBoolean("eliminado"))) {
                    continue;
                }
                Producto producto = documento.toObject(Producto.class);
                if (producto != null) {
                    producto.setFirebaseId(documento.getId());
                    productos.add(producto);
                }
            }

            dao.aplicarRemotos(productos);
            Log.d(TAG, "Sincronización completada");
            return Result.success();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Result.retry();
        } catch (Exception e) {
            if (!sesionValida(usuarioId)) {
                return Result.success();
            }
            Log.w(TAG, "No se completó la sincronización", e);
            Throwable causa = e instanceof ExecutionException ? e.getCause() : e;
            if (causa instanceof FirebaseFirestoreException) {
                FirebaseFirestoreException.Code codigo =
                        ((FirebaseFirestoreException) causa).getCode();
                if (codigo == FirebaseFirestoreException.Code.PERMISSION_DENIED
                        || codigo == FirebaseFirestoreException.Code.INVALID_ARGUMENT) {
                    return Result.failure();
                }
            }
            return Result.retry();
        } finally {
            dao.cerrar();
        }
    }

    private boolean sesionValida(String usuarioId) {
        FirebaseUser usuario = FirebaseAuth.getInstance().getCurrentUser();
        return !isStopped() && usuario != null && usuario.getUid().equals(usuarioId);
    }
}
