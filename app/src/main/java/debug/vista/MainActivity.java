package debug.vista;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import com.devst.chungungo.R;

import debug.modelo.Producto;

public class MainActivity extends SesionActivity {
    @Override
    protected int obtenerLayout() {
        return R.layout.activity_main;
    }

    @Override
    protected void configurarPantalla(Bundle estado) {
        findViewById(R.id.btnAgregarProducto).setOnClickListener(vista ->
                startActivity(new Intent(this, FormularioProductoActivity.class)));
        findViewById(R.id.btnVerInventario).setOnClickListener(vista -> abrirInventario(null));
        findViewById(R.id.cardAlimentos).setOnClickListener(vista -> abrirInventario("Alimentos"));
        findViewById(R.id.cardLimpieza).setOnClickListener(vista -> abrirInventario("Limpieza"));
        findViewById(R.id.cardHigiene).setOnClickListener(vista -> abrirInventario("Higiene"));
        findViewById(R.id.btnCerrarSesion).setOnClickListener(vista -> {
            auth.cerrarSesion();
            abrirPantallaInicial(this, LoginActivity.class);
        });

        repositorio.observarSincronizacion().observe(this, trabajos -> actualizarResumen());
    }

    @Override
    protected void alReanudar() {
        repositorio.sincronizar();
        actualizarResumen();
    }

    private void abrirInventario(String categoria) {
        Intent intent = new Intent(this, ProductosActivity.class);
        if (categoria != null) {
            intent.putExtra(ProductosActivity.CATEGORIA, categoria);
        }
        startActivity(intent);
    }

    private void actualizarResumen() {
        ejecutar(repositorio::listarProductos, productos -> {
            if (productos == null) {
                return;
            }
            int stockBajo = 0;
            for (Producto producto : productos) {
                if (producto.getCantidad() <= producto.getStockMinimo()) {
                    stockBajo++;
                }
            }
            ((TextView) findViewById(R.id.tvTotalProductos)).setText(String.valueOf(productos.size()));
            ((TextView) findViewById(R.id.tvStockBajo)).setText(String.valueOf(stockBajo));
        });
    }
}
