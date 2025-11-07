package com.hadoga.hadoga.view.fragments;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.hadoga.hadoga.R;
import com.hadoga.hadoga.model.database.HadogaDatabase;
import com.hadoga.hadoga.model.entities.Cita;
import com.hadoga.hadoga.model.entities.Paciente;
import com.hadoga.hadoga.model.entities.Sucursal;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

public class NuevaCitaFragment extends Fragment {
    private HadogaDatabase db;
    private Spinner spSucursal, spPaciente, spEstado;
    private EditText etFechaHora, etMotivo, etNotas;
    private Button btnCrearCita;

    private List<Sucursal> sucursales = new ArrayList<>();
    private List<Paciente> pacientes = new ArrayList<>();

    private boolean isEditingMode = false;
    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        db = HadogaDatabase.getInstance(requireContext());
        View view = inflater.inflate(R.layout.fragment_nueva_cita, container, false);

        initUI(view);
        initListeners();

        // Modo edición
        if (getArguments() != null && getArguments().containsKey("citaData")) {
            Cita cita = (Cita) getArguments().getSerializable("citaData");
            cargarDatosCita(view, cita);
        }

        return view;
    }

    private void initUI(View view) {
        spSucursal = view.findViewById(R.id.spSucursal);
        spPaciente = view.findViewById(R.id.spPaciente);
        spEstado = view.findViewById(R.id.spEstado);

        etFechaHora = view.findViewById(R.id.etFechaHora);
        etMotivo = view.findViewById(R.id.etMotivo);
        etNotas = view.findViewById(R.id.etNotas);

        btnCrearCita = view.findViewById(R.id.btnCrearCita);

        // Estado
        ArrayAdapter<String> adapterEstado = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                Arrays.asList("pendiente", "completada", "cancelada")
        );
        adapterEstado.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spEstado.setAdapter(adapterEstado);

        // Cargar sucursales
        Executors.newSingleThreadExecutor().execute(() -> {
            sucursales = db.sucursalDao().getAllSucursales();
            requireActivity().runOnUiThread(() -> {
                ArrayAdapter<String> adSuc = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item);
                adSuc.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

                //  Item por defecto
                adSuc.add("Selecciona una sucursal");

                //  Agregar sucursales reales
                for (Sucursal s : sucursales) adSuc.add(s.getNombreSucursal());

                spSucursal.setAdapter(adSuc);
                spSucursal.setSelection(0);

                // Listener para cargar pacientes al elegir sucursal
                spSucursal.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        // Reiniciar pacientes cada vez que se cambia sucursal
                        ArrayAdapter<String> adapterPacientes = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item);
                        adapterPacientes.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        adapterPacientes.add("Selecciona un paciente");
                        spPaciente.setAdapter(adapterPacientes);
                        pacientes.clear();

                        // Solo cargar si realmente se elige una sucursal valida
                        if (position > 0) {
                            String codigoSucursal = sucursales.get(position - 1).getCodigoSucursal();
                            cargarPacientesPorSucursal(codigoSucursal);
                        }
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) { }
                });


            });
        });
    }

    private void initListeners() {
        // Picker de fecha y hora (no manual)
        etFechaHora.setFocusable(false);
        etFechaHora.setOnClickListener(v -> abrirPickersFechaHora());

        btnCrearCita.setOnClickListener(v -> guardarCita());
    }

    private void cargarPacientesPorSucursal(String codigoSucursal) {
        cargarPacientesPorSucursal(codigoSucursal, -1);
    }

    // Sobrecarga del método: permite cargar pacientes y, opcionalmente, seleccionar uno específico
    private void cargarPacientesPorSucursal(String codigoSucursal, int pacienteSeleccionadoId) {
        Executors.newSingleThreadExecutor().execute(() -> {
            pacientes = db.pacienteDao().obtenerPorSucursal(codigoSucursal);
            requireActivity().runOnUiThread(() -> {
                ArrayAdapter<String> ad = new ArrayAdapter<String>(requireContext(), android.R.layout.simple_spinner_item) {
                    @Override
                    public View getView(int position, View convertView, ViewGroup parent) {
                        View view = super.getView(position, convertView, parent);
                        ((android.widget.TextView) view).setTextColor(getResources().getColor(android.R.color.white));
                        return view;
                    }

                    @Override
                    public View getDropDownView(int position, View convertView, ViewGroup parent) {
                        View view = super.getDropDownView(position, convertView, parent);
                        ((android.widget.TextView) view).setTextColor(getResources().getColor(android.R.color.white));
                        return view;
                    }
                };

                ad.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

                ad.add("Selecciona un paciente");
                int posSel = 0;

                for (int i = 0; i < pacientes.size(); i++) {
                    Paciente p = pacientes.get(i);
                    ad.add(p.getNombre() + " " + p.getApellido());
                    if (p.getId() == pacienteSeleccionadoId) posSel = i + 1; // +1 por placeholder
                }

                spPaciente.setAdapter(ad);
                spPaciente.setSelection(posSel);
            });
        });
    }
    private void abrirPickersFechaHora() {
        final Calendar cal = Calendar.getInstance();

        DatePickerDialog dp = new DatePickerDialog(requireContext(),
                (view, year, month, dayOfMonth) -> {
                    // Después de escoger la fecha, abrimos el time picker
                    TimePickerDialog tp = new TimePickerDialog(requireContext(),
                            (v, hourOfDay, minute) -> {
                                // Formateo YYYY-MM-DD HH:MM (con cero a la izquierda)
                                String fecha = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth);
                                String hora = String.format(Locale.US, "%02d:%02d", hourOfDay, minute);
                                etFechaHora.setText(fecha + " " + hora);
                            },
                            cal.get(Calendar.HOUR_OF_DAY),
                            cal.get(Calendar.MINUTE),
                            true);
                    tp.show();
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
        );
        dp.show();
    }

    private void guardarCita() {
        // Validaciones
        if (spSucursal.getSelectedItemPosition() == 0) {
            Toast.makeText(requireContext(), "Selecciona una sucursal", Toast.LENGTH_SHORT).show();
            return;
        }
        if (spPaciente.getSelectedItemPosition() == 0) {
            Toast.makeText(requireContext(), "Selecciona un paciente", Toast.LENGTH_SHORT).show();
            return;
        }


        String fechaHora = etFechaHora.getText().toString().trim();
        String motivo = etMotivo.getText().toString().trim();
        String notas = etNotas.getText().toString().trim();
        String estadoSel = spEstado.getSelectedItem() != null ? spEstado.getSelectedItem().toString() : "pendiente";

        if (TextUtils.isEmpty(fechaHora)) {
            etFechaHora.setError("Selecciona fecha y hora");
            etFechaHora.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(motivo)) {
            etMotivo.setError("El motivo es obligatorio");
            etMotivo.requestFocus();
            return;
        }

        // Obtener IDs (con validación y ajuste de índice)
        int posSuc = spSucursal.getSelectedItemPosition();
        int posPac = spPaciente.getSelectedItemPosition();

        // Validar que no se haya dejado el placeholder seleccionado
        if (posSuc <= 0) {
            Toast.makeText(requireContext(), "Selecciona una sucursal válida", Toast.LENGTH_SHORT).show();
            return;
        }
        if (posPac <= 0) {
            Toast.makeText(requireContext(), "Selecciona un paciente válido", Toast.LENGTH_SHORT).show();
            return;
        }

        String codigoSucursal = sucursales.get(posSuc - 1).getCodigoSucursal();
        int pacienteId = pacientes.get(posPac - 1).getId();


        // Regla de solapamiento (solo aplica si estado = pendiente)
        Executors.newSingleThreadExecutor().execute(() -> {
            if (estadoSel.equals("pendiente")) {
                String desde, hasta;
                try {
                    Date seleccion = sdf.parse(fechaHora);
                    if (seleccion == null) throw new ParseException("fecha nula", 0);

                    long t = seleccion.getTime();
                    // los 30 minutos de cita
                    long rango30 = 30L * 60L * 1000L;

                    Date dDesde = new Date(t - rango30);
                    Date dHasta = new Date(t + rango30);

                    desde = sdf.format(dDesde);
                    hasta = sdf.format(dHasta);
                } catch (ParseException e) {
                    requireActivity().runOnUiThread(() ->
                            Toast.makeText(requireContext(), "Fecha/hora inválida", Toast.LENGTH_SHORT).show());
                    return;
                }

                int conflictos = db.citaDao().contarSolapadas(codigoSucursal, desde, hasta);
                if (conflictos > 0) {
                    requireActivity().runOnUiThread(() ->
                            Toast.makeText(requireContext(),
                                    "Ya existe una cita en ese rango de 30 minutos.",
                                    Toast.LENGTH_LONG).show());
                    return;
                }
            }

            // Insertar o actualizar si estuviera en modo edición
            String idFirebase = java.util.UUID.randomUUID().toString();

            Cita nueva = new Cita(idFirebase, codigoSucursal, pacienteId, fechaHora, motivo, notas, estadoSel);

            try {
                db.citaDao().insertar(nueva);
                subirCitaAFirebase(nueva);
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), "Cita creada exitosamente", Toast.LENGTH_SHORT).show();
                    requireActivity().getSupportFragmentManager().popBackStack();
                });
            } catch (Exception e) {
                e.printStackTrace();
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(requireContext(), "Error al crear la cita", Toast.LENGTH_SHORT).show());
            }
        });
    }
    private void cargarDatosCita(View view, Cita c) {
        isEditingMode = true; // evita recargas innecesarias

        // Estado
        ArrayAdapter<String> adEstado = (ArrayAdapter<String>) spEstado.getAdapter();
        if (adEstado != null) {
            int pos = adEstado.getPosition(c.getEstado());
            if (pos >= 0) spEstado.setSelection(pos);
        }

        etFechaHora.setText(c.getFechaHora());
        etMotivo.setText(c.getMotivo());
        etNotas.setText(c.getNotas());

        // Cargar sucursales
        Executors.newSingleThreadExecutor().execute(() -> {
            sucursales = db.sucursalDao().getAllSucursales();

            requireActivity().runOnUiThread(() -> {
                ArrayAdapter<String> adSuc = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item);
                adSuc.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

                adSuc.add("Selecciona una sucursal");
                int posSuc = 0;

                for (int i = 0; i < sucursales.size(); i++) {
                    adSuc.add(sucursales.get(i).getNombreSucursal());
                    if (sucursales.get(i).getCodigoSucursal().equals(c.getCodigoSucursalAsignada())) posSuc = i + 1;
                }

                spSucursal.setAdapter(adSuc);
                spSucursal.setSelection(posSuc);

                // Pacientes de esa sucursal
                cargarPacientesPorSucursal(c.getCodigoSucursalAsignada(), c.getPacienteId());

                // Listener
                spSucursal.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        if (isEditingMode) {
                            // Evitar recarga inmediata al entrar
                            isEditingMode = false;
                            return;
                        }

                        if (position > 0) {
                            String codigoSucursal = sucursales.get(position - 1).getCodigoSucursal();
                            cargarPacientesPorSucursal(codigoSucursal);
                        } else {
                            ArrayAdapter<String> vacio = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item);
                            vacio.add("Selecciona un paciente");
                            spPaciente.setAdapter(vacio);
                            pacientes.clear();
                        }
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) { }
                });

                btnCrearCita.setText("Actualizar Cita");
                btnCrearCita.setOnClickListener(v -> actualizarCita(c.getId()));
            });
        });
    }
    private void actualizarCita(int idCita) {
        // Validaciones de selección
        if (spSucursal.getSelectedItemPosition() <= 0) {
            Toast.makeText(requireContext(), "Selecciona la sucursal", Toast.LENGTH_SHORT).show();
            return;
        }
        if (spPaciente.getSelectedItemPosition() <= 0) {
            Toast.makeText(requireContext(), "Selecciona el paciente", Toast.LENGTH_SHORT).show();
            return;
        }

        // Obtener datos de campos
        String fechaHora = etFechaHora.getText().toString().trim();
        String motivo = etMotivo.getText().toString().trim();
        String notas = etNotas.getText().toString().trim();
        String estadoSel = spEstado.getSelectedItem() != null
                ? spEstado.getSelectedItem().toString()
                : "pendiente";

        // Validaciones básicas
        if (TextUtils.isEmpty(fechaHora)) {
            etFechaHora.setError("Selecciona fecha y hora");
            etFechaHora.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(motivo)) {
            etMotivo.setError("El motivo es obligatorio");
            etMotivo.requestFocus();
            return;
        }

        // Obtener IDs considerando el placeholder
        String codigoSucursal = sucursales.get(spSucursal.getSelectedItemPosition() - 1).getCodigoSucursal();
        int pacienteId = pacientes.get(spPaciente.getSelectedItemPosition() - 1).getId();

        Executors.newSingleThreadExecutor().execute(() -> {
            // Validar solapamiento solo si el estado es pendiente
            if (estadoSel.equals("pendiente")) {
                try {
                    Date seleccion = sdf.parse(fechaHora);
                    if (seleccion == null) throw new ParseException("fecha nula", 0);

                    long t = seleccion.getTime();
                    long rango30 = 30L * 60L * 1000L;

                    String desde = sdf.format(new Date(t - rango30));
                    String hasta = sdf.format(new Date(t + rango30));

                    int conflictos = db.citaDao().contarSolapadasExcluyendo(idCita, codigoSucursal, desde, hasta);
                    if (conflictos > 0) {
                        requireActivity().runOnUiThread(() ->
                                Toast.makeText(requireContext(),
                                        "Ya existe una cita en ese rango de 30 minutos.",
                                        Toast.LENGTH_LONG).show());
                        return;
                    }
                } catch (ParseException e) {
                    requireActivity().runOnUiThread(() ->
                            Toast.makeText(requireContext(), "Fecha/hora inválida", Toast.LENGTH_SHORT).show());
                    return;
                }
            }

            // Crear objeto actualizado
            Cita original = db.citaDao().obtenerPorId(idCita);
            Cita act = new Cita(original.getIdFirebase(), codigoSucursal, pacienteId, fechaHora, motivo, notas, estadoSel);
            act.setId(idCita);

            try {
                db.citaDao().actualizar(act);
                subirCitaAFirebase(act);
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), "Cita actualizada exitosamente", Toast.LENGTH_SHORT).show();
                    requireActivity().getSupportFragmentManager().popBackStack();
                });
            } catch (Exception e) {
                e.printStackTrace();
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(requireContext(), "Error al actualizar la cita", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void subirCitaAFirebase(Cita cita) {
        com.google.firebase.firestore.FirebaseFirestore firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance();

        firestore.collection("citas")
                .document(cita.getIdFirebase())
                .set(cita)
                .addOnSuccessListener(aVoid -> {
                    Executors.newSingleThreadExecutor().execute(() -> {
                        cita.setEstadoSincronizacion("SINCRONIZADO");
                        db.citaDao().actualizar(cita);
                    });
                })
                .addOnFailureListener(e -> {
                    Executors.newSingleThreadExecutor().execute(() -> {
                        cita.setEstadoSincronizacion("PENDIENTE");
                        db.citaDao().actualizar(cita);
                    });
                });
    }

}
