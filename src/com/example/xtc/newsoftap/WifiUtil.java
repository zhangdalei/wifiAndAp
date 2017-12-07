package com.example.xtc.newsoftap;

import android.text.TextUtils;

/**
 * Wifi工具
 * <p/>
 * Created by hzj on 2016/5/19.
 */
public class WifiUtil {

    public static String removeDoubleQuotes(String ssid) {
        if (TextUtils.isEmpty(ssid)) {
            return ssid;
        }
        int length = ssid.length();
        if ((length > 1) && (ssid.charAt(0) == '"')
                && (ssid.charAt(length - 1) == '"')) {
            return ssid.substring(1, length - 1);
        }
        if ("0x".equals(ssid) || "0X".equals(ssid)) {
            return null;
        }
        return ssid;
    }

    public static String convertToQuotedString(String ssid) {
        int length = ssid.length();
        if ((length > 1) && (ssid.charAt(0) == '"')
                && (ssid.charAt(length - 1) == '"')) {
            return ssid;
        }
        return "\"" + ssid + "\"";
    }
}