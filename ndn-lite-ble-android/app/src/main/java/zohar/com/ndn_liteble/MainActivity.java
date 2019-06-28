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
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.FrameLayout;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private FrameLayout mMainContentCoordView;

    // 侧滑布局
    private DrawerLayout mDrawerLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mDrawerLayout = findViewById(R.id.dl_main_activity);
        Toolbar mToolbar = findViewById(R.id.tb_main_activity);

        mMainContentCoordView = findViewById(R.id.coordinator_main_content_main);

        final NavigationView mNavView = findViewById(R.id.nv_main_activity);
        // Actionbar
        setSupportActionBar(mToolbar);
        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeAsUpIndicator(R.mipmap.navigation_menu);
        }

        // 侧滑栏
        mNavView.setCheckedItem(R.id.item_device_navigation);
        final DeivceFragment deviceFragment = new DeivceFragment();
        replaceFragment(deviceFragment);
        actionBar.setTitle("设备");
        //侧护栏显示宽度，默认宽度太宽了，这里修改为屏幕的2/3
        ViewGroup.LayoutParams params = mNavView.getLayoutParams();
        params.width = getResources().getDisplayMetrics().widthPixels * 2 / 3;
        mNavView.setLayoutParams(params);
        // 侧滑栏菜单监听事件
        mNavView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
                switch (menuItem.getItemId()){
                    case R.id.item_device_navigation: // 设备
                        replaceFragment(deviceFragment);
                        actionBar.setTitle("设备");
                        break;
                    case R.id.item_bluetooth_navigation: // 蓝牙
                        replaceFragment(new BluetoothFragment());
                        actionBar.setTitle("蓝牙");
                        break;
                    case R.id.item_wifi_navigation: // wifi
                        replaceFragment(new WiFiFragment());
                        actionBar.setTitle("Wi-Fi");
                        break;
                    case R.id.item_device_information_navigation: // 设备信息
                        replaceFragment(new DeviceInformationFragment());
                        actionBar.setTitle("设备信息");
                        break;
                    case R.id.item_settings_navigation: // 设置
                        replaceFragment(new SettingsFragment());
                        actionBar.setTitle("设置");
                        break;
                    case R.id.item_share_navigation: // 分享
                        replaceFragment(new ShareFragment());
                        actionBar.setTitle("分享");
                        break;
                    case R.id.item_feedback_navigation: // 反馈
                        replaceFragment(new FeedbackFragment());
                        actionBar.setTitle("反馈");
                        break;
                }
                mDrawerLayout.closeDrawers();
                return true;
            }
        });

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
}
