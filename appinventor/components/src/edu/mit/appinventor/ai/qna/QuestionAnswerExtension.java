// -*- mode: java; c-basic-offset: 2; -*-
// Copyright 2019 MIT, All rights reserved
// Released under the Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0

package edu.mit.appinventor.ai.qna;

import static android.Manifest.permission.CAMERA;
import android.Manifest;
import android.Manifest.permission;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.util.Log;
import android.view.WindowManager.LayoutParams;
import android.webkit.JavascriptInterface;
import android.webkit.PermissionRequest;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import com.google.appinventor.components.annotations.DesignerComponent;
import com.google.appinventor.components.annotations.DesignerProperty;
import com.google.appinventor.components.annotations.SimpleEvent;
import com.google.appinventor.components.annotations.SimpleObject;
import com.google.appinventor.components.annotations.SimpleProperty;
import com.google.appinventor.components.annotations.UsesAssets;
import com.google.appinventor.components.annotations.UsesPermissions;
import com.google.appinventor.components.common.ComponentCategory;
import com.google.appinventor.components.common.PropertyTypeConstants;
import com.google.appinventor.components.runtime.AndroidNonvisibleComponent;
import com.google.appinventor.components.runtime.Deleteable;
import com.google.appinventor.components.runtime.EventDispatcher;
import com.google.appinventor.components.runtime.Form;
import com.google.appinventor.components.runtime.OnPauseListener;
import com.google.appinventor.components.runtime.OnResumeListener;
import com.google.appinventor.components.runtime.OnStopListener;
import com.google.appinventor.components.runtime.PermissionResultHandler;
import com.google.appinventor.components.runtime.WebViewer;
import com.google.appinventor.components.runtime.util.ErrorMessages;
import com.google.appinventor.components.runtime.util.JsonUtil;
import com.google.appinventor.components.runtime.util.SdkLevel;
import com.google.appinventor.components.runtime.util.YailList;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

@DesignerComponent(version = 1,
    category = ComponentCategory.EXTENSION,
    description = "An extension that embeds a Question-Answer model.",
    iconName = "aiwebres/icon.png",
    nonVisible = true)
@SimpleObject(external = true) //TODO Permissions if necessary
@UsesAssets(fileNames = "tf-core.min.js, tf-converter.min.js, qna.min.js, group1-shard10of24.bin, group1-shard11of24.bin, group1-shard12of24.bin, group1-shard13of24.bin, group1-shard14of24.bin, group1-shard15of24.bin, group1-shard16of24.bin, group1-shard17of24.bin, group1-shard18of24.bin, group1-shard19of24.bin, group1-shard1of24.bin, group1-shard20of24.bin, group1-shard21of24.bin, group1-shard22of24.bin, group1-shard23of24.bin, group1-shard24of24.bin, group1-shard2of24.bin, group1-shard3of24.bin, group1-shard4of24.bin, group1-shard5of24.bin, group1-shard6of24.bin, group1-shard7of24.bin, group1-shard8of24.bin, group1-shard9of24.bin, model.json, processed_vocab.json, index.html, app.js")
public class QuestionAnswerExtension extends AndroidNonvisibleComponent {
  private static final String LOG_TAG = QuestionAnswerExtension.class.getSimpleName();
  private static final String ERROR_WEBVIEWER_NOT_SET =
      "You must specify a WebViewer using the WebViewer designer property before you can call %1s";
  private static final int ERROR_JSON_PARSE_FAILED = 101;
  private static final String MODEL_URL =
      "https://tfhub.dev/tensorflow/tfjs-model/mobilebert/1/";

  private WebView webview = null;
  private boolean initialized = false;
  private boolean enabled = true;

  //TODO Other properties here
  private String defaultContext = "The earth is a sphere. 70% of Earth's surface is water. " +
    "Water is comprised of two hydrogen molecules and one oxygen molecule. Molecules are groups of atoms.";
  private String defaultQuestion = "How much of Earth's surface is water?";

  /**
   * //TODO
   * Creates a new QuestionAnswerExtension extension.
   *
   * @param form the container that this component will be placed in
   */
  public QuestionAnswerExtension(Form form) {
    super(form);
    requestHardwareAcceleration(form);
    WebView.setWebContentsDebuggingEnabled(true);
    //TODO Instantiate variables here
    Log.d(LOG_TAG, "Created QuestionAnswerExtension extension");
  }

