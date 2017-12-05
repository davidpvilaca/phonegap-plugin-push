package com.adobe.phonegap.push.match;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
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

/**
 * Created by alvaro.menezes on 05/12/2017.
 */

public class MatchActivity extends Activity {
  private CountDownTimer countDownTimer;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
      WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
      WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
      WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

    getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

    NotificationManager mNotificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);

    mNotificationManager.cancel(1234);

    setContentView(Meta.getResId(this, "layout", "activity_match"));

    RecyclerView recyclerView = (RecyclerView) findViewById(Meta.getResId(this, "id", "adicionais"));
    recyclerView.setHasFixedSize(true);

    RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
//    RecyclerView.LayoutManager layoutManager = new GridLayoutManager(this, 1);;
    recyclerView.setLayoutManager(layoutManager);

    recyclerView.setAdapter(new AdicionaisAdapter(this));

    Typeface font = Typeface.createFromAsset(getAssets(), "fonts/icomoon.ttf");
    TextView icon_categoria = (TextView) findViewById(Meta.getResId(this, "id", "icon_categoria"));
    icon_categoria.setTypeface(font);

    TextView icon_tipoFrete = (TextView) findViewById(Meta.getResId(this, "id", "icon_tipoFrete"));
    icon_tipoFrete.setTypeface(font);

    TextView icon_rota = (TextView) findViewById(Meta.getResId(this, "id", "icon_rota"));
    icon_rota.setTypeface(font);

    final ProgressBar progressBar = (ProgressBar) findViewById(Meta.getResId(this, "id", "progressBar"));

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

    Button buttonAceitar = (Button) findViewById(Meta.getResId(this, "id", "button_aceitar"));
    Button buttonRejeitar = (Button) findViewById(Meta.getResId(this, "id", "button_rejeitar"));

    buttonAceitar.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        countDownTimer.cancel();
      }
    });

    buttonRejeitar.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        countDownTimer.cancel();
      }
    });

//    MediaPlayer mp = MediaPlayer.create(this, R.raw.)

//    gridview.setOnItemClickListener(new OnItemClickListener() {
//      public void onItemClick(AdapterView<?> parent, View v,
//                              int position, long id) {
//        Toast.makeText(HelloGridView.this, "" + position,
//          Toast.LENGTH_SHORT).show();
//      }
//    });
  }

  public static void startAlarm(Context context) {
    Intent activate = new Intent(context, MatchActivity.class);
    activate.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

    AlarmManager alarms = (AlarmManager) context.getSystemService(ALARM_SERVICE);
    PendingIntent alarmIntent = PendingIntent.getActivity(context, 0, activate, 0);
    alarms.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), alarmIntent);
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

