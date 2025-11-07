package com.hadoga.hadoga.view.fragments;

import android.app.AlertDialog;
import android.net.Uri;
import android.os.Bundle;
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
import androidx.fragment.app.Fragment;

import com.google.firebase.firestore.FirebaseFirestore;
import com.hadoga.hadoga.R;
import com.hadoga.hadoga.model.database.HadogaDatabase;
import com.hadoga.hadoga.model.entities.Paciente;
import com.hadoga.hadoga.model.entities.Sucursal;
import com.hadoga.hadoga.utils.FirebaseService;
import com.hadoga.hadoga.utils.NetworkUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class ListaPacientesFragment extends Fragment {
    private HadogaDatabase db;
    private LinearLayout containerPacientes;
    private Spinner spFiltroSucursal;
    private List<Sucursal> listaSucursales = new ArrayList<>();
    private List<Paciente> listaPacientes = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_lista_pacientes, container, false);

        initUI(view);
        cargarSucursales();
        cargarPacientes(null);

        return view;
    }

    private void initUI(View view) {
        db = HadogaDatabase.getInstance(requireContext());
        containerPacientes = view.findViewById(R.id.containerPacientes);
        spFiltroSucursal = view.findViewById(R.id.spFiltroSucursal);

        // Listener del spinner
        spFiltroSucursal.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) {
                    cargarPacientes(null);
                } else {
                    String codigoSucursal = listaSucursales.get(position - 1).getCodigoSucursal();
                    cargarPacientes(codigoSucursal);
                }
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
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

    private void cargarPacientes(@Nullable String codigoSucursal) {
        Executors.newSingleThreadExecutor().execute(() -> {
            if (codigoSucursal == null) {
                listaPacientes = db.pacienteDao().obtenerTodos();
            } else {
                listaPacientes = db.pacienteDao().obtenerPorSucursal(codigoSucursal);
            }

            requireActivity().runOnUiThread(this::mostrarPacientes);
        });
    }

    private void mostrarPacientes() {
        containerPacientes.removeAllViews();

        if (listaPacientes.isEmpty()) {
            TextView txt = new TextView(requireContext());
            txt.setText("No hay pacientes registrados.");
            txt.setTextColor(getResources().getColor(android.R.color.white));
            txt.setPadding(0, 16, 0, 0);
            containerPacientes.addView(txt);
            return;
        }

        for (Paciente p : listaPacientes) {
            View card = crearCardPaciente(p);
            containerPacientes.addView(card);
        }
    }

    private View crearCardPaciente(Paciente paciente) {
        View cardView = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_paciente_card, containerPacientes, false);

        ImageView ivFoto = cardView.findViewById(R.id.ivFotoPacienteCard);
        TextView tvNombre = cardView.findViewById(R.id.tvNombrePaciente);
        TextView tvSucursal = cardView.findViewById(R.id.tvSucursalPaciente);
        android.widget.Button btnEditar = cardView.findViewById(R.id.btnEditarPaciente);
        android.widget.Button btnBorrar = cardView.findViewById(R.id.btnBorrarPaciente);

        tvNombre.setText(paciente.getNombre() + " " + paciente.getApellido());

        // Mostrar nombre de la sucursal
        Sucursal sucursal = null;
        for (Sucursal s : listaSucursales) {
            if (s.getCodigoSucursal().equals(paciente.getCodigoSucursalAsignada())) {
                sucursal = s;
                break;
            }
        }
        tvSucursal.setText(sucursal != null ? "Sucursal " + sucursal.getNombreSucursal() : "Sin sucursal");

        // Mostrar imagen (si tiene URI válida)
        if (paciente.getFotoUri() != null && !paciente.getFotoUri().isEmpty()) {
            try {
                File file = new File(Uri.parse(paciente.getFotoUri()).getPath());
                if (file.exists()) {
                    ivFoto.setImageURI(Uri.fromFile(file));
                } else {
                    ivFoto.setImageResource(R.drawable.ic_user_placeholder);
                }
            } catch (Exception e) {
                ivFoto.setImageResource(R.drawable.ic_user_placeholder);
            }
        } else {
            ivFoto.setImageResource(R.drawable.ic_user_placeholder);
        }

        // Editar
        btnEditar.setOnClickListener(v -> abrirFragmentEditarPaciente(paciente));

        // Borrar
        btnBorrar.setOnClickListener(v -> borrarPaciente(paciente));

        return cardView;
    }

    private void abrirFragmentEditarPaciente(Paciente paciente) {
        Bundle bundle = new Bundle();
        bundle.putSerializable("pacienteData", paciente);

        AgregarPacienteFragment fragment = new AgregarPacienteFragment();
        fragment.setArguments(bundle);

        requireActivity().getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .addToBackStack(null)
                .commit();
    }

    private void borrarPaciente(Paciente paciente) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Confirmar eliminación")
                .setMessage("¿Seguro que deseas eliminar al paciente \"" + paciente.getNombre() + " " + paciente.getApellido() + "\"?")
                .setPositiveButton("Eliminar", (dialog, which) -> eliminarPacienteDeBD(paciente))
                .setNegativeButton("Cancelar", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void eliminarPacienteDeBD(Paciente paciente) {
        // Sin conexión
        if (!NetworkUtils.isNetworkAvailable(requireContext())) {
            Executors.newSingleThreadExecutor().execute(() -> {
                paciente.setEstadoSincronizacion("ELIMINADO_PENDIENTE");
                db.pacienteDao().actualizar(paciente);
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(),
                            "Sin conexión. Eliminación pendiente de sincronizar.",
                            Toast.LENGTH_LONG).show();
                    cargarPacientes(null);
                });
            });
            return;
        }

        // Con conexión: eliminar de Firestore y localmente
        FirebaseFirestore firestore = FirebaseService.getInstance();
        firestore.collection("pacientes")
                .document(paciente.getCorreoElectronico())
                .delete()
                .addOnSuccessListener(aVoid -> Executors.newSingleThreadExecutor().execute(() -> {
                    db.pacienteDao().eliminar(paciente);
                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(),
                                "Paciente eliminado correctamente.",
                                Toast.LENGTH_SHORT).show();
                        cargarPacientes(null);
                    });
                }))
                .addOnFailureListener(e -> Executors.newSingleThreadExecutor().execute(() -> {
                    paciente.setEstadoSincronizacion("ELIMINADO_PENDIENTE");
                    db.pacienteDao().actualizar(paciente);
                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(),
                                "Error al eliminar en la nube. Eliminación pendiente.",
                                Toast.LENGTH_LONG).show();
                        cargarPacientes(null);
                    });
                }));
    }
}
