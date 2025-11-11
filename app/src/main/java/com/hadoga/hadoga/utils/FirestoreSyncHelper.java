package com.hadoga.hadoga.utils;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.hadoga.hadoga.R;
import com.hadoga.hadoga.model.database.HadogaDatabase;
import com.hadoga.hadoga.model.entities.Cita;
import com.hadoga.hadoga.model.entities.Doctor;
import com.hadoga.hadoga.model.entities.Paciente;
import com.hadoga.hadoga.model.entities.Sucursal;
import com.hadoga.hadoga.model.entities.Usuario;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

public class FirestoreSyncHelper {
    private final Context context;
    private final HadogaDatabase db;
    private final FirebaseFirestore firestore;

    public FirestoreSyncHelper(Context context) {
        this.context = context;
        this.db = HadogaDatabase.getInstance(context);
        this.firestore = FirebaseService.getInstance();
    }

    public void sincronizarTodo() {
        if (!NetworkUtils.isNetworkAvailable(context)) {
            showSnackbarLikeToast("Sin conexión. No se puede sincronizar.", true);
            return;
        }

        showSnackbarLikeToast("Sincronizando datos...", false);

        Executors.newSingleThreadExecutor().execute(() -> {
            sincronizarUsuarios(() -> {
                sincronizarSucursales(() -> {
                    sincronizarDoctores(() -> {
                        sincronizarPacientes(() -> {
                            sincronizarCitas(() -> {
                                new android.os.Handler(context.getMainLooper()).post(() ->
                                        showSnackbarLikeToast("Datos sincronizados correctamente.", false)
                                );
                            });
                        });
                    });
                });
            });
        });
    }

    private void sincronizarUsuarios(Runnable onComplete) {
        List<Usuario> pendientes = db.usuarioDao().getPendientes();
        for (Usuario u : pendientes) {
            Map<String, Object> data = new HashMap<>();
            data.put("nombreClinica", u.getNombreClinica());
            data.put("email", u.getEmail());
            data.put("contrasena", u.getContrasena());
            data.put("estado_sincronizacion", "SINCRONIZADO");

            firestore.collection("usuarios")
                    .document(u.getEmail())
                    .set(data)
                    .addOnSuccessListener(aVoid -> {
                        u.setEstadoSincronizacion("SINCRONIZADO");
                        Executors.newSingleThreadExecutor().execute(() -> db.usuarioDao().update(u));
                    });
        }

        firestore.collection("usuarios")
                .get()
                .addOnSuccessListener(query -> Executors.newSingleThreadExecutor().execute(() -> {
                    for (QueryDocumentSnapshot doc : query) {
                        String email = doc.getId();
                        Usuario local = db.usuarioDao().getUsuarioByEmail(email);
                        Usuario remoto = doc.toObject(Usuario.class);
                        remoto.setEstadoSincronizacion("SINCRONIZADO");

                        if (local == null) db.usuarioDao().insert(remoto);
                        else db.usuarioDao().update(remoto);
                    }
                    onComplete.run();
                }))
                .addOnFailureListener(e -> onComplete.run());
    }

    private void sincronizarSucursales(Runnable onComplete) {

        // Por si existe pendientes eliminarlas
        List<Sucursal> eliminadasPendientes = db.sucursalDao().getEliminadasPendientes();
        for (Sucursal s : eliminadasPendientes) {
            firestore.collection("sucursales")
                    .document(s.getCodigoSucursal())
                    .delete()
                    .addOnSuccessListener(aVoid -> Executors.newSingleThreadExecutor().execute(() -> {
                        db.sucursalDao().delete(s);
                    }))
                    .addOnFailureListener(e -> {
                        // Si falla la eliminación, mantenemos el estado para intentar luego
                    });
        }

        // Subir pendientes de sincronizar
        List<Sucursal> pendientes = db.sucursalDao().getPendientes();
        for (Sucursal s : pendientes) {
            Map<String, Object> data = new HashMap<>();
            data.put("nombreSucursal", s.getNombreSucursal());
            data.put("codigoSucursal", s.getCodigoSucursal());
            data.put("departamento", s.getDepartamento());
            data.put("direccionCompleta", s.getDireccionCompleta());
            data.put("telefono", s.getTelefono());
            data.put("correo", s.getCorreo());
            data.put("estado_sincronizacion", "SINCRONIZADO");

            firestore.collection("sucursales")
                    .document(s.getCodigoSucursal())
                    .set(data)
                    .addOnSuccessListener(aVoid -> {
                        s.setEstadoSincronizacion("SINCRONIZADO");
                        Executors.newSingleThreadExecutor().execute(() -> db.sucursalDao().update(s));
                    });
        }

        // Descargarlas todas de firebase
        firestore.collection("sucursales")
                .get()
                .addOnSuccessListener(query -> Executors.newSingleThreadExecutor().execute(() -> {
                    // Lista de códigos que existen en Firestore
                    List<String> codigosRemotos = new ArrayList<>();

                    for (QueryDocumentSnapshot doc : query) {
                        String codigo = doc.getString("codigoSucursal");
                        if (codigo != null) codigosRemotos.add(codigo);

                        Sucursal local = db.sucursalDao().getSucursalByCodigo(codigo);
                        Sucursal remoto = doc.toObject(Sucursal.class);
                        remoto.setEstadoSincronizacion("SINCRONIZADO");

                        if (local == null) {
                            remoto.setId(0);
                            db.sucursalDao().insert(remoto);
                        } else {
                            remoto.setId(local.getId());
                            db.sucursalDao().update(remoto);
                        }
                    }

                    // Elimina las que no existen en firebase, localmente
                    List<Sucursal> locales = db.sucursalDao().getAllSucursales();
                    for (Sucursal local : locales) {
                        if (!codigosRemotos.contains(local.getCodigoSucursal())) {
                            db.sucursalDao().delete(local);
                        }
                    }

                    onComplete.run();
                }))
                .addOnFailureListener(e -> onComplete.run());
    }

