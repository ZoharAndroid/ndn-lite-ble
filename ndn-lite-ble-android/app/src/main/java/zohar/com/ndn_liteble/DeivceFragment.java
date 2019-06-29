package zohar.com.ndn_liteble;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import com.google.zxing.client.android.CaptureActivity;
import com.google.zxing.client.android.share.ShareActivity;

import zohar.com.ndn_liteble.utils.Constant;

public class DeivceFragment extends Fragment {

    private View view;
    // BLE是否开启布局容器
    private ConstraintLayout mBleView;
    // BLE开启按钮
    private Button mStartBleButton;
    // 蓝牙状态的监听
    private BluetoothListenerRecevier mBluetoothRecevier;


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_devices, container, false);
        mBleView = view.findViewById(R.id.ble_check_constraint);
        mStartBleButton = view.findViewById(R.id.btn_ble_open_main);
        FloatingActionButton mFloatingButton = view.findViewById(R.id.floating_button_main_activity);

        // 悬浮按钮
        mFloatingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getContext(), "悬浮点击按钮", Toast.LENGTH_SHORT).show();
            }
        });

        //ble
        if (!checkBluetoothAble()) {
            // 当前蓝牙不可用，就去开启蓝牙
            mStartBleButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onEnableBluetoothClicked();
                }
            });
        }
        return view;
    }

    /**
     * 开启蓝牙
     */
    public void onEnableBluetoothClicked() {
        Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableIntent, Constant.REQUSET_BLE_CODE);
    }

    /**
     * 检车蓝牙是否可用和开启
     *
     * @return true 开启
     * false：不可用或者没有开启
     */
    private boolean checkBluetoothAble() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(getContext(), "当前设备蓝牙不可用！", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (!bluetoothAdapter.isEnabled()) {
            mBleView.setVisibility(View.VISIBLE);
            return false;
        }

        return true;
    }


    /**
     * 注册蓝牙监听器
     */
    private void registerBluetoothReceiver() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        mBluetoothRecevier = new BluetoothListenerRecevier();
        getActivity().registerReceiver(mBluetoothRecevier, intentFilter);
    }

    /**
     * 销毁蓝牙监听器
     */
    private void unregisterBluetoothReceiver() {
        if (mBluetoothRecevier != null) {
            getActivity().unregisterReceiver(mBluetoothRecevier);
            mBluetoothRecevier = null;
        }
    }

    /**
     * 蓝牙开启和打开的监听器
     */
    class BluetoothListenerRecevier extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()){
                case BluetoothAdapter.ACTION_STATE_CHANGED:
                    int blueState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,0);
                    switch (blueState){
                        case BluetoothAdapter.STATE_OFF:
                            // 蓝牙关闭了，就要显示打开的按钮
                            mBleView.setVisibility(View.VISIBLE);
                            break;
                        case BluetoothAdapter.STATE_ON:
                            mBleView.setVisibility(View.GONE);
                            break;
                        default:
                    }
            }
        }
    }


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
      inflater.inflate(R.menu.toolbar, menu);
      super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.toolbar_scan_qr_code: // 打开相机扫描二维码
                if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
                    ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.CAMERA}, Constant.PERMISSION_CAMER);
                }else {
                    startCameraActivityForResult();
                }
                break;
            case R.id.toolbar_refresh: // 刷新
                break;
            case R.id.create_qr_toolbar: // 创建二维码
                startCreateQRActivity();
                break;

            default:
        }

        return true;
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        switch (requestCode) {
            case Constant.REQUSET_BLE_CODE: // 蓝牙开启
                mBleView.setVisibility(View.GONE);
                break;

            case Constant.REQUSET_QR: // 扫描二维码
                if (requestCode == getActivity().RESULT_OK){
                    String qrResult = data.getStringExtra(Constant.QR_RESULT);
                    Toast.makeText(getContext(), qrResult,Toast.LENGTH_SHORT).show();
                }
                break;
            default:
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case Constant.PERMISSION_CAMER:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    startCameraActivityForResult();
                }else{
                    Toast.makeText(getContext(), "权限授予失败",Toast.LENGTH_SHORT).show();
                }
                break;
            default:
        }
    }


    /**
     * 开启创建Activity界面
     */
    private void startCreateQRActivity(){
        Intent intent = new Intent(getContext(), ShareActivity.class);
        startActivity(intent);
    }

    /**
     * 带返回结果的打开相机扫描二维码
     */
    private void startCameraActivityForResult(){
        Intent qrIntent = new Intent(getContext(), CaptureActivity.class);
        startActivityForResult(qrIntent, Constant.REQUSET_QR);
    }


    @Override
    public void onResume() {
        super.onResume();
        // 注册蓝牙的开关
        registerBluetoothReceiver();
    }

    @Override
    public void onPause() {
        super.onPause();
        // 注销蓝牙的监听
        unregisterBluetoothReceiver();
    }
}
