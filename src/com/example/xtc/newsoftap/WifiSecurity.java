package com.example.xtc.newsoftap;

import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;

/**
 * wifi密码检测
 * <p/>
 * Created by hzj on 2016/4/21.
 */
public class WifiSecurity {

    public static final int SECURITY_NONE = 0;
    public static final int SECURITY_WEP = 1;
    public static final int SECURITY_PSK = 2;
    public static final int SECURITY_EAP = 3;
    public static final int SECURITY_WAPI_PSK = 4;
    public static final int SECURITY_WAPI_CERT = 5;

    /**
     * 判断wifi是否有密码
     *
     * @param scanResult wifi
     * @return
     */
    public static boolean isEncrypt(ScanResult scanResult) {
        if (scanResult == null) {
            return false;
        }
        if (scanResult.capabilities.contains("WEP") || scanResult.capabilities.contains("PSK") ||
                scanResult.capabilities.contains("EAP")) {
            return true;
        }
        return false;
    }

    public static int getSecurity(ScanResult result) {
        if (result.capabilities.contains("WAPI-PSK")) {
            /// M:  WAPI_PSK
            return SECURITY_WAPI_PSK;
        } else if (result.capabilities.contains("WAPI-CERT")) {
            /// M: WAPI_CERT
            return SECURITY_WAPI_CERT;
        } else if (result.capabilities.contains("WEP")) {
            return SECURITY_WEP;
        } else if (result.capabilities.contains("PSK")) {
            return SECURITY_PSK;
        } else if (result.capabilities.contains("EAP")) {
            return SECURITY_EAP;
        }
        return SECURITY_NONE;
    }

    static int getSecurity(WifiConfiguration config) {
        if (config.allowedKeyManagement.get(WifiConfiguration.KeyMgmt.WPA_PSK)) {
            return SECURITY_PSK;
        }
        if (config.allowedKeyManagement.get(WifiConfiguration.KeyMgmt.WPA_EAP) ||
                config.allowedKeyManagement.get(WifiConfiguration.KeyMgmt.IEEE8021X)) {
            return SECURITY_EAP;
        }

        if (config.wepTxKeyIndex >= 0 && config.wepTxKeyIndex < config.wepKeys.length
                && config.wepKeys[config.wepTxKeyIndex] != null) {
            return SECURITY_WEP;
        }
        return (config.wepKeys[0] != null) ? SECURITY_WEP : SECURITY_NONE;
    }

}

