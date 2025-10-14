package com.hadoga.hadoga.view;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.hadoga.hadoga.R;
import com.hadoga.hadoga.model.database.HadogaDatabase;

public class LoginActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Vincula el XML de la activity
        setContentView(R.layout.activity_login);

        // Texto "Regístrate"
        TextView textGoRegister = findViewById(R.id.textGoRegister);

        // Al hacer clic, abrirá la RegisterActivity
        textGoRegister.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });
    }
}