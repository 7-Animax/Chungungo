package debug.conexion;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class ConexionBD extends SQLiteOpenHelper {

    private static final String NOMBRE_BD = "chungungo_local.db";
    private static final int VERSION_BD = 1;

    public ConexionBD(Context context) {
        super(context, NOMBRE_BD, null, VERSION_BD);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        db.execSQL("CREATE TABLE productos (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "nombre TEXT NOT NULL, " +
                "categoria TEXT NOT NULL, " +
                "cantidad REAL NOT NULL, " +
                "unidad TEXT NOT NULL, " +
                "stock_minimo REAL NOT NULL, " +
                "firebase_id TEXT NOT NULL UNIQUE, " +
                "usuario_id TEXT NOT NULL, " +
                "pendiente INTEGER DEFAULT 1, " +
                "eliminado INTEGER DEFAULT 0)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db,
                          int oldVersion, int newVersion) {
        // Migraciones para futuras versiones.
    }
}