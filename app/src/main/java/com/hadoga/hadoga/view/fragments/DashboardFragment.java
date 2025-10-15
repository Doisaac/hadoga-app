package com.hadoga.hadoga.view.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.hadoga.hadoga.R;

public class DashboardFragment extends Fragment {
    public DashboardFragment() {

    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dashboard, container, false);

        View cardAgregarSucursal = view.findViewById(R.id.cardAgregarSucursal);
        cardAgregarSucursal.setOnClickListener(v -> openAgregarSucursalFragment());

        return view;
    }

    // Función que abre el fragmento de "AgregarSucursal"
    private void openAgregarSucursalFragment() {
        FragmentTransaction transaction = requireActivity()
                .getSupportFragmentManager()
                .beginTransaction();

        transaction.replace(R.id.fragmentContainer, new AgregarSucursalFragment());
        transaction.addToBackStack(null); // permite volver con el botón "atrás"
        transaction.commit();
    }
}
