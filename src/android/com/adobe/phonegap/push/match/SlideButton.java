package com.adobe.phonegap.push.match;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.v4.content.res.ResourcesCompat;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.widget.SeekBar;

/**
 * Created by alvaro.menezes on 21/12/2017.
 */
public class SlideButton extends android.support.v7.widget.AppCompatSeekBar {

  private Drawable thumb;
  private SlideButtonListener listener;
  private int backgroundColor;

  private static final int MAX = 255;
  private static final int INITIAL_VALUE = (int)(0.5 * MAX);
  private static final int LEFT_TRESHOLD = (int)(0.2 * MAX);
  private static final int RIGHT_TRESHOLD = (int)(0.8 * MAX);
  private static final int LEFT = (int) (0.01 * MAX);
  private static final int RIGHT = (int) (0.99 * MAX);

  public void setSliderLayout(int progress) {
    int alpha = 0;
    int bgColor = 0;
    int textColor = 0;
    String text = "";

    int colorPrimary = ResourcesCompat.getColor(getResources(),
      Meta.getResId(this.getContext(), "color", "colorPrimary"), null);

    if (progress > INITIAL_VALUE) {
      alpha = 2 * progress - MAX + 150;
      text = progress > 0.65 * MAX ? "ACEITAR" : "";
      textColor = colorPrimary;
      bgColor = backgroundColor;
    }

    if (progress < INITIAL_VALUE) {
      alpha = MAX - 2 * progress + 150;
      text = progress < 0.35 * MAX ? "RECUSAR" : "";
      textColor = Color.WHITE;
      bgColor = colorPrimary;
    }

    this.setBackground(new TextDrawable(getResources(), text, Math.min(alpha, MAX), bgColor, textColor));
    this.getBackground().setAlpha(Math.min((int)(1.7 * (alpha - 227)), MAX));
  }

  public interface SlideButtonListener {
    public void handleLeftSlide();
    public void handleRightSlide();
  }

  private void creation(Context context) {
    setMax(MAX);
    setProgress(INITIAL_VALUE);

    Drawable bg = this.getBackground();
    if (bg != null) {
      bg.setAlpha(0);
    }

    Drawable anim;
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      anim = ResourcesCompat.getDrawable(context.getResources(), Meta.getResId(context, "drawable", "slide_thumb_animated"), null);
    } else {
      anim = ResourcesCompat.getDrawable(context.getResources(), Meta.getResId(context, "drawable", "slide_thumb"), null);
    }

    this.setThumb(anim);
    startAnimation();

    this.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
      @Override
      public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        setSliderLayout(progress);
      }

      @Override
      public void onStartTrackingTouch(SeekBar seekBar) {

      }

      @Override
      public void onStopTrackingTouch(SeekBar seekBar) {

      }
    });
  }

  public SlideButton(Context context) {
    super(context);
    creation(context);
  }

  public SlideButton(Context context, AttributeSet attrs) {
    super(context, attrs);
    creation(context);
  }

  @Override
  public void setBackgroundColor(int color) {
    this.backgroundColor = color;
  }

  @Override
  public void setThumb(Drawable thumb) {
    super.setThumb(thumb);
    this.thumb = thumb;
  }

  private void startAnimation() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      ((AnimatedVectorDrawable) thumb).start();
    }
  }

  private void stopAnimation() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      //((AnimatedVectorDrawable)thumb).reset();
      ((AnimatedVectorDrawable)thumb).stop();
    }
  }

  @Override
  public boolean onTouchEvent(MotionEvent event) {
    if (event.getAction() == MotionEvent.ACTION_DOWN) {
      Rect bounds = thumb.getBounds();
      if (bounds.contains((int) event.getX(), (int) event.getY())) {
        super.onTouchEvent(event);
      } else {
        return false;
      }
    } else if (event.getAction() == MotionEvent.ACTION_UP) {
      if (getProgress() < LEFT_TRESHOLD) {
        handleLeftSlide();
        setProgress(LEFT);
      } else if (getProgress() > RIGHT_TRESHOLD) {
        handleRightSlide();
        setProgress(RIGHT);
      } else {
        setProgress(INITIAL_VALUE);
        startAnimation();
        return true;
      }
    } else {
      super.onTouchEvent(event);
    }

    stopAnimation();
    return true;
  }

  private void handleRightSlide() {
    if (listener != null) {
      listener.handleRightSlide();
    }
  }

  private void handleLeftSlide() {
    if (listener != null) {
      listener.handleLeftSlide();
    }
  }

  public void setSlideButtonListener(SlideButtonListener listener) {
    this.listener = listener;
  }

  public class TextDrawable extends Drawable {

    private final String text;
    private final Paint paint;
    private final int alpha;
    private final int bgColor;


    public TextDrawable(Resources res, String text, int alpha, int bgColor, int textColor) {
      this.text = text;
      this.alpha = alpha;
      this.bgColor = bgColor;

      this.paint = new Paint(Paint.ANTI_ALIAS_FLAG);
      paint.setColor(textColor);
      paint.setAntiAlias(true);
      paint.setFakeBoldText(true);
      paint.setStyle(Paint.Style.FILL);
      float textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 15, res.getDisplayMetrics());
      paint.setTextSize(textSize);
      paint.setTextAlign(Paint.Align.CENTER);
    }

    @Override
    public void draw(Canvas canvas) {
      Rect bounds = getBounds();
      canvas.drawARGB(alpha, Color.red(bgColor), Color.green(bgColor), Color.blue(bgColor));
      canvas.drawText(text, 0, text.length(), bounds.exactCenterX(), bounds.exactCenterY() + 15, paint);
    }

    @Override
    public void setAlpha(int alpha) {
      paint.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
      paint.setColorFilter(cf);
    }

    @Override
    public int getOpacity() {
      return PixelFormat.TRANSLUCENT;
    }
  }
}
