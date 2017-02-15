package ru.neverdark.csm.fragments;


import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.List;

import ru.neverdark.csm.R;
import ru.neverdark.csm.db.GpslogTable;

public class StatsViewMapTabFragment extends Fragment implements OnMapReadyCallback {

    private static final String TAG = "StatsViewMapTabFragment";
    private LatLng mStartPoint;
    private LatLng mFinishPoint;
    private GoogleMap mGoogleMap;

    public StatsViewMapTabFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        Log.v(TAG, "onCreateView: ");
        View view = inflater.inflate(R.layout.fragment_stats_view_map_tab, container, false);
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map_fragment);
        mapFragment.getMapAsync(this);
        setHasOptionsMenu(true);
        return view;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.stats_view_map, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_map_type_normal:
                setMapType(GoogleMap.MAP_TYPE_NORMAL);
                return true;
            case R.id.action_map_type_hybrid:
                setMapType(GoogleMap.MAP_TYPE_HYBRID);
                return true;
            case R.id.action_map_type_satellite:
                setMapType(GoogleMap.MAP_TYPE_SATELLITE);
                return true;
            case R.id.action_map_type_terrain:
                setMapType(GoogleMap.MAP_TYPE_TERRAIN);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setMapType(int mapType) {
        if (mGoogleMap != null) {
            mGoogleMap.setMapType(mapType);
        }
    }

    public static StatsViewMapTabFragment getInstance(List<GpslogTable.TrackRecord> points) {
        StatsViewMapTabFragment fragment = new StatsViewMapTabFragment();
        fragment.prepareData(points);
        return fragment;
    }

    private PolylineOptions rectOptions = new PolylineOptions();
    private LatLngBounds mBounds;

    private void prepareData(List<GpslogTable.TrackRecord> points) {
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        rectOptions.color(Color.RED).width(4).geodesic(true);

        for (GpslogTable.TrackRecord record: points) {
            LatLng latLng = new LatLng(record.latitude, record.longitude);
            rectOptions.add(latLng);
            builder.include(latLng);
        }
        mBounds = builder.build();
        mStartPoint = new LatLng(points.get(0).latitude, points.get(0).longitude);
        mFinishPoint = new LatLng(
                points.get(points.size() - 1).latitude,
                points.get(points.size() - 1).longitude
        );
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        int padding = (int) getResources().getDimension(R.dimen.map_padding);
        googleMap.setPadding(padding, padding, padding, padding);

        googleMap.getUiSettings().setMapToolbarEnabled(false);
        googleMap.addPolyline(rectOptions);
        googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(mBounds, 0));

        MarkerOptions startMarker = new MarkerOptions()
                .position(mStartPoint)
                .title(getString(R.string.start))
                .icon(getMarkerIcon(R.color.light_green_400));
        MarkerOptions finishMarker = new MarkerOptions()
                .position(mFinishPoint)
                .title(getString(R.string.finish))
                .icon(getMarkerIcon(R.color.orange_400));
        googleMap.addMarker(startMarker);
        googleMap.addMarker(finishMarker);


        mGoogleMap = googleMap;
    }

    /**
     * Возвращает иконку маркера в нужном цветовом оттенке основывываясь на переданном цвете
     * @param colorResId id цвета из ресурсов
     * @return иконка для маркера
     */
    private BitmapDescriptor getMarkerIcon(int colorResId) {
        float[] hsv = new float[3];
        Color.colorToHSV(ContextCompat.getColor(getContext(), colorResId), hsv);
        return BitmapDescriptorFactory.defaultMarker(hsv[0]);
    }
}
