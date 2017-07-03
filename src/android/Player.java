/*
 The MIT License (MIT)

 Copyright (c) 2017 Nedim Cholich

 Permission is hereby granted, free of charge, to any person obtaining a copy
 of this software and associated documentation files (the "Software"), to deal
 in the Software without restriction, including without limitation the rights
 to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 copies of the Software, and to permit persons to whom the Software is
 furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included in all
 copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 SOFTWARE.
 */
package co.frontyard.cordova.plugin.exoplayer;

import android.app.*;
import android.content.*;
import android.media.*;
import android.net.*;
import android.os.*;
import android.support.v4.content.ContextCompat;
import android.util.*;
import android.view.*;
import android.widget.*;
import com.google.android.exoplayer2.*;
import com.google.android.exoplayer2.extractor.*;
import com.google.android.exoplayer2.source.*;
import com.google.android.exoplayer2.source.dash.*;
import com.google.android.exoplayer2.source.hls.*;
import com.google.android.exoplayer2.source.smoothstreaming.*;
import com.google.android.exoplayer2.trackselection.*;
import com.google.android.exoplayer2.ui.*;
import com.google.android.exoplayer2.upstream.*;
import com.google.android.exoplayer2.util.*;
import java.lang.*;
import java.lang.Math;
import java.lang.Override;

import org.apache.cordova.*;
import org.json.*;

public class Player {
    private static final String TAG = "ExoPlayerPlugin";
    private final Activity activity;
    private final CallbackContext callbackContext;
    private final Configuration config;
    private final Handler handler = new Handler();
    private SimpleExoPlayer exoPlayer;
    private SimpleExoPlayerView exoView;
    private CordovaWebView webView;
    private int controllerVisibility;
    private boolean paused = false;
    private boolean seeking = false;
    private float currentPlaybackRate = 1.0f;
    private AudioManager audioManager;

    public Player(Configuration config, Activity activity, CallbackContext callbackContext, CordovaWebView webView) {
        this.config = config;
        this.activity = activity;
        this.callbackContext = callbackContext;
        this.webView = webView;
        this.audioManager = (AudioManager) activity.getSystemService(Context.AUDIO_SERVICE);

        ActivityManager activityManager = (ActivityManager)activity.getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        activityManager.getMemoryInfo(memoryInfo);
        Log.d(TAG, "MEM: avail: " + memoryInfo.availMem + ", total: " + memoryInfo.totalMem + ", lowMem: " + memoryInfo.lowMemory + ", threshold: " + memoryInfo.threshold);
    }

    private ExoPlayer.EventListener playerEventListener = new ExoPlayer.EventListener() {
        @Override
        public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {
            Log.i(TAG, "Playback parameters changed");
        }

        @Override
        public void onPlayerError(ExoPlaybackException error) {
            JSONObject payload = Payload.playerErrorEvent(Player.this.exoPlayer, Player.this.activity, error, null);
            new CallbackResponse(Player.this.callbackContext).send(PluginResult.Status.ERROR, payload, true);
        }

        @Override
        public void onLoadingChanged(boolean isLoading) {
            JSONObject payload = Payload.loadingEvent(Player.this.exoPlayer, isLoading);
            new CallbackResponse(Player.this.callbackContext).send(PluginResult.Status.OK, payload, true);
        }

        @Override
        public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
            JSONObject payload = Payload.stateEvent(Player.this.exoPlayer, playbackState, Player.this.currentPlaybackRate);
            new CallbackResponse(Player.this.callbackContext).send(PluginResult.Status.OK, payload, true);
        }

        @Override
        public void onPositionDiscontinuity() {
            JSONObject payload = Payload.positionDiscontinuityEvent(Player.this.exoPlayer);
            new CallbackResponse(Player.this.callbackContext).send(PluginResult.Status.OK, payload, true);
        }

        @Override
        public void onTimelineChanged(Timeline timeline, Object manifest) {
            // Need to see if we want to send this to Cordova.
        }

