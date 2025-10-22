package com.hadoga.hadoga.view.fragments;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
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
import com.hadoga.hadoga.model.entities.Paciente;
import com.hadoga.hadoga.model.entities.Sucursal;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.concurrent.Executors;

public class AgregarPacienteFragment extends Fragment {
    private HadogaDatabase db;
    private ImageView ivFotoPaciente;
    private Button btnSeleccionarFoto, btnCrearPaciente;
    private EditText etNombre, etApellido, etFechaNacimiento, etCorreo, etTelefono, etDireccion, etObservaciones;
    private RadioGroup rgGenero;
    private Spinner spSucursal;
    private CheckBox cbDiabetes, cbAnemia, cbGastritis, cbHipertension, cbHemorragias, cbAsma, cbTrastornosCardiacos, cbConvulsiones, cbTiroides;

    private Uri fotoSeleccionadaUri;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        db = HadogaDatabase.getInstance(requireContext());
        View view = inflater.inflate(R.layout.fragment_agregar_paciente, container, false);

        initUI(view);
        initListeners(view);

        // Si viene en modo edición
        if (getArguments() != null && getArguments().containsKey("pacienteData")) {
            Paciente paciente = (Paciente) getArguments().getSerializable("pacienteData");
            cargarDatosPaciente(view, paciente);
        }

