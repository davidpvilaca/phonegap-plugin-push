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
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.adobe.phonegap.push.PushConstants;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by alvaro.menezes on 05/12/2017.
 */

public class MatchActivity extends Activity implements PushConstants {
  private CountDownTimer countDownTimer;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
      WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
      WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
      WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

//    NotificationManager mNotificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
//
//    mNotificationManager.cancel(1234);

    setContentView(Meta.getResId(this, "layout", "activity_match"));

    Typeface font = Typeface.createFromAsset(getAssets(), "fonts/icomoon.ttf");
    setIconFont("icon_category", font);
    setIconFont("icon_freight_type", font);
    setIconFont("icon_route", font);

    Bundle extras = getIntent().getBundleExtra(MATCH_NOTIFICATION_EXTRAS);
    setActivityValues(extras.getString(MATCH_ORDER_DETAILS));

    startCountdown();

    Button buttonAceitar = findViewById(Meta.getResId(this, "id", "button_accept"));
    Button buttonRejeitar = findViewById(Meta.getResId(this, "id", "button_reject"));
    buttonAceitar.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        countDownTimer.cancel();
        SharedPreferences sharedPref = getApplicationContext().getSharedPreferences(COM_ADOBE_PHONEGAP_PUSH, Context.MODE_PRIVATE);
        String userToken = sharedPref.getString(USER_TOKEN, "");

      }
    });
    buttonRejeitar.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        countDownTimer.cancel();
        SharedPreferences sharedPref = getApplicationContext().getSharedPreferences(COM_ADOBE_PHONEGAP_PUSH, Context.MODE_PRIVATE);
        String userToken = sharedPref.getString(USER_TOKEN, "");
        
      }
    });
  }

  private void startCountdown() {
    final ProgressBar progressBar = findViewById(Meta.getResId(this, "id", "progressBar"));

    countDownTimer = new CountDownTimer(30200, 50) {
      @Override
      public void onTick(long millisUntilFinished) {
        progressBar.setProgress((int)millisUntilFinished);
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

  private void setActivityValues(String json) {
    try {
      JSONObject jsonOrder = new JSONObject( json ) ;

      setItemValue("icon_category", IconMap.icons.get(jsonOrder.getString("categoryIcon")));
      setItemValue("icon_freight_type", IconMap.icons.get(jsonOrder.getString("freightTypeIcon")));
      setItemValue("icon_route", IconMap.icons.get(jsonOrder.getString("routeIcon")));
      setItemValue("category", jsonOrder.getString("category"));
      setItemValue("freight_type", jsonOrder.getString("freightType"));
      setItemValue("route", jsonOrder.getString("route"));
      setItemValue("eta", jsonOrder.getString("eta"));
      setItemValue("address", jsonOrder.getString("address"));

      RecyclerView recyclerView = findViewById(Meta.getResId(this, "id", "additional_services"));
//    recyclerView.setHasFixedSize(true);
      RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
      recyclerView.setLayoutManager(layoutManager);
      recyclerView.setAdapter(new AdditionalsAdapter(jsonOrder.getJSONArray("additionals"), this));

    } catch (JSONException e) {
      e.printStackTrace();
    }
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

  private void playSound(){
//    MediaPlayer mp = MediaPlayer.create(this, R.raw.)

//    gridview.setOnItemClickListener(new OnItemClickListener() {
//      public void onItemClick(AdapterView<?> parent, View v,
//                              int position, long id) {
//        Toast.makeText(HelloGridView.this, "" + position,
//          Toast.LENGTH_SHORT).show();
//      }
//    });
  }

//  private void JsonRequest(){
//    JsonObjectRequest jsObjRequest = new JsonObjectRequest
//      (Request.Method.GET, "http://url-do-request", null, new Response.Listener<JSONObject>() {
//
//        @Override
//        public void onResponse(JSONObject response) {
//          // TODO Auto-generated method stub
//        }
//      }, new Response.ErrorListener() {
//        @Override
//        public void onErrorResponse(VolleyError error) {
//          // TODO Auto-generated method stub
//        }
//      });
//  }
}

