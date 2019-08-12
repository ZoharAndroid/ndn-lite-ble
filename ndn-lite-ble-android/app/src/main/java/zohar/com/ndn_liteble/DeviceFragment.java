package zohar.com.ndn_liteble;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.nfc.Tag;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.client.android.CaptureActivity;
import com.google.zxing.client.android.share.ShareActivity;

import net.named_data.jndn.Data;
import net.named_data.jndn.Face;
import net.named_data.jndn.Interest;
import net.named_data.jndn.InterestFilter;
import net.named_data.jndn.Name;
import net.named_data.jndn.OnData;
import net.named_data.jndn.OnInterestCallback;
import net.named_data.jndn.OnTimeout;
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
import zohar.com.ndn_liteble.adapter.BoardAdapter;
import zohar.com.ndn_liteble.model.Board;
import zohar.com.ndn_liteble.utils.Constant;
import zohar.com.ndn_liteble.utils.SendInterestTaskV2;

import static NDNLiteSupport.SignOnBasicControllerBLE.secureSignOn.SignOnControllerConsts.KD_PUB_CERTIFICATE_NAME_PREFIX;
import static NDNLiteSupport.SignOnBasicControllerBLE.secureSignOn.secureSignOnVariants.SecureSignOnVariantStrings.SIGN_ON_VARIANT_BASIC_ECC_256;
import static NDNLiteSupport.SignOnBasicControllerBLE.secureSignOn.utils.SecurityHelpers.asnEncodeRawECPublicKeyBytes;

public class DeviceFragment extends Fragment {

    private static final String TAG = "DeviceFragment";

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
    // ProgressBar显示匹配节点的界面
    private ConstraintLayout mLoadingView;

    // 蓝牙状态的监听
    private BluetoothListenerRecevier mBluetoothRecevier;
    // BLEUnicastConnectionMaintainer
    private BLEUnicastConnectionMaintainer mBLEUnicastConnectionMaintainer;
    // SignOnBasicControllerBLE
    private SignOnBasicControllerBLE mSignOnBasicControllerBLE;
    // signon回调
    private SignOnBasicControllerBLE.SecureSignOnBasicControllerBLECallbacks mSecureSignOnBasicControllerBLECallbacks;
    // 创建BLE face
    private BLEFace m_bleFace;
    private BLEFace m_bleFace2;

    // 发送兴趣包的回调函数
    private OnInterestCallback onInterest;

    // The device identifier of the example nRF52840, in hex string format.
    private String m_expectedDeviceIdentifierHexString = "010101010101010101010101";
    private String m_expectedDeviceIdentifierHexString2 = "010101010101010101010102";
    private RecyclerView mRecycleNode;

    // 板子的数量
    private List<Board> boards = new ArrayList<>();
    private BoardAdapter boardAdapter;

    /**
     * 通过这个方法，可以传递参数到Fragment
     *
     * @return
     */
    public static DeviceFragment newInstance(){
        Bundle bundle = new Bundle();
        DeviceFragment deviceFragment = new DeviceFragment();
        deviceFragment.setArguments(bundle);
        return deviceFragment;
    }


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

        // 显示加载界面
        showLoadingView(true);

