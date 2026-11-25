package debug.vistaModelo;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import debug.conexion.ConexionBD;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.UUID;

import debug.modelo.Producto;

public class ProductoDAO {

    private final ConexionBD conexion;
    private final String usuarioId;

    public ProductoDAO(Context context, String usuarioId) {

        if (usuarioId == null || usuarioId.isEmpty()) {
            throw new IllegalArgumentException("Usuario no identificado");
        }

        conexion = new ConexionBD(context);
        this.usuarioId = usuarioId;
    }

    // CREATE
    public boolean agregarProducto(Producto producto) {

        SQLiteDatabase db = conexion.getWritableDatabase();

        producto.setFirebaseId(UUID.randomUUID().toString());
        producto.setUsuarioId(usuarioId);

        ContentValues valores = obtenerValores(producto);

        valores.put("pendiente", 1);
        valores.put("eliminado", 0);

        long resultado = db.insert("productos", null, valores);

        if (resultado != -1) {
            producto.setId((int) resultado);
            return true;
        }

        return false;
    }

    // READ
    public ArrayList<Producto> listarProductos() {

        ArrayList<Producto> lista = new ArrayList<>();

        SQLiteDatabase db = conexion.getReadableDatabase();

        try (Cursor cursor = db.rawQuery(
                "SELECT * FROM productos " +
                        "WHERE usuario_id = ? AND eliminado = 0 " +
                        "ORDER BY nombre",
                new String[]{usuarioId})) {

            while (cursor.moveToNext()) {
                lista.add(leerProducto(cursor));
            }
        }

        return lista;
    }

    public Producto buscarProducto(int id) {
        SQLiteDatabase db = conexion.getReadableDatabase();
        try (Cursor cursor = db.query("productos", null,
                "id = ? AND usuario_id = ? AND eliminado = 0",
                new String[]{String.valueOf(id), usuarioId}, null, null, null)) {
            return cursor.moveToFirst() ? leerProducto(cursor) : null;
        }
    }

    // UPDATE
    public boolean actualizarProducto(Producto producto) {

        ContentValues valores = new ContentValues();

        valores.put("nombre", producto.getNombre());
        valores.put("categoria", producto.getCategoria());
        valores.put("cantidad", producto.getCantidad());
        valores.put("unidad", producto.getUnidad());
        valores.put("stock_minimo", producto.getStockMinimo());

        return registrarCambio(producto.getId(), valores);
    }

    // DELETE
    public boolean eliminarProducto(int id) {

        ContentValues valores = new ContentValues();

        valores.put("eliminado", 1);

        return registrarCambio(id, valores);
    }

    // Registrar cambios pendientes
    private boolean registrarCambio(int id, ContentValues valores) {

        SQLiteDatabase db = conexion.getWritableDatabase();

        db.beginTransaction();

        try {

            int resultado = db.update(
                    "productos",
                    valores,
                    "id = ? AND usuario_id = ? AND eliminado = 0",
                    new String[]{String.valueOf(id), usuarioId}
            );

            if (resultado > 0) {

                db.execSQL(
                        "UPDATE productos SET pendiente = 1, version = version + 1 " +
                                "WHERE id = ? AND usuario_id = ?",
                        new Object[]{id, usuarioId}
                );
            }

            db.setTransactionSuccessful();

            return resultado > 0;

        } finally {
            db.endTransaction();
        }
    }

    // Convertir producto en valores para SQLite
    private ContentValues obtenerValores(Producto producto) {

        ContentValues valores = new ContentValues();

        valores.put("nombre", producto.getNombre());
        valores.put("categoria", producto.getCategoria());
        valores.put("cantidad", producto.getCantidad());
        valores.put("unidad", producto.getUnidad());
        valores.put("stock_minimo", producto.getStockMinimo());
        valores.put("firebase_id", producto.getFirebaseId());
        valores.put("usuario_id", usuarioId);

        return valores;
    }

