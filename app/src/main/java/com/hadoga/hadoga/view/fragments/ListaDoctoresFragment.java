package com.hadoga.hadoga.view.fragments;

import android.app.AlertDialog;
import android.net.Uri;
import android.os.Bundle;
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
import androidx.fragment.app.Fragment;

import com.hadoga.hadoga.R;
import com.hadoga.hadoga.model.database.HadogaDatabase;
import com.hadoga.hadoga.model.entities.Doctor;
import com.hadoga.hadoga.model.entities.Sucursal;

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

            requireActivity().runOnUiThread(() -> {
                mostrarDoctores(listaDoctores);
            });
        });
    }

    private void filtrarDoctoresPorSucursal() {
        String sucursalSeleccionada = (String) spFiltroSucursal.getSelectedItem();

        if (sucursalSeleccionada == null || sucursalSeleccionada.equals("Todas las sucursales")) {
            mostrarDoctores(listaDoctores);
            return;
        }

        int idSucursalSeleccionada = -1;
        for (Sucursal s : listaSucursales) {
            if (s.getNombreSucursal().equals(sucursalSeleccionada)) {
                idSucursalSeleccionada = s.getId();
                break;
            }
        }

        if (idSucursalSeleccionada == -1) {
            mostrarDoctores(new ArrayList<>());
            return;
        }

        List<Doctor> filtrados = new ArrayList<>();
        for (Doctor d : listaDoctores) {
            if (d.getSucursalAsignada() == idSucursalSeleccionada) {
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

    private String obtenerNombreSucursal(int idSucursal) {
        for (Sucursal s : listaSucursales) {
            if (s.getId() == idSucursal) {
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
        Executors.newSingleThreadExecutor().execute(() -> {
            db.doctorDao().eliminar(doctor);

            requireActivity().runOnUiThread(() -> {
                Toast.makeText(requireContext(), "Doctor eliminado correctamente", Toast.LENGTH_SHORT).show();

                // Recargar lista
                cargarDoctores();
            });
        });
    }
}
