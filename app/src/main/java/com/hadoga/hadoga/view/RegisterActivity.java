package com.hadoga.hadoga.view;

import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.firebase.firestore.FirebaseFirestore;
import com.hadoga.hadoga.R;
import com.hadoga.hadoga.model.database.HadogaDatabase;
import com.hadoga.hadoga.model.entities.Usuario;
import com.hadoga.hadoga.utils.FirebaseService;
import com.hadoga.hadoga.utils.NetworkUtils;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {
    // Definición de elementos
    private EditText inputNombreClinica, inputEmail, inputPassword, inputConfirmPassword;
    private Button btnRegister;
    private HadogaDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        initUI();
        initListeners();
    }

    // Inicializa las vistas y base de datos
    private void initUI() {
        inputNombreClinica = findViewById(R.id.inputClinicName);
        inputEmail = findViewById(R.id.inputEmail);
        inputPassword = findViewById(R.id.inputPassword);
        inputConfirmPassword = findViewById(R.id.inputConfirmPassword);
        btnRegister = findViewById(R.id.btnCreateAccount);

        // Inicializar Room
        db = HadogaDatabase.getInstance(this);
    }

    // Configura listeners de botones
    private void initListeners() {
        // Botón de regresar
        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        // Botón de registrar
        btnRegister.setOnClickListener(v -> registerUser());

        // Texto para volver al login
        TextView textGoLogin = findViewById(R.id.textGoLogin);
        textGoLogin.setOnClickListener(v -> {
            Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });
    }

    // Valida los datos ingresados y registra el usuario
    private void registerUser() {
        String nombre = inputNombreClinica.getText().toString().trim();
        String email = inputEmail.getText().toString().trim();
        String password = inputPassword.getText().toString().trim();
        String confirmPassword = inputConfirmPassword.getText().toString().trim();

        // Validaciones
        if (TextUtils.isEmpty(nombre) || nombre.length() < 3) {
            inputNombreClinica.setError("Debe tener al menos 3 caracteres");
            return;
        }

        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            inputEmail.setError("Correo inválido");
            return;
        }

        if (TextUtils.isEmpty(password) || password.length() < 6) {
            inputPassword.setError("Debe tener al menos 6 caracteres");
            return;
        }

        if (!password.equals(confirmPassword)) {
            inputConfirmPassword.setError("Las contraseñas no coinciden");
            return;
        }

        // Validar si ya existe el email
        if (db.usuarioDao().existeEmail(email) != null) {
            inputEmail.setError("Este correo ya está registrado");
            return;
        }

        String hashedPassword = com.hadoga.hadoga.utils.PasswordUtils.hashPassword(password);

        // Crear objeto usuario
        Usuario nuevo = new Usuario(nombre, email, hashedPassword);

        // Verificar conexión
        if (!NetworkUtils.isNetworkAvailable(this)) {
            // Guardar localmente (offline)
            nuevo.setEstadoSincronizacion("PENDIENTE");

            new Thread(() -> db.usuarioDao().insert(nuevo)).start();

            showSnackbarLikeToast("Sin conexión. Guardado localmente (pendiente de sincronizar).", false);
            goToLogin();
            return;
        }

        // Si hay conexión
        FirebaseFirestore firestore = FirebaseService.getInstance();

        Map<String, Object> userMap = new HashMap<>();
        userMap.put("nombreClinica", nombre);
        userMap.put("email", email);
        userMap.put("contrasena", hashedPassword);
        userMap.put("estado_sincronizacion", "SINCRONIZADO");

        firestore.collection("usuarios").document(email)
                .set(userMap)
                .addOnSuccessListener(aVoid -> {
                    nuevo.setEstadoSincronizacion("SINCRONIZADO");
                    new Thread(() -> db.usuarioDao().insert(nuevo)).start();

                    showSnackbarLikeToast("Usuario registrado y sincronizado correctamente.", false);
                    goToLogin();
                })
                .addOnFailureListener(e -> {
                    nuevo.setEstadoSincronizacion("PENDIENTE");
                    new Thread(() -> db.usuarioDao().insert(nuevo)).start();

                    showSnackbarLikeToast("Error al sincronizar. Guardado localmente.", true);
                    goToLogin();
                });
    }

    private void goToLogin() {
        Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

    private void showSnackbarLikeToast(String message, boolean isError) {
        LayoutInflater inflater = getLayoutInflater();
        View layout = inflater.inflate(R.layout.custom_toast, null);

        LinearLayout container = layout.findViewById(R.id.toast_container);
        ImageView icon = layout.findViewById(R.id.toast_icon);
        TextView text = layout.findViewById(R.id.toast_message);

        text.setText(message);

        // Cambiar color de fondo e ícono según el estado
        int color = isError
                ? ContextCompat.getColor(this, android.R.color.holo_red_dark)
                : ContextCompat.getColor(this, R.color.colorBlue);

        GradientDrawable background = new GradientDrawable();
        background.setCornerRadius(24f);
        background.setColor(color);
        container.setBackground(background);

        icon.setImageResource(isError ? R.drawable.ic_error : R.drawable.ic_check_circle);

        Toast toast = new Toast(getApplicationContext());
        toast.setDuration(Toast.LENGTH_LONG);
        toast.setView(layout);
        toast.setGravity(Gravity.BOTTOM, 0, 120);
        toast.show();
    }

}