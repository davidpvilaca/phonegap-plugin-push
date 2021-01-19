package com.adobe.phonegap.push.match;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ImageView;

import java.io.InputStream;
import java.net.URL;

public class BeeBeeImageLoader extends AsyncTask<String, Void, Bitmap> {
  ImageView image;

  public BeeBeeImageLoader(ImageView image) {
    this.image = image;
  }

  protected Bitmap doInBackground(String... urls) {
    String url = urls[0];
    Bitmap bitmap = null;

    try {
      InputStream is = new URL(url).openStream();
      bitmap = BitmapFactory.decodeStream(is);
    } catch (Exception e) {
      Log.e("Error", e.getMessage());
      e.printStackTrace();
    }
    return bitmap;
  }

  protected void onPostExecute(Bitmap result) {
    image.setImageBitmap(result);
  }
}
