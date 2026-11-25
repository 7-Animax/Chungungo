package debug.servicios;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.Source;

import java.util.HashMap;
import java.util.Map;

import debug.modelo.Producto;

public class ProductoService {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final CollectionReference productos = db.collection("productos");

    public Task<Void> esperarEscriturasPendientes() {
        return db.waitForPendingWrites();
    }

    public Task<Void> guardarProducto(Producto producto) {
        Map<String, Object> datos = new HashMap<>();
        datos.put("firebaseId", producto.getFirebaseId());
        datos.put("usuarioId", producto.getUsuarioId());
        datos.put("nombre", producto.getNombre());
        datos.put("categoria", producto.getCategoria());
        datos.put("cantidad", producto.getCantidad());
        datos.put("unidad", producto.getUnidad());
        datos.put("stockMinimo", producto.getStockMinimo());
        return productos.document(producto.getFirebaseId()).set(datos);
    }

    public Task<QuerySnapshot> listarProductos(String usuarioId) {
        return productos.whereEqualTo("usuarioId", usuarioId).get(Source.SERVER);
    }

    public Task<Void> eliminarProducto(Producto producto) {
        String id = producto.getFirebaseId();
        return productos.document(id).delete().continueWithTask(tarea -> {
            Exception error = tarea.getException();
            if (!(error instanceof FirebaseFirestoreException)
                    || ((FirebaseFirestoreException) error).getCode()
                    != FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                return tarea;
            }

            // Un documento inexistente tampoco tiene propietario en las reglas.
            return listarProductos(producto.getUsuarioId()).onSuccessTask(resultado -> {
                if (resultado.getMetadata().hasPendingWrites()) {
                    return Tasks.forException(new IllegalStateException(
                            "Hay escrituras remotas pendientes de confirmación"));
                }
                boolean existe = resultado.getDocuments().stream()
                        .anyMatch(documento -> id.equals(documento.getId()));
                return existe ? Tasks.forException(error) : Tasks.forResult(null);
            });
        });
    }
}
