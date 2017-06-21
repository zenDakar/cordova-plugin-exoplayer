[![MIT License](http://img.shields.io/badge/license-MIT-blue.svg?style=flat)](LICENSE) [![Build Status](https://travis-ci.org/frontyard/cordova-plugin-exoplayer.svg?branch=2.0.0)](https://travis-ci.org/frontyard/cordova-plugin-exoplayer) [![Code Climate](https://codeclimate.com/github/frontyard/cordova-plugin-exoplayer/badges/gpa.svg)](https://codeclimate.com/github/frontyard/cordova-plugin-exoplayer)

This fork inserts the exoPlayer below the cordova webView and removes all stuff related to player controls, as that will be handled inside your cordova web-app instead.

# Cordova ExoPlayer Plugin

Cordova media player plugin using Google's ExoPlayer framework.

Please send us links to your cool projects made with this plugin so we can include them on this page!

## Changes in version 2.4.4
- Added `stop` method to fully stop the stream but not realease the player. Call `setStream` to restart the playback or `release` to dispose of it.

## Changes in version 2.4.2
- Plugin no longer acts on audio focus event but rather let's Cordova app decide what to do.

## Changes in version 2.4.1
- When audio focus changes, plugin will send `AUDIO_FOCUS_EVENT` with parameter `audioFocus` that can be on of:
 `AUDIOFOCUS_LOSS_TRANSIENT`, `AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK`, `AUDIOFOCUS_GAIN`, `AUDIOFOCUS_LOSS`. 

## Changes in version 2.4.0
- Controller will no longer show automatically on touch and key events.
- Added showController and hideController methods for explicit control of controller visibility.
- Plugin will not longer close on KEYCODE_BACK key event, please call .close() explicitly.
- Plugin will request audio focus on startup. It will also listen for audio focus events and pause/play on those events. It will close on AUDIOFOCUS_LOSS. 
- START_EVENT will return additional info `audioFocus` which can be AUDIOFOCUS_REQUEST_FAILED or AUDIOFOCUS_REQUEST_GRANTED.
- STATE_EVENT will return additional info `controllerVisible` to tell the app if controller is visible or not.

## Changes in version 2.3.0

- Renamed plugin's namespace from window.exoplayer to window.ExoPlayer
- Removed `init` method as there is no need to keep instance of the plugin around. Just call methods directly on window.ExoPlayer
- `show` now takes configuration parameters as the first argumeent since we don't need init any more.
- Renamed `playOffset` configuration element to `seekTo` to match the coresponding method that does the same thing
- Replaced `play` and `pause` methods with single `playPause` that does both and keeps track of playback status

## Using

Create a new Cordova Project

    $ cordova create hello com.example.helloapp Hello
    
Install the plugin

    $ cd hello
    $ cordova plugin add cordova-plugin-exoplayer
    

Edit `www/js/index.js` and add the following code inside `onDeviceReady`

```js
    var successCallback = function(json) {
    };

    var errorCallback = function(error) {
    };

    var params = {
        url: "http://www.youtube.com/api/manifest/dash/id/bf5bb2419360daf1/source/youtube?as=fmp4_audio_clear,fmp4_sd_hd_clear&sparams=ip,ipbits,expire,source,id,as&ip=0.0.0.0&ipbits=0&expire=19000000000&signature=51AF5F39AB0CEC3E5497CD9C900EBFEAECCCB5C7.8506521BFC350652163895D4C26DEE124209AA9E&key=ik0"
    };

    window.ExoPlayer.show(parameters, successCallback, errorCallback);
```

Plugin methods exported via window.ExoPlayer
```js
{
    setStream(url, controllerConfig) // switch stream without disposing of the player. controllerConfig is "controller" part of the inital parameters. 
    playPause() // will pause if playing and play if paused :-)
    seekTo(milliseconds) // jump to particular poing into the stream
    getState(successCallback, errorCallback) // returns player state
    showController() // shows player controller
    hideController() // hides player controller
    close() // close and dispose of player, very important to call this method when your app exits!
}
```

This is what `parameters` look like for the `show` call, most of them are optional: 
```js
{
    url: 'https://devimages.apple.com.edgekey.net/streaming/examples/bipbop_4x3/bipbop_4x3_variant.m3u8',
    userAgent: 'MyAwesomePlayer', // default is 'ExoPlayerPlugin'
    aspectRatio: 'FILL_SCREEN', // default is FIT_SCREEN
    hideTimeout: 5000, // Hide controls after this many milliseconds, default is 5 sec
    seekTo: 10 * 60 * 60 * 1000, // Start playback 10 minutes into video specified in milliseconds, default is 0
    skipTime: 60 * 1000, // Amount of time to use when going forward/backward, default is 1 min
    audioOnly: true, // Only play audio in the backgroud, default is false.
    subtitleUrl: 'http://url.to/subtitle.srt', // Optional subtitle url
    controller: { // If this object is not present controller will not be visible
        streamImage: 'http://url.to/channel.png',
        streamTitle: 'My channel',
        streamDescription: '2nd line you can use to display whatever you want',
        hideProgress: true,
        controlIcons: {
            'exo_rew': 'http://url.to/rew.png',
            'exo_play': 'http://url.to/play.png',
            'exo_pause': 'http://url.to/pause.png',
            'exo_ffwd': 'http://url.to/ffwd.png'
        }
    }
}
```
Controller is composed of several pieces. To the left there is optional streamImage, followed by two lines on the right, top and bottom. Top line is reserved for streamTitle, while bottom line can either be streamDescription or progress bar. If you provide streamDescription, progress bar will not be visible. Optionaly you can turn off progress bar by passing hideProgress: true if you don't want to show either.

Playback control buttons are centered on the screen and use default ExoPlayer icons. Optionally you can override these by your own images via controlIcons object.

You can pass `subtitleUrl` for subtitle to be shown over the video. We currently support .srt and .vtt subtitle formats. Subtitles are not supported on all stream types, as ExoPlayer has requirement that both video and subtitle "must have the same number of periods, and must not have any dynamic windows", which means for simple mp4s it should work, but on more complex HLS/Dash setups it might not. 

If you pass in `audioOnly: true`, make sure to manually close the player on some event (like escape button) since the plugin won't be detecting keypresses when playing audio in the background.

If you want to show default control buttons (play/pause, rewind, forward) you need an empty controlIncons object:
```js
    controlIcons: {
    }
```
 
Plugin will send following events back to Cordova app through successCallback specified through show function:
```js
START_EVENT
STOP_EVENT
KEY_EVENT
TOUCH_EVENT
LOADING_EVENT
STATE_CHANGED_EVENT
POSITION_DISCONTINUITY_EVENT
SEEK_EVENT
PLAYER_ERROR_EVENT
```
Each event will send JSON payload coresponding to that event. Some events (where appropriate) will also send additional information about playback like duration, postion, etc. 

Example of key events:
```js
{
    'eventType':'KEY_EVENT',
    'eventAction':'ACTION_DOWN',
    'eventKeycode':'KEYCODE_VOLUME_UP'
}

{   
    'eventType':'KEY_EVENT',
    'eventAction':'ACTION_UP',
    'eventKeycode':'KEYCODE_VOLUME_UP'
}
```


Example of touch events:
```js
{
    'eventType':'TOUCH_EVENT',
    'eventAction':'ACTION_DOWN',
    'eventAxisX':543,
    'eventAxisY':1321.8009033203125
}

{   
    'eventType':'TOUCH_EVENT',
    'eventAction':'ACTION_MOVE',
    'eventAxisX':543,
    'eventAxisY':1320.5
}

{
    'eventType':'TOUCH_EVENT',
    'eventAction':'ACTION_UP',
    'eventAxisX':543,
    'eventAxisY':1320.5
}
```

Install Android platform

    cordova platform add android
    
Run the code

    cordova run
    
## Contributing
    
1. Fork it
2. Create your feature branch off of current upstram branch (currently 2.0.0)
3. Commit and push your changes to that branch
4. Create new Pull Request

## More Info

For more information on setting up Cordova see [the documentation](http://cordova.apache.org/docs/en/latest/guide/cli/index.html)

For more info on plugins see the [Plugin Development Guide](http://cordova.apache.org/docs/en/latest/guide/hybrid/plugins/index.html)

General ExoPlayer [documentation](https://google.github.io/ExoPlayer/)

ExoPlayer [source code](https://github.com/google/ExoPlayer)