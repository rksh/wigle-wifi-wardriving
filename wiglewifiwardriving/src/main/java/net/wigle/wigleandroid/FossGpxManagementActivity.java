package net.wigle.wigleandroid;

import static android.view.View.GONE;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.RelativeLayout;

import net.wigle.wigleandroid.model.RouteDescriptor;
import net.wigle.wigleandroid.ui.UINumberFormat;
import net.wigle.wigleandroid.util.Logging;
import net.wigle.wigleandroid.util.PreferenceKeys;

import org.maplibre.android.MapLibre;
import org.maplibre.android.annotations.Polyline;
import org.maplibre.android.annotations.PolylineOptions;
import org.maplibre.android.camera.CameraPosition;
import org.maplibre.android.camera.CameraUpdate;
import org.maplibre.android.camera.CameraUpdateFactory;
import org.maplibre.android.geometry.LatLng;
import org.maplibre.android.geometry.LatLngBounds;
import org.maplibre.android.maps.MapView;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class FossGpxManagementActivity extends AbstractGpxManagementActivity {
    private final NumberFormat numberFormat;

    private MapView mapView;

    private Polyline routePolyline;

    public FossGpxManagementActivity() {
        final MainActivity.State s = MainActivity.getStaticState();
        if (null != s) {
            this.dbHelper = s.dbHelper;
        } else {
            this.dbHelper = null;
        }
        Locale locale = Locale.getDefault();
        numberFormat = NumberFormat.getNumberInstance(locale);
        numberFormat.setMaximumFractionDigits(1);
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        MapLibre.getInstance(this);
        setContentView(R.layout.activity_foss_gps_mgmt);
        super.onCreate(savedInstanceState);
        /*mapView.getMapAsync(mapLibreMap -> {
            mapLibreMap.setStyle("test", (Style.OnStyleLoaded) style -> {
                style.addSource(new VectorSource("openmaptiles",
                        new TileSet(
                                "openmaptiles",
                                "https://demotiles.maplibre.org/tiles-omt/{z}/{x}/{y}.pbf"
                        )
                ));
                style.addLayer(new LineLayer("road", "openmaptiles").withProperties(
                        PropertyFactory.lineColor("red"),
                        PropertyFactory.lineWidth(2.0f)
                ));
            });
        });*/
    }

    @Override
    public void onDestroy() {
        Logging.info("FOSS GPX MGMT: onDestroy");
        if (mapView != null) {
            mapView.onDestroy();
        }
        super.onDestroy();
        //setResult(Result.OK);
        finish();
    }

    @Override
    public void onResume() {
        Logging.info("FOSS GPX MGMT: onResume");
        super.onResume();
        if (mapView != null) {
            mapView.onResume();
    } else {
        final SharedPreferences prefs = getSharedPreferences(PreferenceKeys.SHARED_PREFS, 0);
        setupMap(prefs);
    }
}

    @Override
    public void onPause() {
        Logging.info("FOSS GPX MGMT: onPause");
        super.onPause();
        if (mapView != null) {
            mapView.onPause();
        }
    }

    @Override
    protected void setupMap(SharedPreferences prefs) {
        final RelativeLayout rlView = findViewById( R.id.ml_gpx_map_rl );
        mapView = rlView.findViewById(R.id.maplibreView);
        if (null != mapView) {
            mapView.getMapAsync(mapLibreMap -> {
                mapLibreMap.setStyle("https://demotiles.maplibre.org/style.json");
                mapLibreMap.setCameraPosition(
                        new CameraPosition.Builder().target(
                                new LatLng(0.0, 0.0)).zoom(1.0).build());
            });
        } else {
            Logging.error("Failed to find mapView");
        }
    }

    @Override
    public void configureMapForRoute(RouteDescriptor routeDescriptor) {
        if ((routeDescriptor != null)) {
            mapView.getMapAsync(mapLibreMap -> {
                // Clear existing polyline if any
                if (routePolyline != null) {
                    routePolyline.remove();
                }

                // Get points from PolylineRoute and convert to MapLibre LatLng
                List<double[]> routePoints = routeDescriptor.getRoutePoints();
                
                if (routePoints != null && !routePoints.isEmpty()) {
                    // Convert points to MapLibre LatLng
                    List<LatLng> maplibrePoints = new ArrayList<>();
                    for (double[] point : routePoints) {
                        maplibrePoints.add(new LatLng(point[0], point[1]));
                    }

                    // Create MapLibre PolylineOptions
                    PolylineOptions polylineOptions = new PolylineOptions();
                    polylineOptions.addAll(maplibrePoints);
                    polylineOptions.color(routeDescriptor.getRouteColor());
                    polylineOptions.width(routeDescriptor.getRouteWidth());

                    // Add polyline to map
                    routePolyline = mapLibreMap.addPolyline(polylineOptions);

                    // Set camera bounds to show the route
                    LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
                    boundsBuilder.include(new LatLng(routeDescriptor.getNELatitude(), routeDescriptor.getNELongitude()));
                    boundsBuilder.include(new LatLng(routeDescriptor.getSWLatitude(), routeDescriptor.getSWLongitude()));
                    LatLngBounds bounds = boundsBuilder.build();
                    
                    CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngBounds(
                            bounds, DEFAULT_MAP_PADDING);
                    mapLibreMap.animateCamera(cameraUpdate);
                }
            });
            
            infoView.setVisibility(View.VISIBLE);
            final String distString = UINumberFormat.metersToString(prefs,
                    numberFormat, this, routeDescriptor.getDistanceMeters(), true);
            distanceText.setText(distString);
        } else {
            infoView.setVisibility(GONE);
            distanceText.setText("");
        }
    }

    @Override
    public void clearCurrentRoute() {
        if (routePolyline != null ) {
            routePolyline.remove();
        }
    }
}