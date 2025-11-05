package com.hadoga.hadoga.utils;

import android.content.Context;
import android.widget.Toast;

import com.google.firebase.firestore.FirebaseFirestore;
import com.hadoga.hadoga.model.database.HadogaDatabase;
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
        });
    }

    private void sincronizarUsuarios() {
        List<Usuario> pendientes = db.usuarioDao().getPendientes();
        for (Usuario u : pendientes) {
            firestore.collection("usuarios")
                    .document(u.getEmail())
                    .set(u)
                    .addOnSuccessListener(aVoid -> {
                        u.setEstadoSincronizacion("SINCRONIZADO");
                        Executors.newSingleThreadExecutor().execute(() -> db.usuarioDao().update(u));
                    })
                    .addOnFailureListener(e -> {
                        // Seguirá pendiente
                    });
        }
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
                    })
                    .addOnFailureListener(e -> {
                        // No se cambia el estado, sigue pendiente
                    });
        }

        // Eliminar los marcados como ELIMINADO_PENDIENTE
        List<Sucursal> eliminadas = db.sucursalDao().getByEstado("ELIMINADO_PENDIENTE");
        for (Sucursal s : eliminadas) {
            firestore.collection("sucursales")
                    .document(s.getCodigoSucursal())
                    .delete()
                    .addOnSuccessListener(aVoid -> Executors.newSingleThreadExecutor().execute(() -> db.sucursalDao().delete(s)))
                    .addOnFailureListener(e -> {
                        // Si falla, queda marcado como pendiente de eliminar
                    });
        }
    }

}
