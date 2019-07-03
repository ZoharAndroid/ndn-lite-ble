package zohar.com.ndn_liteble;

import android.Manifest;
import android.app.Activity;
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
import android.support.v4.widget.ContentLoadingProgressBar;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.FormatException;
import com.google.zxing.client.android.CaptureActivity;
import com.google.zxing.client.android.share.ShareActivity;

import net.named_data.jndn.Data;
import net.named_data.jndn.Face;
import net.named_data.jndn.Interest;
import net.named_data.jndn.InterestFilter;
import net.named_data.jndn.Name;
import net.named_data.jndn.OnData;
import net.named_data.jndn.OnInterestCallback;
import net.named_data.jndn.security.v2.CertificateV2;
import net.named_data.jndn.util.Blob;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import NDNLiteSupport.BLEFace.BLEFace;
import NDNLiteSupport.BLEUnicastConnectionMaintainer.BLEUnicastConnectionMaintainer;
import NDNLiteSupport.NDNLiteSupportInit;
import NDNLiteSupport.SignOnBasicControllerBLE.SignOnBasicControllerBLE;
import NDNLiteSupport.SignOnBasicControllerBLE.secureSignOn.SignOnControllerResultCodes;
import zohar.com.ndn_liteble.utils.Constant;
import zohar.com.ndn_liteble.utils.SendInterestTask;

import static NDNLiteSupport.SignOnBasicControllerBLE.secureSignOn.SignOnControllerConsts.KD_PUB_CERTIFICATE_NAME_PREFIX;
import static NDNLiteSupport.SignOnBasicControllerBLE.secureSignOn.secureSignOnVariants.SecureSignOnVariantStrings.SIGN_ON_VARIANT_BASIC_ECC_256;
import static NDNLiteSupport.SignOnBasicControllerBLE.secureSignOn.utils.SecurityHelpers.asnEncodeRawECPublicKeyBytes;

public class DeviceFragment extends Fragment {

    private static final String TAG = "DeviceFragment";

    // 主界面现实的内容
    private TextView m_log;
    // fragment主界面View
    private View view;
    // BLE是否开启布局容器
    private ConstraintLayout mBleView;
    // BLE开启按钮
    private Button mStartBleButton;
    // 蓝牙设备不支持
    private TextView mTvBle;
    // 蓝牙文本注释
    private TextView mTvBleNote;
    // 悬浮按钮
    FloatingActionButton mFloatingButton;
    // 蓝牙状态的监听
    private BluetoothListenerRecevier mBluetoothRecevier;
    // BLEUnicastConnectionMaintainer
    private BLEUnicastConnectionMaintainer mBLEUnicastConnectionMaintainer;
    // SignOnBasicControllerBLE
    private SignOnBasicControllerBLE mSignOnBasicControllerBLE;
    // signon回调
    private SignOnBasicControllerBLE.SecureSignOnBasicControllerBLECallbacks mSecureSignOnBasicControllerBLECallbacks;


    // The device identifier of the example nRF52840, in hex string format.
    private String m_expectedDeviceIdentifierHexString = "010101010101010101010101";
    private String m_expectedDeviceIdentifierHexString2 = "010101010101010101010102";

    // 创建BLE face
    private BLEFace m_bleFace;
    private BLEFace m_bleFace2;

