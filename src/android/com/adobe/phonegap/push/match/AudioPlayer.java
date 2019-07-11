package com.adobe.phonegap.push.match;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;

/**
 * Created by alvaro.menezes on 06/12/2017.
 */

public class AudioPlayer {
  private static MediaPlayer mMediaPlayer;
  private static int lastVolume = -1;

  public static void stop(Context c) {
    try {
      if (lastVolume != -1) {
        AudioManager audioManager = (AudioManager) c.getSystemService(Context.AUDIO_SERVICE);
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, lastVolume, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
      }

      mMediaPlayer.release();
      mMediaPlayer = null;
    } catch (NullPointerException e) {
      e.printStackTrace();
    }
  }

  public static void play(Context c) {
    stop(c);

    try {
      AudioManager audioManager = (AudioManager)c.getSystemService(Context.AUDIO_SERVICE);
      int maxVolumeMusic = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
      lastVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
      audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVolumeMusic, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);

      Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
  //    mMediaPlayer = MediaPlayer.create(this, Meta.getResId(this, "raw", "gas"));
      mMediaPlayer = MediaPlayer.create(c, notification);
      
      mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
      mMediaPlayer.setLooping(true);
      mMediaPlayer.start();
    } catch (NullPointerException e) {
      e.printStackTrace();
    }
  }
}
