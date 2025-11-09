package com.hadoga.hadoga.view.fragments;

import android.app.AlertDialog;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.hadoga.hadoga.R;
import com.hadoga.hadoga.model.database.HadogaDatabase;
import com.hadoga.hadoga.model.entities.Cita;
import com.hadoga.hadoga.model.entities.Paciente;
import com.hadoga.hadoga.model.entities.Sucursal;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class ListaCitasFragment extends Fragment {
    private HadogaDatabase db;
    private LinearLayout containerCitas;
    private Spinner spFiltroSucursal;

    private List<Sucursal> listaSucursales = new ArrayList<>();
    private List<Cita> listaCitas = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_listar_citas, container, false);

        initUI(view);
        cargarSucursales();
        cargarCitas(null);

        return view;
    }

    private void initUI(View view) {
        db = HadogaDatabase.getInstance(requireContext());
        containerCitas = view.findViewById(R.id.containerCitas);
        spFiltroSucursal = view.findViewById(R.id.spFiltroSucursal);

        spFiltroSucursal.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) {
                    cargarCitas(null);
                } else {
                    String codigoSucursal = listaSucursales.get(position - 1).getCodigoSucursal();
                    cargarCitas(codigoSucursal);
                }
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
                ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

                adapter.add("Todas las sucursales");
                for (Sucursal s : listaSucursales) {
                    adapter.add(s.getNombreSucursal());
                }
                spFiltroSucursal.setAdapter(adapter);
            });
        });
    }

    private void cargarCitas(@Nullable String codigoSucursal) {
        Executors.newSingleThreadExecutor().execute(() -> {
            if (codigoSucursal == null) {
                listaCitas = db.citaDao().obtenerTodas();
            } else {
                listaCitas = db.citaDao().obtenerPorSucursal(codigoSucursal);
            }

            requireActivity().runOnUiThread(this::mostrarCitas);
        });
    }

    private void mostrarCitas() {
        containerCitas.removeAllViews();

        if (listaCitas.isEmpty()) {
            TextView txt = new TextView(requireContext());
            txt.setText("No hay citas registradas.");
            txt.setTextColor(getResources().getColor(android.R.color.white));
            txt.setPadding(0, 16, 0, 0);
            containerCitas.addView(txt);
            return;
        }

        for (Cita cita : listaCitas) {
            View card = crearCardCita(cita);
            containerCitas.addView(card);
        }
    }

    private View crearCardCita(Cita cita) {
        View cardView = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_cita_card, containerCitas, false);

        TextView tvNombre = cardView.findViewById(R.id.tvNombrePacienteCita);
        TextView tvFecha = cardView.findViewById(R.id.tvFechaCita);
        TextView tvSucursal = cardView.findViewById(R.id.tvSucursalCita);
        TextView tvEstado = cardView.findViewById(R.id.tvEstadoCita);
        android.widget.Button btnEditar = cardView.findViewById(R.id.btnEditarCita);
        android.widget.Button btnBorrar = cardView.findViewById(R.id.btnBorrarCita);

        // Obtener el paciente
        Paciente paciente = db.pacienteDao().obtenerPorId(cita.getPacienteId());
        String nombreCompleto = (paciente != null)
                ? paciente.getNombre() + " " + paciente.getApellido()
                : "Paciente desconocido";
        tvNombre.setText(nombreCompleto);

        // Sucursal
        // Buscar sucursal correspondiente a la cita
        Sucursal sucursal = null;
        for (Sucursal s : listaSucursales) {
            if (s.getCodigoSucursal().equals(cita.getCodigoSucursalAsignada())) {
                sucursal = s;
                break;
            }
        }
        tvSucursal.setText("Sucursal: " + (sucursal != null ? sucursal.getNombreSucursal() : "N/D"));

        tvFecha.setText("Fecha: " + cita.getFechaHora());
        tvEstado.setText("Estado: " + cita.getEstado());

        // Editar
        btnEditar.setOnClickListener(v -> abrirFragmentEditarCita(cita));

        // Borrar
        btnBorrar.setOnClickListener(v -> borrarCita(cita));

        return cardView;
    }

    private void abrirFragmentEditarCita(Cita cita) {
        Bundle bundle = new Bundle();
        bundle.putSerializable("citaData", cita);

        NuevaCitaFragment fragment = new NuevaCitaFragment();
        fragment.setArguments(bundle);

        requireActivity().getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .addToBackStack(null)
                .commit();
    }

    private void borrarCita(Cita cita) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Confirmar eliminación")
                .setMessage("¿Seguro que deseas eliminar la cita del " + cita.getFechaHora() + "?")
                .setPositiveButton("Eliminar", (dialog, which) -> eliminarCitaDeBD(cita))
                .setNegativeButton("Cancelar", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void eliminarCitaDeBD(Cita cita) {
        Executors.newSingleThreadExecutor().execute(() -> {
            boolean hayConexion = com.hadoga.hadoga.utils.NetworkUtils.isNetworkAvailable(requireContext());

            if (hayConexion) {
                eliminarCitaEnFirebase(cita);
            } else {
                cita.setEstadoSincronizacion("ELIMINADO_PENDIENTE");
                db.citaDao().actualizar(cita);
            }

            requireActivity().runOnUiThread(() -> {
                showSnackbarLikeToast("Cita eliminada correctamente.", false);
                cargarCitas(null);
            });
        });
    }

    private void eliminarCitaEnFirebase(Cita cita) {
        com.google.firebase.firestore.FirebaseFirestore firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance();

        firestore.collection("citas")
                .document(cita.getIdFirebase())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Executors.newSingleThreadExecutor().execute(() -> {
                        db.citaDao().eliminar(cita);
                    });
                })
                .addOnFailureListener(e -> {
                    Executors.newSingleThreadExecutor().execute(() -> {
                        cita.setEstadoSincronizacion("ELIMINADO_PENDIENTE");
                        db.citaDao().actualizar(cita);
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