    private void sincronizarDoctores(Runnable onComplete) {
        // Eliminar pendientes de eliminar
        List<Doctor> eliminadosPendientes = db.doctorDao().getEliminadosPendientes();
        for (Doctor d : eliminadosPendientes) {
            firestore.collection("doctores")
                    .document(d.getNumeroColegiado())
                    .delete()
                    .addOnSuccessListener(aVoid -> Executors.newSingleThreadExecutor().execute(() -> {
                        db.doctorDao().eliminar(d);
                    }))
                    .addOnFailureListener(e -> {
                        // Mantiene como pendiente si falla la eliminación
                    });
        }

        // Subir pendientes de sincronizar
        List<Doctor> pendientes = db.doctorDao().getPendientes();
        for (Doctor d : pendientes) {
            Map<String, Object> data = new HashMap<>();
            data.put("nombre", d.getNombre());
            data.put("apellido", d.getApellido());
            data.put("fechaNacimiento", d.getFechaNacimiento());
            data.put("numeroColegiado", d.getNumeroColegiado());
            data.put("sexo", d.getSexo());
            data.put("especialidad", d.getEspecialidad());
            data.put("sucursalAsignada", d.getSucursalAsignada());
            data.put("fotoUri", d.getFotoUri());
            data.put("estado_sincronizacion", "SINCRONIZADO");

            firestore.collection("doctores")
                    .document(d.getNumeroColegiado())
                    .set(data)
                    .addOnSuccessListener(aVoid -> {
                        d.setEstadoSincronizacion("SINCRONIZADO");
                        Executors.newSingleThreadExecutor().execute(() -> db.doctorDao().actualizar(d));
                    });
        }

        // Descargar todos los doctores de firebase
        firestore.collection("doctores")
                .get()
                .addOnSuccessListener(query -> Executors.newSingleThreadExecutor().execute(() -> {
                    // Lista de colegiados que existen en Firestore
                    List<String> colegiadosRemotos = new ArrayList<>();

                    for (QueryDocumentSnapshot doc : query) {
                        String colegiado = doc.getString("numeroColegiado");
                        if (colegiado != null) colegiadosRemotos.add(colegiado);

                        Doctor local = db.doctorDao().getDoctorByColegiado(colegiado);
                        Doctor remoto = doc.toObject(Doctor.class);
                        remoto.setEstadoSincronizacion("SINCRONIZADO");

                        if (local == null) {
                            remoto.setId(0);
                            db.doctorDao().insertar(remoto);
                        } else {
                            remoto.setId(local.getId());
                            db.doctorDao().actualizar(remoto);
                        }
                    }

                    // Elimina localmente los que ya no existen en firebase
                    List<Doctor> locales = db.doctorDao().getAllDoctores();
                    for (Doctor local : locales) {
                        if (!colegiadosRemotos.contains(local.getNumeroColegiado())) {
                            db.doctorDao().eliminar(local);
                        }
                    }

                    onComplete.run();
                }))
                .addOnFailureListener(e -> onComplete.run());
    }

