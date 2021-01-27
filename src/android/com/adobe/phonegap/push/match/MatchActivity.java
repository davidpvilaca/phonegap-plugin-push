package com.adobe.phonegap.push.match;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Vibrator;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.text.Html;
import android.text.Spanned;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;

import com.adobe.phonegap.push.PushConstants;
import com.adobe.phonegap.push.PushHandlerActivity;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

import static android.support.v4.app.NotificationCompat.PRIORITY_MAX;

/**
 * Created by alvaro.menezes on 05/12/2017.
 */

public class MatchActivity extends Activity implements PushConstants {
  private static long DURATION = 30000;
  public static final String LOG_TAG = "Push_Plugin";
  private CountDownTimer mCountDownTimer;
  private RejectedOrders mRejectedOrders = null;
  private BeeBeeApiService mBeeBeeApiService = null;
  private FusedLocationProviderClient mFusedLocationClient = null;
  private ProgressDialog mProgressDialog = null;
  private AlertDialog mAlertDialog = null;
  private Bundle mExtras = null;
  private String mScheduleDate = null;
  private boolean mIsScheduled = false;

  enum OrderType {
    Unknown,
    StaticRoute,
    DynamicRoute,
    ByHour
  }

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

    mExtras = getIntent().getBundleExtra(NOTIFICATION_EXTRAS);

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
      final int driverId = sharedPref.getInt(USER_ID, 0);
      final String apiUrl = sharedPref.getString(API_URL, "");
      mBeeBeeApiService = new BeeBeeApiService(this, orderId, userToken, apiUrl);

      mScheduleDate = jsonOrder.has("scheduleDate") ? jsonOrder.getString("scheduleDate") : null;
      mIsScheduled = mScheduleDate != null && !mScheduleDate.isEmpty() && !mScheduleDate.equalsIgnoreCase("null");

