package com.hadoga.hadoga.view;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

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
            Toast.makeText(this, "Credenciales incorrectas", Toast.LENGTH_SHORT).show();
            return;
        }

        // Guardar credenciales del usuario (en caso de clic en recordarme)
        if (checkRemember.isChecked()) {
            saveUserCredentials(email, password);
        } else {
            // Si no da clic, entonces se limpia el preferences
            clearUserCredentials();
        }

        Toast.makeText(this, "Bienvenido", Toast.LENGTH_SHORT).show();

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

}