        @Override
        public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {
            // Need to see if we want to send this to Cordova.
        }
    };

    private AudioManager.OnAudioFocusChangeListener audioFocusChangeListener = new AudioManager.OnAudioFocusChangeListener() {
        public void onAudioFocusChange(int focusChange) {
            if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
                JSONObject payload = Payload.audioFocusEvent(Player.this.exoPlayer, "AUDIOFOCUS_LOSS_TRANSIENT");
                new CallbackResponse(Player.this.callbackContext).send(PluginResult.Status.OK, payload, true);
            }
            else if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {
                JSONObject payload = Payload.audioFocusEvent(Player.this.exoPlayer, "AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK");
                new CallbackResponse(Player.this.callbackContext).send(PluginResult.Status.OK, payload, true);
            }
            else if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
                JSONObject payload = Payload.audioFocusEvent(Player.this.exoPlayer, "AUDIOFOCUS_GAIN");
                new CallbackResponse(Player.this.callbackContext).send(PluginResult.Status.OK, payload, true);
            }
            else if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
                JSONObject payload = Payload.audioFocusEvent(Player.this.exoPlayer, "AUDIOFOCUS_LOSS");
                new CallbackResponse(Player.this.callbackContext).send(PluginResult.Status.OK, payload, true);
            }
        }
    };

    public void createPlayer() {
        if (!config.isAudioOnly()) {
            createView();
        }
        preparePlayer(config.getUri());
    }

    public void createView() {
        exoView = new SimpleExoPlayerView(this.activity);

        exoView .setLayoutParams(new LinearLayout.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT));
        if (config.isAspectRatioFillScreen()) {
            exoView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FILL);
        }
        exoView.setFastForwardIncrementMs(config.getSkipTimeMs());
        exoView.setRewindIncrementMs(config.getSkipTimeMs());
        exoView.setKeepScreenOn(true);
        exoView.setUseController(false);

        //Insert the exoView below the cordova webView
        FrameLayout webViewParent = (FrameLayout)webView.getView().getParent();
        int colorResId = activity.getResources().getIdentifier("webview_background_color", "color", activity.getPackageName());
        webViewParent.setBackgroundColor(ContextCompat.getColor(activity.getApplicationContext(), colorResId));
        webViewParent.addView(exoView, 0);

        //Make webView transparent
        colorResId = activity.getResources().getIdentifier("transparent", "color", activity.getPackageName());
        webView.getView().setBackgroundColor(ContextCompat.getColor(activity.getApplicationContext(), colorResId));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            webView.getView().setLayerType(View.LAYER_TYPE_HARDWARE, null);
        } else {
            webView.getView().setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }

        exoView.requestFocus();
    }

    private int setupAudio() {
        activity.setVolumeControlStream(AudioManager.STREAM_MUSIC);
        return audioManager.requestAudioFocus(audioFocusChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
    }

    private void preparePlayer(Uri uri) {
        int audioFocusResult = setupAudio();
        String audioFocusString = audioFocusResult == AudioManager.AUDIOFOCUS_REQUEST_FAILED ?
                "AUDIOFOCUS_REQUEST_FAILED" :
                "AUDIOFOCUS_REQUEST_GRANTED";
        DefaultBandwidthMeter bandwidthMeter = new DefaultBandwidthMeter();
        //TrackSelection.Factory videoTrackSelectionFactory = new AdaptiveVideoTrackSelection.Factory(bandwidthMeter);
        TrackSelector trackSelector = new DefaultTrackSelector();
        LoadControl loadControl = new DefaultLoadControl();

        exoPlayer = ExoPlayerFactory.newSimpleInstance(this.activity, trackSelector, loadControl);
        exoPlayer.addListener(playerEventListener);
        if (exoView != null) {
            exoView.setPlayer(exoPlayer);
        }

        MediaSource mediaSource = getMediaSource(uri, bandwidthMeter);
        if (mediaSource != null) {
            long offset = config.getSeekTo();
            if (offset > -1) {
                exoPlayer.seekTo(offset);
            }
            exoPlayer.prepare(mediaSource);
            exoPlayer.setPlayWhenReady(true);
            JSONObject payload = Payload.startEvent(exoPlayer, audioFocusString);
            new CallbackResponse(Player.this.callbackContext).send(PluginResult.Status.OK, payload, true);
        } else {
            sendError("Failed to construct mediaSource for " + uri);
        }
    }

    private MediaSource getMediaSource(Uri uri, DefaultBandwidthMeter bandwidthMeter) {
        String userAgent = Util.getUserAgent(this.activity, config.getUserAgent());
        Handler mainHandler = new Handler();
        int connectTimeout = 10 * 1000;
        int readTimeout = 10 * 1000;
        int retryCount = 10;

        HttpDataSource.Factory httpDataSourceFactory = new DefaultHttpDataSourceFactory(userAgent, bandwidthMeter, connectTimeout, readTimeout, true);
        DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(this.activity, bandwidthMeter, httpDataSourceFactory);
        MediaSource mediaSource;
        int type = Util.inferContentType(uri);
        switch (type) {
            case C.TYPE_DASH:
                long livePresentationDelayMs = DashMediaSource.DEFAULT_LIVE_PRESENTATION_DELAY_PREFER_MANIFEST_MS;
                DefaultDashChunkSource.Factory dashChunkSourceFactory = new DefaultDashChunkSource.Factory(dataSourceFactory);
                // Last param is AdaptiveMediaSourceEventListener
                mediaSource = new DashMediaSource(uri, dataSourceFactory, dashChunkSourceFactory, retryCount, livePresentationDelayMs, mainHandler, null);
                break;
            case C.TYPE_HLS:
                // Last param is AdaptiveMediaSourceEventListener
                mediaSource = new HlsMediaSource(uri, dataSourceFactory, retryCount, mainHandler, null);
                break;
            case C.TYPE_SS:
                DefaultSsChunkSource.Factory ssChunkSourceFactory = new DefaultSsChunkSource.Factory(dataSourceFactory);
                // Last param is AdaptiveMediaSourceEventListener
                mediaSource = new SsMediaSource(uri, dataSourceFactory, ssChunkSourceFactory, mainHandler, null);
                break;
            default:
                ExtractorsFactory extractorsFactory = new DefaultExtractorsFactory();
                mediaSource = new ExtractorMediaSource(uri, dataSourceFactory, extractorsFactory, mainHandler, null);
                break;
        }

        String subtitleUrl = config.getSubtitleUrl();
        if (subtitleUrl != null) {
            Uri subtitleUri = Uri.parse(subtitleUrl);
            String subtitleType = inferSubtitleType(subtitleUri);
            Log.i(TAG, "Subtitle present: " + subtitleUri + ", type=" + subtitleType);
            Format textFormat = Format.createTextSampleFormat(null, subtitleType, null, Format.NO_VALUE, Format.NO_VALUE, "en", null);
            MediaSource subtitleSource = new SingleSampleMediaSource(subtitleUri, httpDataSourceFactory, textFormat, C.TIME_UNSET);
            return new MergingMediaSource(mediaSource, subtitleSource);
        } else {
            return mediaSource;
        }
    }

    private static String inferSubtitleType(Uri uri) {
        String fileName = uri.getPath().toLowerCase();

        if (fileName.endsWith(".vtt")) {
            return MimeTypes.TEXT_VTT;
        } else {
            // Assume it's srt.
            return MimeTypes.APPLICATION_SUBRIP;
        }
    }

    public void close() {
        audioManager.abandonAudioFocus(audioFocusChangeListener);
        if (exoPlayer != null) {
            exoPlayer.release();
            exoPlayer = null;
        }
    }

    public void setStream(Uri uri) {
        if (uri != null) {
            DefaultBandwidthMeter bandwidthMeter = new DefaultBandwidthMeter();
            MediaSource mediaSource = getMediaSource(uri, bandwidthMeter);
            exoPlayer.prepare(mediaSource);
            play();
        }
    }

    public void playPause() {
        if (this.paused) {
            play();
        } else {
            pause();
        }
    }

    public void pause() {
        if (!paused) {
            paused = true;
            stopSeek();
            if (currentPlaybackRate != 1) {
                setPlaybackRate(1, false);
            }
            exoPlayer.setPlayWhenReady(false);
        }
    }

    public void play() {
        paused = false;
        stopSeek();
        if (currentPlaybackRate != 1) {
            setPlaybackRate(1, false);
        }
        exoPlayer.setPlayWhenReady(true);
    }

    public void play(long timeMillis) {
        long duration = exoPlayer.getDuration();
        if (duration > 0 && timeMillis > 0 && timeMillis < duration) {
            exoPlayer.seekTo(timeMillis);
        }

        play();
    }

    public void stop() {
        paused = false;
        stopSeek();
        if (currentPlaybackRate != 1) {
            setPlaybackRate(1, false);
        }
        exoPlayer.stop();
    }

    public void seekTo(long timeMillis) {
        seeking = true;
        long seekPosition = exoPlayer.getDuration() == 0 ? 0 : Math.min(Math.max(0, timeMillis), exoPlayer.getDuration());
        exoPlayer.seekTo(seekPosition);
        JSONObject payload = Payload.seekEvent(Player.this.exoPlayer, timeMillis);
        new CallbackResponse(Player.this.callbackContext).send(PluginResult.Status.OK, payload, true);
    }

    private void stopSeek() {
      seeking = false;
      if (handler != null) {
        handler.removeCallbacks(null);
      }
    }

    // public void setPlaybackRate(float speed, boolean muteAudio) {
    //     if (Math.abs(speed) >= 16) {
    //       speed = 2;
    //     }
    //
    //     currentPlaybackRate = speed;
    //
    //     exoPlayer.setPlaybackParameters(new PlaybackParameters(speed,1));
    // }

    public void setPlaybackRate(float speed, boolean muteAudio) {
        if (Math.abs(speed) >= config.getMaxPlaybackRate()) {
            speed = 2;
        }

        currentPlaybackRate = speed;

        if (Math.abs(speed) >= 8) {
            if (seeking == false) {
                seeking = true;
                final int delay = 2000; //2 seconds

                //When the playback rate is 8x or higher we pause the player and
                // reset the players internal playback rate, to prevent the player from looking weird when seeking
                pause();
                if (currentPlaybackRate != 1) {
                    exoPlayer.setPlaybackParameters(new PlaybackParameters(1,1));
                }

                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Log.d(TAG, "setPlaybackRate: " + currentPlaybackRate + ", getPosition: " + exoPlayer.getCurrentPosition() + ", seekTo: " + (exoPlayer.getCurrentPosition() + (long)(currentPlaybackRate * 1000) + delay));
                        seekTo(exoPlayer.getCurrentPosition() + (long)(currentPlaybackRate * 1000) + delay);
                        handler.postDelayed(this, delay);
                    }
                }, delay);
            }
        } else {
            stopSeek();
            exoPlayer.setPlaybackParameters(new PlaybackParameters(currentPlaybackRate,1));
        }
    }

    public float getPlaybackRate() {
        return currentPlaybackRate;
    }

    public long getDuration() {
        return exoPlayer.getDuration();
    }

    public long getPosition() {
        return exoPlayer.getCurrentPosition();
    }

    public JSONObject getPlayerState() {
        return Payload.stateEvent(exoPlayer,
                null != exoPlayer ? exoPlayer.getPlaybackState() : SimpleExoPlayer.STATE_ENDED, Player.this.currentPlaybackRate);
    }

    private void sendError(String msg) {
        Log.e(TAG, msg);
        JSONObject payload = Payload.playerErrorEvent(Player.this.exoPlayer, Player.this.activity, null, msg);
        new CallbackResponse(Player.this.callbackContext).send(PluginResult.Status.ERROR, payload, true);
    }
}
