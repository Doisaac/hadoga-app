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

import com.google.firebase.firestore.FirebaseFirestore;
import com.hadoga.hadoga.R;
import com.hadoga.hadoga.model.database.HadogaDatabase;
import com.hadoga.hadoga.model.entities.Doctor;
import com.hadoga.hadoga.model.entities.Sucursal;
import com.hadoga.hadoga.utils.FirebaseService;
import com.hadoga.hadoga.utils.NetworkUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
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

        // en caso sea modo edición
        if (getArguments() != null && getArguments().containsKey("doctorData")) {
            Doctor doctor = (Doctor) getArguments().getSerializable("doctorData");
            cargarDatosDoctor(view, doctor);
        }

        return view;
    }

    private void cargarDatosDoctor(View view, Doctor d) {
        etNombre.setText(d.getNombre());
        etApellido.setText(d.getApellido());
        etFechaNacimiento.setText(d.getFechaNacimiento());
        etColegiado.setText(d.getNumeroColegiado());

        // Género
        if (d.getSexo().equalsIgnoreCase("masculino")) {
            rgGenero.check(R.id.rbMasculino);
        } else if (d.getSexo().equalsIgnoreCase("femenino")) {
            rgGenero.check(R.id.rbFemenino);
        }

        // Imagen (si hay URI)
        if (d.getFotoUri() != null && !d.getFotoUri().isEmpty()) {
            fotoSeleccionadaUri = Uri.parse(d.getFotoUri());
            ivFotoDoctor.setImageURI(fotoSeleccionadaUri);
        } else {
            ivFotoDoctor.setImageResource(R.drawable.ic_user_placeholder);
        }

        // Spinner de especialidad
        ArrayAdapter<CharSequence> adapterEsp = (ArrayAdapter<CharSequence>) spEspecialidad.getAdapter();
        if (adapterEsp != null) {
            int posEsp = adapterEsp.getPosition(d.getEspecialidad());
            if (posEsp >= 0) spEspecialidad.setSelection(posEsp);
        }

        // Spinner de sucursal, ojo cargar después de obtener de la BD
        Executors.newSingleThreadExecutor().execute(() -> {
            List<Sucursal> listaSucursales = db.sucursalDao().getAllSucursales();
            requireActivity().runOnUiThread(() -> {
                ArrayAdapter<String> adapterSucursal = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item);
                adapterSucursal.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

                int posSeleccionada = -1;
                for (int i = 0; i < listaSucursales.size(); i++) {
                    Sucursal s = listaSucursales.get(i);
                    adapterSucursal.add(s.getNombreSucursal());

                    // Comparar usando el código de sucursal
                    if (s.getCodigoSucursal().equals(d.getSucursalAsignada())) {
                        posSeleccionada = i;
                    }
                }

                spSucursal.setAdapter(adapterSucursal);
                if (posSeleccionada >= 0) spSucursal.setSelection(posSeleccionada);
            });
        });

        // Cambiar texto del botón
        Button btnGuardar = view.findViewById(R.id.btnGuardarDoctor);
        btnGuardar.setText("Actualizar Doctor");

        // Acción del botón
        btnGuardar.setOnClickListener(v -> actualizarDoctor(d.getId()));
    }

    private void actualizarDoctor(int idDoctor) {
        String nombre = etNombre.getText().toString().trim();
        String apellido = etApellido.getText().toString().trim();
        String fechaNac = etFechaNacimiento.getText().toString().trim();
        String colegiado = etColegiado.getText().toString().trim();
        String especialidad = spEspecialidad.getSelectedItem() != null ? spEspecialidad.getSelectedItem().toString() : "";
        String sucursalNombre = spSucursal.getSelectedItem() != null ? spSucursal.getSelectedItem().toString() : "";
        int generoId = rgGenero.getCheckedRadioButtonId();

        if (TextUtils.isEmpty(nombre) || TextUtils.isEmpty(apellido) || TextUtils.isEmpty(colegiado)) {
            Toast.makeText(requireContext(), "Completa todos los campos obligatorios", Toast.LENGTH_SHORT).show();
            return;
        }

        if (generoId == -1) {
            Toast.makeText(requireContext(), "Selecciona un género", Toast.LENGTH_SHORT).show();
            return;
        }

        RadioButton rbGenero = requireView().findViewById(generoId);
        String genero = rbGenero.getText().toString().toLowerCase();

        Executors.newSingleThreadExecutor().execute(() -> {
            List<Sucursal> listaSucursales = db.sucursalDao().getAllSucursales();
            String codigoSucursalSeleccionada = null;
            for (Sucursal s : listaSucursales) {
                if (s.getNombreSucursal().equals(sucursalNombre)) {
                    codigoSucursalSeleccionada = s.getCodigoSucursal();
                    break;
                }
            }

            if (codigoSucursalSeleccionada == null) {
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(requireContext(), "Sucursal no encontrada", Toast.LENGTH_SHORT).show()
                );
                return;
            }

            Doctor actualizado = new Doctor(
                    nombre,
                    apellido,
                    fechaNac,
                    colegiado,
                    genero,
                    especialidad,
                    codigoSucursalSeleccionada,  // aquí el cambio importante
                    fotoSeleccionadaUri != null ? fotoSeleccionadaUri.toString() : null
            );
            actualizado.setId(idDoctor);

            if (!NetworkUtils.isNetworkAvailable(requireContext())) {
                actualizado.setEstadoSincronizacion("PENDIENTE");
                db.doctorDao().actualizar(actualizado);
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(),
                            "Sin conexión. Actualización guardada localmente (pendiente de sincronizar)",
                            Toast.LENGTH_LONG).show();
                    requireActivity().getSupportFragmentManager().popBackStack();
                });
                return;
            }

            FirebaseFirestore firestore = FirebaseService.getInstance();
            firestore.collection("doctores")
                    .document(actualizado.getNumeroColegiado())
                    .set(actualizado)
                    .addOnSuccessListener(aVoid -> Executors.newSingleThreadExecutor().execute(() -> {
                        actualizado.setEstadoSincronizacion("SINCRONIZADO");
                        db.doctorDao().actualizar(actualizado);
                        requireActivity().runOnUiThread(() -> {
                            Toast.makeText(requireContext(), "Doctor actualizado correctamente", Toast.LENGTH_SHORT).show();
                            requireActivity().getSupportFragmentManager().popBackStack();
                        });
                    }))
                    .addOnFailureListener(e -> Executors.newSingleThreadExecutor().execute(() -> {
                        actualizado.setEstadoSincronizacion("PENDIENTE");
                        db.doctorDao().actualizar(actualizado);
                        requireActivity().runOnUiThread(() ->
                                Toast.makeText(requireContext(),
                                        "Error al actualizar en la nube. Guardado localmente como pendiente.",
                                        Toast.LENGTH_LONG).show()
                        );
                        requireActivity().getSupportFragmentManager().popBackStack();
                    }));
        });
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

        // Spinner de sucursales
        Executors.newSingleThreadExecutor().execute(() -> {
            var listaSucursales = db.sucursalDao().getAllSucursales();
            requireActivity().runOnUiThread(() -> {

                ArrayAdapter<String> adapterSucursal = new ArrayAdapter<String>(
                        requireContext(), android.R.layout.simple_spinner_item) {

                    @Override
                    public boolean isEnabled(int position) {
                        // Deshabilitar el primer item (placeholder)
                        return position != 0;
                    }

                    @Override
                    public View getDropDownView(int position, View convertView, ViewGroup parent) {
                        View viewItem = super.getDropDownView(position, convertView, parent);
                        android.widget.TextView textView = (android.widget.TextView) viewItem;

                        if (position == 0) {
                            // Placeholder gris
                            textView.setTextColor(getResources().getColor(android.R.color.darker_gray));
                        } else {
                            textView.setTextColor(getResources().getColor(android.R.color.white));
                        }

                        return viewItem;
                    }

                };

                adapterSucursal.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

                // Agregar el item por defecto
                adapterSucursal.add("Selecciona una sucursal");

                // Agregar las sucursales reales
                for (var s : listaSucursales) {
                    adapterSucursal.add(s.getNombreSucursal());
                }

                spSucursal.setAdapter(adapterSucursal);
                spSucursal.setSelection(0);
            });
        });
    }

    private void initListeners(View view) {
        btnSeleccionarFoto.setOnClickListener(v -> abrirGaleria());
        btnGuardarDoctor.setOnClickListener(v -> guardarDoctor());
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
                // Permitir acceso persistente
                requireContext().getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);

                try (InputStream inputStream = requireContext().getContentResolver().openInputStream(uri)) {
                    File file = new File(requireContext().getFilesDir(), "doctor_" + System.currentTimeMillis() + ".jpg");
                    try (OutputStream outputStream = new FileOutputStream(file)) {
                        byte[] buffer = new byte[1024];
                        int length;
                        while ((length = inputStream.read(buffer)) > 0) {
                            outputStream.write(buffer, 0, length);
                        }
                    }

                    // Guardar ruta local y mostrar imagen
                    fotoSeleccionadaUri = Uri.fromFile(file);
                    ivFotoDoctor.setImageURI(fotoSeleccionadaUri);

                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(requireContext(), "Error al cargar la imagen", Toast.LENGTH_SHORT).show();
                }
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
            String codigoSucursalSeleccionada = null;

            for (Sucursal s : listaSucursales) {
                if (s.getNombreSucursal().equals(sucursalNombre)) {
                    codigoSucursalSeleccionada = s.getCodigoSucursal();
                    break;
                }
            }

            if (codigoSucursalSeleccionada == null) {
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(requireContext(), "Sucursal no encontrada", Toast.LENGTH_SHORT).show()
                );
                return;
            }

            Doctor nuevoDoctor = new Doctor(
                    nombre, apellido, fechaNac, colegiado, genero,
                    especialidad, codigoSucursalSeleccionada,
                    fotoSeleccionadaUri != null ? fotoSeleccionadaUri.toString() : null
            );
            try {
                // Si no hay conexión
                if (!NetworkUtils.isNetworkAvailable(requireContext())) {
                    nuevoDoctor.setEstadoSincronizacion("PENDIENTE");

                    Executors.newSingleThreadExecutor().execute(() -> {
                        db.doctorDao().insertar(nuevoDoctor);
                        requireActivity().runOnUiThread(() ->
                                Toast.makeText(requireContext(), "Doctor guardado localmente (sin conexión)", Toast.LENGTH_LONG).show()
                        );
                        requireActivity().getSupportFragmentManager().popBackStack();
                    });
                    return;
                }

                // Si hay conexión, intentar subir a Firestore
                var firestore = FirebaseService.getInstance();
                firestore.collection("doctores")
                        .document(nuevoDoctor.getNumeroColegiado())
                        .set(nuevoDoctor)
                        .addOnSuccessListener(aVoid -> Executors.newSingleThreadExecutor().execute(() -> {
                            nuevoDoctor.setEstadoSincronizacion("SINCRONIZADO");
                            db.doctorDao().insertar(nuevoDoctor);

                            requireActivity().runOnUiThread(() -> {
                                Toast.makeText(requireContext(), "Doctor guardado y sincronizado", Toast.LENGTH_SHORT).show();
                                requireActivity().getSupportFragmentManager().popBackStack();
                            });
                        }))
                        .addOnFailureListener(e -> Executors.newSingleThreadExecutor().execute(() -> {
                            nuevoDoctor.setEstadoSincronizacion("PENDIENTE");
                            db.doctorDao().insertar(nuevoDoctor);
                            requireActivity().runOnUiThread(() ->
                                    Toast.makeText(requireContext(), "Doctor guardado localmente (pendiente de sincronizar)", Toast.LENGTH_LONG).show()
                            );
                            requireActivity().getSupportFragmentManager().popBackStack();
                        }));
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
