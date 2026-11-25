package debug.vista;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.work.WorkInfo;

import com.devst.chungungo.R;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

import debug.modelo.Producto;

public class ProductosActivity extends SesionActivity {
    static final String CATEGORIA = "categoria";

    private final ArrayList<Producto> productosVisibles = new ArrayList<>();
    private ArrayAdapter<String> adaptador;
    private TextView estadoSync;
    private String categoria;

    @Override
    protected int obtenerLayout() {
        return R.layout.activity_productos;
    }

    @Override
    protected void configurarPantalla(Bundle estado) {
        categoria = getIntent().getStringExtra(CATEGORIA);
        if (categoria != null) {
            ((TextView) findViewById(R.id.tvTituloInventario)).setText(categoria);
        }
        findViewById(R.id.btnVolverInventario).setOnClickListener(vista -> finish());
        findViewById(R.id.btnAgregarInventario).setOnClickListener(vista ->
                startActivity(new Intent(this, FormularioProductoActivity.class)
                        .putExtra(CATEGORIA, categoria)));
        estadoSync = findViewById(R.id.tvEstadoSync);

        ListView lista = findViewById(R.id.listaProductos);
        adaptador = new ArrayAdapter<>(this, R.layout.fila_producto, R.id.tvDetalleProducto);
        lista.setAdapter(adaptador);
        lista.setEmptyView(findViewById(R.id.tvInventarioVacio));
        lista.setOnItemClickListener((padre, vista, posicion, id) ->
                mostrarOpciones(productosVisibles.get(posicion)));

        repositorio.observarSincronizacion().observe(this, trabajos -> {
            mostrarEstado(trabajos);
            actualizarListado();
        });
    }

    @Override
    protected void alReanudar() {
        repositorio.sincronizar();
        actualizarListado();
    }

    private void actualizarListado() {
        ejecutar(repositorio::listarProductos, productos -> {
            if (productos == null) {
                return;
            }

            ArrayList<String> filas = new ArrayList<>();
            NumberFormat formato = NumberFormat.getNumberInstance();
            formato.setMaximumFractionDigits(6);
            productosVisibles.clear();
            for (Producto producto : productos) {
                if (categoria == null || categoria.equals(producto.getCategoria())) {
                    productosVisibles.add(producto);
                    filas.add(getString(R.string.detalle_producto,
                            producto.getNombre(), producto.getCategoria(),
                            formato.format(producto.getCantidad()), producto.getUnidad(),
                            formato.format(producto.getStockMinimo())));
                }
            }
            adaptador.setNotifyOnChange(false);
            adaptador.clear();
            adaptador.addAll(filas);
            adaptador.notifyDataSetChanged();
        });
    }

    private void mostrarEstado(List<WorkInfo> trabajos) {
        boolean pendiente = false;
        boolean error = false;
        for (WorkInfo trabajo : trabajos) {
            if (trabajo.getState() == WorkInfo.State.RUNNING) {
                estadoSync.setText(R.string.sync_en_progreso);
                return;
            }
            pendiente |= !trabajo.getState().isFinished();
            error |= trabajo.getState() == WorkInfo.State.FAILED;
        }
        if (pendiente) {
            estadoSync.setText(R.string.sync_pendiente);
        } else if (error) {
            estadoSync.setText(R.string.sync_error);
        } else {
            estadoSync.setText(trabajos.isEmpty()
                    ? R.string.sync_pendiente : R.string.sync_completa);
        }
    }

    private void mostrarOpciones(Producto producto) {
        new AlertDialog.Builder(this)
                .setTitle(producto.getNombre())
                .setItems(new String[]{"Editar", "Eliminar"}, (dialogo, opcion) -> {
                    if (opcion == 0) {
                        startActivity(new Intent(this, FormularioProductoActivity.class)
                                .putExtra(FormularioProductoActivity.PRODUCTO_ID, producto.getId()));
                    } else {
                        confirmarEliminacion(producto);
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void confirmarEliminacion(Producto producto) {
        new AlertDialog.Builder(this)
                .setTitle("Eliminar producto")
                .setMessage(getString(R.string.confirmar_eliminacion, producto.getNombre()))
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Eliminar", (dialogo, opcion) ->
                        ejecutar(() -> repositorio.eliminarProducto(producto.getId()), eliminado -> {
                            if (Boolean.TRUE.equals(eliminado)) {
                                mensaje("Producto eliminado del inventario. Se sincronizará con Firebase.");
                            } else if (Boolean.FALSE.equals(eliminado)) {
                                mensaje("El producto ya no está disponible.");
                            }
                            actualizarListado();
                        }))
                .show();
    }
}
