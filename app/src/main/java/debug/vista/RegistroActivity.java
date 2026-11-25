package debug.vista;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import com.devst.chungungo.R;

import debug.servicios.AuthService;

public class RegistroActivity extends AppCompatActivity {
    private EditText nombre;
    private EditText correo;
    private EditText clave;
    private EditText confirmacion;
    private Button registrar;

    @Override
    protected void onCreate(Bundle estado) {
        super.onCreate(estado);
        SesionActivity.mostrarPantalla(this, R.layout.activity_registro);
        nombre = findViewById(R.id.etNombreRegistro);
        correo = findViewById(R.id.etCorreoRegistro);
        clave = findViewById(R.id.etContrasenaRegistro);
        confirmacion = findViewById(R.id.etConfirmarContrasena);
        registrar = findViewById(R.id.btnRegistrarse);

        registrar.setOnClickListener(vista -> registrar());
        findViewById(R.id.tvIrLogin).setOnClickListener(vista -> {
            if (registrar.isEnabled()) {
                finish();
            }
        });
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (registrar.isEnabled()) {
                    finish();
                }
            }
        });
    }

    private void registrar() {
        String nombreUsuario = nombre.getText().toString().trim();
        String email = correo.getText().toString().trim();
        String contrasena = clave.getText().toString();
        if (nombreUsuario.isEmpty()) {
            mostrarError(nombre, "Escribe tu nombre.");
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            mostrarError(correo, "Escribe un correo electrónico válido.");
            return;
        }
        if (contrasena.length() < 6) {
            mostrarError(clave, "Usa al menos 6 caracteres.");
            return;
        }
        if (!contrasena.equals(confirmacion.getText().toString())) {
            mostrarError(confirmacion, "Las contraseñas deben coincidir.");
            return;
        }

        registrar.setEnabled(false);
        new AuthService().registrar(nombreUsuario, email, contrasena, new AuthService.AuthCallback() {
            @Override
            public void onSuccess(String mensaje) {
                if (!isFinishing() && !isDestroyed()) {
                    Toast.makeText(RegistroActivity.this, mensaje, Toast.LENGTH_LONG).show();
                    Intent intent = new Intent(RegistroActivity.this, LoginActivity.class)
                            .putExtra(LoginActivity.CORREO_REGISTRADO, email)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                }
            }

            @Override
            public void onError(String error) {
                if (!isFinishing() && !isDestroyed()) {
                    registrar.setEnabled(true);
                    Toast.makeText(RegistroActivity.this, error, Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void mostrarError(EditText campo, String error) {
        campo.setError(error);
        campo.requestFocus();
    }
}
