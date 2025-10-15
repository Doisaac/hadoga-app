package com.hadoga.hadoga.view.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.hadoga.hadoga.R;
import com.hadoga.hadoga.view.LoginActivity;

public class DashboardFragment extends Fragment {
    public DashboardFragment() {

    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dashboard, container, false);

        initListeners(view);

        return view;
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
