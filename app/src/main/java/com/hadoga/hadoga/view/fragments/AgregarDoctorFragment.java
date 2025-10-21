package com.hadoga.hadoga.view.fragments;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.hadoga.hadoga.R;
import com.hadoga.hadoga.model.database.HadogaDatabase;
import com.hadoga.hadoga.model.entities.Doctor;

import java.util.concurrent.Executors;

public class AgregarDoctorFragment extends Fragment {
    private ImageView ivFotoDoctor;
    private Button btnSeleccionarFoto;
    private EditText etNombre, etApellido, etFechaNacimiento, etColegiado;
    private RadioGroup rgGenero;
    private Spinner spEspecialidad, spSucursal;
    private Button btnGuardarDoctor;
    private HadogaDatabase db;
    private Uri fotoSeleccionadaUri = null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        db = HadogaDatabase.getInstance(requireContext());
        View view = inflater.inflate(R.layout.fragment_agregar_doctor, container, false);

        initUI(view);
        initListeners(view);

        return view;
    }

    private void initUI(View view) {
        ivFotoDoctor = view.findViewById(R.id.ivFotoDoctor);
        btnSeleccionarFoto = view.findViewById(R.id.btnSeleccionarFoto);
        etNombre = view.findViewById(R.id.etNombre);
        etApellido = view.findViewById(R.id.etApellido);
        etFechaNacimiento = view.findViewById(R.id.etFechaNacimiento);
        etColegiado = view.findViewById(R.id.etColegiado);
        rgGenero = view.findViewById(R.id.rgGenero);
        spEspecialidad = view.findViewById(R.id.spEspecialidad);
        spSucursal = view.findViewById(R.id.spSucursal);
        btnGuardarDoctor = view.findViewById(R.id.btnGuardarDoctor);

        // Spinner de especialidades
        ArrayAdapter<CharSequence> adapterEspecialidad = ArrayAdapter.createFromResource(requireContext(), R.array.especialidades, android.R.layout.simple_spinner_item);
        adapterEspecialidad.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spEspecialidad.setAdapter(adapterEspecialidad);

        // Spinner de sucursales (cargadas desde la BD)
        Executors.newSingleThreadExecutor().execute(() -> {
            var listaSucursales = db.sucursalDao().getAllSucursales();
            requireActivity().runOnUiThread(() -> {
                ArrayAdapter<String> adapterSucursal = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item);
                adapterSucursal.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                for (var s : listaSucursales) adapterSucursal.add(s.getNombreSucursal());
                spSucursal.setAdapter(adapterSucursal);
            });
        });
    }

    private void initListeners(View view) {
        btnSeleccionarFoto.setOnClickListener(v -> abrirGaleria());
        btnGuardarDoctor.setOnClickListener(v -> guardarDoctor());
    }

    private void abrirGaleria() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        seleccionarImagenLauncher.launch(intent);
    }

    private final ActivityResultLauncher<Intent> seleccionarImagenLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
            Uri uri = result.getData().getData();
            if (uri != null) {
                fotoSeleccionadaUri = uri;
                ivFotoDoctor.setImageURI(uri);
            }
        }
    });

    private void guardarDoctor() {
        String nombre = etNombre.getText().toString().trim();
        String apellido = etApellido.getText().toString().trim();
        String fechaNac = etFechaNacimiento.getText().toString().trim();
        String colegiado = etColegiado.getText().toString().trim();
        String especialidad = spEspecialidad.getSelectedItem() != null ? spEspecialidad.getSelectedItem().toString() : "";
        String sucursalNombre = spSucursal.getSelectedItem() != null ? spSucursal.getSelectedItem().toString() : "";
        int generoId = rgGenero.getCheckedRadioButtonId();

        // Validaciones
        if (TextUtils.isEmpty(nombre)) {
            etNombre.setError("El nombre es obligatorio");
            etNombre.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(apellido)) {
            etApellido.setError("El apellido es obligatorio");
            etApellido.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(fechaNac)) {
            etFechaNacimiento.setError("Ingresa la fecha de nacimiento");
            etFechaNacimiento.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(colegiado)) {
            etColegiado.setError("Número de colegiado obligatorio");
            etColegiado.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(especialidad)) {
            Toast.makeText(requireContext(), "Selecciona una especialidad", Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(sucursalNombre)) {
            Toast.makeText(requireContext(), "Selecciona una sucursal", Toast.LENGTH_SHORT).show();
            return;
        }
        if (generoId == -1) {
            Toast.makeText(requireContext(), "Selecciona un género", Toast.LENGTH_SHORT).show();
            return;
        }

        RadioButton rbGenero = requireView().findViewById(generoId);
        String genero = rbGenero.getText().toString().toLowerCase();

        // Obtener ID de sucursal según el nombre seleccionado
        Executors.newSingleThreadExecutor().execute(() -> {
            var listaSucursales = db.sucursalDao().getAllSucursales();
            int idSucursalSeleccionada = -1;
            for (var s : listaSucursales) {
                if (s.getNombreSucursal().equals(sucursalNombre)) {
                    idSucursalSeleccionada = s.getId();
                    break;
                }
            }

            if (idSucursalSeleccionada == -1) {
                int finalIdSucursalSeleccionada = idSucursalSeleccionada;
                requireActivity().runOnUiThread(() -> Toast.makeText(requireContext(), "Sucursal no encontrada (ID: " + finalIdSucursalSeleccionada + ")", Toast.LENGTH_SHORT).show());
                return;
            }

            // Guardar en DB
            Doctor nuevoDoctor = new Doctor(nombre, apellido, fechaNac, colegiado, genero, especialidad, idSucursalSeleccionada, fotoSeleccionadaUri != null ? fotoSeleccionadaUri.toString() : null);

            try {
                db.doctorDao().insertar(nuevoDoctor);
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), "Doctor agregado exitosamente", Toast.LENGTH_SHORT).show();
                    requireActivity().getSupportFragmentManager().popBackStack();
                });
            } catch (Exception e) {
                requireActivity().runOnUiThread(() -> {
                    if (e.getMessage() != null && e.getMessage().contains("UNIQUE constraint failed")) {
                        etColegiado.setError("El número de colegiado ya está registrado");
                        etColegiado.requestFocus();
                    } else {
                        Toast.makeText(requireContext(), "Error al guardar el doctor", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }


}
