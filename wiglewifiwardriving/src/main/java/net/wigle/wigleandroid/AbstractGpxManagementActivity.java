package net.wigle.wigleandroid;

import static net.wigle.wigleandroid.background.GpxExportRunnable.EXPORT_GPX_DIALOG;

import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.ActionBar;
import androidx.core.graphics.Insets;
import androidx.core.view.OnApplyWindowInsetsListener;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import net.wigle.wigleandroid.background.GpxExportRunnable;
import net.wigle.wigleandroid.db.DBException;
import net.wigle.wigleandroid.db.DatabaseHelper;
import net.wigle.wigleandroid.model.RouteDescriptor;
import net.wigle.wigleandroid.ui.GpxRecyclerAdapter;
import net.wigle.wigleandroid.ui.ScreenChildActivity;
import net.wigle.wigleandroid.ui.WiGLEToast;
import net.wigle.wigleandroid.util.Logging;
import net.wigle.wigleandroid.util.RouteConfigurable;
import net.wigle.wigleandroid.util.PreferenceKeys;
import net.wigle.wigleandroid.util.RouteExportSelector;

import java.text.DateFormat;
import java.util.concurrent.ExecutorService;

public abstract class AbstractGpxManagementActivity extends ScreenChildActivity implements RouteConfigurable, RouteExportSelector, DialogListener {
    protected DatabaseHelper dbHelper;
    protected final int DEFAULT_MAP_PADDING = 25;
    protected final String CURRENT_ROUTE_LINE_TAG = "currentRoutePolyline";
    protected View infoView;
    protected TextView distanceText;
    protected SharedPreferences prefs;
    protected long exportRouteId = -1L;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        View backButtonWrapper = findViewById(R.id.gpx_back_layout);
        if (null != backButtonWrapper) {
            ViewCompat.setOnApplyWindowInsetsListener(backButtonWrapper, new OnApplyWindowInsetsListener() {
                        @Override
                        public @org.jspecify.annotations.NonNull WindowInsetsCompat onApplyWindowInsets(@org.jspecify.annotations.NonNull View v, @org.jspecify.annotations.NonNull WindowInsetsCompat insets) {
                            final Insets innerPadding = insets.getInsets(
                                    WindowInsetsCompat.Type.statusBars() |
                                            WindowInsetsCompat.Type.displayCutout());
                            v.setPadding(
                                    innerPadding.left, innerPadding.top, innerPadding.right, innerPadding.bottom
                            );
                            return insets;
                        }
                    }
            );
        }

        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        ImageButton backButton = findViewById(R.id.gpx_back_button);
        if (null != backButton) {
            backButton.setOnClickListener(v -> finish());
        }
        prefs = getSharedPreferences(PreferenceKeys.SHARED_PREFS, 0);
        setupMap(prefs);
        setupList();
    }

    protected abstract void setupMap(SharedPreferences prefs);

    @Override
    public abstract void configureMapForRoute(RouteDescriptor routeDescriptor);

    @Override
    public abstract void clearCurrentRoute();

    private void setupList() {
        RecyclerView recyclerView = findViewById(R.id.gpx_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.addItemDecoration(new DividerItemDecoration(recyclerView.getContext(), DividerItemDecoration.VERTICAL));
        if (null != dbHelper) {
            try {
                Cursor cursor = dbHelper.routeMetaIterator();
                final DateFormat itemDateFormat = android.text.format.DateFormat.getDateFormat(this.getApplicationContext());
                final DateFormat itemTimeFormat = android.text.format.DateFormat.getTimeFormat(this.getApplicationContext());
                GpxRecyclerAdapter adapter = new GpxRecyclerAdapter(this, this, cursor, this, this, prefs, itemDateFormat, itemTimeFormat);
                recyclerView.setAdapter(adapter);
            } catch (DBException dbex) {
                Logging.error("Failed to setup list for GPX management: ", dbex);
            }
        }
    }

    @Override
    public void handleDialog(int dialogId) {
        switch (dialogId) {
            case EXPORT_GPX_DIALOG: {
                if (!exportRouteGpxFile(exportRouteId)) {
                    Logging.warn("Failed to export gpx.");
                    //WiGLEToast.showOverFragment(this, R.string.error_general,
                    //        getString(R.string.gpx_failed));
                }
                break;
            }
            default:
                Logging.warn("Data unhandled dialogId: " + dialogId);
        }
    }

    private boolean exportRouteGpxFile(long runId) {
        final long totalRoutePoints = ListFragment.lameStatic.dbHelper.getRoutePointCount(runId);
        if (totalRoutePoints > 1) {
            ExecutorService es = ListFragment.lameStatic.executorService;
            if (null != es) {
                try {
                    es.submit(new GpxExportRunnable(this, true, totalRoutePoints, runId));
                } catch (IllegalArgumentException e) {
                    Logging.error("failed to submit job: ", e);
                    WiGLEToast.showOverFragment(this, R.string.export_gpx,
                            getResources().getString(R.string.duplicate_job));
                    return false;
                }
            } else {
                Logging.error("null LameStatic ExecutorService - unable to submit route export");
            }
        } else {
            Logging.error("no points to create route");
            WiGLEToast.showOverFragment(this, R.string.gpx_failed,
                    getResources().getString(R.string.gpx_no_points));
            //NO POINTS
        }
        return true;
    }

    @Override
    public void setRouteToExport(long routeId) {
        exportRouteId = routeId;
    }
}
