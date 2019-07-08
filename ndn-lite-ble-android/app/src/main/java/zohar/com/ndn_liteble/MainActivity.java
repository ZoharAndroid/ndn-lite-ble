package zohar.com.ndn_liteble;


import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.FrameLayout;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";


    private DeviceFragment deviceFragment = DeviceFragment.newInstance();
    private BluetoothFragment bluetoothFragment = BluetoothFragment.newInstance();
    private DeviceInformationFragment deviceInformationFragment = DeviceInformationFragment.newInstance();
    private FeedbackFragment feedbackFragment = FeedbackFragment.newInstance();
    private SettingsFragment settingsFragment = SettingsFragment.newInstance();
    private ShareFragment shareFragment = ShareFragment.newInstance();
    private WiFiFragment wiFiFragment = WiFiFragment.newInstance();


    private FrameLayout mMainContentCoordView;

    // 侧滑布局
    private DrawerLayout mDrawerLayout;
    private FragmentManager fragmentManager;
    private ActionBar actionBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i("BluetoothFragment", "MainActivity - on create");
        setContentView(R.layout.activity_main);
        mDrawerLayout = findViewById(R.id.dl_main_activity);
        Toolbar mToolbar = findViewById(R.id.tb_main_activity);

        mMainContentCoordView = findViewById(R.id.coordinator_main_content_main);

        final NavigationView mNavView = findViewById(R.id.nv_main_activity);
        // Actionbar
        setSupportActionBar(mToolbar);
        actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeAsUpIndicator(R.mipmap.navigation_menu);
        }

        initFragments();

        // 侧滑栏
        mNavView.setCheckedItem(R.id.item_device_navigation);
        switchToFragment(R.id.item_device_navigation);
        actionBar.setTitle("设备");
        //侧护栏显示宽度，默认宽度太宽了，这里修改为屏幕的2/3
        ViewGroup.LayoutParams params = mNavView.getLayoutParams();
        params.width = getResources().getDisplayMetrics().widthPixels * 2 / 3;
        mNavView.setLayoutParams(params);
        // 侧滑栏菜单监听事件
        mNavView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
                switchToFragment(menuItem.getItemId());
                mDrawerLayout.closeDrawers();
                return true;
            }
        });

    }

    /**
     * 初始化 fragment
     *
     * 将所有Fragment添加到Fragment的tranaction中
     */
    private void initFragments(){
        fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.add(R.id.coordinator_main_content_main, deviceFragment);
        transaction.add(R.id.coordinator_main_content_main, bluetoothFragment);
        transaction.add(R.id.coordinator_main_content_main, deviceInformationFragment);
        transaction.add(R.id.coordinator_main_content_main, feedbackFragment);
        transaction.add(R.id.coordinator_main_content_main, settingsFragment);
        transaction.add(R.id.coordinator_main_content_main, shareFragment);
        transaction.add(R.id.coordinator_main_content_main, wiFiFragment);
        transaction.commit();
    }


    /**
     * 隐藏所有的fragment，
     */
    private void hideAllFragments(FragmentTransaction transaction) {
        transaction.hide(deviceFragment);
        transaction.hide(bluetoothFragment);
        transaction.hide(deviceInformationFragment);
        transaction.hide(feedbackFragment);
        transaction.hide(settingsFragment);
        transaction.hide(shareFragment);
        transaction.hide(wiFiFragment);

    }

    /**
     * 切换Fragment
     *
     * @param index
     */
    private void switchToFragment(int index) {
        FragmentTransaction ft = fragmentManager.beginTransaction();
        hideAllFragments(ft);
        switch (index) {
            case R.id.item_device_navigation: // 设备
                ft.show(deviceFragment);
                actionBar.setTitle("设备");
                break;
            case R.id.item_bluetooth_navigation: // 蓝牙
                ft.show(bluetoothFragment);
                actionBar.setTitle("蓝牙");
                break;
            case R.id.item_wifi_navigation: // wifi
                ft.show(wiFiFragment);
                actionBar.setTitle("Wi-Fi");
                break;
            case R.id.item_device_information_navigation: // 设备信息
                ft.show(deviceInformationFragment);
                actionBar.setTitle("设备信息");
                break;
            case R.id.item_settings_navigation: // 设置
                ft.show(settingsFragment);
                actionBar.setTitle("设置");
                break;
            case R.id.item_share_navigation: // 分享
                ft.show(shareFragment);
                actionBar.setTitle("分享");
                break;
            case R.id.item_feedback_navigation: // 反馈
                ft.show(feedbackFragment);
                actionBar.setTitle("反馈");
                break;
        }

        ft.commit();

    }

    /**
     * 替换Fragment
     *
     * @param fragment
     */
    private void replaceFragment(Fragment fragment){
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.coordinator_main_content_main, fragment);
        transaction.commit();
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // 点击返回按钮
                mDrawerLayout.openDrawer(GravityCompat.START);
        }

        return false;
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.i("BluetoothFragment", "MainActivity - onStart");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i("BluetoothFragment", "MainActivity - onResume");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i("BluetoothFragment", "MainActivity - onPause");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.i("BluetoothFragment", "MainActivity - onStop");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i("BluetoothFragment", "MainActivity - onDestroy");
    }
}
