package com.hadoga.hadoga.view;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.hadoga.hadoga.R;

public class LoginActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Vincula el XML de la activity
        setContentView(R.layout.activity_login);
    }
}