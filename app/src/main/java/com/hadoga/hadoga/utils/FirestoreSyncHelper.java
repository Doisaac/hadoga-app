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
            sincronizarUsuarios();
            sincronizarSucursales();
            sincronizarDoctores();

            // Mostrar mensaje al finalizar
            android.os.Handler handler = new android.os.Handler(context.getMainLooper());
            handler.post(() -> {
                Toast.makeText(context, "Datos sincronizados correctamente.", Toast.LENGTH_LONG).show();
            });
        });
    }

    private void sincronizarUsuarios() {
        // Subir pendientes
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

        // Descargar desde Firestore
        firestore.collection("usuarios")
                .get()
                .addOnSuccessListener(query -> Executors.newSingleThreadExecutor().execute(() -> {
                    for (QueryDocumentSnapshot doc : query) {
                        String email = doc.getId();
                        Usuario local = db.usuarioDao().getUsuarioByEmail(email);
                        Usuario remoto = doc.toObject(Usuario.class);
                        remoto.setEstadoSincronizacion("SINCRONIZADO");

                        if (local == null) {
                            remoto.setId(0);
                            db.usuarioDao().insert(remoto);
                        } else {
                            remoto.setId(local.getId());
                            db.usuarioDao().update(remoto);
                        }
                    }
                }));
    }

    private void sincronizarSucursales() {
        // Subir pendientes
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

        // Eliminar pendientes
        List<Sucursal> eliminadas = db.sucursalDao().getByEstado("ELIMINADO_PENDIENTE");
        for (Sucursal s : eliminadas) {
            firestore.collection("sucursales")
                    .document(s.getCodigoSucursal())
                    .delete()
                    .addOnSuccessListener(aVoid ->
                            Executors.newSingleThreadExecutor().execute(() -> db.sucursalDao().delete(s))
                    );
        }

        // Descargar desde Firestore
        firestore.collection("sucursales")
                .get()
                .addOnSuccessListener(query -> Executors.newSingleThreadExecutor().execute(() -> {
                    for (QueryDocumentSnapshot doc : query) {
                        String codigo = doc.getString("codigoSucursal");
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
                }));
    }

    private void sincronizarDoctores() {
        // Subir pendientes
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

        // Eliminar pendientes
        List<Doctor> eliminados = db.doctorDao().getByEstado("ELIMINADO_PENDIENTE");
        for (Doctor d : eliminados) {
            firestore.collection("doctores")
                    .document(d.getNumeroColegiado())
                    .delete()
                    .addOnSuccessListener(aVoid ->
                            Executors.newSingleThreadExecutor().execute(() -> db.doctorDao().eliminar(d))
                    );
        }

        // Descargar desde Firestore
        firestore.collection("doctores")
                .get()
                .addOnSuccessListener(query -> Executors.newSingleThreadExecutor().execute(() -> {
                    for (QueryDocumentSnapshot doc : query) {
                        String colegiado = doc.getString("numeroColegiado");
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
                }));
    }
}
