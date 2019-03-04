package com.adobe.phonegap.push.match;

import android.Manifest;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Vibrator;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.text.Html;
import android.text.Spanned;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.adobe.phonegap.push.PushConstants;
import com.adobe.phonegap.push.PushHandlerActivity;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Random;

import static android.support.v4.app.NotificationCompat.PRIORITY_MAX;

/**
 * Created by alvaro.menezes on 05/12/2017.
 */

public class MatchActivity extends Activity implements PushConstants {
  private static long DURATION = 30000;
  private CountDownTimer mCountDownTimer;
  private RejectedOrders mRejectedOrders = null;
  private OrderApiService mOrderApiService = null;
  private FusedLocationProviderClient mFusedLocationClient = null;
  private ProgressDialog mProgressDialog = null;
  private Bundle mExtras = null;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
      WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
      WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
      WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

    setContentView(Meta.getResId(this, "layout", "activity_match"));

    mProgressDialog = new ProgressDialog(this);
    mRejectedOrders = new RejectedOrders(this);
    mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

    mExtras = getIntent().getBundleExtra(MATCH_NOTIFICATION_EXTRAS);

    try {
      JSONObject jsonOrder = new JSONObject(mExtras.getString(MATCH_ORDER_DETAILS));
      final int orderId = jsonOrder.getInt("id");
      final String uid = jsonOrder.getString("uid");

      if (mRejectedOrders.exists(uid)) {
        finish();
        return;
      }

      SharedPreferences sharedPref = getApplicationContext().getSharedPreferences(COM_ADOBE_PHONEGAP_PUSH, Context.MODE_PRIVATE);
      final String userToken = sharedPref.getString(USER_TOKEN, "");
      final String apiUrl = sharedPref.getString(API_URL, "");
      mOrderApiService = new OrderApiService(this, orderId, userToken, apiUrl);

      setButtonEvents(orderId, uid);
      setActivityValues(jsonOrder);
      setActivityScheduledIfNeeded(jsonOrder);
//      loadETA(orderId);

    } catch (JSONException e) {
      e.printStackTrace();
    }

//    startAlerts();
  }

  private void loadETA(final int orderId) {
    ProgressBar etaProgressBar = (ProgressBar) findViewById(Meta.getResId(this, "id", "eta_progressBar"));

    int color = ResourcesCompat.getColor(getResources(),
      Meta.getResId(this, "color", "colorPrimary"), null);
    etaProgressBar.getIndeterminateDrawable().setColorFilter(color, PorterDuff.Mode.SRC_IN);

    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
      ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

      mFusedLocationClient.getLastLocation()
        .addOnSuccessListener(this, new OnSuccessListener<Location>() {
          @Override
          public void onSuccess(Location location) {
            // Got last known location. In some rare situations this can be null.
            if (location == null) {
              showETADetails("Erro no GPS.");
            } else {
              mOrderApiService.eta(location,
                new Response.Listener<JSONObject>() {
                  @Override
                  public void onResponse(JSONObject response) {
                    showETADetails(getETAText(response));
                  }
                },
                new Response.ErrorListener() {
                  @Override
                  public void onErrorResponse(VolleyError error) {
                    showETADetails("Erro no ETA.");
                    error.printStackTrace();
                  }
                }
              );
            }
          }
        });
    } else {
      showETADetails("Sem permissão GPS.");
    }
  }

  private String getETAText(JSONObject response) {
    try {
      int distance = response.getInt("distance");
      int durationInTrafficMinutes = response.getInt("durationInTraffic") / 60;
      int durationInTrafficHours = 0;

      if (durationInTrafficMinutes > 60) {
        durationInTrafficHours = durationInTrafficMinutes / 60;
        durationInTrafficMinutes = durationInTrafficMinutes % 60;
      }

      String textETA = "";
      if (durationInTrafficHours > 0) {
        textETA += durationInTrafficHours + "h ";
      }

      textETA += durationInTrafficMinutes + "min - " + distance + "km";

      return textETA;

    } catch (JSONException e) {
      e.printStackTrace();
    }

    return "Erros no ETA.";
  }

  private void showETADetails(String textETA) {
    final Context ctx = this;

    TextView etaView = (TextView) findViewById(Meta.getResId(ctx, "id", "eta"));
    TextView etaDetailsView = (TextView) findViewById(Meta.getResId(ctx, "id", "eta_details"));
    ProgressBar etaProgressBar = (ProgressBar) findViewById(Meta.getResId(ctx, "id", "eta_progressBar"));

    etaView.setText(textETA);
    etaView.setVisibility(View.VISIBLE);

    etaDetailsView.setVisibility(View.VISIBLE);

    etaProgressBar.setVisibility(View.INVISIBLE);
  }

  private void setButtonEvents(final int orderId, final String uid) throws JSONException {
    final Context ctx = this;

//    SlideButton slideButton = (SlideButton)findViewById(Meta.getResId(this, "id", "slide_button"));
//
//    slideButton.setSlideButtonListener(new SlideButton.SlideButtonListener() {
//      @Override
//      public void handleLeftSlide() {
//          reject(ctx, orderId, uid, mOrderApiService);
//      }
//
//      @Override
//      public void handleRightSlide() {
//        accept(ctx, orderId, mOrderApiService);
//      }
//    });

    Button buttonAccept = (Button) findViewById(Meta.getResId(this, "id", "button_accept"));
    buttonAccept.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        accept(ctx, orderId, mOrderApiService);
      }
    });

    Button buttonReject = (Button) findViewById(Meta.getResId(this, "id", "button_reject"));
    buttonReject.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        reject(ctx, orderId, uid, mOrderApiService);
      }
    });
  }

  private void accept(final Context ctx, final int orderId, OrderApiService orderApiService) {
    stopAlerts();

    mProgressDialog.setTitle("Aceitando Frete");
    mProgressDialog.setMessage("Aguarde...");
    mProgressDialog.setCancelable(false);
    mProgressDialog.show();

    orderApiService.accept(
      new Response.Listener<JSONObject>() {
        @Override
        public void onResponse(JSONObject response) {
          notifyUser("Aceitar Frete", "Pedido aceito", orderId);
          startActivity(getHandlerActivityIntent(orderId, response, true));

          finish();
        }
      },
      new Response.ErrorListener() {
        @Override
        public void onErrorResponse(VolleyError error) {
          String errorMsg = error.getMessage();

          if (errorMsg != null && errorMsg != "") {
            notifyUser("Aceitar Frete", errorMsg, orderId);
          } else {
            notifyUser("Aceitar Frete", "Não foi possível aceitar o pedido.", orderId);
          }

          error.printStackTrace();
          finish();
        }
      }
    );
  }

  private Intent getHandlerActivityIntent(int orderId, JSONObject response, boolean trackOrder) {
    Bundle extras = new Bundle(mExtras);

    if (trackOrder) {
      addTrackOrderParams(extras, response);
    }

    Intent notificationIntent = new Intent(this, PushHandlerActivity.class);
    notificationIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
    notificationIntent.putExtra(PUSH_BUNDLE, extras);
    notificationIntent.putExtra(NOT_ID, orderId);

    return notificationIntent;
  }

  private void addTrackOrderParams(Bundle extras, JSONObject response) {
    try {
      extras.putString("driverCompany", response.getJSONObject("driverCompany").toString());
    } catch (JSONException e) {
      e.printStackTrace();
    }

    extras.putString("action", "ORDER_MATCH_ACCEPTED");
  }

  private void notifyUser(String title, String text, int notId) {
    NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

    int requestCode = new Random().nextInt();
    PendingIntent contentIntent = PendingIntent.getActivity(this, requestCode, getHandlerActivityIntent(notId, null, false), PendingIntent.FLAG_UPDATE_CURRENT);

    NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this)
      .setWhen(System.currentTimeMillis())
      .setSmallIcon(this.getApplicationInfo().icon)
      .setPriority(PRIORITY_MAX)
      .setAutoCancel(true);

    if (contentIntent != null) {
      mBuilder.setContentIntent(contentIntent);
    }

    mBuilder.setDefaults(Notification.DEFAULT_VIBRATE);
    mBuilder.setSound(android.provider.Settings.System.DEFAULT_NOTIFICATION_URI);

    NotificationCompat.BigTextStyle bigText = new NotificationCompat.BigTextStyle();

    if (text != null) {
      mBuilder.setContentText(fromHtml(text));

      bigText.bigText(Html.fromHtml(text));
      bigText.setBigContentTitle(fromHtml(title));

      mBuilder.setStyle(bigText);
    }

    CharSequence appName = this.getPackageManager().getApplicationLabel(this.getApplicationInfo());

    mNotificationManager.notify((String) appName, notId, mBuilder.build());

    Toast.makeText(this, text, Toast.LENGTH_LONG).show();
  }

  private Spanned fromHtml(String source) {
    if (source != null)
      return Html.fromHtml(source);
    else
      return null;
  }

  private void reject(final Context ctx, final int orderId, final String uid, OrderApiService orderApiService) {
    stopAlerts();
    mRejectedOrders.add(uid);
    Toast.makeText(ctx, "Pedido rejeitado", Toast.LENGTH_SHORT).show();

    orderApiService.reject(
      new Response.Listener<JSONObject>() {
        @Override
        public void onResponse(JSONObject response) {
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
    stopAlerts();
    super.onDestroy();
  }

  private void startAlerts() {
    AudioPlayer.play(this);

    Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
    v.vibrate(new long[]{100L, 100L}, 0);

//    startCountdown();
  }

  private void stopAlerts() {
    if (mCountDownTimer != null) {
      mCountDownTimer.cancel();
    }

    if (mProgressDialog != null) {
      mProgressDialog.dismiss();
    }

    AudioPlayer.stop(this);
    Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
    v.cancel();
  }

  private void startCountdown() {
    final ProgressBar progressBar = (ProgressBar) findViewById(Meta.getResId(this, "id", "progressBar"));

    int interval = 50;
    mCountDownTimer = new CountDownTimer(DURATION + interval, interval) {
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

    mCountDownTimer.start();
  }

  private void setIconFont(String id, Typeface font) {
    TextView view = (TextView) findViewById(Meta.getResId(this, "id", id));
    view.setTypeface(font);
  }

  private void setActivityValues(JSONObject jsonOrder) throws JSONException {
    Typeface font = Typeface.createFromAsset(getAssets(), "fonts/icomoon.ttf");
    setIconFont("icon_category", font);
//    setIconFont("icon_freight_type", font);
//    setIconFont("icon_route", font);

    setItemValue("icon_category", IconMap.icons.get(jsonOrder.getString("categoryIcon")));
//    setItemValue("icon_freight_type", IconMap.icons.get(jsonOrder.getString("freightTypeIcon")));
//    setItemValue("icon_route", IconMap.icons.get(jsonOrder.getString("routeIcon")));
    setItemValue("category", jsonOrder.getString("category"));
    setItemValue("freight_type", jsonOrder.getString("freightType"));
    setItemValue("route", jsonOrder.getString("route"));
    setItemValue("address", jsonOrder.getString("address"));

    String locations = jsonOrder.getString("locations");
    if (locations != null && !locations.isEmpty() && !locations.equalsIgnoreCase("null")) {
      TextView locationsTextView = findViewById(Meta.getResId(this, "id", "locations"));
      setItemValue("locations", locations);
      locationsTextView.setVisibility(View.VISIBLE);
    }

    String destinationAddress = jsonOrder.getString("destinationAddress");
    if (destinationAddress != null && !destinationAddress.isEmpty() && !destinationAddress.equalsIgnoreCase("null")) {
      LinearLayout layoutDestination = findViewById(Meta.getResId(this, "id", "layout_destination"));
      setItemValue("destination_address", destinationAddress);
      layoutDestination.setVisibility(View.VISIBLE);
    }
  }

  private void setActivityScheduledIfNeeded(JSONObject jsonOrder) throws JSONException {
    String scheduleDate = jsonOrder.has("scheduleDate") ? jsonOrder.getString("scheduleDate") : null;

    if (scheduleDate != null && !scheduleDate.isEmpty() && !scheduleDate.equalsIgnoreCase("null")) {
      int colorScheduled = ResourcesCompat.getColor(getResources(),
        Meta.getResId(this, "color", "colorScheduled"), null);
      int blueBadgeId = Meta.getResId(this, "drawable", "blue_badge");

      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        ProgressBar pgBar = findViewById(Meta.getResId(this, "id", "progressBar"));
        pgBar.getProgressDrawable().setTint(colorScheduled);
      }

      LinearLayout yellowLayout = findViewById(Meta.getResId(this, "id", "layout_yellow"));
      yellowLayout.setBackgroundResource(blueBadgeId);

      TextView badgeOrigin = findViewById(Meta.getResId(this, "id", "badge_origin"));
      badgeOrigin.setBackgroundResource(blueBadgeId);

      TextView badgeDestination = findViewById(Meta.getResId(this, "id", "badge_destination"));
      badgeDestination.setBackgroundResource(blueBadgeId);

      LinearLayout layoutAccept = findViewById(Meta.getResId(this, "id", "layout_accept"));
      layoutAccept.setBackgroundColor(colorScheduled);

      TextView scheduleDateTextView = findViewById(Meta.getResId(this, "id", "schedule_date"));
      setItemValue("schedule_date", scheduleDate);
      scheduleDateTextView.setVisibility(View.VISIBLE);
    }
  }

  private void setItemValue(String id, String value) {
    TextView view = findViewById(Meta.getResId(this, "id", id));
    view.setText(value);
  }

  public static void startAlarm(Context context, Bundle extras) {
    Intent intent = new Intent(context, MatchActivity.class);
    intent.putExtra(MATCH_NOTIFICATION_EXTRAS, extras);
    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

    context.startActivity(intent);

//    PendingIntent alarmIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
//
//    AlarmManager alarms = (AlarmManager) context.getSystemService(ALARM_SERVICE);
//    alarms.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), alarmIntent);
  }
}
