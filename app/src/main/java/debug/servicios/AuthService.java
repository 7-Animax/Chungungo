package debug.servicios;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import debug.modelo.Usuario;

public class AuthService {
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    public AuthService() {
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
    }

    // Registrar un nuevo usuario
    public void registrar(String nombre, String correo,
                          String clave,
                          AuthCallback callback) {

        auth.createUserWithEmailAndPassword(correo, clave)
                .addOnCompleteListener(task -> {

                    if (task.isSuccessful()) {

                        String uid = auth.getCurrentUser().getUid();

                        Usuario usuario = new Usuario(
                                uid, nombre, correo, "usuario"
                        );

                        // Guardar información adicional en Firestore
                        db.collection("usuarios")
                                .document(uid)
                                .set(usuario)
                                .addOnSuccessListener(unused ->
                                        callback.onSuccess("Registro exitoso")
                                )
                                .addOnFailureListener(e ->
                                        callback.onError(e.getMessage())
                                );

                    } else {
                        callback.onError(task.getException().getMessage());
                    }
                });
    }

    // Iniciar sesión
    public void iniciarSesion(String correo, String clave,
                              AuthCallback callback) {

        auth.signInWithEmailAndPassword(correo, clave)
                .addOnCompleteListener(task -> {

                    if (task.isSuccessful()) {
                        callback.onSuccess("Inicio de sesión exitoso");
                    } else {
                        callback.onError(task.getException().getMessage());
                    }
                });
    }

    // Cerrar sesión
    public void cerrarSesion() {
        auth.signOut();
    }

    // Comprobar si existe un usuario autenticado
    public boolean sesionActiva() {
        return auth.getCurrentUser() != null;
    }

    // Obtener identificador del usuario actual
    public String obtenerUsuarioId() {

        if (auth.getCurrentUser() != null) {
            return auth.getCurrentUser().getUid();
        }

        return null;
    }

    // Respuesta de las operaciones
    public interface AuthCallback {
        void onSuccess(String mensaje);
        void onError(String error);
    }
}
