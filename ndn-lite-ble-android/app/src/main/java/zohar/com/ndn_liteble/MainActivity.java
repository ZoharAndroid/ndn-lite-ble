package zohar.com.ndn_liteble;


import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Switch;
import android.widget.Toast;

import com.google.zxing.client.android.CaptureActivity;

import zohar.com.ndn_liteble.utils.Constant;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    // 侧滑布局
    private DrawerLayout mDrawerLayout;
    // BLE是否开启布局容器
    private ConstraintLayout mBleView;
    // BLE开启按钮
    private Button mStartBleButton;



    // 蓝牙状态的监听
    private BluetoothListenerRecevier mBluetoothRecevier;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mDrawerLayout = findViewById(R.id.dl_main_activity);
        Toolbar mToolbar = findViewById(R.id.tb_main_activity);
        FloatingActionButton mFloatingButton = findViewById(R.id.floating_button_main_activity);
        mBleView = findViewById(R.id.ble_check_constraint);
        mStartBleButton = findViewById(R.id.btn_ble_open_main);
        final NavigationView mNavView = findViewById(R.id.nv_main_activity);
        // actionbar
        setSupportActionBar(mToolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeAsUpIndicator(R.mipmap.navigation_menu);
            actionBar.setTitle("设备");
        }

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

        // 侧滑栏
        mNavView.setCheckedItem(R.id.item_device_navigation);
        //侧护栏显示宽度，默认宽度太宽了，这里修改为屏幕的2/3
        ViewGroup.LayoutParams params = mNavView.getLayoutParams();
        params.width = getResources().getDisplayMetrics().widthPixels * 2 / 3;
        mNavView.setLayoutParams(params);
        // 侧滑栏菜单监听事件
        mNavView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
                mDrawerLayout.closeDrawers();
                return true;
            }
        });


        // 悬浮按钮
        mFloatingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MainActivity.this, "悬浮点击按钮", Toast.LENGTH_SHORT).show();
            }
        });

    }

    /**
     * 开启蓝牙
     */
    public void onEnableBluetoothClicked() {
        Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableIntent, Constant.REQUSET_BLE_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        switch (requestCode) {
            case Constant.REQUSET_BLE_CODE: // 蓝牙开启
                mBleView.setVisibility(View.GONE);
                break;

            case Constant.REQUSET_QR: // 扫描二维码
                if (requestCode == RESULT_OK){
                    String qrResult = data.getStringExtra(Constant.QR_RESULT);
                    Toast.makeText(MainActivity.this,qrResult,Toast.LENGTH_SHORT).show();
                }
                break;
            default:
        }
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
            Toast.makeText(MainActivity.this, "当前设备蓝牙不可用！", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (!bluetoothAdapter.isEnabled()) {
            mBleView.setVisibility(View.VISIBLE);
            return false;
        }

        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toobar, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.toolbar_scan_qr_code: // 打开相机扫描二维码
                if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
                    ActivityCompat.requestPermissions(MainActivity.this,new String[]{Manifest.permission.CAMERA}, Constant.PERMISSION_CAMER);
                }else {
                    startCameraActivityForResult();
                }
                break;
            case R.id.toolbar_refresh:
                break;
            case R.id.create_qr_toolbar: // 创建二维码
                break;
            case android.R.id.home:
                // 点击返回按钮
                mDrawerLayout.openDrawer(GravityCompat.START);
            default:
        }

        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case Constant.PERMISSION_CAMER:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    startCameraActivityForResult();
                }else{
                    Toast.makeText(MainActivity.this, "权限授予失败",Toast.LENGTH_SHORT).show();
                }
                break;
                default:
        }
    }

    /**
     * 带返回结果的打开相机扫描二维码
     */
    private void startCameraActivityForResult(){
        Intent qrIntent = new Intent(MainActivity.this, CaptureActivity.class);
        startActivityForResult(qrIntent, Constant.REQUSET_QR);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 注册蓝牙的开关
        registerBluetoothReceiver();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // 注销蓝牙的监听
        unregisterBluetoothReceiver();
    }

    /**
     * 注册蓝牙监听器
     */
    private void registerBluetoothReceiver() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        mBluetoothRecevier = new BluetoothListenerRecevier();
        registerReceiver(mBluetoothRecevier, intentFilter);
    }

    /**
     * 销毁蓝牙监听器
     */
    private void unregisterBluetoothReceiver() {
        if (mBluetoothRecevier != null) {
            unregisterReceiver(mBluetoothRecevier);
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

}
