package com.hadoga.hadoga.view.fragments;

import android.app.AlertDialog;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
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
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.hadoga.hadoga.R;
import com.hadoga.hadoga.model.database.HadogaDatabase;
import com.hadoga.hadoga.model.entities.Doctor;
import com.hadoga.hadoga.model.entities.Sucursal;
import com.hadoga.hadoga.utils.FirebaseService;
import com.hadoga.hadoga.utils.NetworkUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class ListaDoctoresFragment extends Fragment {
    private LinearLayout containerDoctores;
    private Spinner spFiltroSucursal;
    private HadogaDatabase db;

    private List<Sucursal> listaSucursales = new ArrayList<>();
    private List<Doctor> listaDoctores = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_lista_doctores, container, false);

        initUI(view);
        cargarSucursales();
        cargarDoctores();

        return view;
    }

    private void initUI(View view) {
        containerDoctores = view.findViewById(R.id.containerDoctores);
        spFiltroSucursal = view.findViewById(R.id.spFiltroSucursal);
        db = HadogaDatabase.getInstance(requireContext());

        spFiltroSucursal.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                filtrarDoctoresPorSucursal();
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
            }
        });
    }

    private void cargarSucursales() {
        Executors.newSingleThreadExecutor().execute(() -> {
            listaSucursales = db.sucursalDao().getAllSucursales();

            requireActivity().runOnUiThread(() -> {
                List<String> nombresSucursales = new ArrayList<>();
                nombresSucursales.add("Todas las sucursales"); // opción general
                for (Sucursal s : listaSucursales) {
                    nombresSucursales.add(s.getNombreSucursal());
                }

                ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, nombresSucursales);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spFiltroSucursal.setAdapter(adapter);
            });
        });
    }

    private void cargarDoctores() {
        Executors.newSingleThreadExecutor().execute(() -> {
            listaDoctores = db.doctorDao().obtenerTodos();
            requireActivity().runOnUiThread(() -> mostrarDoctores(listaDoctores));
        });
    }

    private void filtrarDoctoresPorSucursal() {
        String nombreSucursalSeleccionada = (String) spFiltroSucursal.getSelectedItem();

        if (nombreSucursalSeleccionada == null || nombreSucursalSeleccionada.equals("Todas las sucursales")) {
            mostrarDoctores(listaDoctores);
            return;
        }

        // Buscar el código de sucursal según el nombre seleccionado
        String codigoSucursalSeleccionada = null;
        for (Sucursal s : listaSucursales) {
            if (s.getNombreSucursal().equals(nombreSucursalSeleccionada)) {
                codigoSucursalSeleccionada = s.getCodigoSucursal();
                break;
            }
        }

        if (codigoSucursalSeleccionada == null) {
            mostrarDoctores(new ArrayList<>());
            return;
        }

        // Filtrar doctores según el código de sucursal
        List<Doctor> filtrados = new ArrayList<>();
        for (Doctor d : listaDoctores) {
            if (d.getSucursalAsignada() != null && d.getSucursalAsignada().equals(codigoSucursalSeleccionada)) {
                filtrados.add(d);
            }
        }

        mostrarDoctores(filtrados);
    }

    private void mostrarDoctores(List<Doctor> lista) {
        containerDoctores.removeAllViews();

        if (lista.isEmpty()) {
            TextView txt = new TextView(requireContext());
            txt.setText("No hay doctores registrados.");
            txt.setTextColor(getResources().getColor(android.R.color.white));
            txt.setPadding(0, 16, 0, 0);
            containerDoctores.addView(txt);
            return;
        }

        for (Doctor doctor : lista) {
            View card = crearCardDoctor(doctor);
            containerDoctores.addView(card);
        }
    }

    private View crearCardDoctor(Doctor doctor) {
        View cardView = LayoutInflater.from(requireContext()).inflate(R.layout.item_doctor_card, containerDoctores, false);

        ImageView ivFoto = cardView.findViewById(R.id.ivFotoDoctorCard);
        TextView tvNombre = cardView.findViewById(R.id.tvNombreDoctor);
        TextView tvSucursal = cardView.findViewById(R.id.tvSucursalDoctor);
        Button btnEditar = cardView.findViewById(R.id.btnEditarDoctor);
        Button btnBorrar = cardView.findViewById(R.id.btnBorrarDoctor);

        // Mostrar datos
        tvNombre.setText(doctor.getNombre() + " " + doctor.getApellido());

        // Buscar nombre de sucursal por ID
        String nombreSucursal = obtenerNombreSucursal(doctor.getSucursalAsignada());
        tvSucursal.setText("Sucursal " + nombreSucursal);

        // Imagen (placeholder si no tiene URI)
        if (doctor.getFotoUri() != null && !doctor.getFotoUri().isEmpty()) {
            ivFoto.setImageURI(Uri.parse(doctor.getFotoUri()));
        } else {
            ivFoto.setImageResource(R.drawable.ic_user_placeholder);
        }

        // Editar
        btnEditar.setOnClickListener(v -> abrirFragmentEditarDoctor(doctor));

        // Borrar
        btnBorrar.setOnClickListener(v -> borrarDoctor(doctor));

        return cardView;
    }

    private String obtenerNombreSucursal(String codigoSucursal) {
        for (Sucursal s : listaSucursales) {
            if (s.getCodigoSucursal() != null && s.getCodigoSucursal().equals(codigoSucursal)) {
                return s.getNombreSucursal();
            }
        }
        return "Desconocida";
    }

    private void abrirFragmentEditarDoctor(Doctor doctor) {
        Bundle bundle = new Bundle();
        bundle.putSerializable("doctorData", doctor);

        AgregarDoctorFragment fragment = new AgregarDoctorFragment();
        fragment.setArguments(bundle);

        requireActivity().getSupportFragmentManager().beginTransaction().replace(R.id.fragmentContainer, fragment).addToBackStack(null).commit();

    }

    private void borrarDoctor(Doctor doctor) {
        new AlertDialog.Builder(requireContext()).setTitle("Confirmar eliminación").setMessage("¿Seguro que deseas eliminar al doctor \"" + doctor.getNombre() + " " + doctor.getApellido() + "\"?").setPositiveButton("Eliminar", (dialog, which) -> eliminarDoctorDeBD(doctor)).setNegativeButton("Cancelar", (dialog, which) -> dialog.dismiss()).show();
    }

    private void eliminarDoctorDeBD(Doctor doctor) {
        // Sin conexión se marca como pendiente de eliminación
        if (!NetworkUtils.isNetworkAvailable(requireContext())) {
            Executors.newSingleThreadExecutor().execute(() -> {
                doctor.setEstadoSincronizacion("ELIMINADO_PENDIENTE");
                db.doctorDao().actualizar(doctor);
                requireActivity().runOnUiThread(() -> {
                    showSnackbarLikeToast("Sin conexión. El doctor se eliminó localmente.", null);
                    cargarDoctores();
                });
            });
            return;
        }

        // Con conexión se elimina en Firestore y local
        FirebaseFirestore firestore = FirebaseService.getInstance();
        firestore.collection("doctores")
                .document(doctor.getNumeroColegiado())
                .delete()
                .addOnSuccessListener(aVoid -> Executors.newSingleThreadExecutor().execute(() -> {
                    db.doctorDao().eliminar(doctor);
                    requireActivity().runOnUiThread(() -> {
                        showSnackbarLikeToast("Doctor eliminado correctamente.", false);
                        cargarDoctores();
                    });
                }))
                .addOnFailureListener(e -> Executors.newSingleThreadExecutor().execute(() -> {
                    doctor.setEstadoSincronizacion("ELIMINADO_PENDIENTE");
                    db.doctorDao().actualizar(doctor);
                    requireActivity().runOnUiThread(() -> {
                        showSnackbarLikeToast("Error al eliminar en la nube. El doctor se eliminó localmente.", null);
                        cargarDoctores();
                    });
                }));
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

