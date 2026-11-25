package debug.vista;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.devst.chungungo.R;

import debug.servicios.AuthService;

public class LoginActivity extends AppCompatActivity {
    static final String CORREO_REGISTRADO = "correoRegistrado";
    private Button iniciar;
    private EditText correo;
    private EditText clave;
    private AuthService auth;

    @Override
    protected void onCreate(Bundle estado) {
        super.onCreate(estado);
        auth = new AuthService();
        if (auth.sesionActiva()) {
            SesionActivity.abrirPantallaInicial(this, MainActivity.class);
            return;
        }

        SesionActivity.mostrarPantalla(this, R.layout.activity_login);
        correo = findViewById(R.id.etCorreo);
        clave = findViewById(R.id.etContrasena);
        iniciar = findViewById(R.id.btnIniciarSesion);
        if (estado == null) {
            correo.setText(getIntent().getStringExtra(CORREO_REGISTRADO));
        }
        iniciar.setOnClickListener(vista -> iniciarSesion());
        findViewById(R.id.tvRegistrarse).setOnClickListener(vista -> {
            if (iniciar.isEnabled()) {
                startActivity(new Intent(this, RegistroActivity.class));
            }
        });
    }

    private void iniciarSesion() {
        String email = correo.getText().toString().trim();
        String contrasena = clave.getText().toString();
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            correo.setError("Escribe un correo electrónico válido.");
            correo.requestFocus();
            return;
        }
        if (contrasena.isEmpty()) {
            clave.setError("Escribe tu contraseña.");
            clave.requestFocus();
            return;
        }

        iniciar.setEnabled(false);
        auth.iniciarSesion(email, contrasena, new AuthService.AuthCallback() {
            @Override
            public void onSuccess(String mensaje) {
                if (!isFinishing() && !isDestroyed()) {
                    SesionActivity.abrirPantallaInicial(LoginActivity.this, MainActivity.class);
                }
            }

            @Override
            public void onError(String error) {
                if (!isFinishing() && !isDestroyed()) {
                    iniciar.setEnabled(true);
                    Toast.makeText(LoginActivity.this, error, Toast.LENGTH_LONG).show();
                }
            }
        });
    }
}
