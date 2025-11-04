package com.hadoga.hadoga.view;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.hadoga.hadoga.R;
import com.hadoga.hadoga.model.database.HadogaDatabase;
import com.hadoga.hadoga.model.entities.Usuario;

public class LoginActivity extends AppCompatActivity {
    // Definición de elementos
    private EditText inputEmail, inputPassword;
    private Button btnLogin;
    private CheckBox checkRemember;
    private HadogaDatabase db;

    private SharedPreferences preferences;

    // Constantes para el preferences
    private static final String PREF_NAME = "HadogaPrefs";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_PASSWORD = "password";
    private static final String KEY_REMEMBER = "remember";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Vincula el XML de la activity
        setContentView(R.layout.activity_login);

        initUI();
        initListeners();
        // Carga los usuarios guardados en preferences (shared preferences)
        loadRememberedUser();

        // Texto "Regístrate"
        TextView textGoRegister = findViewById(R.id.textGoRegister);

        // Al hacer clic, abrirá la RegisterActivity
        textGoRegister.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });
    }

    // Inicializa vistas, preferences y base de datos
    private void initUI() {
        inputEmail = findViewById(R.id.inputEmail);
        inputPassword = findViewById(R.id.inputPassword);
        btnLogin = findViewById(R.id.btnLogin);
        checkRemember = findViewById(R.id.checkRememberMe);

        db = HadogaDatabase.getInstance(this);
        preferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
    }

    // Configura listeners
    private void initListeners() {
        // Botón login
        btnLogin.setOnClickListener(v -> attemptLogin());

        // Texto "Regístrate" para abrir vista de registro
        TextView textGoRegister = findViewById(R.id.textGoRegister);
        textGoRegister.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });
    }

    /**
     * Intenta iniciar sesión, si no es un usuario correcto muestra
     * error.
     */
    private void attemptLogin() {
        String email = inputEmail.getText().toString().trim();
        String password = inputPassword.getText().toString().trim();

        // Validaciones
        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            inputEmail.setError("Correo inválido");
            return;
        }

        if (TextUtils.isEmpty(password)) {
            inputPassword.setError("Ingrese su contraseña");
            return;
        }

        // En caso pase las validaciones entonces, busca el usuario con login
        Usuario user = db.usuarioDao().login(email, password);

        if (user == null) {
            showSnackbarLikeToast("Credenciales incorrectas", true);
            return;
        }

        // Guardar credenciales del usuario (en caso de clic en recordarme)
        if (checkRemember.isChecked()) {
            saveUserCredentials(email, password);
        } else {
            // Si no da clic, entonces se limpia el preferences
            clearUserCredentials();
        }

        showSnackbarLikeToast("Bienvenido", false);

        // Guardar el email del usuario logueado para usarlo en DashboardFragment
        SharedPreferences prefs = getSharedPreferences("hadoga_prefs", MODE_PRIVATE);
        prefs.edit().putString("usuario_email", email).apply();

        // Redirigir al MainActivity
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    // Guarda las credenciales de login en SharedPreferences
    private void saveUserCredentials(String email, String password) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(KEY_EMAIL, email);
        editor.putString(KEY_PASSWORD, password);
        editor.putBoolean(KEY_REMEMBER, true);
        editor.apply();
    }

    // Limpia las credenciales que se han guardado
    private void clearUserCredentials() {
        SharedPreferences.Editor editor = preferences.edit();
        editor.clear();
        editor.apply();
    }

    // Carga las credenciales si “Recordarme” está activo
    private void loadRememberedUser() {

        boolean recordarme = preferences.getBoolean(KEY_REMEMBER, false);
        if (recordarme) {
            String savedEmail = preferences.getString(KEY_EMAIL, "");
            String savedPassword = preferences.getString(KEY_PASSWORD, "");

            inputEmail.setText(savedEmail);
            inputPassword.setText(savedPassword);
            checkRemember.setChecked(true);
        }
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