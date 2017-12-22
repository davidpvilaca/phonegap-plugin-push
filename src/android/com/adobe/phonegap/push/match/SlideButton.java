package com.adobe.phonegap.push.match;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.VectorDrawable;
import android.os.Build;
import android.support.graphics.drawable.AnimatedVectorDrawableCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.DragEvent;
import android.view.MotionEvent;
import android.widget.SeekBar;

/**
 * Created by alvaro.menezes on 21/12/2017.
 */

public class SlideButton extends android.support.v7.widget.AppCompatSeekBar {

  private Drawable thumb;
  private SlideButtonListener listener;

  private static final int MAX = 255;
  private static final int INITIAL_VALUE = (int)(0.5 * MAX);
  private static final int LEFT_TRESHOLD = (int)(0.2 * MAX);
  private static final int RIGHT_TRESHOLD = (int)(0.8 * MAX);
  private static final int LEFT = (int) (0.03 * MAX);
  private static final int RIGHT = (int) (0.97 * MAX);

  public void setThumbLevel(int progress) {
    int percentage = 0;
    int color = 0;

    if (progress > INITIAL_VALUE) {
      percentage = 2 * progress - MAX;

//      color = Color.argb(255, 254 - percentage, 243, 102);
      color = Color.argb(percentage, 0, 243, 102);
      // thumb.setLevel(5 * progress - 255);
      Log.d("slide", "" + thumb.getLevel());
    } else {
      percentage = MAX - 2 * progress;
//      color = Color.argb(255, 254, 243 - percentage, 51);
      color = Color.argb(percentage, 243, 0, 51);
      // thumb.setLevel(255 - 5 * progress);
      Log.d("slide", "" + thumb.getLevel());
    }

    thumb.setColorFilter(color, PorterDuff.Mode.SRC_ATOP);

    this.getBackground().setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
  }

  public interface SlideButtonListener {
    public void handleLeftSlide();
    public void handleRightSlide();
  }

  private void creation(Context context) {
    setMax(MAX);
    setProgress(INITIAL_VALUE);

    Drawable anim;
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      anim = ResourcesCompat.getDrawable(context.getResources(), Meta.getResId(context, "drawable", "slide_thumb_animated"), null);
    } else {
      anim = ResourcesCompat.getDrawable(context.getResources(), Meta.getResId(context, "drawable", "slide_thumb"), null);
    }

//    Drawable anim = context.getDrawable(Meta.getResId(context, "drawable", "slide_thumb_animated"));
    this.setThumb(anim);
    startAnimation();

    this.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
      @Override
      public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        setThumbLevel(progress);
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
  public void setThumb(Drawable thumb) {
    super.setThumb(thumb);
    this.thumb = thumb;
  }

  private void startAnimation() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      ((AnimatedVectorDrawable) thumb).start();
    }
  }

  private void stopAnimation() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      ((AnimatedVectorDrawable)thumb).reset();
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
}
