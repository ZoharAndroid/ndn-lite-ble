package zohar.com.ndn_liteble;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class WiFiFragment extends Fragment {

    public static WiFiFragment newInstance() {
        WiFiFragment wiFiFragment = new WiFiFragment();
        return wiFiFragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_wifi,container, false);

        return view;
    }
}
