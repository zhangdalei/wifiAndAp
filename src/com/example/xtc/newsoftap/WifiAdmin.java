package com.example.xtc.newsoftap;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static android.net.ConnectivityManager.TETHERING_WIFI;

/**
 * wifi管理工具
 * <p/>
 * Created by hzj on 2016/4/21.
 */
public class WifiAdmin {

    private static final String TAG = "WifiAdmin";

    private WifiManager mWifiManager;
    private ConnectivityManager mCm;
    private Handler mHandler = new Handler();
    private OnStartTetheringCallback mStartTetheringCallback = new OnStartTetheringCallback();

    // 构造器
    public WifiAdmin(Context context) {
        mWifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        mCm = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    // 打开WIFI
    public void openWifi() {
        if (!isWifiEnabled()) {
            Log.i(TAG, "正在开启WiFi");
            mWifiManager.setWifiEnabled(true);
            Log.i(TAG, "开启WiFi");
        }
    }

    // 关闭wifi
    public void closeWifi() {
        if (isWifiEnabled()) {
            Log.i(TAG, "关闭WiFi");
            mWifiManager.setWifiEnabled(false);
        }
    }

    public int getWifiState() {
        return mWifiManager.getWifiState();
    }

    public boolean isWifiEnabled() {
        return mWifiManager.isWifiEnabled();
    }

    // 得到配置好的网络
    public List<WifiConfiguration> getConfiguration() {
        return mWifiManager.getConfiguredNetworks();
    }

    private int getNetworkId(String SSID) {
        WifiConfiguration configuration = getWifiConfiguration(SSID);
        if (configuration != null) {
            return configuration.networkId;
        }
        return -1;
    }

    public boolean startScan(int chtime, int[] channels) {
        Log.d(TAG, "开始扫描，startScan，chtime：" + chtime);
        boolean result = false;
        try {
            Class<?> c = mWifiManager.getClass();
            Method startScan = c.getDeclaredMethod("startScan", int.class,
                    channels.getClass());
            startScan.setAccessible(true);
            result = (boolean) startScan.invoke(mWifiManager, chtime, channels);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    public boolean startScan() {
        Log.d(TAG, "开始扫描，startScan");
        return mWifiManager.startScan();
    }

    public List<ScanResult> getScanResults() {
        //得到扫描结果
        List<ScanResult> results = mWifiManager.getScanResults();
        List<ScanResult> mWifiList = new ArrayList<>();
        if (results == null || results.size() <= 0) {
            Log.d(TAG, "wifi扫描结果为空");
            return mWifiList;
        }
        for (ScanResult result : results) {
            if (result == null || TextUtils.isEmpty(result.SSID)) {
                continue;
            }

            if (result.capabilities.contains("[IBSS]")) {
                continue;
            }
            boolean found = false;
            for (ScanResult item : mWifiList) {
                if (item.SSID.equals(result.SSID)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                mWifiList.add(result);
            }
        }
        Collections.sort(mWifiList, new Comparator<ScanResult>() {
            @Override
            public int compare(ScanResult lhs, ScanResult rhs) {
                if (lhs.level == rhs.level) {
                    return 0;
                }
                return lhs.level > rhs.level ? -1 : 1;// 信号强度排序
//                return lhs.SSID.compareToIgnoreCase(rhs.SSID); // wifi名称排序
            }
        });
        return mWifiList;
    }

    // 得到WifiInfo的所有信息包
    public WifiInfo getWifiInfo() {
        return mWifiManager.getConnectionInfo();
    }

    /**
     * 创建wifi热点的。
     *
     * @param ssid         SSID的名称
     * @param password     指定SSID网络的密码，当不需要密码是置空""
     * @param securityType 热点类型：无密码 / WEP密码验证（未充分测试）/ WAP或WAP2 PSK密码验证
     * @return WifiConfiguration
     */
    public WifiConfiguration createWifiInfo(String ssid, String password, int securityType) {
        WifiConfiguration config = new WifiConfiguration();
        config.SSID = WifiUtil.convertToQuotedString(ssid);
        config.hiddenSSID = true;

        WifiConfiguration tempConfig = getWifiConfiguration(ssid);
        if (tempConfig != null) {
            mWifiManager.removeNetwork(tempConfig.networkId);
        }

        if (securityType == WifiSecurity.SECURITY_NONE) {//WIFICIPHER_NOPASS
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
            config.wepTxKeyIndex = 0;
        } else if (securityType == WifiSecurity.SECURITY_WEP) { //WIFICIPHER_WEP
            int length = password.length();
            if ((length == 10 || length == 26 || length == 58) &&
                    password.matches("[0-9A-Fa-f]*")) {
                config.wepKeys[0] = password;
            } else {
                config.wepKeys[0] = '"' + password + '"';
            }

            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
            config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
            config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.SHARED);
            config.wepTxKeyIndex = 0;
        } else {//WIFICIPHER_WPA
            config.preSharedKey = "\"" + password + "\"";

            config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
            config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
            //config.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
            config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
            config.status = WifiConfiguration.Status.ENABLED;
        }
        return config;
    }

    /**
     * android7.0之前的用此方法打开热点
     * SSID: 热点SSID
     */
    public void createAp(String SSID) {
        if(mWifiManager.isWifiEnabled())
            mWifiManager.setWifiEnabled(false);
        try {
            WifiConfiguration apConfiguration = new WifiConfiguration();
            apConfiguration.SSID = SSID;
            apConfiguration.preSharedKey = "12345678";
            apConfiguration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);//WPA_PSK 不使用密码
            Method method = mWifiManager.getClass().getMethod("setWifiApEnabled",WifiConfiguration.class, boolean.class);
            method.invoke(mWifiManager,apConfiguration, true);
        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * android7.0以上，打开热点
     */
    public void createAp2(String SSID) {
        if(mWifiManager.isWifiEnabled())
            mWifiManager.setWifiEnabled(false);
        mCm.startTethering(TETHERING_WIFI, true, mStartTetheringCallback, mHandler);
    }
    /**
     * 关闭热点
     */
    public void closeAp() {
        if(ifApEnable()) {
            try {
                Method method = mWifiManager.getClass().getMethod("getWifiApConfiguration");
                method.setAccessible(true);
                WifiConfiguration config = (WifiConfiguration) method.invoke(mWifiManager);
                Method method2 = mWifiManager.getClass().getMethod("setWifiApEnabled", WifiConfiguration.class, boolean.class);
                method2.invoke(mWifiManager, config, false);
            }catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 热点是否打开
     */
    public boolean ifApEnable() {
        try {
            Method method = mWifiManager.getClass().getMethod("isWifiApEnabled");
            method.setAccessible(true);
            return (Boolean)method.invoke(mWifiManager);
        }catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    // 添加一个网络并连接
    public boolean addNetworkAndEnable(String ssid, String password, int securityType) {
        int networkId = addNetwork(ssid, password, securityType);
        Log.i(TAG, "创建连接，networkId: " + networkId);
        return mWifiManager.enableNetwork(networkId, true);
    }

    public int addNetwork(String ssid, String password, int securityType) {
        WifiConfiguration configuration = createWifiInfo(ssid, password, securityType);
        Log.i(TAG, "创建连接，configuration: " + configuration.toString());
        return mWifiManager.addNetwork(configuration);
    }

    /**
     * 删除已保存的wifi
     */
    public boolean removeWifi(String SSID) {
        int networkId = getNetworkId(SSID);
        if (networkId == -1) {
            return false;
        }
        return removeWifi(networkId);
    }

    /**
     * 删除已保存的wifi，并断开连接
     */
    public boolean removeAndDisconnectedWifi(String SSID) {
        int networkId = getNetworkId(SSID);
        if (networkId == -1) {
            return false;
        }
        Log.w(TAG, "取消连接，删除wifi:" + networkId);
        return removeWifi(networkId) && mWifiManager.disconnect();
    }

    /**
     * 断开连接中的WiFi
     *
     * @param SSID        当前连接的SSID
     * @param autoConnect 设置次SSID是否可用，如果为false，则不会再自动连接
     */
    public boolean disconnectWifi(String SSID, boolean autoConnect) {
        int networkId = getNetworkId(SSID);
        if (networkId == -1) {
            return false;
        }
        if (autoConnect) {
            return mWifiManager.disconnect();
        }
        return mWifiManager.disableNetwork(networkId) && mWifiManager.disconnect();
    }

    private boolean removeWifi(int networkId) {
        boolean removeNetwork = mWifiManager.removeNetwork(networkId);
        boolean saveConfiguration = mWifiManager.saveConfiguration();
        return removeNetwork && saveConfiguration;
    }

    /**
     * 清除所有的WiFi
     */
    public boolean removeAllConfiguration() {
        List<WifiConfiguration> configuration = getConfiguration();
        if (configuration == null) {
            return false;
        }

        boolean removeResult = true;
        for (WifiConfiguration wifiConfiguration : configuration) {
            boolean disableResult = mWifiManager.disableNetwork(wifiConfiguration.networkId);
            Log.d(TAG, "disableResult = " + disableResult);
            if (!removeWifi(wifiConfiguration.networkId)) {
                removeResult = false;
            }
        }
        Log.w(TAG, "清除所有已保存WiFi，removeResult = " + removeResult);
        return removeResult;
    }

    public WifiConfiguration getWifiConfiguration(String SSID) {
        List<WifiConfiguration> configuration = getConfiguration();
        if (configuration == null || configuration.size() <= 0) {
            return null;
        }

        SSID = WifiUtil.convertToQuotedString(SSID);
        for (WifiConfiguration config : configuration) {
            if (config != null && SSID.equals(config.SSID)) {
                return config;
            }
        }
        return null;
    }

    public String getConnectedSSID() {
        WifiInfo wifiInfo = getWifiInfo();
        if (wifiInfo == null) {
            return null;
        }
        String connectSSID = wifiInfo.getSSID();
        if ("<unknown ssid>".equals(connectSSID)) {
            return null;
        }
        return WifiUtil.removeDoubleQuotes(connectSSID);
    }

    /**
     * 判断指定wifi是否已连接
     *
     * @param ssid wifi的ssid
     * @return
     */
    public boolean isConnected(String ssid) {
        String connectSSID = getConnectedSSID();
        if (TextUtils.isEmpty(connectSSID)) {
            return false;
        }
        return connectSSID.equals(ssid);
    }

    public interface OnConnectCallBack {
        void onFinish(boolean result);
    }

    private static final class OnStartTetheringCallback extends
            ConnectivityManager.OnStartTetheringCallback {
        @Override
        public void onTetheringStarted() {
            Log.d(TAG, "ap started success.");
        }
        @Override
        public void onTetheringFailed() {
            Log.d(TAG, "ap started failed.");
        }
    }
}