        // Callback for when an interest is received. In this example, the nRf52840 sends an interest to
        // us after sign on is complete, and triggers this callback.
        onInterest = new OnInterestCallback() {
            @Override
            public void onInterest(Name prefix, Interest interest, Face face, long interestFilterId,
                                   InterestFilter filter) {
                Log.i(TAG, "onInterest()回调, interest的前缀为: " + prefix.toUri());
                // 如果interest的是board1
                if (prefix.toUri().equals(KD_PUB_CERTIFICATE_NAME_PREFIX + m_expectedDeviceIdentifierHexString)) {
                    Log.i(TAG, "onInterest - Got interest for certificate of device with device identifier: " +
                            m_expectedDeviceIdentifierHexString);

                    try {
                        Log.i(TAG, "Responding to interest from device with its certificate...");
                        face.putData(SignOnBasicControllerBLE.getInstance().getKDPubCertificateOfDevice(m_expectedDeviceIdentifierHexString));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                //  如果interest回调会board2
                if (prefix.toUri().equals(KD_PUB_CERTIFICATE_NAME_PREFIX + m_expectedDeviceIdentifierHexString2)) {
                    Log.i(TAG, "onInterest - Got interest for certificate of device with device identifier: " +
                            m_expectedDeviceIdentifierHexString2);
                    try {
                        Log.i(TAG, "onInterest - Responding to interest from device with its certificate...");
                        face.putData(SignOnBasicControllerBLE.getInstance().getKDPubCertificateOfDevice(m_expectedDeviceIdentifierHexString2));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

            }
        };


        // SignOn回调方法
        mSecureSignOnBasicControllerBLECallbacks = new SignOnBasicControllerBLE.SecureSignOnBasicControllerBLECallbacks() {
            @Override
            public void onDeviceSignOnComplete(String deviceIdentifierHexString) {

                // 判断当前数据是否已经添加过了
                boolean isAddFlag = false;
                Log.i(TAG, "onDeviceSignOnComplete - Onboarding was successful for device with device identifier hex string : " +
                        deviceIdentifierHexString);
                Log.i(TAG, "onDeviceSignOnComplete - Mac address of device succesfully onboarded: " +
                        mSignOnBasicControllerBLE.getMacAddressOfDevice(deviceIdentifierHexString));
                Log.i(TAG, "onDeviceSignOnComplete - Name of device's KDPubCertificate: " +
                        mSignOnBasicControllerBLE.getKDPubCertificateOfDevice(deviceIdentifierHexString)
                                .getName().toUri()
                );


                Board board = new Board();
                board.setMacAddress(mSignOnBasicControllerBLE.getMacAddressOfDevice(deviceIdentifierHexString));
                board.setIdentifierHex(deviceIdentifierHexString);
                board.setKDPubCertificate(mSignOnBasicControllerBLE.getKDPubCertificateOfDevice(deviceIdentifierHexString).getName().toUri());

                for (Board temp : boards) {
                    if (temp.getIdentifierHex().equals(board.getIdentifierHex())) {
                        isAddFlag = true;
                        break;
                    }
                }

                // 如果没有添加过，那么添加到List中
                if (!isAddFlag) {
                    boards.add(board);
                    // 更新UI
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            boardAdapter.notifyItemInserted(boards.size() - 1); // 刷新recyclerview要显示的位置
                            mRecycleNode.scrollToPosition(boards.size() - 1); // 将recyclerview定位到最后一个位置
                        }
                    });
                }

                // Create a BLE face to the device that onboarding completed successfully for.
                if (deviceIdentifierHexString.equals(m_expectedDeviceIdentifierHexString)) {
                    Log.i(TAG, "onDeviceSignOnComplete: create ble face for board 1");
                    m_bleFace = new BLEFace(mSignOnBasicControllerBLE.getMacAddressOfDevice(deviceIdentifierHexString),
                            onInterest);
                } else if (deviceIdentifierHexString.equals(m_expectedDeviceIdentifierHexString2)) {
                    Log.i(TAG, "onDeviceSignOnComplete: create ble face for board 2");
                    m_bleFace2 = new BLEFace(mSignOnBasicControllerBLE.getMacAddressOfDevice(deviceIdentifierHexString),
                            onInterest);
                } else
                    Log.i(TAG, "onDeviceSignOnComplete: wrong device identifier...");

                // 隐藏加载界面
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        showLoadingView(false);
                        mRecycleNode.setVisibility(View.VISIBLE);
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
        } catch (
                Exception e) {
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

        // RadioGroup选择
        boardAdapter.setOnCheckedChangeListener(new BoardAdapter.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId, int position) {

                // 获取当前点击的实例
                int currentBoardId;
                Board board = boards.get(position);
                if (board.getIdentifierHex().equals(m_expectedDeviceIdentifierHexString)) {
                    currentBoardId = 1;
                } else {
                    currentBoardId = 2;
                }
                Log.i(TAG, "当前板子的id" + currentBoardId);
                switch (checkedId) {
                    case R.id.rb_only_controller: // 只能控制自己
                        Name commandInterest1 = new Name("/NDN-IoT/TrustChange/Board" + currentBoardId + "/ControllerOnly");
                        Log.i(TAG, "onMenuItemClick: constructed name is:" + commandInterest1.toString());
                        if (currentBoardId == 1) {
                            // 第一块板子
                            SendInterestTaskV2 SITask = new SendInterestTaskV2();
                            SITask.execute(commandInterest1); // 开启子线程发送兴趣包
                        } else if (currentBoardId == 2) {
                            // 第二块板子
                            SendInterestTaskV3 SITask = new SendInterestTaskV3();
                            SITask.execute(commandInterest1);
                        }
                        Toast.makeText(getContext(), "开始转变策略：Controller", Toast.LENGTH_SHORT).show();
                        break;
                    case R.id.rb_all_node: // 能相互控制
                        Name commandInterest2 = new Name("/NDN-IoT/TrustChange/Board" + currentBoardId + "/AllNode");
                        Log.i(TAG, "onMenuItemClick: constructed name is:" + commandInterest2.toString());
                        if (currentBoardId == 1) {
                            SendInterestTaskV2 SITask2 = new SendInterestTaskV2();
                            SITask2.execute(commandInterest2); // 开启子线程发送兴趣包
                        } else if (currentBoardId == 2) {
                            SendInterestTaskV3 SITask2 = new SendInterestTaskV3();
                            SITask2.execute(commandInterest2);
                        }
                        Toast.makeText(getContext(), "开始转变策略：All node", Toast.LENGTH_SHORT).show();
                        break;
                    default:
                }
            }
        });

        //板子点击的图片
        boardAdapter.setOnClickBoardImageListener(new BoardAdapter.OnClickBoardImageListener() {
            @Override
            public void onClickBoardImageListener(View v, final Board board) {
                // 创建popup menu
                final PopupMenu popupMenu = new PopupMenu(v.getContext(), v);
                // 添加布局
                popupMenu.getMenuInflater().inflate(R.menu.menu_police_select, popupMenu.getMenu());
                // 注册点击事件
                popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {

                        // 获取当前点击的实例
                        int currentBoardId;

                        if (board.getIdentifierHex().equals(m_expectedDeviceIdentifierHexString)) {
                            currentBoardId = 1;
                        } else {
                            currentBoardId = 2;
                        }
                        Log.i(TAG, "当前板子的id" + currentBoardId);
                        switch (item.getItemId()) {
                            case R.id.only_controller: // 只能控制自己
                                Name commandInterest1 = new Name("/NDN-IoT/TrustChange/Board" + currentBoardId + "/ControllerOnly");
                                Log.i(TAG, "onMenuItemClick: constructed name is:" + commandInterest1.toString());
                                if (currentBoardId == 1) {
                                    // 第一块板子
                                    SendInterestTaskV2 SITask = new SendInterestTaskV2();
                                    SITask.execute(commandInterest1); // 开启子线程发送兴趣包
                                } else if (currentBoardId == 2) {
                                    // 第二块板子
                                    SendInterestTaskV3 SITask = new SendInterestTaskV3();
                                    SITask.execute(commandInterest1);
                                }
                                Toast.makeText(getContext(), "开始转变策略：Controller", Toast.LENGTH_SHORT).show();
                                break;
                            case R.id.all_node: // 能相互控制
                                Name commandInterest2 = new Name("/NDN-IoT/TrustChange/Board" + currentBoardId + "/AllNode");
                                Log.i(TAG, "onMenuItemClick: constructed name is:" + commandInterest2.toString());
                                if (currentBoardId == 1) {
                                    SendInterestTaskV2 SITask2 = new SendInterestTaskV2();
                                    SITask2.execute(commandInterest2); // 开启子线程发送兴趣包
                                } else if (currentBoardId == 2) {
                                    SendInterestTaskV3 SITask2 = new SendInterestTaskV3();
                                    SITask2.execute(commandInterest2);
                                }
                                Toast.makeText(getContext(), "开始转变策略：All node", Toast.LENGTH_SHORT).show();
                                break;
                            default:
                        }


                        return false;
                    }
                });
                // 显示popup menu
                popupMenu.show();
            }
        });

        // switch事件
        boardAdapter.setOnClickSwitchListener(new BoardAdapter.OnClickSwitchListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked, int position) {
                Board board = boards.get(position);
                // 获取当前点击的实例
                int currentBoardId;

                if (board.getIdentifierHex().equals(m_expectedDeviceIdentifierHexString)) {
                    currentBoardId = 1;
                } else {
                    currentBoardId = 2;
                }
                Log.i(TAG, "当前板子的id：" + currentBoardId);
                Name commandInterest;
                if (currentBoardId == 1){
                    SendInterestTaskV2 sendInterestTaskV2;
                    if (isChecked){
                        // create interest name
                        commandInterest = new Name("/NDN-IoT/Board1/LED/ON");
                        // create a thread and execute it to send interest
                        sendInterestTaskV2 = new SendInterestTaskV2();
                        sendInterestTaskV2.execute(commandInterest);
                    }else{
                        commandInterest = new Name("/NDN-IoT/Board1/LED/OFF");
                        sendInterestTaskV2 = new SendInterestTaskV2();
                        sendInterestTaskV2.execute(commandInterest);
                    }
                }
                if (currentBoardId == 2) {
                    SendInterestTaskV3 sendInterestTaskV3;
                    if (isChecked) {
                        // board2 as above
                        commandInterest = new Name("/NDN-IoT/Board2/LED/ON");
                         sendInterestTaskV3 = new SendInterestTaskV3();
                        sendInterestTaskV3.execute(commandInterest);
                    }else{
                        commandInterest = new Name("/NDN-IoT/Board2/LED/OFF");
                        sendInterestTaskV3 = new SendInterestTaskV3();
                        sendInterestTaskV3.execute(commandInterest);
                    }
                }
            }
        });

    }

    /**
     * 初始化UI控件
     */
    private void initUIView() {
        mLoadingView = view.findViewById(R.id.cl_loading_device);
        mBleView = view.findViewById(R.id.ble_check_constraint);
        mStartBleButton = view.findViewById(R.id.btn_ble_open_main);
        mFloatingButton = view.findViewById(R.id.floating_button_main_activity);
        mTvBle = view.findViewById(R.id.tv_bluetooth_disable);
        mTvBleNote = view.findViewById(R.id.tv_bluetooth_disable_note);
        mRecycleNode = view.findViewById(R.id.recycle_show_node_device);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        layoutManager.setOrientation(LinearLayout.VERTICAL);
        mRecycleNode.setLayoutManager(layoutManager);
        mRecycleNode.addItemDecoration(new DividerItemDecoration(getContext(), DividerItemDecoration.VERTICAL));
        boardAdapter = new BoardAdapter(boards);
        mRecycleNode.setAdapter(boardAdapter);

    }

    /**
     * 是否显示加载进度条
     *
     * @param isShow true ： 显示
     *               false： 不现实
     */
    private void showLoadingView(boolean isShow) {
        if (isShow) {
            mLoadingView.setVisibility(View.VISIBLE);
        } else {
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
            mTvBleNote.setVisibility(View.GONE);
            mStartBleButton.setVisibility(View.GONE);
            Log.i(TAG, "当前设备蓝牙不可用");
        }else if (!bluetoothAdapter.isEnabled()) {
            mBleView.setVisibility(View.VISIBLE);
            // 当前蓝牙不可用，就去开启蓝牙
            mStartBleButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.d(TAG, "打开蓝牙");
                    onEnableBluetoothClicked();
                }
            });
        } else {
            // 如果蓝牙已经打开了，开启ndn-lite方法
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
                BLEUnicastConnectionMaintainer.getInstance().initialize(getActivity());
                boards.clear();
                boardAdapter.notifyDataSetChanged();
                mRecycleNode.setVisibility(View.GONE);
                // 重新调用ndn-lite主方法
                ndnLiteMainMethod();
                Toast.makeText(getContext(),"Todo：刷新", Toast.LENGTH_SHORT).show();
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
                    Log.i(TAG, "相加开启成功！");
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
                Log.i(TAG, "相机请求");
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.i(TAG, "相机权限请求成功！");
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

    /**
     * 发送兴趣包 v2
     */
    public class SendInterestTaskV2 extends AsyncTask<Name, Integer, Boolean> {


        Data comebackData = new Data();

        @Override
        protected void onPreExecute() {
            Log.i(TAG, "SendInterestTaskV2 : onPreExecute ");
        }

        @Override
        protected Boolean doInBackground(Name... names) {
            Log.i(TAG, "SendInterestTaskV2 : doInBackground, names: " + names[0]);

            IncomingData incomingData = new IncomingData();

            Interest pendingInterest = new Interest(names[0]);
            try {
                m_bleFace.expressInterest(pendingInterest, incomingData);
                m_bleFace.processEvents();
                Thread.sleep(50);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return false;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
        }

        /**
         * 回来的数据包
         */
        private class IncomingData implements OnData,OnTimeout {
            @Override
            public void onData(Interest interest, Data data) {
                Log.i(TAG, "获取数据包：" + data.getName().toUri());
                String msg = data.getContent().toString();
                Log.i( TAG, "收到的数据包: " + msg);
                if (msg.length() == 0) {
                    Log.i(TAG, "数据包为空");
                } else if (msg.length() > 0) {
                    comebackData.setContent(data.getContent());
                }
            }

            @Override
            public void onTimeout(Interest interest) {
                Log.i(TAG, "SendInterestTaskV3 - incomingData - onTimeout -> " + interest.toUri());
            }
        }

    }

    public class SendInterestTaskV3 extends AsyncTask<Name, Integer, Boolean> {

        Data comeBackData = new Data();

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Boolean doInBackground(Name... names) {

            Log.i(TAG, "SendInterestTaskV3 - doInBackground - names ->" + names[0].toUri());
            Interest pendingInterest = new Interest(names[0]);
            incomingData incomD = new incomingData();
            try {
                m_bleFace2.expressInterest(pendingInterest, incomD);
                m_bleFace2.processEvents();
                Thread.sleep(50);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
        }


        @Override
        protected void onPostExecute(Boolean aBoolean) {
            super.onPostExecute(aBoolean);
        }

        private class incomingData implements OnData, OnTimeout {
            @Override
            public void onData(Interest interest, Data data) {
                Log.i(TAG, "接受数据包");
                Log.i(TAG, "Got data packet with name:" + data.getName().toUri());
                String msg = data.getContent().toString();
                Log.i(TAG, "onData: " + msg);
                if (msg.length() == 0) {
                    Log.i(TAG, "Data is null");
                } else if (msg.length() > 0) {
                    comeBackData.setContent(data.getContent());
                }
            }

            @Override
            public void onTimeout(Interest interest) {
                Log.i(TAG, "SendInterestTaskV3 - incomingData - onTimeout -> " + interest.toUri());
            }
        }
    }
}
