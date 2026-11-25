package com.devst.chungungo;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.Source;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class SyncWorker extends Worker {

    public SyncWorker(@NonNull Context context,
                      @NonNull WorkerParameters parametros) {

        super(context, parametros);
    }

    @NonNull
    @Override
    public Result doWork() {

        String usuarioId = getInputData().getString("usuarioId");

        if (usuarioId == null || !sesionValida(usuarioId)) {
            return Result.failure();
        }

        ProductoDAO dao =
                new ProductoDAO(getApplicationContext(), usuarioId);

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        try {

            // Enviar cambios pendientes a Firebase
            for (ProductoDAO.Cambio cambio : dao.obtenerPendientes()) {

                if (!sesionValida(usuarioId) || isStopped()) {
                    return Result.failure();
                }

                Producto producto = cambio.producto;

                Map<String, Object> datos = new HashMap<>();

                datos.put("firebaseId", producto.getFirebaseId());
                datos.put("usuarioId", usuarioId);
                datos.put("nombre", producto.getNombre());
                datos.put("categoria", producto.getCategoria());
                datos.put("cantidad", producto.getCantidad());
                datos.put("unidad", producto.getUnidad());
                datos.put("stockMinimo", producto.getStockMinimo());
                datos.put("eliminado", cambio.eliminado);

                Tasks.await(
                        db.collection("productos")
                                .document(producto.getFirebaseId())
                                .set(datos),
                        30,
                        TimeUnit.SECONDS
                );

                dao.confirmarCambio(cambio);
            }

            // Recuperar productos desde Firebase
            QuerySnapshot resultado = Tasks.await(
                    db.collection("productos")
                            .whereEqualTo("usuarioId", usuarioId)
                            .get(Source.SERVER),
                    30,
                    TimeUnit.SECONDS
            );

            ArrayList<Producto> productos = new ArrayList<>();

            for (DocumentSnapshot documento : resultado.getDocuments()) {

                if (Boolean.TRUE.equals(
                        documento.getBoolean("eliminado"))) {

                    continue;
                }

                Producto producto = documento.toObject(Producto.class);

                if (producto != null) {

                    producto.setFirebaseId(documento.getId());
                    producto.setUsuarioId(usuarioId);

                    productos.add(producto);
                }
            }

            if (!sesionValida(usuarioId) || isStopped()) {
                return Result.failure();
            }

            // Actualizar la base de datos local
            dao.aplicarRemotos(productos);

            Log.d("SyncWorker", "Sincronizacion completada");

            return Result.success();

        } catch (Exception e) {

            Log.e("SyncWorker", "Error de sincronizacion", e);

            return Result.retry();

        } finally {

            dao.cerrar();
        }
    }

    private boolean sesionValida(String usuarioId) {

        FirebaseUser usuario =
                FirebaseAuth.getInstance().getCurrentUser();

        return usuario != null &&
                usuarioId.equals(usuario.getUid());
    }
}