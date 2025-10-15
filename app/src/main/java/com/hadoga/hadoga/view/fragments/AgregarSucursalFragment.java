package com.hadoga.hadoga.view.fragments;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.hadoga.hadoga.R;
import com.hadoga.hadoga.model.database.HadogaDatabase;
import com.hadoga.hadoga.model.entities.Sucursal;

import java.util.concurrent.Executors;

public class AgregarSucursalFragment extends Fragment {
    private EditText etNombreSucursal, etCodigoSucursal, etDireccion, etTelefono, etCorreo;
    private Spinner spDepartamento;
    private HadogaDatabase db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        db = HadogaDatabase.getInstance(requireContext());
        View view = inflater.inflate(R.layout.fragment_agregar_sucursal, container, false);

        initUI(view);

        initListeners(view);

        return view;
    }

    private void initUI(View view) {
        etNombreSucursal = view.findViewById(R.id.etNombreSucursal);
        etCodigoSucursal = view.findViewById(R.id.etCodigoSucursal);
        spDepartamento = view.findViewById(R.id.spDepartamento);
        etDireccion = view.findViewById(R.id.etDireccion);
        etTelefono = view.findViewById(R.id.etTelefono);
        etCorreo = view.findViewById(R.id.etCorreo);

        // Agrega los departamentos
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(requireContext(), R.array.departamentos, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spDepartamento.setAdapter(adapter);
    }

    private void initListeners(View view) {
        View btnGuardar = view.findViewById(R.id.btnGuardarSucursal);
        btnGuardar.setOnClickListener(v -> guardarSucursal());
    }

    private void guardarSucursal() {
        String nombre = etNombreSucursal.getText().toString().trim();
        String codigo = etCodigoSucursal.getText().toString().trim();
        String depto = spDepartamento.getSelectedItem() != null ? spDepartamento.getSelectedItem().toString() : "";
        String direccion = etDireccion.getText().toString().trim();
        String telefono = etTelefono.getText().toString().trim();
        String correo = etCorreo.getText().toString().trim();

        // --- Validaciones ---
        if (nombre.length() < 3) {
            etNombreSucursal.setError("El nombre debe tener al menos 3 caracteres");
            etNombreSucursal.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(codigo)) {
            etCodigoSucursal.setError("El código de sucursal es obligatorio");
            etCodigoSucursal.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(depto)) {
            Toast.makeText(requireContext(), "Selecciona un departamento", Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(direccion)) {
            etDireccion.setError("La dirección es obligatoria");
            etDireccion.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(telefono)) {
            etTelefono.setError("El teléfono es obligatorio");
            etTelefono.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(correo)) {
            etCorreo.setError("El teléfono es obligatorio");
            etCorreo.requestFocus();
            return;
        }

        if (!TextUtils.isEmpty(telefono) && !telefono.matches("\\d{8,12}")) {
            etTelefono.setError("Solo números (8–12 dígitos)");
            etTelefono.requestFocus();
            return;
        }

        if (!TextUtils.isEmpty(correo) && !Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
            etCorreo.setError("Correo inválido");
            etCorreo.requestFocus();
            return;
        }

        // --- Insertar a base de datos ---
        Sucursal sucursal = new Sucursal(nombre, codigo, depto, direccion, telefono, correo);

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                db.sucursalDao().insert(sucursal);
                requireActivity().runOnUiThread(() -> Toast.makeText(requireContext(), "Sucursal guardada exitosamente", Toast.LENGTH_SHORT).show());
                requireActivity().getSupportFragmentManager().popBackStack();
            } catch (Exception e) {
                requireActivity().runOnUiThread(() -> {
                    if (e.getMessage() != null && e.getMessage().contains("UNIQUE constraint failed")) {
                        etCodigoSucursal.setError("El código ya está registrado");
                        etCodigoSucursal.requestFocus();
                    } else {
                        Toast.makeText(requireContext(), "Error al guardar la sucursal", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });


    }
}
