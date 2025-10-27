package com.hadoga.hadoga.view.fragments;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
                    int sucursalId = listaSucursales.get(position - 1).getId();
                    cargarCitas(sucursalId);
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

    private void cargarCitas(@Nullable Integer sucursalId) {
        Executors.newSingleThreadExecutor().execute(() -> {
            if (sucursalId == null) {
                listaCitas = db.citaDao().obtenerTodas();
            } else {
                listaCitas = db.citaDao().obtenerPorSucursal(sucursalId);
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
        Sucursal sucursal = null;
        for (Sucursal s : listaSucursales) {
            if (s.getId() == cita.getSucursalId()) {
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
            db.citaDao().eliminar(cita);

            requireActivity().runOnUiThread(() -> {
                Toast.makeText(requireContext(),
                        "Cita eliminada correctamente",
                        Toast.LENGTH_SHORT).show();
                cargarCitas(null);
            });
        });
    }
}
