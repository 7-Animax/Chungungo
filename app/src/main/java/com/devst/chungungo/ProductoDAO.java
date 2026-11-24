package com.devst.chungungo;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;

public class ProductoDAO {

    private final ConexionBD conexion;

    public ProductoDAO(Context context) {
        conexion = new ConexionBD(context);
    }

    // Crear producto
    public boolean agregarProducto(Producto producto) {

        SQLiteDatabase db = conexion.getWritableDatabase();

        ContentValues valores = obtenerValores(producto);

        long resultado = db.insert("productos", null, valores);

        db.close();

        return resultado != -1;
    }

    // Listar productos
    public ArrayList<Producto> listarProductos() {

        ArrayList<Producto> lista = new ArrayList<>();

        SQLiteDatabase db = conexion.getReadableDatabase();

        Cursor cursor = db.rawQuery(
                "SELECT * FROM productos ORDER BY nombre", null);

        while (cursor.moveToNext()) {

            Producto producto = new Producto(
                    cursor.getInt(0),
                    cursor.getString(1),
                    cursor.getString(2),
                    cursor.getDouble(3),
                    cursor.getString(4),
                    cursor.getDouble(5)
            );

            lista.add(producto);
        }

        cursor.close();
        db.close();

        return lista;
    }

    // Actualizar producto
    public boolean actualizarProducto(Producto producto) {

        SQLiteDatabase db = conexion.getWritableDatabase();

        ContentValues valores = obtenerValores(producto);

        int resultado = db.update(
                "productos",
                valores,
                "id = ?",
                new String[]{String.valueOf(producto.getId())}
        );

        db.close();

        return resultado > 0;
    }

    // Eliminar producto
    public boolean eliminarProducto(int id) {

        SQLiteDatabase db = conexion.getWritableDatabase();

        int resultado = db.delete(
                "productos",
                "id = ?",
                new String[]{String.valueOf(id)}
        );

        db.close();

        return resultado > 0;
    }

    private ContentValues obtenerValores(Producto producto) {

        ContentValues valores = new ContentValues();

        valores.put("nombre", producto.getNombre());
        valores.put("categoria", producto.getCategoria());
        valores.put("cantidad", producto.getCantidad());
        valores.put("unidad", producto.getUnidad());
        valores.put("stock_minimo", producto.getStockMinimo());

        return valores;
    }
}