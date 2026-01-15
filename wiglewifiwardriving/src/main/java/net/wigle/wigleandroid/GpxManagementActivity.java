package net.wigle.wigleandroid;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.RelativeLayout;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Polyline;

import net.wigle.wigleandroid.model.RouteDescriptor;
import net.wigle.wigleandroid.ui.ThemeUtil;
import net.wigle.wigleandroid.ui.UINumberFormat;
import net.wigle.wigleandroid.util.GMapsConverter;
import net.wigle.wigleandroid.util.Logging;
import net.wigle.wigleandroid.util.PreferenceKeys;

import java.text.NumberFormat;
import java.util.Locale;

import static android.view.View.GONE;

public class GpxManagementActivity extends AbstractGpxManagementActivity {
    private final NumberFormat numberFormat;

    private MapView mapView;
    private Polyline routePolyline;

    public GpxManagementActivity() {
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
        setContentView(R.layout.activity_gpx_mgmt);
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onDestroy() {
        Logging.info("GPX MGMT: onDestroy");
        if (mapView != null) {
            mapView.onDestroy();
        }
        super.onDestroy();
        //setResult(Result.OK);
        finish();
    }

    @Override
    public void onResume() {
        Logging.info("GPX MGMT: onResume");
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
        Logging.info("GPX MGMT: onPause");
        super.onPause();
        if (mapView != null) {
            mapView.onPause();
        }
    }

    @Override
    protected void setupMap(final SharedPreferences prefs) {
        mapView = new MapView( this );
        try {
            mapView.onCreate(null);
            mapView.getMapAsync(googleMap -> ThemeUtil.setMapTheme(googleMap, mapView.getContext(), prefs, R.raw.night_style_json));
        } catch (NullPointerException ex) {
            Logging.error("npe in mapView.onCreate: " + ex, ex);
        }
        MapsInitializer.initialize( this );
        final RelativeLayout rlView = findViewById( R.id.gpx_map_rl );
        rlView.addView( mapView );
        infoView = findViewById(R.id.gpx_info);
        distanceText = findViewById(R.id.gpx_rundistance);
    }

    @Override
    public void configureMapForRoute(final RouteDescriptor routeDescriptor) {
        if ((routeDescriptor != null)) {
            mapView.getMapAsync(googleMap -> {
                LatLngBounds.Builder builder = new LatLngBounds.Builder();
                builder.include(new LatLng(routeDescriptor.getNEExtent().latitude, routeDescriptor.getNEExtent().longitude) );
                builder.include(new LatLng(routeDescriptor.getSWExtent().latitude, routeDescriptor.getSWExtent().longitude) );
                LatLngBounds bounds = builder.build();
                final CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, DEFAULT_MAP_PADDING);
                googleMap.animateCamera(cu);
                routePolyline = googleMap.addPolyline(
                        GMapsConverter.getPolyLineOptionsForRoute(routeDescriptor.getSegmentRoute()));
                routePolyline.setTag(CURRENT_ROUTE_LINE_TAG);
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