package debug.servicios;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import debug.modelo.Producto;
import debug.modelo.Movimiento;

public class ProductoService {
    private FirebaseFirestore db;

    public ProductoService() {
        db = FirebaseFirestore.getInstance();
    }

    // CREATE: agregar producto
    public void agregarProducto(Producto producto,
                                ProductoCallback callback) {

        db.collection("productos")
                .add(producto)
                .addOnSuccessListener(document -> {
                    callback.onSuccess("Producto agregado");
                })
                .addOnFailureListener(e -> {
                    callback.onError(e.getMessage());
                });
    }

    // READ: listar productos del usuario
    public void listarProductos(String usuarioId,
                                ProductoCallback callback) {

        db.collection("productos")
                .whereEqualTo("usuarioId", usuarioId)
                .orderBy("nombre", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(resultado -> {

                    callback.onSuccess(resultado);

                })
                .addOnFailureListener(e -> {
                    callback.onError(e.getMessage());
                });
    }

    // UPDATE: actualizar producto
    public void actualizarProducto(String id,
                                   Producto producto,
                                   ProductoCallback callback) {

        db.collection("productos")
                .document(id)
                .set(producto)
                .addOnSuccessListener(unused -> {
                    callback.onSuccess("Producto actualizado");
                })
                .addOnFailureListener(e -> {
                    callback.onError(e.getMessage());
                });
    }

    // DELETE: eliminar producto
    public void eliminarProducto(String id,
                                 ProductoCallback callback) {

        db.collection("productos")
                .document(id)
                .delete()
                .addOnSuccessListener(unused -> {
                    callback.onSuccess("Producto eliminado");
                })
                .addOnFailureListener(e -> {
                    callback.onError(e.getMessage());
                });
    }

    // Respuesta de las operaciones
    public interface ProductoCallback {
        void onSuccess(Object resultado);
        void onError(String error);
    }
}