    private void sincronizarPacientes(Runnable onComplete) {
        List<Paciente> pendientes = db.pacienteDao().getPendientes();

        // Eliminar los pacientes marcados como "ELIMINADO_PENDIENTE"
        List<Paciente> eliminadosPendientes = db.pacienteDao().getEliminadosPendientes();
        for (Paciente p : eliminadosPendientes) {
            firestore.collection("pacientes")
                    .document(p.getCorreoElectronico())
                    .delete()
                    .addOnSuccessListener(aVoid -> Executors.newSingleThreadExecutor().execute(() -> {
                        db.pacienteDao().eliminar(p);
                    }))
                    .addOnFailureListener(e -> {
                        // pendientes de eliminar
                    });
        }

        // Subir los pendientes locales a Firestore
        for (Paciente p : pendientes) {
            firestore.collection("pacientes")
                    .document(p.getCorreoElectronico())
                    .set(p)
                    .addOnSuccessListener(aVoid -> {
                        p.setEstadoSincronizacion("SINCRONIZADO");
                        Executors.newSingleThreadExecutor().execute(() -> db.pacienteDao().actualizar(p));
                    });
        }

        // Descargar los datos más recientes desde Firestore
        firestore.collection("pacientes")
                .get()
                .addOnSuccessListener(query -> Executors.newSingleThreadExecutor().execute(() -> {
                    for (QueryDocumentSnapshot doc : query) {
                        String correo = doc.getString("correoElectronico");
                        Paciente remoto = doc.toObject(Paciente.class);
                        remoto.setEstadoSincronizacion("SINCRONIZADO");

                        Paciente local = db.pacienteDao().obtenerPorCorreo(correo);
                        if (local == null) db.pacienteDao().insertar(remoto);
                        else db.pacienteDao().actualizar(remoto);
                    }
                    onComplete.run();
                }))
                .addOnFailureListener(e -> onComplete.run());
    }

    private void sincronizarCitas(Runnable onComplete) {
        // 1. Eliminar citas marcadas como "ELIMINADO_PENDIENTE"
        List<Cita> eliminadasPendientes = db.citaDao().getEliminadosPendientes();
        for (Cita c : eliminadasPendientes) {
            firestore.collection("citas")
                    .document(c.getIdFirebase())
                    .delete()
                    .addOnSuccessListener(aVoid -> Executors.newSingleThreadExecutor().execute(() -> {
                        db.citaDao().eliminar(c);
                    }))
                    .addOnFailureListener(e -> {
                        // Mantener como pendiente si falla la eliminación
                    });
        }

        // 2. Subir citas pendientes (nuevas o actualizadas)
        List<Cita> pendientes = db.citaDao().getPendientes();
        for (Cita c : pendientes) {
            firestore.collection("citas")
                    .document(c.getIdFirebase())
                    .set(c)
                    .addOnSuccessListener(aVoid -> {
                        c.setEstadoSincronizacion("SINCRONIZADO");
                        Executors.newSingleThreadExecutor().execute(() -> db.citaDao().actualizar(c));
                    })
                    .addOnFailureListener(e -> {
                        // Si falla, se mantiene como pendiente
                    });
        }

        // 3. Descargar citas desde Firestore
        firestore.collection("citas")
                .get()
                .addOnSuccessListener(query -> Executors.newSingleThreadExecutor().execute(() -> {
                    for (com.google.firebase.firestore.QueryDocumentSnapshot doc : query) {
                        Cita remota = doc.toObject(Cita.class);
                        remota.setEstadoSincronizacion("SINCRONIZADO");

                        Cita local = db.citaDao().getByFirebaseId(remota.getIdFirebase());
                        if (local == null) db.citaDao().insertar(remota);
                        else db.citaDao().actualizar(remota);
                    }
                    onComplete.run();
                }))
                .addOnFailureListener(e -> onComplete.run());
    }
    private void showSnackbarLikeToast(String message, boolean isError) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View layout = inflater.inflate(R.layout.custom_toast, null);

        LinearLayout container = layout.findViewById(R.id.toast_container);
        ImageView icon = layout.findViewById(R.id.toast_icon);
        TextView text = layout.findViewById(R.id.toast_message);

        text.setText(message);

        int color = isError
                ? ContextCompat.getColor(context, android.R.color.holo_red_dark)
                : ContextCompat.getColor(context, R.color.colorBlue);

        GradientDrawable background = new GradientDrawable();
        background.setCornerRadius(24f);
        background.setColor(color);
        container.setBackground(background);

        icon.setImageResource(isError ? R.drawable.ic_error : R.drawable.ic_check_circle);

        Toast toast = new Toast(context.getApplicationContext());
        toast.setDuration(Toast.LENGTH_LONG);
        toast.setView(layout);
        toast.setGravity(Gravity.BOTTOM, 0, 120);
        toast.show();
    }
}
