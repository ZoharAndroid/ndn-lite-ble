package zohar.com.ndn_liteble;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import zohar.com.ndn_liteble.R;

public class BluetoothFragment extends Fragment {

    private static final String TAG = "BluetoothFragment";

    public static BluetoothFragment newInstance(){
        BluetoothFragment bluetoothFragment = new BluetoothFragment();
        Bundle bundle = new Bundle();
        bluetoothFragment.setArguments(bundle);
        return bluetoothFragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        Log.i(TAG, "BluetoothFragment - onattch");
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "BluetoothFragment - onCreate");
        setHasOptionsMenu(true);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.i(TAG, "BluetoothFragment - onCreateView");
        View view = inflater.inflate(R.layout.fragment_bluetooth,container, false);

        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Log.i(TAG, "BluetoothFragment - onActivityCreated");
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.i(TAG, "BluetoothFragment - onStart");
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.i(TAG, "BluetoothFragment - onResume");
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.i(TAG, "BluetoothFragment - onPause");
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.i(TAG, "BluetoothFragment - onStop");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.i(TAG, "BluetoothFragment - onDestroyView");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "BluetoothFragment - onDestroy");
    }

    @Override
    public void onDetach() {
        super.onDetach();
        Log.i(TAG, "BluetoothFragment - onDetach");
    }
}
