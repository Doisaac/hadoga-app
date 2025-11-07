package com.hadoga.hadoga.utils;

import android.content.Context;
import android.widget.Toast;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.hadoga.hadoga.model.database.HadogaDatabase;
import com.hadoga.hadoga.model.entities.Cita;
import com.hadoga.hadoga.model.entities.Doctor;
import com.hadoga.hadoga.model.entities.Paciente;
import com.hadoga.hadoga.model.entities.Sucursal;
import com.hadoga.hadoga.model.entities.Usuario;

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
            Toast.makeText(context, "Sin conexión. No se puede sincronizar.", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(context, "Sincronizando datos...", Toast.LENGTH_SHORT).show();

        Executors.newSingleThreadExecutor().execute(() -> {
            sincronizarUsuarios(() -> {
                sincronizarSucursales(() -> {
                    sincronizarDoctores(() -> {
                        sincronizarPacientes(() -> {
                            sincronizarCitas(() -> {
                                new android.os.Handler(context.getMainLooper()).post(() ->
                                        Toast.makeText(context, "Datos sincronizados correctamente.", Toast.LENGTH_LONG).show()
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
        List<Sucursal> pendientes = db.sucursalDao().getPendientes();
        for (Sucursal s : pendientes) {
            firestore.collection("sucursales")
                    .document(s.getCodigoSucursal())
                    .set(s)
                    .addOnSuccessListener(aVoid -> {
                        s.setEstadoSincronizacion("SINCRONIZADO");
                        Executors.newSingleThreadExecutor().execute(() -> db.sucursalDao().update(s));
                    });
        }

        firestore.collection("sucursales")
                .get()
                .addOnSuccessListener(query -> Executors.newSingleThreadExecutor().execute(() -> {
                    for (QueryDocumentSnapshot doc : query) {
                        String codigo = doc.getString("codigoSucursal");
                        Sucursal local = db.sucursalDao().getSucursalByCodigo(codigo);
                        Sucursal remoto = doc.toObject(Sucursal.class);
                        remoto.setEstadoSincronizacion("SINCRONIZADO");

                        if (local == null) db.sucursalDao().insert(remoto);
                        else db.sucursalDao().update(remoto);
                    }
                    onComplete.run();
                }))
                .addOnFailureListener(e -> onComplete.run());
    }

    private void sincronizarDoctores(Runnable onComplete) {
        List<Doctor> pendientes = db.doctorDao().getPendientes();
        for (Doctor d : pendientes) {
            firestore.collection("doctores")
                    .document(d.getNumeroColegiado())
                    .set(d)
                    .addOnSuccessListener(aVoid -> {
                        d.setEstadoSincronizacion("SINCRONIZADO");
                        Executors.newSingleThreadExecutor().execute(() -> db.doctorDao().actualizar(d));
                    });
        }

        firestore.collection("doctores")
                .get()
                .addOnSuccessListener(query -> Executors.newSingleThreadExecutor().execute(() -> {
                    for (QueryDocumentSnapshot doc : query) {
                        String colegiado = doc.getString("numeroColegiado");
                        Doctor local = db.doctorDao().getDoctorByColegiado(colegiado);
                        Doctor remoto = doc.toObject(Doctor.class);
                        remoto.setEstadoSincronizacion("SINCRONIZADO");

                        if (local == null) db.doctorDao().insertar(remoto);
                        else db.doctorDao().actualizar(remoto);
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
}