      setButtonEvents(orderId, uid);
      setActivityValues(jsonOrder, driverId);
      setActivityScheduledIfNeeded(jsonOrder);

    } catch (JSONException e) {
      e.printStackTrace();
    }

    startAlerts();
  }

  private void setButtonEvents(final int orderId, final String uid) {
    final Context ctx = this;

    SlideButton slideButton = findViewById(Meta.getResId(this, "id", "slide_button"));

    slideButton.setSlideButtonListener(new SlideButton.SlideButtonListener() {
      @Override
      public void handleLeftSlide() {
        reject(ctx, orderId, uid, mBeeBeeApiService);
      }

      @Override
      public void handleRightSlide() {
        if (mIsScheduled) {
          AlertDialog dialog = buildScheduledConfirmDialog(ctx, orderId, uid);
          dialog.show();
        } else {
          accept(ctx, orderId, mBeeBeeApiService);
        }
      }
    });
  }

  private AlertDialog buildScheduledConfirmDialog(final Context ctx, int orderId, String uid) {
    SimpleDateFormat formatter = new SimpleDateFormat();
    Date scheduleDate = null;
    try {
      formatter.applyPattern("dd/MM/yyyy HH:mm");
      scheduleDate = formatter.parse(mScheduleDate);
    } catch (ParseException e) {
    }

    formatter.applyPattern("dd/MM/yyyy");
    String strDate = formatter.format(scheduleDate);
    formatter.applyPattern("HH:mm");
    String strTime = formatter.format(scheduleDate);

    AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
    builder.setTitle("Atenção: Frete Agendado");
    builder.setMessage(Html.fromHtml("<br/>Data: <b>" + strDate + "</b><br/><br/>Hora de início: <b>" + strTime + "</b>"));
    builder.setPositiveButton("ACEITAR", new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialog, int id) {
        // User clicked OK button
        accept(ctx, orderId, mBeeBeeApiService);
      }
    });
    builder.setNegativeButton("RECUSAR", new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialog, int id) {
        // User cancelled the dialog
        reject(ctx, orderId, uid, mBeeBeeApiService);
      }
    });

    builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
      public void onDismiss(DialogInterface dialog) {
        // User dismissed
        SlideButton slideButton = findViewById(Meta.getResId(ctx, "id", "slide_button"));
        slideButton.resetProgress();
      }
    });
    AlertDialog dialog = builder.create();

    // Let's start with animation work. We just need to create a style and use it here as follow.
    if (dialog.getWindow() != null)
      dialog.getWindow().getAttributes().windowAnimations = android.R.style.Animation_Dialog;

    return dialog;
  }

  private void accept(final Context ctx, final int orderId, BeeBeeApiService beeBeeApiService) {
    stopAlerts();

    mProgressDialog.setTitle("Aceitando Frete");
    mProgressDialog.setMessage("Aguarde...");
    mProgressDialog.setCancelable(false);
    mProgressDialog.show();

    beeBeeApiService.accept(
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
          AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
          builder.setTitle("Ops...");
          builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
              finish();
            }
          });

          if (errorMsg != null && !errorMsg.isEmpty()) {
            builder.setMessage(errorMsg);
          } else {
            builder.setMessage("Não foi possível aceitar o pedido :(");
          }

          mAlertDialog = builder.show();
          mProgressDialog.dismiss();
          error.printStackTrace();
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

  private void reject(final Context ctx, final int orderId, final String uid, BeeBeeApiService beeBeeApiService) {
    stopAlerts();
    mRejectedOrders.add(uid);
    Toast.makeText(ctx, "Pedido rejeitado", Toast.LENGTH_SHORT).show();

    beeBeeApiService.reject(
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
    try {
      AudioPlayer.play(this);

      Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
      v.vibrate(new long[]{100L, 100L}, 0);

      startCountdown();
    } catch (NullPointerException e) {
      e.printStackTrace();
    }
  }

  private void stopAlerts() {
    try {
      if (mCountDownTimer != null) {
        mCountDownTimer.cancel();
      }

      if (mProgressDialog != null) {
        mProgressDialog.dismiss();
      }

      if (mAlertDialog != null) {
        mAlertDialog.dismiss();
      }

      AudioPlayer.stop(this);

      Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
      v.cancel();
    } catch (NullPointerException e) {
      e.printStackTrace();
    }
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

  private void setActivityValues(JSONObject jsonOrder, int driverId) throws JSONException {
    SlideButton slideButton = findViewById(Meta.getResId(this, "id", "slide_button"));
    int colorAccent = ResourcesCompat.getColor(getResources(),
      Meta.getResId(this, "color", "colorAccent"), null);
    slideButton.setBackgroundColor(colorAccent);

    Typeface font = Typeface.createFromAsset(getAssets(), "fonts/icomoon.ttf");
    setIconFont("icon_category", font);
    setItemValue("driver_commission_text", jsonOrder.getString("driverComission"));

    setItemValue("icon_category", IconMap.icons.get(jsonOrder.getString("categoryIcon")));
    setItemValue("category_text", jsonOrder.getString("category"));
    setItemValue("freight_type_text", jsonOrder.getString("freightType"));
    setItemValue("order_route_summary_text", jsonOrder.getString("routeSummary"));
    setItemValue("origin_text", jsonOrder.getString("address"));

    String destinationAddress = jsonOrder.getString("destinationAddress");
    String firstObservations = jsonOrder.getString("firstObservations");
    LinearLayout layoutDestination = findViewById(Meta.getResId(this, "id", "layout_destination"));
    if (destinationAddress != null && !destinationAddress.isEmpty() && !destinationAddress.equalsIgnoreCase("null")) {
      setItemValue("badge_destination", "Destino Final");
      setItemValue("destination_text", destinationAddress);
      layoutDestination.setVisibility(View.VISIBLE);
    } else if (firstObservations != null && !firstObservations.isEmpty() && !firstObservations.equalsIgnoreCase("null")) {
      setItemValue("badge_destination", "Instruções");
      setItemValue("destination_text", firstObservations);
      layoutDestination.setVisibility(View.VISIBLE);
    }

    final int orderTypeId = jsonOrder.getInt("orderTypeId");
    final String driverBonus = jsonOrder.getString("driverBonus");
    boolean showDriverBonus = driverBonus != null && !driverBonus.isEmpty();

    String staticMapUrl = jsonOrder.getString("staticMapUrl");

    TextView driverBonusText = findViewById(Meta.getResId(this, "id", "driver_bonus_text"));
    TextView dynamicCostText = findViewById(Meta.getResId(this, "id", "dynamic_cost_text"));
    TextView layoutGrayDivider = findViewById(Meta.getResId(this, "id", "layout_gray_divider"));
    TextView initialValueText = findViewById(Meta.getResId(this, "id", "initial_value_text"));
    TextView totalValueText = findViewById(Meta.getResId(this, "id", "total_value_text"));
    LinearLayout layoutCostExtras = findViewById(Meta.getResId(this, "id", "layout_cost_extras"));
    LinearLayout favoriteDriverLayout = findViewById(Meta.getResId(this, "id", "favorite_driver_layout"));
    ImageView mapPreview = findViewById(Meta.getResId(this, "id", "map_preview"));

    driverBonusText.setVisibility(View.GONE);
    dynamicCostText.setVisibility(View.GONE);
    layoutGrayDivider.setVisibility(View.GONE);
    layoutCostExtras.setVisibility(View.GONE);
    initialValueText.setVisibility(View.GONE);
    totalValueText.setVisibility(View.GONE);
    favoriteDriverLayout.setVisibility(View.GONE);
    mapPreview.setVisibility(View.GONE);
    setItemValue("order_type_text", "ROTA FIXA");

    try {
      JSONArray favoriteDriversFromCompany = jsonOrder.getJSONArray("requesterCompanyFavoriteDrivers");
      boolean isFavoriteDriver = false;

      Log.d(LOG_TAG, "Iniciando verificação de driver favorito");

      Log.d(LOG_TAG, "DRIVER LOGADO: " + driverId);

      for (int i = 0; i < favoriteDriversFromCompany.length(); i++) {
        int id = favoriteDriversFromCompany.getJSONObject(i).getInt("id");
        Log.d(LOG_TAG, "Comparando com id: " + id);
        if (id == driverId) {
          isFavoriteDriver = true;
          break;
        }
      }

      Log.d(LOG_TAG, "------------ IS FAVORITE DRIVER: " + isFavoriteDriver + " ----------------");

      if (isFavoriteDriver) {
        favoriteDriverLayout.setVisibility(View.VISIBLE);
      }
    } catch (JSONException e) {
      Log.d(LOG_TAG, "------------ IS FAVORITE DRIVER: error on get ----------------");
      Log.d(LOG_TAG, jsonOrder.toString());
    }

    if (showDriverBonus) {
      setItemValue("driver_bonus_text", driverBonus);
      driverBonusText.setText(driverBonus);
      driverBonusText.setVisibility(View.VISIBLE);
      layoutGrayDivider.setVisibility(View.VISIBLE);
      layoutCostExtras.setVisibility(View.VISIBLE);
    }

    if (orderTypeId == OrderType.DynamicRoute.ordinal()) {
      setItemValue("order_type_text", "ROTA LIVRE");
      dynamicCostText.setVisibility(View.VISIBLE);
      initialValueText.setVisibility(View.VISIBLE);
      totalValueText.setVisibility(View.VISIBLE);
      layoutGrayDivider.setVisibility(View.VISIBLE);
      layoutCostExtras.setVisibility(View.VISIBLE);
    }

    if (!staticMapUrl.equals("null")) {
      mapPreview.setVisibility(View.VISIBLE);
      new BeeBeeImageLoader(mapPreview).execute(staticMapUrl);
    }
  }

  private void setActivityScheduledIfNeeded(JSONObject jsonOrder) throws JSONException {
    if (mIsScheduled) {
      int blueBadgeId = Meta.getResId(this, "drawable", "blue_badge");

      LinearLayout yellowLayout = findViewById(Meta.getResId(this, "id", "layout_header"));
      yellowLayout.setBackgroundResource(blueBadgeId);

      TextView badgeOrigin = findViewById(Meta.getResId(this, "id", "badge_origin"));
      badgeOrigin.setBackgroundResource(blueBadgeId);

      TextView badgeDestination = findViewById(Meta.getResId(this, "id", "badge_destination"));
      badgeDestination.setBackgroundResource(blueBadgeId);

      TextView scheduleDateTextView = findViewById(Meta.getResId(this, "id", "schedule_date_text"));
      setItemValue("schedule_date_text", mScheduleDate);
      scheduleDateTextView.setVisibility(View.VISIBLE);
    }
  }

  private void setItemValue(String id, String value) {
    TextView view = findViewById(Meta.getResId(this, "id", id));
    view.setText(value);
  }

  public static void startAlarm(Context context, Bundle extras) {
    Intent intent = new Intent(context, MatchActivity.class);
    intent.putExtra(NOTIFICATION_EXTRAS, extras);
    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

    context.startActivity(intent);
  }
}