        return view;
    }

    private void initUI(View view) {
        ivFotoPaciente = view.findViewById(R.id.ivFotoPaciente);
        btnSeleccionarFoto = view.findViewById(R.id.btnSeleccionarFoto);
        btnCrearPaciente = view.findViewById(R.id.btnCrearPaciente);

        etNombre = view.findViewById(R.id.etNombre);
        etApellido = view.findViewById(R.id.etApellido);
        etFechaNacimiento = view.findViewById(R.id.etFechaNacimiento);
        etCorreo = view.findViewById(R.id.etCorreo);
        etTelefono = view.findViewById(R.id.etTelefono);
        etDireccion = view.findViewById(R.id.etDireccion);
        etObservaciones = view.findViewById(R.id.etObservaciones);
        rgGenero = view.findViewById(R.id.rgGenero);
        spSucursal = view.findViewById(R.id.spSucursal);

        cbDiabetes = view.findViewById(R.id.cbDiabetes);
        cbAnemia = view.findViewById(R.id.cbAnemia);
        cbGastritis = view.findViewById(R.id.cbGastritis);
        cbHipertension = view.findViewById(R.id.cbHipertension);
        cbHemorragias = view.findViewById(R.id.cbHemorragias);
        cbAsma = view.findViewById(R.id.cbAsma);
        cbTrastornosCardiacos = view.findViewById(R.id.cbTrastornosCardiacos);
        cbConvulsiones = view.findViewById(R.id.cbConvulsiones);
        cbTiroides = view.findViewById(R.id.cbTiroides);

        // Cargar sucursales desde la BD
        Executors.newSingleThreadExecutor().execute(() -> {
            List<Sucursal> listaSucursales = db.sucursalDao().getAllSucursales();
            requireActivity().runOnUiThread(() -> {
                ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                for (Sucursal s : listaSucursales) {
                    adapter.add(s.getNombreSucursal());
                }
                spSucursal.setAdapter(adapter);
            });
        });
    }

    private void initListeners(View view) {
        btnSeleccionarFoto.setOnClickListener(v -> abrirGaleria());
        btnCrearPaciente.setOnClickListener(v -> guardarPaciente());
    }

    private void abrirGaleria() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        seleccionarImagenLauncher.launch(intent);
    }

    private final ActivityResultLauncher<Intent> seleccionarImagenLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
            Uri uri = result.getData().getData();
            if (uri != null) {
                requireContext().getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);

                try (InputStream inputStream = requireContext().getContentResolver().openInputStream(uri)) {
                    File file = new File(requireContext().getFilesDir(), "paciente_" + System.currentTimeMillis() + ".jpg");
                    try (OutputStream outputStream = new FileOutputStream(file)) {
                        byte[] buffer = new byte[1024];
                        int length;
                        while ((length = inputStream.read(buffer)) > 0) {
                            outputStream.write(buffer, 0, length);
                        }
                    }

                    fotoSeleccionadaUri = Uri.fromFile(file);
                    ivFotoPaciente.setImageURI(fotoSeleccionadaUri);

                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(requireContext(), "Error al cargar la imagen", Toast.LENGTH_SHORT).show();
                }
            }
        }
    });

    private void guardarPaciente() {
        String nombre = etNombre.getText().toString().trim();
        String apellido = etApellido.getText().toString().trim();
        String fechaNac = etFechaNacimiento.getText().toString().trim();
        String correo = etCorreo.getText().toString().trim();
        String telefono = etTelefono.getText().toString().trim();
        String direccion = etDireccion.getText().toString().trim();
        String observaciones = etObservaciones.getText().toString().trim();
        int generoId = rgGenero.getCheckedRadioButtonId();
        String sucursalNombre = spSucursal.getSelectedItem() != null ? spSucursal.getSelectedItem().toString() : "";

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
            etFechaNacimiento.setError("Fecha de nacimiento obligatoria");
            etFechaNacimiento.requestFocus();
            return;
        }
        if (generoId == -1) {
            Toast.makeText(requireContext(), "Selecciona un género", Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(sucursalNombre)) {
            Toast.makeText(requireContext(), "Selecciona una sucursal", Toast.LENGTH_SHORT).show();
            return;
        }

        RadioButton rbGenero = requireView().findViewById(generoId);
        String genero = rbGenero.getText().toString().toLowerCase();

        Executors.newSingleThreadExecutor().execute(() -> {
            List<Sucursal> listaSucursales = db.sucursalDao().getAllSucursales();
            int idSucursalSeleccionada = -1;
            for (Sucursal s : listaSucursales) {
                if (s.getNombreSucursal().equals(sucursalNombre)) {
                    idSucursalSeleccionada = s.getId();
                    break;
                }
            }

            if (idSucursalSeleccionada == -1) {
                requireActivity().runOnUiThread(() -> Toast.makeText(requireContext(), "Sucursal no encontrada", Toast.LENGTH_SHORT).show());
                return;
            }

            Paciente nuevo = new Paciente(nombre, apellido, fechaNac, genero, correo, telefono, direccion, observaciones, cbDiabetes.isChecked(), cbAnemia.isChecked(), cbGastritis.isChecked(), cbHipertension.isChecked(), cbHemorragias.isChecked(), cbAsma.isChecked(), cbTrastornosCardiacos.isChecked(), cbConvulsiones.isChecked(), cbTiroides.isChecked(), idSucursalSeleccionada, fotoSeleccionadaUri != null ? fotoSeleccionadaUri.toString() : null);

            try {
                db.pacienteDao().insertar(nuevo);
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), "Paciente agregado exitosamente", Toast.LENGTH_SHORT).show();
                    requireActivity().getSupportFragmentManager().popBackStack();
                });
            } catch (Exception e) {
                e.printStackTrace();
                requireActivity().runOnUiThread(() -> Toast.makeText(requireContext(), "Error al guardar el paciente", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void cargarDatosPaciente(View view, Paciente p) {
        etNombre.setText(p.getNombre());
        etApellido.setText(p.getApellido());
        etFechaNacimiento.setText(p.getFechaNacimiento());
        etCorreo.setText(p.getCorreoElectronico());
        etTelefono.setText(p.getNumeroTelefono());
        etDireccion.setText(p.getDireccion());
        etObservaciones.setText(p.getObservaciones());

        // Género
        if (p.getSexo().equalsIgnoreCase("masculino")) rgGenero.check(R.id.rbMasculino);
        else if (p.getSexo().equalsIgnoreCase("femenino")) rgGenero.check(R.id.rbFemenino);

        // Imagen
        if (p.getFotoUri() != null && !p.getFotoUri().isEmpty()) {
            fotoSeleccionadaUri = Uri.parse(p.getFotoUri());
            ivFotoPaciente.setImageURI(fotoSeleccionadaUri);
        } else {
            ivFotoPaciente.setImageResource(R.drawable.ic_user_placeholder);
        }

        // Checkboxes
        cbDiabetes.setChecked(p.isDiabetes());
        cbAnemia.setChecked(p.isAnemia());
        cbGastritis.setChecked(p.isGastritis());
        cbHipertension.setChecked(p.isHipertensionHta());
        cbHemorragias.setChecked(p.isHemorragias());
        cbAsma.setChecked(p.isAsma());
        cbTrastornosCardiacos.setChecked(p.isTrastornosCardiacos());
        cbConvulsiones.setChecked(p.isConvulsiones());
        cbTiroides.setChecked(p.isTiroides());

        // Spinner de sucursal
        Executors.newSingleThreadExecutor().execute(() -> {
            List<Sucursal> lista = db.sucursalDao().getAllSucursales();
            requireActivity().runOnUiThread(() -> {
                ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

                int posSel = -1;
                for (int i = 0; i < lista.size(); i++) {
                    Sucursal s = lista.get(i);
                    adapter.add(s.getNombreSucursal());
                    if (s.getId() == p.getSucursalId()) posSel = i;
                }

                spSucursal.setAdapter(adapter);
                if (posSel >= 0) spSucursal.setSelection(posSel);
            });
        });

        // Cambiar texto del botón
        btnCrearPaciente.setText("Actualizar Paciente");
        btnCrearPaciente.setOnClickListener(v -> actualizarPaciente(p.getId()));
    }

    private void actualizarPaciente(int idPaciente) {
        //  Update
        String nombre = etNombre.getText().toString().trim();
        String apellido = etApellido.getText().toString().trim();
        String fechaNac = etFechaNacimiento.getText().toString().trim();
        String correo = etCorreo.getText().toString().trim();
        String telefono = etTelefono.getText().toString().trim();
        String direccion = etDireccion.getText().toString().trim();
        String observaciones = etObservaciones.getText().toString().trim();
        int generoId = rgGenero.getCheckedRadioButtonId();
        String sucursalNombre = spSucursal.getSelectedItem() != null ? spSucursal.getSelectedItem().toString() : "";

        if (TextUtils.isEmpty(nombre) || TextUtils.isEmpty(apellido) || generoId == -1) {
            Toast.makeText(requireContext(), "Completa los campos obligatorios", Toast.LENGTH_SHORT).show();
            return;
        }

        RadioButton rbGenero = requireView().findViewById(generoId);
        String genero = rbGenero.getText().toString().toLowerCase();

        Executors.newSingleThreadExecutor().execute(() -> {
            List<Sucursal> listaSucursales = db.sucursalDao().getAllSucursales();
            int idSucursalSeleccionada = -1;
            for (Sucursal s : listaSucursales) {
                if (s.getNombreSucursal().equals(sucursalNombre)) {
                    idSucursalSeleccionada = s.getId();
                    break;
                }
            }

            if (idSucursalSeleccionada == -1) {
                requireActivity().runOnUiThread(() -> Toast.makeText(requireContext(), "Sucursal no encontrada", Toast.LENGTH_SHORT).show());
                return;
            }

            Paciente actualizado = new Paciente(nombre, apellido, fechaNac, genero, correo, telefono, direccion, observaciones, cbDiabetes.isChecked(), cbAnemia.isChecked(), cbGastritis.isChecked(), cbHipertension.isChecked(), cbHemorragias.isChecked(), cbAsma.isChecked(), cbTrastornosCardiacos.isChecked(), cbConvulsiones.isChecked(), cbTiroides.isChecked(), idSucursalSeleccionada, fotoSeleccionadaUri != null ? fotoSeleccionadaUri.toString() : null);
            actualizado.setId(idPaciente);

            try {
                db.pacienteDao().actualizar(actualizado);
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), "Paciente actualizado exitosamente", Toast.LENGTH_SHORT).show();
                    requireActivity().getSupportFragmentManager().popBackStack();
                });
            } catch (Exception e) {
                e.printStackTrace();
                requireActivity().runOnUiThread(() -> Toast.makeText(requireContext(), "Error al actualizar el paciente", Toast.LENGTH_SHORT).show());
            }
        });
    }
}
