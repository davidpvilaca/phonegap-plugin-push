package com.adobe.phonegap.push.match;

import android.content.Context;
import android.location.Location;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
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

  public void eta(Location location, Response.Listener<JSONObject> success, Response.ErrorListener error) {
    JsonRequest(Request.Method.GET, BASE_URL + mOrderId + "/eta?lat=" + location.getLatitude()
      + "&lng=" + location.getLongitude(), null, success, error);
  }

  private void JsonRequest(String url, Response.Listener<JSONObject> success, Response.ErrorListener error){
    this.JsonRequest(Request.Method.PUT, url, null, success, error);
  }

  private void JsonRequest(int method, String url, JSONObject requestObject, Response.Listener<JSONObject> successListener,
                           final Response.ErrorListener errorListener){

    JsonObjectRequest jsObjRequest = new JsonObjectRequest
      (method, url, requestObject, successListener, new Response.ErrorListener() {
        @Override
        public void onErrorResponse(VolleyError error) {
          String message = handleServerError(error);
          if (message != null) {
            errorListener.onErrorResponse(new VolleyError(message));
          } else {
            errorListener.onErrorResponse(error);
          }
        }
      })
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

  private String handleServerError(VolleyError err) {
    NetworkResponse response = err.networkResponse;

    Log.d("ServerError", new String(response.data));

    try {
      JSONObject result = new JSONObject(new String(response.data));
      if (result != null && result.has("error")) {
        return result.getString("error");
      }

    } catch (JSONException e) {
      e.printStackTrace();
    }

    return null;
  }
}
