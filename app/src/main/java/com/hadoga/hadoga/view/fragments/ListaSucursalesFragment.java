package com.hadoga.hadoga.view.fragments;

import android.app.AlertDialog;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.hadoga.hadoga.R;
import com.hadoga.hadoga.model.database.HadogaDatabase;
import com.hadoga.hadoga.model.entities.Sucursal;
import com.hadoga.hadoga.utils.FirebaseService;
import com.hadoga.hadoga.utils.NetworkUtils;

import java.util.List;
import java.util.concurrent.Executors;

public class ListaSucursalesFragment extends Fragment {
    private LinearLayout containerSucursales;
    private HadogaDatabase db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_lista_sucursales, container, false);

        initUI(view);
        cargarSucursales();

        return view;
    }

    private void initUI(View view) {
        containerSucursales = view.findViewById(R.id.containerSucursales);
        db = HadogaDatabase.getInstance(requireContext());
    }

    private void cargarSucursales() {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<Sucursal> lista = db.sucursalDao().getAllSucursales();
            requireActivity().runOnUiThread(() -> mostrarSucursales(lista));
        });
    }

    private void mostrarSucursales(List<Sucursal> lista) {
        containerSucursales.removeAllViews();

        if (lista.isEmpty()) {
            TextView txt = new TextView(requireContext());
            txt.setText("No hay sucursales registradas.");
            txt.setTextColor(getResources().getColor(android.R.color.white));
            txt.setPadding(0, 16, 0, 0);
            containerSucursales.addView(txt);
            return;
        }

        for (Sucursal sucursal : lista) {
            View card = crearCardSucursal(sucursal);
            containerSucursales.addView(card);
        }
    }

    private View crearCardSucursal(Sucursal sucursal) {
        View cardView = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_sucursal_card, containerSucursales, false);

        TextView tvNombre = cardView.findViewById(R.id.tvNombreSucursal);
        TextView tvCodigo = cardView.findViewById(R.id.tvCodigoSucursal);
        Button btnEditar = cardView.findViewById(R.id.btnEditarSucursal);
        Button btnBorrar = cardView.findViewById(R.id.btnBorrarSucursal);

        tvNombre.setText(sucursal.getNombreSucursal());
        tvCodigo.setText(sucursal.getCodigoSucursal());

        // brir fragment con datos para editar
        btnEditar.setOnClickListener(v -> abrirFragmentEditarSucursal(sucursal));

        // Borrar
        btnBorrar.setOnClickListener(v -> borrarSucursal(sucursal));

        return cardView;
    }

    private void abrirFragmentEditarSucursal(Sucursal sucursal) {
        Bundle bundle = new Bundle();
        bundle.putSerializable("sucursalData", sucursal);

        AgregarSucursalFragment fragment = new AgregarSucursalFragment();
        fragment.setArguments(bundle);

        requireActivity().getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .addToBackStack(null)
                .commit();
    }

    private void borrarSucursal(Sucursal sucursal) {
        Executors.newSingleThreadExecutor().execute(() -> {
            // Verificar si existen doctores asignados a la sucursal
            var doctoresAsociados = db.doctorDao().getDoctoresDeSucursal(sucursal.getCodigoSucursal());

            requireActivity().runOnUiThread(() -> {
                if (!doctoresAsociados.isEmpty()) {
                    // Si hay doctores, mostrar aviso
                    showSnackbarLikeToast("Primero elimina los doctores asociados a esta sucursal antes de borrarla.", true);
                } else {
                    // Si no hay doctores, continuar con el diálogo de confirmación normal
                    new AlertDialog.Builder(requireContext())
                            .setTitle("Confirmar eliminación")
                            .setMessage("¿Seguro que deseas eliminar la sucursal \"" + sucursal.getNombreSucursal() + "\"?")
                            .setPositiveButton("Eliminar", (dialog, which) -> {
                                eliminarSucursalDeBD(sucursal);
                            })
                            .setNegativeButton("Cancelar", (dialog, which) -> dialog.dismiss())
                            .show();
                }
            });
        });
    }

    private void eliminarSucursalDeBD(Sucursal sucursal) {
        // Si no hay conexión se marca como eliminada pendiente
        if (!NetworkUtils.isNetworkAvailable(requireContext())) {
            Executors.newSingleThreadExecutor().execute(() -> {
                sucursal.setEstadoSincronizacion("ELIMINADO_PENDIENTE");
                db.sucursalDao().update(sucursal);

                requireActivity().runOnUiThread(() -> {
                    showSnackbarLikeToast("Sin conexión. La sucursal se eliminó localmente.", null);
                    cargarSucursales();
                });
            });
            return;
        }

        // Si hay conexión se elimina en Firestore y localmente
        FirebaseFirestore firestore = FirebaseService.getInstance();

        firestore.collection("sucursales")
                .document(sucursal.getCodigoSucursal())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Executors.newSingleThreadExecutor().execute(() -> {
                        db.sucursalDao().delete(sucursal);

                        requireActivity().runOnUiThread(() -> {
                            showSnackbarLikeToast("Sucursal eliminada correctamente.", false);
                            cargarSucursales();
                        });
                    });
                })
                .addOnFailureListener(e -> {
                    Executors.newSingleThreadExecutor().execute(() -> {
                        sucursal.setEstadoSincronizacion("ELIMINADO_PENDIENTE");
                        db.sucursalDao().update(sucursal);

                        requireActivity().runOnUiThread(() -> {
                            showSnackbarLikeToast("Error al eliminar en la nube. La sucursal se eliminó localmente.", null);
                            cargarSucursales();
                        });
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
