package com.hadoga.hadoga.view.fragments;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.hadoga.hadoga.R;
import com.hadoga.hadoga.model.database.HadogaDatabase;
import com.hadoga.hadoga.model.entities.Usuario;
import com.hadoga.hadoga.utils.FirestoreSyncHelper;
import com.hadoga.hadoga.view.LoginActivity;

public class DashboardFragment extends Fragment {

    private TextView txtNombreClinica; // referencia al TextView del XML

    public DashboardFragment() {}

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dashboard, container, false);

        // Inicializar referencia al TextView
        txtNombreClinica = view.findViewById(R.id.txtNombreClinica);

        // Mostrar nombre dinámico
        mostrarNombreClinica();

        // Inicializar los listeners de las tarjetas
        initListeners(view);

        return view;
    }
    private void mostrarNombreClinica() {
        SharedPreferences prefs = requireContext().getSharedPreferences("hadoga_prefs", requireContext().MODE_PRIVATE);
        String email = prefs.getString("usuario_email", null);

        if (email != null) {
            new Thread(() -> {
                Usuario usuario = HadogaDatabase.getInstance(requireContext()).usuarioDao().getUsuarioByEmail(email);
                if (usuario != null && getActivity() != null) {
                    requireActivity().runOnUiThread(() ->
                            txtNombreClinica.setText(usuario.getNombreClinica())
                    );
                }
            }).start();
        }
    }


    private void initListeners(View view) {
        view.findViewById(R.id.cardAgregarPaciente).setOnClickListener(v -> navigateTo(new AgregarPacienteFragment()));

        view.findViewById(R.id.cardVerPacientes).setOnClickListener(v -> navigateTo(new ListaPacientesFragment()));

        view.findViewById(R.id.cardAgregarCita).setOnClickListener(v -> navigateTo(new NuevaCitaFragment()));

        view.findViewById(R.id.cardVerCitas).setOnClickListener(v -> navigateTo(new ListaCitasFragment()));

        view.findViewById(R.id.cardAgregarDoctor).setOnClickListener(v -> navigateTo(new AgregarDoctorFragment()));

        view.findViewById(R.id.cardVerDoctores).setOnClickListener(v -> navigateTo(new ListaDoctoresFragment()));

        view.findViewById(R.id.cardAgregarSucursal).setOnClickListener(v -> navigateTo(new AgregarSucursalFragment()));

        view.findViewById(R.id.cardVerSucursales).setOnClickListener(v -> navigateTo(new ListaSucursalesFragment()));

        view.findViewById(R.id.cardVerMapa).setOnClickListener(v -> navigateTo(new MapaFragment()));

        // Botón de sincronizar datos
        view.findViewById(R.id.btnSincronizar).setOnClickListener(v -> {
            FirestoreSyncHelper syncHelper = new FirestoreSyncHelper(requireContext());
            syncHelper.sincronizarTodo();
        });

        // Botón de cerrar sesión
        view.findViewById(R.id.btnCerrarSesion).setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), LoginActivity.class);
            startActivity(intent);
            requireActivity().finish();
        });
    }

    private void navigateTo(Fragment fragment) {
        requireActivity().getSupportFragmentManager().beginTransaction().replace(R.id.fragmentContainer, fragment).addToBackStack(null).commit();
    }
}
