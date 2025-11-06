package com.hadoga.hadoga.utils;

import android.content.Context;
import android.widget.Toast;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.hadoga.hadoga.model.database.HadogaDatabase;
import com.hadoga.hadoga.model.entities.Doctor;
import com.hadoga.hadoga.model.entities.Sucursal;
import com.hadoga.hadoga.model.entities.Usuario;

import java.util.List;
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
                        // Al finalizar todo
                        new android.os.Handler(context.getMainLooper()).post(() ->
                                Toast.makeText(context, "Datos sincronizados correctamente.", Toast.LENGTH_LONG).show()
                        );
                    });
                });
            });
        });
    }

    private void sincronizarUsuarios(Runnable onComplete) {
        List<Usuario> pendientes = db.usuarioDao().getPendientes();
        for (Usuario u : pendientes) {
            firestore.collection("usuarios")
                    .document(u.getEmail())
                    .set(u)
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
}
