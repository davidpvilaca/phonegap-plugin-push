package com.adobe.phonegap.push.match;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Vibrator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.adobe.phonegap.push.PushConstants;
import com.android.volley.Response;
import com.android.volley.VolleyError;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by alvaro.menezes on 05/12/2017.
 */

public class MatchActivity extends Activity implements PushConstants {
  private CountDownTimer countDownTimer;
  private static long DURATION = 30000;
  private RejectedOrders rejectedOrders;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
      WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
      WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
      WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

    setContentView(Meta.getResId(this, "layout", "activity_match"));

    rejectedOrders = new RejectedOrders(this);
    Bundle extras = getIntent().getBundleExtra(MATCH_NOTIFICATION_EXTRAS);
    try {
      JSONObject jsonOrder = new JSONObject(extras.getString(MATCH_ORDER_DETAILS));
      final int orderId = jsonOrder.getInt("id");

      if (rejectedOrders.exists(orderId)) {
        finish();
      }

      setButtonEvents(orderId);
      setActivityValues(jsonOrder);
    } catch (JSONException e) {
      e.printStackTrace();
    }

    AudioPlayer.play(this);

    Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
    v.vibrate(new long[]{100L, 100L}, 0);

    startCountdown();
  }

  private void setButtonEvents(final int orderId) throws JSONException {
    SharedPreferences sharedPref = getApplicationContext().getSharedPreferences(COM_ADOBE_PHONEGAP_PUSH, Context.MODE_PRIVATE);
    final String userToken = sharedPref.getString(USER_TOKEN, "");
    final OrderApiService orderApiService = new OrderApiService(this, orderId, userToken);
    final Context ctx = this;

    Button buttonAccept = findViewById(Meta.getResId(this, "id", "button_accept"));
    buttonAccept.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        accept(ctx, orderId, orderApiService);
      }
    });

    Button buttonReject = findViewById(Meta.getResId(this, "id", "button_reject"));
    buttonReject.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        reject(ctx, orderId, orderApiService);
      }
    });
  }

  private void accept(final Context ctx, final int orderId, OrderApiService orderApiService) {
    stopAlerts();

    orderApiService.accept(
      new Response.Listener<JSONObject>() {
        @Override
        public void onResponse(JSONObject response) {
          Toast.makeText(ctx, "Pedido " + orderId + " aceito", Toast.LENGTH_SHORT).show();
          finish();
        }
      },
      new Response.ErrorListener() {
        @Override
        public void onErrorResponse(VolleyError error) {
          Toast.makeText(ctx, "Não foi possível aceitar o pedido " + orderId +
            ". Favor tentar novamente", Toast.LENGTH_LONG).show();
          error.printStackTrace();
        }
      }
    );
  }

  private void reject(final Context ctx, final int orderId, OrderApiService orderApiService) {
    stopAlerts();
    rejectedOrders.add(orderId);

    orderApiService.reject(
      new Response.Listener<JSONObject>() {
        @Override
        public void onResponse(JSONObject response) {
          Toast.makeText(ctx, "Pedido " + orderId + " rejeitado", Toast.LENGTH_SHORT).show();
        }
      },
      new Response.ErrorListener() {
        @Override
        public void onErrorResponse(VolleyError error) {
          error.printStackTrace();
        }
      }
    );

    finish();
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    stopAlerts();
  }

  private void stopAlerts() {
    countDownTimer.cancel();
    AudioPlayer.stop(this);
    Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
    v.cancel();
  }

  private void startCountdown() {
    final ProgressBar progressBar = findViewById(Meta.getResId(this, "id", "progressBar"));

    int interval = 50;
    countDownTimer = new CountDownTimer(DURATION + interval, interval) {
      @Override
      public void onTick(long millisUntilFinished) {
        progressBar.setProgress((int) millisUntilFinished);
      }

      @Override
      public void onFinish() {
        progressBar.setProgress(0);
        finish();
      }
    };
    countDownTimer.start();
  }

  private void setIconFont(String id, Typeface font) {
    TextView view = findViewById(Meta.getResId(this, "id", id));
    view.setTypeface(font);
  }

  private void setActivityValues(JSONObject jsonOrder) throws JSONException {
    Typeface font = Typeface.createFromAsset(getAssets(), "fonts/icomoon.ttf");
    setIconFont("icon_category", font);
    setIconFont("icon_freight_type", font);
    setIconFont("icon_route", font);

    setItemValue("icon_category", IconMap.icons.get(jsonOrder.getString("categoryIcon")));
    setItemValue("icon_freight_type", IconMap.icons.get(jsonOrder.getString("freightTypeIcon")));
    setItemValue("icon_route", IconMap.icons.get(jsonOrder.getString("routeIcon")));
    setItemValue("category", jsonOrder.getString("category"));
    setItemValue("freight_type", jsonOrder.getString("freightType"));
    setItemValue("route", jsonOrder.getString("route"));
    setItemValue("eta", jsonOrder.getString("eta"));
    setItemValue("address", jsonOrder.getString("address"));

    RecyclerView recyclerView = findViewById(Meta.getResId(this, "id", "additional_services"));
    RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
    recyclerView.setLayoutManager(layoutManager);
    recyclerView.setAdapter(new AdditionalsAdapter(jsonOrder.getJSONArray("optionals"), this));
  }

  private void setItemValue(String id, String value) {
    TextView view = findViewById(Meta.getResId(this, "id", id));
    view.setText(value);
  }

  public static void startAlarm(Context context, Bundle extras) {
    Intent intent = new Intent(context, MatchActivity.class);
    intent.putExtra(MATCH_NOTIFICATION_EXTRAS, extras);
    PendingIntent alarmIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

    AlarmManager alarms = (AlarmManager) context.getSystemService(ALARM_SERVICE);
    alarms.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), alarmIntent);
  }
}