    // 发送兴趣包的回调函数
    private OnInterestCallback onInterest;
    private LinearLayout mLoadingView;


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_devices, container, false);

        // 初始化UI
        initUIView();
        // 申请位置权限
        requestLocationPermission();
        //ble
        checkBluetoothAble();
        // 初始化事件
        initEvent();

        return view;
    }


    /**
     * ndn主方法
     */
    private void ndnLiteMainMethod() {

        // Callback for when an interest is received. In this example, the nRf52840 sends an interest to
        // us after sign on is complete, and triggers this callback.
        onInterest = new OnInterestCallback() {
            @Override
            public void onInterest(Name prefix, Interest interest, Face face, long interestFilterId,
                                   InterestFilter filter) {
                logMessage(TAG, "onInterest got called, prefix of interest: " + prefix.toUri());
            }
        };


        // SignOn回调方法
        mSecureSignOnBasicControllerBLECallbacks = new SignOnBasicControllerBLE.SecureSignOnBasicControllerBLECallbacks() {
            @Override
            public void onDeviceSignOnComplete(String deviceIdentifierHexString) {
                // 隐藏加载界面
                showLoadingView(false);
                logMessage(TAG, "Onboarding was successful for device with device identifier hex string : " +
                        deviceIdentifierHexString);
                logMessage(TAG, "Mac address of device succesfully onboarded: " +
                        mSignOnBasicControllerBLE.getMacAddressOfDevice(deviceIdentifierHexString));
                logMessage(TAG, "Name of device's KDPubCertificate: " +
                        mSignOnBasicControllerBLE.getKDPubCertificateOfDevice(deviceIdentifierHexString)
                                .getName().toUri()
                );

                // Create a BLE face to the device that onboarding completed successfully for.
                m_bleFace = new BLEFace(mSignOnBasicControllerBLE.getMacAddressOfDevice(deviceIdentifierHexString),
                        onInterest);

                Interest  test_interest = new Interest(new Name("/phone/test/interest"));
                test_interest.setChildSelector(-1);

                //LogHelpers.LogByteArrayDebug(TAG, "test_interest_bytes: ", test_interest.wireEncode().getImmutableArray());

                m_bleFace.expressInterest(test_interest, new OnData() {
                    @Override
                    public void onData(Interest interest, Data data) {
                        logMessage(TAG, "Received data in response to test interest sent to device with device identifier: " );
                    }
                });
            }

            @Override
            public void onDeviceSignOnError(String deviceIdentifierHexString, SignOnControllerResultCodes.SignOnControllerResultCode resultCode) {
                // 设备SignOn失败
                if (deviceIdentifierHexString != null) {
                    Log.i(TAG, "设备SigOn错误: " + deviceIdentifierHexString +
                            "；  mac 地址： " + mSignOnBasicControllerBLE.getMacAddressOfDevice(deviceIdentifierHexString) + "\n" +
                            "SignOnControllerResultCode: " + resultCode);
                } else {
                    Log.w(TAG, "Sign on error for unknown device." + "\n" +
                            "SignOnControllerResultCode: " + resultCode);
                }
            }
        };


        NDNLiteSupportInit.NDNLiteSupportInit();

        CertificateV2 trustAnchorCertificate = new CertificateV2();

        // 初始化BLEUnicastConnectionMaintainer
        // 必须这样做才能使SecureSignOnControllerble和Bleface完全正常工作）
        mBLEUnicastConnectionMaintainer = BLEUnicastConnectionMaintainer.getInstance();
        mBLEUnicastConnectionMaintainer.initialize(getActivity());

        // 初始化SignOnControllerBLE
        mSignOnBasicControllerBLE = SignOnBasicControllerBLE.getInstance();
        mSignOnBasicControllerBLE.initialize(SIGN_ON_VARIANT_BASIC_ECC_256,
                mSecureSignOnBasicControllerBLECallbacks, trustAnchorCertificate);

        // Creating a certificate from the device1's KS key pair public key.
        CertificateV2 KSpubCertificateDevice1 = new CertificateV2();
        try {
            KSpubCertificateDevice1.setContent(
                    new Blob(asnEncodeRawECPublicKeyBytes(Constant.BOOTSTRAP_ECC_PUBLIC_NO_POINT_IDENTIFIER))
            );
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Adding device1 to the SignOnControllerBLE's list of devices pending onboarding; if
        // this is not done, the SignOnControllerBLE would ignore bootstrapping requests from the
        // device.
        mSignOnBasicControllerBLE.addDevicePendingSignOn(KSpubCertificateDevice1, Constant.DEVICE_IDENTIFIER_1,
                Constant.SECURE_SIGN_ON_CODE);

        // Creating a certificate from the device2's KS key pair public key.
        CertificateV2 KSpubCertificateDevice2 = new CertificateV2();
        try {
            KSpubCertificateDevice2.setContent(
                    new Blob(asnEncodeRawECPublicKeyBytes(Constant.BOOTSTRAP_ECC_PUBLIC_NO_POINT_IDENTIFIER))
            );
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Adding device2 to the SignOnControllerBLE's list of devices pending onboarding; if
        // this is not done, the SignOnControllerBLE would ignore bootstrapping requests from the
        // device.
        mSignOnBasicControllerBLE.addDevicePendingSignOn(KSpubCertificateDevice1, Constant.DEVICE_IDENTIFIER_2,
                Constant.SECURE_SIGN_ON_CODE);
    }

    private void logMessage(String TAG, String msg) {
        Log.d(TAG, msg);
        logMessageUI(TAG, msg);
    }

    private void logMessageUI(final String TAG, final String msg) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                m_log.append(TAG + ":" + "\n");
                m_log.append(msg + "\n");
                m_log.append("------------------------------" + "\n");
            }
        });
    }

    /**
     * 请求位置权限
     */
    private void requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, Constant.REQUEST_COARSE_LOCATION);
        }
    }

    /**
     * 请求相机权限
     */
    private void requestCameraPermission() {
        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, Constant.REQUEST_CAMER_PERMISSION);
        } else {
            startCameraActivityForResult();
        }
    }


    /**
     * 初始化事件
     */
    private void initEvent() {
        // 悬浮按钮
        mFloatingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getContext(), "悬浮点击按钮", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * 初始化UI控件
     */
    private void initUIView() {
        m_log = view.findViewById(R.id.ui_log);
        mLoadingView = view.findViewById(R.id.ll_loading_device);

        mBleView = view.findViewById(R.id.ble_check_constraint);
        mStartBleButton = view.findViewById(R.id.btn_ble_open_main);
        mFloatingButton = view.findViewById(R.id.floating_button_main_activity);
        mTvBle = view.findViewById(R.id.tv_bluetooth_disable);
        mTvBleNote = view.findViewById(R.id.tv_bluetooth_disable_note);

    }

    /**
     * 是否显示加载进度条
     *
     * @param isShow
     *  true ： 显示
     *  false： 不现实
     */
    private void showLoadingView(boolean isShow){
        if (isShow){
            mLoadingView.setVisibility(View.VISIBLE);
        }else{
            mLoadingView.setVisibility(View.INVISIBLE);
        }
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
    private void checkBluetoothAble() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            // 显示不可用界面
            mBleView.setVisibility(View.VISIBLE);
            mTvBle.setText("当前设备蓝牙不可用");
            mTvBleNote.setVisibility(View.INVISIBLE);
            mStartBleButton.setVisibility(View.INVISIBLE);
            return;
        }

        if (!bluetoothAdapter.isEnabled()) {
            mBleView.setVisibility(View.VISIBLE);
            // 当前蓝牙不可用，就去开启蓝牙
            mStartBleButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.d(TAG, "打开蓝牙");
                    onEnableBluetoothClicked();
                }
            });
        }else{
            // 如果蓝牙已经打开了
            // 1. 启用加载加界面
            showLoadingView(true);
            // 2. 开启ndn-lite方法
            ndnLiteMainMethod();
        }

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
            switch (intent.getAction()) {
                case BluetoothAdapter.ACTION_STATE_CHANGED:
                    int blueState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0);
                    switch (blueState) {
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
                requestCameraPermission();
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
                if (resultCode == getActivity().RESULT_OK) {
                    mBleView.setVisibility(View.GONE);
                    ndnLiteMainMethod();
                }
                break;
            case Constant.REQUSET_QR: // 扫描二维码
                if (resultCode == getActivity().RESULT_OK) {
                    Log.i(TAG,"相加开启成功！");
                    String qrResult = data.getStringExtra(Constant.QR_RESULT);
                    Toast.makeText(getContext(), qrResult, Toast.LENGTH_SHORT).show();
                }
                break;
            default:
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case Constant.REQUEST_CAMER_PERMISSION:
                Log.i(TAG,"相机请求");
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.i(TAG,"相机权限请求成功！");
                    startCameraActivityForResult();
                } else {
                    Toast.makeText(getContext(), "权限授予失败", Toast.LENGTH_SHORT).show();
                }
                break;
            case Constant.REQUEST_COARSE_LOCATION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                } else {
                    Toast.makeText(getContext(), "权限授予失败", Toast.LENGTH_SHORT).show();
                }
                break;
            default:
        }
    }


    /**
     * 开启创建Activity界面
     */
    private void startCreateQRActivity() {
        Intent intent = new Intent(getContext(), ShareActivity.class);
        startActivity(intent);
    }

    /**
     * 带返回结果的打开相机扫描二维码
     */
    private void startCameraActivityForResult() {
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