package com.adobe.phonegap.push.match;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by alvaro.menezes on 07/12/2017.
 */

public class RejectedOrders {

  private Activity mActivity;
  private static final String REJECTED_ORDERS = "REJECTED_ORDERS";
  private static final int MAX_LENGTH = 50;

  public RejectedOrders(Activity activity){
    mActivity = activity;
  }

  public void add(String uid) {
    SharedPreferences prefs = mActivity.getPreferences(Context.MODE_PRIVATE);
    SharedPreferences.Editor prefsEditor = prefs.edit();

    if (prefs.contains(REJECTED_ORDERS)) {
      String strRejectedOrders = prefs.getString(REJECTED_ORDERS, null);
      ArrayList<String> rejectedOrders = new ArrayList<String>(Arrays.asList(strRejectedOrders.split(",")));
      rejectedOrders.add(uid);

      int begin = rejectedOrders.size() > MAX_LENGTH ? rejectedOrders.size() - MAX_LENGTH : 0;
      int end = rejectedOrders.size();
      prefsEditor.putString(REJECTED_ORDERS, TextUtils.join(",", rejectedOrders.subList(begin, end)));
    } else {
      prefsEditor.putString(REJECTED_ORDERS, uid);
    }

    prefsEditor.apply();
  }

  public boolean exists(String poolDateId){
    SharedPreferences prefs = mActivity.getPreferences(Context.MODE_PRIVATE);

    if (prefs.contains(REJECTED_ORDERS)) {
      String strRejectedOrders = prefs.getString(REJECTED_ORDERS, null);
      List<String> rejectedOrders = Arrays.asList(strRejectedOrders.split(","));

      return rejectedOrders.contains(poolDateId);
    }

    return false;
  }
}
