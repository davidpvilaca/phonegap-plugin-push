package com.adobe.phonegap.push.match;

import android.content.Context;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by alvaro.menezes on 06/12/2017.
 */

public class OrderApiService {
  // TODO URL deve vir do app
  private static final String BASE_URL = "https://api.dev.beebee.com.br/api/v1/orders/";

  private int mOrderId;
  private String mUserToken;
  private RequestQueue mQueue;

  public OrderApiService(Context ctx, int orderId, String userToken) {
    mOrderId = orderId;
    mUserToken = userToken;
    mQueue = Volley.newRequestQueue(ctx);
  }

  public void accept(Response.Listener<JSONObject> success, Response.ErrorListener error) {
    JsonRequest(BASE_URL + mOrderId + "/accept", success, error);
  }

  public void reject(Response.Listener<JSONObject> success, Response.ErrorListener error) {
    JsonRequest(BASE_URL + mOrderId + "/reject", success, error);
  }

  private void JsonRequest(String url, Response.Listener<JSONObject> success, Response.ErrorListener error){
    JsonObjectRequest jsObjRequest = new JsonObjectRequest
    (Request.Method.PUT, url, null, success, error)
    {
      @Override
      public Map<String, String> getHeaders() throws AuthFailureError {
        Map<String, String> params = new HashMap<String, String>();
        params.put("Authorization", "Bearer " + mUserToken);

        return params;
      }
    };

    mQueue.add(jsObjRequest);
  }
}
