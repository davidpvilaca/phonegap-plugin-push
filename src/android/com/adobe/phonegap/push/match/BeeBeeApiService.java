package com.adobe.phonegap.push.match;

import android.content.Context;
import android.location.Location;
import android.os.Build;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HurlStack;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.SSLContext;
import java.net.Socket;

/**
 * Created by alvaro.menezes on 06/12/2017.
 */

public class BeeBeeApiService {

  private String mUserToken;
  private String mOrderUrl;
  private String mUserUrl;
  private RequestQueue mQueue;

  public BeeBeeApiService(Context ctx, int orderId, String userToken, String apiUrl) {
    mUserToken = userToken;
    mOrderUrl = apiUrl + "/orders/" + orderId;
    mUserUrl = apiUrl + "/users";

    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT) {
      HurlStack stack = null;

      try {
        stack = new HurlStack(null, new TLSSocketFactory());
      } catch (KeyManagementException e) {
        e.printStackTrace();
        Log.d("BeeBeeApiService", "Could not create new stack for TLS v1.2");
        stack = new HurlStack();
      } catch (NoSuchAlgorithmException e) {
        e.printStackTrace();
        Log.d("BeeBeeApiService", "Could not create new stack for TLS v1.2");
        stack = new HurlStack();
      }
      mQueue = Volley.newRequestQueue(ctx, stack);
    } else {
      mQueue = Volley.newRequestQueue(ctx);
    }
  }

  public void accept(Response.Listener<JSONObject> success, Response.ErrorListener error) {
    JsonRequest(mOrderUrl + "/accept", success, error);
  }

  public void reject(Response.Listener<JSONObject> success, Response.ErrorListener error) {
    JsonRequest(mOrderUrl + "/reject", success, error);
  }

  public void eta(Location location, Response.Listener<JSONObject> success, Response.ErrorListener error) {
    JsonRequest(Request.Method.GET, mOrderUrl + "/eta?lat=" + location.getLatitude()
      + "&lng=" + location.getLongitude(), null, success, error);
  }

  public void pong() {
    JsonRequest(Request.Method.POST, mUserUrl + "/pong", null, null, null);
  }

  private void JsonRequest(String url, Response.Listener<JSONObject> success, Response.ErrorListener error) {
    this.JsonRequest(Request.Method.PUT, url, null, success, error);
  }

  private void JsonRequest(int method, String url, JSONObject requestObject, Response.Listener<JSONObject> successListener,
                           final Response.ErrorListener errorListener) {
    JsonObjectRequest jsObjRequest = new JsonObjectRequest
      (method, url, requestObject, successListener, new Response.ErrorListener() {
        @Override
        public void onErrorResponse(VolleyError error) {
          if (errorListener != null) {
            String message = handleServerError(error);
            if (message != null) {
              errorListener.onErrorResponse(new VolleyError(message));
            } else {
              errorListener.onErrorResponse(error);
            }
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

    jsObjRequest.setRetryPolicy(new DefaultRetryPolicy(
      (int) TimeUnit.SECONDS.toMillis(30),
      0,
      DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
    ));

    mQueue.add(jsObjRequest);
  }

  private String handleServerError(VolleyError err) {
    NetworkResponse response = err.networkResponse;
    if (response != null && response.data != null) {

      Log.d("ServerError", new String(response.data));

      try {

        JSONObject result = new JSONObject(new String(response.data));
        if (result != null && result.has("error")) {
          return result.getString("error");
        }

      } catch (JSONException e) {
        e.printStackTrace();
      }
    }

    return null;
  }

  public class TLSSocketFactory extends SSLSocketFactory {

    private SSLSocketFactory internalSSLSocketFactory;

    public TLSSocketFactory() throws KeyManagementException, NoSuchAlgorithmException {
      SSLContext context = SSLContext.getInstance("TLS");
      context.init(null, null, null);
      internalSSLSocketFactory = context.getSocketFactory();
    }

    @Override
    public String[] getDefaultCipherSuites() {
      return internalSSLSocketFactory.getDefaultCipherSuites();
    }

    @Override
    public String[] getSupportedCipherSuites() {
      return internalSSLSocketFactory.getSupportedCipherSuites();
    }

    @Override
    public Socket createSocket(Socket s, String host, int port, boolean autoClose) throws IOException {
      return enableTLSOnSocket(internalSSLSocketFactory.createSocket(s, host, port, autoClose));
    }

    @Override
    public Socket createSocket(String host, int port) throws IOException, UnknownHostException {
      return enableTLSOnSocket(internalSSLSocketFactory.createSocket(host, port));
    }

    @Override
    public Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException, UnknownHostException {
      return enableTLSOnSocket(internalSSLSocketFactory.createSocket(host, port, localHost, localPort));
    }

    @Override
    public Socket createSocket(InetAddress host, int port) throws IOException {
      return enableTLSOnSocket(internalSSLSocketFactory.createSocket(host, port));
    }

    @Override
    public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) throws IOException {
      return enableTLSOnSocket(internalSSLSocketFactory.createSocket(address, port, localAddress, localPort));
    }

    private Socket enableTLSOnSocket(Socket socket) {
      if(socket != null && (socket instanceof SSLSocket)) {
        ((SSLSocket)socket).setEnabledProtocols(new String[] {"TLSv1.1", "TLSv1.2"});
      }
      return socket;
    }
  }
}
