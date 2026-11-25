package debug.vista;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.devst.chungungo.R;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;

import debug.modelo.Producto;

public class FormularioProductoActivity extends SesionActivity {
    static final String PRODUCTO_ID = "productoId";

    private EditText nombre;
    private EditText cantidad;
    private EditText stockMinimo;
    private Spinner categoria;
    private Spinner unidad;
    private Button guardar;
    private Button limpiar;
    private int productoId;
    private Producto productoOriginal;

    @Override
    protected int obtenerLayout() {
        return R.layout.activity_formulario_producto;
    }

    @Override
    protected void configurarPantalla(Bundle estado) {
        nombre = findViewById(R.id.etNombreProducto);
        cantidad = findViewById(R.id.etCantidad);
        stockMinimo = findViewById(R.id.etStockMinimo);
        categoria = findViewById(R.id.spCategoria);
        unidad = findViewById(R.id.spUnidad);
        guardar = findViewById(R.id.btnGuardarProducto);
        limpiar = findViewById(R.id.btnLimpiar);

        prepararSpinner(categoria, "Selecciona", "Alimentos", "Limpieza", "Higiene", "Otros");
        prepararSpinner(unidad, "Selecciona", "Unidad", "kg", "g", "L", "mL", "Paquete");
        findViewById(R.id.btnVolver).setOnClickListener(vista -> finish());
        guardar.setOnClickListener(vista -> guardarProducto());

        productoId = getIntent().getIntExtra(PRODUCTO_ID, 0);
        if (productoId == 0) {
            seleccionar(categoria, getIntent().getStringExtra(ProductosActivity.CATEGORIA));
            limpiar.setOnClickListener(vista -> limpiarCampos());
            return;
        }

        ((TextView) findViewById(R.id.tvTituloFormulario)).setText(R.string.editar_producto);
        guardar.setText(R.string.guardar_cambios);
        limpiar.setText(R.string.restaurar_campos);
        cambiarBotones(false);
        ejecutar(() -> repositorio.buscarProducto(productoId), producto -> {
            if (producto == null) {
                mensaje("El producto ya no está disponible.");
                finish();
                return;
            }
            productoOriginal = producto;
            if (estado == null || !estado.getBoolean("productoCargado")) {
                mostrarProducto(producto);
            } else {
                agregarSiFalta(categoria, producto.getCategoria());
                agregarSiFalta(unidad, producto.getUnidad());
                categoria.setSelection(estado.getInt("categoriaSeleccionada"));
                unidad.setSelection(estado.getInt("unidadSeleccionada"));
            }
            limpiar.setOnClickListener(vista -> mostrarProducto(productoOriginal));
            cambiarBotones(true);
        });
    }

    @Override
    protected void onSaveInstanceState(Bundle estado) {
        super.onSaveInstanceState(estado);
        estado.putBoolean("productoCargado", productoOriginal != null);
        if (categoria != null) {
            estado.putInt("categoriaSeleccionada", categoria.getSelectedItemPosition());
            estado.putInt("unidadSeleccionada", unidad.getSelectedItemPosition());
        }
    }

    private void guardarProducto() {
        String nombreProducto = nombre.getText().toString().trim();
        if (nombreProducto.isEmpty()) {
            nombre.setError("Escribe el nombre del producto.");
            nombre.requestFocus();
            return;
        }
        if (categoria.getSelectedItemPosition() <= 0 || unidad.getSelectedItemPosition() <= 0) {
            mensaje("Selecciona una categoría y una unidad.");
            return;
        }
        Double cantidadProducto = leerNumero(cantidad);
        if (cantidadProducto == null) {
            return;
        }
        Double stockProducto = leerNumero(stockMinimo);
        if (stockProducto == null) {
            return;
        }

        Producto producto = new Producto(productoId, nombreProducto,
                categoria.getSelectedItem().toString(), cantidadProducto,
                unidad.getSelectedItem().toString(), stockProducto);
        cambiarBotones(false);
        ejecutar(() -> productoId == 0
                ? repositorio.agregarProducto(producto) : repositorio.actualizarProducto(producto),
                guardado -> {
                    cambiarBotones(true);
                    if (Boolean.TRUE.equals(guardado)) {
                        mensaje("Producto guardado en este dispositivo. Se sincronizará con Firebase.");
                        finish();
                    } else if (Boolean.FALSE.equals(guardado)) {
                        mensaje("No se pudo guardar el producto.");
                    }
                });
    }

    private Double leerNumero(EditText campo) {
        String texto = campo.getText().toString().trim().replace(',', '.');
        if (texto.matches("[0-9]+(\\.[0-9]+)?|\\.[0-9]+")) {
            double valor = Double.parseDouble(texto);
            if (Double.isFinite(valor)) {
                return valor;
            }
        }
        campo.setError("Escribe un número mayor o igual a cero.");
        campo.requestFocus();
        return null;
    }

    private void prepararSpinner(Spinner spinner, String... opciones) {
        ArrayAdapter<String> adaptador = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, new ArrayList<>(Arrays.asList(opciones)));
        adaptador.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adaptador);
    }

    private void mostrarProducto(Producto producto) {
        nombre.setText(producto.getNombre());
        cantidad.setText(BigDecimal.valueOf(producto.getCantidad()).stripTrailingZeros().toPlainString());
        stockMinimo.setText(BigDecimal.valueOf(producto.getStockMinimo()).stripTrailingZeros().toPlainString());
        seleccionar(categoria, producto.getCategoria());
        seleccionar(unidad, producto.getUnidad());
    }

    private void seleccionar(Spinner spinner, String valor) {
        if (valor != null) {
            spinner.setSelection(agregarSiFalta(spinner, valor));
        }
    }

    @SuppressWarnings("unchecked")
    private int agregarSiFalta(Spinner spinner, String valor) {
        ArrayAdapter<String> adaptador = (ArrayAdapter<String>) spinner.getAdapter();
        int posicion = adaptador.getPosition(valor);
        if (posicion < 0) {
            adaptador.add(valor);
            posicion = adaptador.getCount() - 1;
        }
        return posicion;
    }

    private void limpiarCampos() {
        nombre.setText("");
        cantidad.setText("");
        stockMinimo.setText("");
        categoria.setSelection(0);
        unidad.setSelection(0);
    }

    private void cambiarBotones(boolean habilitados) {
        guardar.setEnabled(habilitados);
        limpiar.setEnabled(habilitados);
    }
}
