package fr.werobot.lyrc_control_app;

import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.KeyEvent;
import android.view.MotionEvent;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements FragmentManager.OnBackStackChangedListener {
    private DevicesFragment deviceFragment;
    private ArrayList<OrientationListener> orientationListeners = new ArrayList<>();
    public ControllerHelper controllerHelper = new ControllerHelper();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportFragmentManager().addOnBackStackChangedListener(this);
        deviceFragment = new DevicesFragment();
        if (savedInstanceState == null)
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.fragment, deviceFragment, "devices")
                    .commit();
        else
            onBackStackChanged();

        if (this.controllerHelper.haveControllerConnected()) {
            System.out.println("Controller detected: " + this.controllerHelper.getControllerId());
        }
    }

    @Override
    public boolean onGenericMotionEvent(MotionEvent event) {
        return controllerHelper.onGenericMotionEvent(event);
    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return controllerHelper.onKeyDown(keyCode) || super.onKeyDown(keyCode, event);
    }

    @Override
    public void onBackStackChanged() {
        getSupportActionBar()
                .setDisplayHomeAsUpEnabled(
                        getSupportFragmentManager()
                                .getBackStackEntryCount() > 0);
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
