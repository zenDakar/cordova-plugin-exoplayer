/*global cordova, module*/
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
module.exports = {
    setup: function (parameters, successCallback, errorCallback) {
        cordova.exec(successCallback, errorCallback, "ExoPlayer", "setup", [parameters || {url: "http://dummy"}]);
    },
    setStream: function (url, loop, successCallback, errorCallback) {
        cordova.exec(successCallback, errorCallback, "ExoPlayer", "setStream", [url, loop]);
    },
    playPause: function (successCallback, errorCallback) {
        cordova.exec(successCallback, errorCallback, "ExoPlayer", "playPause", []);
    },
    play: function (milliseconds, successCallback, errorCallback) {
        cordova.exec(successCallback, errorCallback, "ExoPlayer", "play", [milliseconds]);
    },
    pause: function (successCallback, errorCallback) {
        cordova.exec(successCallback, errorCallback, "ExoPlayer", "pause", []);
    },
    stop: function (successCallback, errorCallback) {
        cordova.exec(successCallback, errorCallback, "ExoPlayer", "stop", []);
    },
    seekTo: function (milliseconds, successCallback, errorCallback) {
        cordova.exec(successCallback, errorCallback, "ExoPlayer", "seekTo", [milliseconds]);
    },
    setPlaybackRate: function (speed, muteAudio, successCallback, errorCallback) {
        cordova.exec(successCallback, errorCallback, "ExoPlayer", "setPlaybackRate", [speed, muteAudio]);
    },
    getPlaybackRate: function (successCallback, errorCallback) {
        cordova.exec(successCallback, errorCallback, "ExoPlayer", "getPlaybackRate", []);
    },
    getDuration: function (successCallback, errorCallback) {
        cordova.exec(successCallback, errorCallback, "ExoPlayer", "getDuration", []);
    },
    getPosition: function (successCallback, errorCallback) {
        cordova.exec(successCallback, errorCallback, "ExoPlayer", "getPosition", []);
    },
    getState: function (successCallback, errorCallback) {
        cordova.exec(successCallback, errorCallback, "ExoPlayer", "getState", []);
    },
    close: function (successCallback, errorCallback) {
        cordova.exec(successCallback, errorCallback, "ExoPlayer", "close", []);
    }
};
