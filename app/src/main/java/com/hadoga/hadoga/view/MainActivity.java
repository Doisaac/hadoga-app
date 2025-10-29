package com.hadoga.hadoga.view;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.hadoga.hadoga.R;
import com.hadoga.hadoga.view.fragments.DashboardFragment;
import com.hadoga.hadoga.view.fragments.ListaCitasFragment;
import com.hadoga.hadoga.view.fragments.ListaPacientesFragment;

public class MainActivity extends AppCompatActivity {
    // Definición del BottomNavigationView
    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bottomNavigationView = findViewById(R.id.bottomNavigationView);

        // Cargar fragmento inicial
        loadFragment(new DashboardFragment());

        // Listener para el menú inferior
        bottomNavigationView.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;

            // Opción "Inicio"
            if (item.getItemId() == R.id.menu_dashboard) {
                selectedFragment = new DashboardFragment();
            }

            // Opción "Citas"
            else if (item.getItemId() == R.id.menu_citas) {
                selectedFragment = new ListaCitasFragment();
            }

            // Opción "Expedientes"
            else if (item.getItemId() == R.id.menu_expedientes) {
                selectedFragment = new ListaPacientesFragment();
            }

            // Cargar el fragmento seleccionado
            if (selectedFragment != null) {
                loadFragment(selectedFragment);
                return true;
            }
            return false;
        });
    }

    // Método para reemplazar el fragmento actual
    private void loadFragment(@NonNull Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit();
    }
}
