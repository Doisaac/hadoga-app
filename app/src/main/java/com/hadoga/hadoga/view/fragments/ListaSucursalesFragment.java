package com.hadoga.hadoga.view.fragments;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.hadoga.hadoga.R;
import com.hadoga.hadoga.model.database.HadogaDatabase;
import com.hadoga.hadoga.model.entities.Sucursal;

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

            requireActivity().runOnUiThread(() -> {
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
            });
        });
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
        new AlertDialog.Builder(requireContext())
                .setTitle("Confirmar eliminación")
                .setMessage("¿Seguro que deseas eliminar la sucursal \"" + sucursal.getNombreSucursal() + "\"?")
                .setPositiveButton("Eliminar", (dialog, which) -> {
                    eliminarSucursalDeBD(sucursal);
                })
                .setNegativeButton("Cancelar", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void eliminarSucursalDeBD(Sucursal sucursal) {
        Executors.newSingleThreadExecutor().execute(() -> {
            db.sucursalDao().delete(sucursal);

            requireActivity().runOnUiThread(() -> {
                Toast.makeText(requireContext(),
                        "Sucursal eliminada correctamente",
                        Toast.LENGTH_SHORT).show();

                // Recargar la lista
                cargarSucursales();
            });
        });
    }
}
