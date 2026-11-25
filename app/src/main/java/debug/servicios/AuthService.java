package debug.servicios;

import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;

public class AuthService {
    private final FirebaseAuth auth = FirebaseAuth.getInstance();

    public void registrar(String nombre, String correo, String clave, AuthCallback callback) {
        auth.createUserWithEmailAndPassword(correo, clave)
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        callback.onError(mensajeError(task.getException()));
                        return;
                    }

                    FirebaseUser usuario = task.getResult().getUser();
                    UserProfileChangeRequest perfil = new UserProfileChangeRequest.Builder()
                            .setDisplayName(nombre)
                            .build();
                    usuario.updateProfile(perfil).addOnCompleteListener(actualizacion -> {
                        cerrarSesion();
                        callback.onSuccess(actualizacion.isSuccessful()
                                ? "Cuenta creada. Inicia sesión para continuar."
                                : "Cuenta creada. El nombre no se pudo guardar.");
                    });
                });
    }

    public void iniciarSesion(String correo, String clave, AuthCallback callback) {
        auth.signInWithEmailAndPassword(correo, clave)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        callback.onSuccess("Sesión iniciada");
                    } else {
                        callback.onError(mensajeError(task.getException()));
                    }
                });
    }

    public void cerrarSesion() {
        auth.signOut();
    }

    public boolean sesionActiva() {
        return obtenerUsuarioId() != null;
    }

    public String obtenerUsuarioId() {
        FirebaseUser usuario = auth.getCurrentUser();
        return usuario == null ? null : usuario.getUid();
    }

    private String mensajeError(Exception error) {
        if (error instanceof FirebaseNetworkException) {
            return "No hay conexión con Firebase. Revisa internet e inténtalo de nuevo.";
        }
        if (error instanceof FirebaseTooManyRequestsException) {
            return "Demasiados intentos. Vuelve a intentarlo más tarde.";
        }
        if (error instanceof FirebaseAuthException) {
            switch (((FirebaseAuthException) error).getErrorCode()) {
                case "ERROR_EMAIL_ALREADY_IN_USE":
                    return "Ya existe una cuenta con ese correo.";
                case "ERROR_WEAK_PASSWORD":
                    return "La contraseña no cumple la política de seguridad.";
                case "ERROR_INVALID_EMAIL":
                    return "Escribe un correo electrónico válido.";
                case "ERROR_WRONG_PASSWORD":
                case "ERROR_USER_NOT_FOUND":
                case "ERROR_INVALID_CREDENTIAL":
                case "ERROR_INVALID_LOGIN_CREDENTIALS":
                    return "El correo o la contraseña son incorrectos.";
                case "ERROR_USER_DISABLED":
                    return "Esta cuenta está deshabilitada.";
            }
        }
        return "No se pudo completar la operación de autenticación.";
    }

    public interface AuthCallback {
        void onSuccess(String mensaje);
        void onError(String error);
    }
}
