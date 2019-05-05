package fr.werobot.lyrc_control_app;

import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements FragmentManager.OnBackStackChangedListener {

    private DevicesFragment deviceFragment;
    private ArrayList<OrientationListener> orientationListeners = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportFragmentManager().addOnBackStackChangedListener(this);
        deviceFragment = new DevicesFragment();
        if (savedInstanceState == null)
            getSupportFragmentManager().beginTransaction().add(R.id.fragment, deviceFragment, "devices").commit();
        else
            onBackStackChanged();
    }

    @Override
    public void onBackStackChanged() {
        getSupportActionBar().setDisplayHomeAsUpEnabled(getSupportFragmentManager().getBackStackEntryCount() > 0);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        deviceFragment.onConfigurationChanged(newConfig);
        for (OrientationListener orientationListener : orientationListeners) {
            orientationListener.run(newConfig.orientation);
        }
    }

    public interface OrientationListener {
        void run(int orientation);
    }

    public void registerOrientationListener(OrientationListener orientationListener) {
        this.orientationListeners.add(orientationListener);
    }

}
