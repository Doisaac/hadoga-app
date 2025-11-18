package com.hadoga.hadoga.view.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.hadoga.hadoga.R;

import java.util.Objects;

public class MapaFragment extends Fragment implements OnMapReadyCallback {

    private GoogleMap mMap;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_mapa, container, false);
        SupportMapFragment mapFragment =
                (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Toolbar toolbar = view.findViewById(R.id.toolbar);
        toolbar.setTitle("Ubicacion");

        // Inflar el menú directamente en la Toolbar del fragmento
        toolbar.inflateMenu(R.menu.menu_tipo_mapa);

        // Accion cuando se selecciona un ítem del menú
        toolbar.setOnMenuItemClickListener(menuItem -> {
            if (mMap == null) {
                return false;
            }

            int itemId = menuItem.getItemId();
            if (itemId == R.id.mapa_normal) {
                mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                return true;
            } else if (itemId == R.id.mapa_satelite) {
                mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
                return true;
            } else if (itemId == R.id.mapa_terrain) {
                mMap.setMapType(GoogleMap.MAP_TYPE_TERRAIN);
                return true;
            } else if (itemId == R.id.mapa_hybrid) {
                mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
                return true;
            }
            return false;
        });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        LatLng hadoga = new LatLng(13.97832716221565, -89.56562773671737);
        mMap.addMarker(new MarkerOptions()
                .position(hadoga)
                .title("Clinicas HADOGA"));

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(hadoga, 15));
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setCompassEnabled(true);
        mMap.getUiSettings().setMapToolbarEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(true);
        mMap.getUiSettings().setAllGesturesEnabled(true);
        mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
    }
}
