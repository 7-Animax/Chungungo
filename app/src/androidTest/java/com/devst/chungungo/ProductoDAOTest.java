package com.devst.chungungo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.ContextWrapper;
import android.database.sqlite.SQLiteDatabase;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import debug.modelo.Producto;
import debug.vistaModelo.ProductoDAO;

@RunWith(AndroidJUnit4.class)
public class ProductoDAOTest {
    private File archivo;
    private ProductoDAO inventario;
    private ProductoDAO otraCuenta;

    @Before
    public void abrirBaseDePrueba() throws IOException {
        Context aplicacion = InstrumentationRegistry.getInstrumentation().getTargetContext();
        archivo = File.createTempFile("inventario-test-", ".db", aplicacion.getCacheDir());
        Context contexto = new ContextWrapper(aplicacion) {
            @Override
            public File getDatabasePath(String nombre) {
                return archivo;
            }
        };
        inventario = new ProductoDAO(contexto, "propietario");
        otraCuenta = new ProductoDAO(contexto, "otra-cuenta");
    }

    @After
    public void cerrarBaseDePrueba() {
        inventario.cerrar();
        otraCuenta.cerrar();
        SQLiteDatabase.deleteDatabase(archivo);
    }

    @Test
    public void confirmacionAnteriorConservaUnaEdicionPendiente() {
        Producto arroz = crearProducto();
        ProductoDAO.Cambio anterior = inventario.obtenerPendientes().get(0);
        arroz.setCantidad(8.5);
        assertTrue(inventario.actualizarProducto(arroz));

        inventario.confirmarCambio(anterior);
        ArrayList<Producto> remotos = new ArrayList<>();
        remotos.add(anterior.producto);
        inventario.aplicarRemotos(remotos);

        assertEquals(8.5, inventario.buscarProducto(arroz.getId()).getCantidad(), 0.0);
        assertEquals(1, inventario.obtenerPendientes().size());
        assertEquals(anterior.producto.getFirebaseId(),
                inventario.obtenerPendientes().get(0).producto.getFirebaseId());
    }

    @Test
    public void unaConfirmacionRepetidaNoMarcaUnaEdicionPosterior() {
        Producto arroz = crearProducto();
        ProductoDAO.Cambio anterior = inventario.obtenerPendientes().get(0);
        inventario.confirmarCambio(anterior);

        arroz.setCantidad(10);
        assertTrue(inventario.actualizarProducto(arroz));
        inventario.confirmarCambio(anterior);

        assertEquals(1, inventario.obtenerPendientes().size());
        assertEquals(10.0, inventario.buscarProducto(arroz.getId()).getCantidad(), 0.0);
    }

    @Test
    public void borradoPendienteNoReapareceConUnaLecturaAntigua() {
        Producto arroz = crearProducto();
        ProductoDAO.Cambio alta = inventario.obtenerPendientes().get(0);
        assertTrue(inventario.eliminarProducto(arroz.getId()));

        ArrayList<Producto> remotos = new ArrayList<>();
        remotos.add(alta.producto);
        inventario.aplicarRemotos(remotos);
        inventario.confirmarCambio(alta);

        assertTrue(inventario.listarProductos().isEmpty());
        ProductoDAO.Cambio borrado = inventario.obtenerPendientes().get(0);
        assertTrue(borrado.eliminado);
        inventario.confirmarCambio(borrado);
        assertTrue(inventario.obtenerPendientes().isEmpty());
    }

    @Test
    public void lasOperacionesLocalesRespetanAlPropietario() {
        Producto arroz = crearProducto();
        assertTrue(otraCuenta.listarProductos().isEmpty());
        assertTrue(otraCuenta.obtenerPendientes().isEmpty());
        assertNull(otraCuenta.buscarProducto(arroz.getId()));
        assertFalse(otraCuenta.actualizarProducto(arroz));
        assertFalse(otraCuenta.eliminarProducto(arroz.getId()));
        assertEquals(1, inventario.listarProductos().size());
    }

    private Producto crearProducto() {
        Producto producto = new Producto(0, "Arroz", "Alimentos", 5, "kg", 1);
        assertTrue(inventario.agregarProducto(producto));
        return producto;
    }
}
