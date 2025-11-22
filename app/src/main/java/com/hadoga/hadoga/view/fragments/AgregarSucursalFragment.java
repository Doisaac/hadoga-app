package com.hadoga.hadoga.view.fragments;

import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.firebase.firestore.FirebaseFirestore;
import com.hadoga.hadoga.R;
import com.hadoga.hadoga.model.database.HadogaDatabase;
import com.hadoga.hadoga.model.entities.Sucursal;
import com.hadoga.hadoga.utils.FirebaseService;
import com.hadoga.hadoga.utils.NetworkUtils;

import java.util.HashMap;
import java.util.Map;
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

        // Si el fragment viene con datos (modo edición)
        if (getArguments() != null && getArguments().containsKey("sucursalData")) {
            Sucursal sucursal = (Sucursal) getArguments().getSerializable("sucursalData");
            cargarDatosSucursal(view, sucursal);
        }

        return view;
    }

    private void cargarDatosSucursal(View view, Sucursal s) {
        etNombreSucursal.setText(s.getNombreSucursal());
        etCodigoSucursal.setText(s.getCodigoSucursal());
        etDireccion.setText(s.getDireccionCompleta());
        etTelefono.setText(s.getTelefono());
        etCorreo.setText(s.getCorreo());
        etCodigoSucursal.setEnabled(false);

        ArrayAdapter<CharSequence> adapter = (ArrayAdapter<CharSequence>) spDepartamento.getAdapter();
        if (adapter != null) {
            int position = adapter.getPosition(s.getDepartamento());
            if (position >= 0) spDepartamento.setSelection(position);
        }

        Button btnGuardar = view.findViewById(R.id.btnGuardarSucursal);
        btnGuardar.setText("Actualizar Sucursal");

        btnGuardar.setOnClickListener(v -> actualizarSucursal(s.getId()));
    }


    private void actualizarSucursal(int idSucursal) {
        String nombre = etNombreSucursal.getText().toString().trim();
        String codigo = etCodigoSucursal.getText().toString().trim();
        String depto = spDepartamento.getSelectedItem() != null ? spDepartamento.getSelectedItem().toString() : "";
        String direccion = etDireccion.getText().toString().trim();
        String telefono = etTelefono.getText().toString().trim();
        String correo = etCorreo.getText().toString().trim();

        // Validaciones
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
            showSnackbarLikeToast("Selecciona un departamento.", true);
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
            etCorreo.setError("El correo es obligatorio");
            etCorreo.requestFocus();
            return;
        }

        if (!telefono.matches("\\d{8,12}")) {
            etTelefono.setError("Solo números (8–12 dígitos)");
            etTelefono.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
            etCorreo.setError("Correo inválido");
            etCorreo.requestFocus();
            return;
        }

        // Crear objeto actualizado
        Sucursal actualizada = new Sucursal(nombre, codigo, depto, direccion, telefono, correo);
        actualizada.setId(idSucursal);

        // Verificar conexión
        if (!NetworkUtils.isNetworkAvailable(requireContext())) {
            // Sin conexión lo va a guardar localmente como pendiente
            actualizada.setEstadoSincronizacion("PENDIENTE");

            Executors.newSingleThreadExecutor().execute(() -> {
                db.sucursalDao().update(actualizada);
                requireActivity().runOnUiThread(() -> {
                    showSnackbarLikeToast("Sin conexión. Sucursal actualizada localmente.", null);
                    requireActivity().getSupportFragmentManager().popBackStack();
                });
            });
            return;
        }

        // Con conexión actualiza ambos
        FirebaseFirestore firestore = FirebaseService.getInstance();

        Map<String, Object> data = new HashMap<>();
        data.put("nombreSucursal", nombre);
        data.put("codigoSucursal", codigo);
        data.put("departamento", depto);
        data.put("direccionCompleta", direccion);
        data.put("telefono", telefono);
        data.put("correo", correo);
        data.put("estado_sincronizacion", "SINCRONIZADO");

        firestore.collection("sucursales")
                .document(codigo)
                .set(data)
                .addOnSuccessListener(aVoid -> {
                    actualizada.setEstadoSincronizacion("SINCRONIZADO");
                    Executors.newSingleThreadExecutor().execute(() -> db.sucursalDao().update(actualizada));

                    requireActivity().runOnUiThread(() -> {
                        showSnackbarLikeToast("Sucursal actualizada y sincronizada correctamente.", false);
                        requireActivity().getSupportFragmentManager().popBackStack();
                    });
                })
                .addOnFailureListener(e -> {
                    actualizada.setEstadoSincronizacion("PENDIENTE");
                    Executors.newSingleThreadExecutor().execute(() -> db.sucursalDao().update(actualizada));

                    requireActivity().runOnUiThread(() -> {
                        showSnackbarLikeToast("Error al sincronizar. Sucursal guardada localmente.", null);
                        requireActivity().getSupportFragmentManager().popBackStack();
                    });
                });
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

        // Validaciones
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
        // Crear objeto sucursal
        Sucursal sucursal = new Sucursal(nombre, codigo, depto, direccion, telefono, correo);

        // Sin conexión
        if (!NetworkUtils.isNetworkAvailable(requireContext())) {
            // Sin conexión → guardar localmente con estado pendiente
            sucursal.setEstadoSincronizacion("PENDIENTE");
            Executors.newSingleThreadExecutor().execute(() -> {
                db.sucursalDao().insert(sucursal);
                requireActivity().runOnUiThread(() -> {
                    showSnackbarLikeToast("Sin conexión. Sucursal guardada localmente.", null);
                    requireActivity().getSupportFragmentManager().popBackStack();
                });
            });
            return;
        }

        // Con conexión → crear también en Firestore
        FirebaseFirestore firestore = FirebaseService.getInstance();

        Map<String, Object> data = new HashMap<>();
        data.put("nombreSucursal", nombre);
        data.put("codigoSucursal", codigo);
        data.put("departamento", depto);
        data.put("direccionCompleta", direccion);
        data.put("telefono", telefono);
        data.put("correo", correo);
        data.put("estado_sincronizacion", "SINCRONIZADO");

        firestore.collection("sucursales")
                .document(codigo)
                .set(data)
                .addOnSuccessListener(aVoid -> {
                    sucursal.setEstadoSincronizacion("SINCRONIZADO");
                    Executors.newSingleThreadExecutor().execute(() -> db.sucursalDao().insert(sucursal));

                    requireActivity().runOnUiThread(() -> {
                        showSnackbarLikeToast("Sucursal guardada y sincronizada correctamente.", false);
                        requireActivity().getSupportFragmentManager().popBackStack();
                    });
                })
                .addOnFailureListener(e -> {
                    sucursal.setEstadoSincronizacion("PENDIENTE");
                    Executors.newSingleThreadExecutor().execute(() -> db.sucursalDao().insert(sucursal));

                    requireActivity().runOnUiThread(() -> {
                        showSnackbarLikeToast("Error al sincronizar. Guardada localmente.", null);
                        requireActivity().getSupportFragmentManager().popBackStack();
                    });
                });
    }

    private void showSnackbarLikeToast(String message, Boolean isError) {
        LayoutInflater inflater = getLayoutInflater();
        View layout = inflater.inflate(R.layout.custom_toast, null);

        LinearLayout container = layout.findViewById(R.id.toast_container);
        TextView text = layout.findViewById(R.id.toast_message);
        ImageView icon = layout.findViewById(R.id.toast_icon);

        text.setText(message);

        int color;
        int iconRes;

        if (isError == null) {
            color = ContextCompat.getColor(requireContext(), R.color.colorWarning);
            iconRes = R.drawable.ic_check_circle;
        } else if (isError) {
            color = ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark);
            iconRes = R.drawable.ic_error;
        } else {
            color = ContextCompat.getColor(requireContext(), R.color.colorBlue);
            iconRes = R.drawable.ic_check_circle;
        }

        icon.setImageResource(iconRes);

        GradientDrawable background = new GradientDrawable();
        background.setCornerRadius(24f);
        background.setColor(color);
        container.setBackground(background);

        Toast toast = new Toast(requireContext().getApplicationContext());
        toast.setDuration(Toast.LENGTH_LONG);
        toast.setView(layout);
        toast.setGravity(Gravity.BOTTOM, 0, 120);
        toast.show();
    }

}
