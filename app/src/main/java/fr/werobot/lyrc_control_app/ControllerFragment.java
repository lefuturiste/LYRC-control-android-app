package fr.werobot.lyrc_control_app;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;


public class ControllerFragment extends Fragment {

    public ControllerFragment() {
    }

    /*
     * Lifecycle
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    /*
     * UI
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_controller, container, false);
        TextView textView = view.findViewById(R.id.controller_debug_text);
        textView.setMovementMethod(ScrollingMovementMethod.getInstance());
        textView.append("start \n");
        System.out.println("On create view called");
        MainActivity activity = (MainActivity) getActivity();
        if (activity != null) {
            activity.controllerHelper.registerOnControllerInputEventListener((keyId, x, y) -> {
                String data = keyId + "; " + x + "; " + y;
                System.out.println(data);
                textView.append(data + " \n");
            });
        }
        return view;
    }

}
