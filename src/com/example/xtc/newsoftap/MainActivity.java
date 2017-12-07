package com.example.xtc.newsoftap;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity implements View.OnClickListener{

    private static final String TAG = "WIFIAP";
    private WifiAdmin wifiAdmin;
    private Button apOnBtn;
    private Button apOffBtn;
    private Button wifiScanBtn;
    public static TextView apTest;
    private TextView connectStatus;
    private static volatile Boolean needScan = false;
    private ScanResultReceiver wifiScanReceiver = new ScanResultReceiver();
    private List<ScanResult> scanResults = new ArrayList<ScanResult>();
    BroadcastReceiver wifiReceiver;

    private long startTime = 0L;
    private long apstartTime = 0L;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);//去掉标题栏
        setContentView(R.layout.activity_main);
        apOnBtn = (Button)findViewById(R.id.apOnBtn);
        apOffBtn = (Button)findViewById(R.id.apOffBtn);
        wifiScanBtn = (Button)findViewById(R.id.wifiScanBtn);
        apTest = (TextView)findViewById(R.id.apText);
        connectStatus = (TextView) findViewById(R.id.wifiTest);

        apOnBtn.setOnClickListener(this);
        apOffBtn.setOnClickListener(this);
        wifiScanBtn.setOnClickListener(this);

        initReceiver();

        wifiAdmin  = new WifiAdmin(this);
        if(wifiAdmin.ifApEnable())
            apTest.setText("已经打开AP");
        else
            apTest.setText("已经关闭AP");

        if (null != wifiAdmin.getConnectedSSID()){
            connectStatus.setText(" CONNECTED");
        }

        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        filter.addAction(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION);
        filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        registerReceiver(wifiScanReceiver,filter);

    }

    /**
     * 按键处理
     */
    @Override
    public void onClick(View v) {
        switch(v.getId()) {
            case R.id.apOnBtn:
                apstartTime = System.currentTimeMillis();
                wifiAdmin.createAp("Androidap");
                wifiAdmin.createAp2("Androidap");
                break;
            case R.id.apOffBtn:
                if(wifiAdmin.ifApEnable())
                    wifiAdmin.closeAp();
                break;
            case R.id.wifiScanBtn:
                if(wifiAdmin.ifApEnable()) {
                    wifiAdmin.closeAp();
                } else {
                    if(!wifiAdmin.isWifiEnabled())
                        wifiAdmin.openWifi();
                    wifiScanAndCount();
                }
                needScan = true;
                break;
            default:
                Log.d(TAG, "not definit id value.");
                break;
        }
    }

    /**
     * 处理ap连接和wifi连接过程显示
     */
    private void initReceiver() {
        wifiReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                switch (action) {
                    case "android.net.wifi.WIFI_AP_STATE_CHANGED":
                        //便携式热点的状态为：10---正在关闭；11---已关闭；12---正在开启；13---已开启
                        int wifistate = intent.getIntExtra("wifi_state", 0);
                        Log.d("WIFIAP", wifistate + "---");
                        switch (wifistate) {
                            case 10:
                                apTest.setText("正在关闭AP");
                                break;
                            case 11:
                                apTest.setText("已经关闭AP");
                                if(needScan) {
                                    if (!wifiAdmin.isWifiEnabled())
                                        wifiAdmin.openWifi();
                                }
                                break;
                            case 12:
                                apTest.setText("正在开启AP");
                                break;
                            case 13:
                                long apOpenTime = System.currentTimeMillis() - apstartTime;
                                apTest.setText("已经打开AP" + "用时" + apOpenTime/1000);
                                break;
                        }
                        break;
                    case WifiManager.NETWORK_STATE_CHANGED_ACTION:
                        NetworkInfo mNetworkInfo = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
                        if (mNetworkInfo == null) {
                            return;
                        }

                        NetworkInfo.DetailedState networkstate = mNetworkInfo.getDetailedState();
                        Log.i(TAG, networkstate.toString());
                        connectStatus.setText(networkstate.toString());

                        if (networkstate == NetworkInfo.DetailedState.CONNECTED) {
                            String BSSID = intent.getStringExtra(WifiManager.EXTRA_BSSID);
                            if (TextUtils.isEmpty(BSSID)) {
                                return;
                            }
                            WifiInfo wifiInfo = intent.getParcelableExtra(WifiManager.EXTRA_WIFI_INFO);
                            String connectedWifi = WifiUtil.removeDoubleQuotes(wifiInfo.getSSID());
                            long connectedTime = System.currentTimeMillis() - startTime;
                            if (connectedTime > 30) connectedTime = 0;
                            Log.i(TAG, connectedWifi + "连接成功 "+ "连接时间：" + connectedTime/1000);
                            connectStatus.setText(connectedWifi + "连接成功" + "用时" + connectedTime/1000);
                        }
                        break;
                    default:
                        break;
                }
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        filter.addAction("android.net.wifi.WIFI_AP_STATE_CHANGED");
        registerReceiver(wifiReceiver, filter);
    }

    /**
     * 处理扫描工程和开始连接：接收到WIFI ENBALE后，进行扫描，接收到扫描结果后进行连接
     */
    public class ScanResultReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(WifiManager.WIFI_STATE_CHANGED_ACTION.equals(action)) {
                int wifiState = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE,0);
                Log.d(TAG, "当前wifi状态" + wifiState);
                if(wifiState == WifiManager.WIFI_STATE_ENABLED)
                    wifiScanAndCount();
            } else if (WifiManager.SCAN_RESULTS_AVAILABLE_ACTION.equals(action)) {
                Log.d(TAG, "获取到扫描结果");
                scanResults = wifiAdmin.getScanResults();
                if(scanResults.size() <= 0)
                    Log.d(TAG, "没有收到扫描到wifi");
                for (ScanResult result : scanResults) {
                    Log.d(TAG, "scanresult:" + result.SSID + " " + result.BSSID + needScan);

                    if(needScan && result.SSID.startsWith("Android")) {
                        Log.d(TAG, "找到目标SSID." + result.SSID);
                        needScan = false;
                        wifiAdmin.addNetworkAndEnable(result.SSID, "12345678", WifiSecurity.SECURITY_WAPI_PSK);// SECURITY_WAPI_PSK
                        return;
                    }
                }
                    needScan = false;
            }
        }
    }

    private Boolean wifiScanAndCount() {
        startTime = System.currentTimeMillis();
        return wifiAdmin.startScan();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(wifiScanReceiver);
        unregisterReceiver(wifiReceiver);
    }
}