  @SuppressLint("SetJavaScriptEnabled")
  private void configureWebView(WebView webview) {
    this.webview = webview;
    webview.getSettings().setJavaScriptEnabled(true);
    webview.getSettings().setMediaPlaybackRequiresUserGesture(false);
    webview.addJavascriptInterface(new AppInventorTFJS(), "QuestionAnswerExtension");
    webview.setWebViewClient(new WebViewClient() {
      @Override
      public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
        String url = request.getUrl().toString();
        Log.d(LOG_TAG, "shouldInterceptRequest called");
        if (url.startsWith(MODEL_URL)) {
          if (url.indexOf("?") >= 0) {
            url = url.substring(0, url.indexOf("?"));
          }
          Log.d(LOG_TAG, "overriding " + url);
          InputStream is;
          try {
            is = form.openAssetForExtension(QuestionAnswerExtension.this,
                url.substring(MODEL_URL.length()));
            String contentType, charSet;
            if (url.endsWith(".json")) {
              contentType = "application/json";
              charSet = "UTF-8";
            } else {
              contentType = "application/octet-stream";
              charSet = "binary";
            }
            if (SdkLevel.getLevel() >= SdkLevel.LEVEL_LOLLIPOP) {
              Map<String, String> responseHeaders = new HashMap<>();
              responseHeaders.put("Access-Control-Allow-Origin", "*");
              return new WebResourceResponse(contentType, charSet, 200, "OK", responseHeaders, is);
            } else {
              return new WebResourceResponse(contentType, charSet, is);
            }
          } catch (IOException e) {
            e.printStackTrace();
          }
        }
        Log.d(LOG_TAG, url);
        return super.shouldInterceptRequest(view, request);
      }
    });
    webview.setWebChromeClient(new WebChromeClient() {
      @Override
      public void onPermissionRequest(final PermissionRequest request) {
        Log.d(LOG_TAG, "onPermissionRequest called");
        String[] requestedResources = request.getResources();
        for (String r : requestedResources) {
          if (r.equals(PermissionRequest.RESOURCE_VIDEO_CAPTURE)) {
            form.askPermission(permission.CAMERA, new PermissionResultHandler() {
              @Override
              public void HandlePermissionResponse(String permission, boolean granted) {
                if (granted) {
                  request.grant(new String[]{PermissionRequest.RESOURCE_VIDEO_CAPTURE});
                } else {
                  form.dispatchPermissionDeniedEvent(QuestionAnswerExtension.this, "Enable", permission);
                }
              }
            });
          }
        }
      }
    });
  }

  @SuppressWarnings("squid:S00100")
  @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_COMPONENT
      + ":com.google.appinventor.components.runtime.WebViewer")
  @SimpleProperty(userVisible = false)
  public void WebViewer(WebViewer webviewer) {
    if (webviewer != null) {
      configureWebView((WebView) webviewer.getView());
      webview.requestLayout();
    }
    try {
      Log.d(LOG_TAG, "isHardwareAccelerated? " + webview.isHardwareAccelerated());
      webview.loadUrl(form.getAssetPathForExtension(this, "index.html"));
    } catch(FileNotFoundException e) {
      Log.e(LOG_TAG, "Unable to load tensorflow", e);
    }
  }

  public void Initialize() {
    if (webview != null) {
      initialized = true;
    }
  }

  //TODO Properties
  @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_BOOLEAN, defaultValue = "True")
  @SimpleProperty
  public void Enabled(boolean enabled) {
    this.enabled = enabled;
    if (initialized) {
      assertWebView("Enabled");
      webview.evaluateJavascript(enabled ? "startVideo();" : "stopVideo();", null);
    }
  }

  @SimpleProperty(description = "Enables or disables the model.")
  public boolean Enabled() {
    return enabled;
  }

  @SuppressWarnings("squid:S00100")
  @SimpleEvent(description = "Event indicating that the model is ready.")
  public void ModelReady() {
    EventDispatcher.dispatchEvent(this, "ModelReady");
  }

  @SuppressWarnings("squid:S00100")
  @SimpleEvent(description = "Event indicating that an error has occurred.")
  public void Error(int errorCode, String errorMessage) {
    EventDispatcher.dispatchEvent(this, "Error", errorCode, errorMessage);
  }

  @SuppressWarnings("squid:S00100")
  @SimpleEvent(description = "Event indicating that model successfully got a result.")
  public void GotResult(Object result) {
    EventDispatcher.dispatchEvent(this, "GotResult", result);
  }

  private static void requestHardwareAcceleration(Activity activity) {
    activity.getWindow().setFlags(LayoutParams.FLAG_HARDWARE_ACCELERATED,
        LayoutParams.FLAG_HARDWARE_ACCELERATED);
  }

  @SuppressWarnings("SameParameterValue")
  private void assertWebView(String method) {
    if (webview == null) {
      throw new IllegalStateException(String.format(ERROR_WEBVIEWER_NOT_SET, method));
    }
  }

  private class AppInventorTFJS {
    @JavascriptInterface
    public void ready() {
      form.runOnUiThread(new Runnable() {
        @Override
        public void run() {
          ModelReady();
          webview.evaluateJavascript("useModel();", null);
        }
      });
    }

    @JavascriptInterface
    public void reportResult(final String result) {
      Log.d(LOG_TAG, "QuestionAnswerExtension result: " + result);
      try {
        //final Object parsedResult = JsonUtil.getObjectFromJson(result, true);
        form.runOnUiThread(new Runnable() {
          @Override
          public void run() {
            GotResult(result);
          }
        });
        Log.d(LOG_TAG, result);
      } catch (final JSONException e) {
        form.runOnUiThread(new Runnable() {
          @Override
          public void run() {
            Error(ERROR_JSON_PARSE_FAILED, e.getMessage());
          }
        });
        Log.e(LOG_TAG, "Error parsing JSON from web view", e);
      } catch (final Error e) {
        form.runOnUiThread(new Runnable() {
          @Override
          public void run() {
            Error(ERROR_JSON_PARSE_FAILED, e.getMessage());
          }
        });
        Log.e(LOG_TAG, "Error", e);
      } catch (final Exception e) {
        form.runOnUiThread(new Runnable() {
          @Override
          public void run() {
            Error(ERROR_JSON_PARSE_FAILED, e.getMessage());
          }
        });
        Log.e(LOG_TAG, "Exception", e);
      }
    }

    @JavascriptInterface
    public void error(final int errorCode, final String errorMessage) {
      form.runOnUiThread(new Runnable() {
        @Override
        public void run() {
          Error(errorCode, errorMessage);
        }
      });
    }
  }
}