    // Convertir registro SQLite en objeto Producto
    private Producto leerProducto(Cursor cursor) {

        Producto producto = new Producto(
                cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                cursor.getString(cursor.getColumnIndexOrThrow("nombre")),
                cursor.getString(cursor.getColumnIndexOrThrow("categoria")),
                cursor.getDouble(cursor.getColumnIndexOrThrow("cantidad")),
                cursor.getString(cursor.getColumnIndexOrThrow("unidad")),
                cursor.getDouble(cursor.getColumnIndexOrThrow("stock_minimo"))
        );

        producto.setFirebaseId(
                cursor.getString(cursor.getColumnIndexOrThrow("firebase_id"))
        );

        producto.setUsuarioId(
                cursor.getString(cursor.getColumnIndexOrThrow("usuario_id"))
        );

        return producto;
    }

    // Obtener productos pendientes de sincronizar
    public ArrayList<Cambio> obtenerPendientes() {

        ArrayList<Cambio> lista = new ArrayList<>();

        SQLiteDatabase db = conexion.getReadableDatabase();

        try (Cursor cursor = db.rawQuery(
                "SELECT * FROM productos " +
                        "WHERE usuario_id = ? AND pendiente > 0",
                new String[]{usuarioId})) {

            while (cursor.moveToNext()) {

                lista.add(new Cambio(
                        leerProducto(cursor),
                        cursor.getLong(cursor.getColumnIndexOrThrow("version")),
                        cursor.getInt(cursor.getColumnIndexOrThrow("eliminado")) == 1
                ));
            }
        }

        return lista;
    }

    // Confirmar sincronización con Firebase
    public void confirmarCambio(Cambio cambio) {

        SQLiteDatabase db = conexion.getWritableDatabase();

        String condicion =
                "id = ? AND usuario_id = ? AND version = ? AND pendiente > 0";

        String[] argumentos = {
                String.valueOf(cambio.producto.getId()),
                usuarioId,
                String.valueOf(cambio.version)
        };

        if (cambio.eliminado) {

            db.delete("productos", condicion, argumentos);

        } else {

            ContentValues valores = new ContentValues();

            valores.put("pendiente", 0);

            db.update("productos", valores, condicion, argumentos);
        }
    }

    // Guardar los productos recuperados desde Firebase
    public void aplicarRemotos(ArrayList<Producto> productos) {

        SQLiteDatabase db = conexion.getWritableDatabase();

        HashSet<String> identificadores = new HashSet<>();

        db.beginTransaction();

        try {

            for (Producto producto : productos) {

                if (producto.getFirebaseId() == null ||
                        !usuarioId.equals(producto.getUsuarioId())) {

                    throw new IllegalArgumentException(
                            "Producto remoto no válido"
                    );
                }

                identificadores.add(producto.getFirebaseId());

                ContentValues valores = obtenerValores(producto);

                valores.put("pendiente", 0);
                valores.put("eliminado", 0);

                db.insertWithOnConflict(
                        "productos",
                        null,
                        valores,
                        SQLiteDatabase.CONFLICT_IGNORE
                );

                db.update(
                        "productos",
                        valores,
                        "firebase_id = ? AND usuario_id = ? AND pendiente = 0",
                        new String[]{
                                producto.getFirebaseId(),
                                usuarioId
                        }
                );
            }

            ArrayList<String> eliminados = new ArrayList<>();

            try (Cursor cursor = db.rawQuery(
                    "SELECT firebase_id FROM productos " +
                            "WHERE usuario_id = ? AND pendiente = 0",
                    new String[]{usuarioId})) {

                while (cursor.moveToNext()) {

                    String id = cursor.getString(0);

                    if (!identificadores.contains(id)) {
                        eliminados.add(id);
                    }
                }
            }

            for (String id : eliminados) {

                db.delete(
                        "productos",
                        "firebase_id = ? AND usuario_id = ? AND pendiente = 0",
                        new String[]{id, usuarioId}
                );
            }

            db.setTransactionSuccessful();

        } finally {
            db.endTransaction();
        }
    }

    public void cerrar() {
        conexion.close();
    }

    // Representar un cambio pendiente
    public static class Cambio {

        public final Producto producto;
        public final long version;
        public final boolean eliminado;

        public Cambio(Producto producto, long version,
                      boolean eliminado) {

            this.producto = producto;
            this.version = version;
            this.eliminado = eliminado;
        }
    }
}