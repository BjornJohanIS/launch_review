package com.iyaffle.launchreview;

import android.widget.Toast;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.content.pm.ActivityInfo;
import android.net.Uri;

import androidx.annotation.NonNull;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.BinaryMessenger;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;

import java.util.List;

public class LaunchReviewPlugin implements FlutterPlugin, ActivityAware, MethodCallHandler {
    private Activity activity;
    private MethodChannel channel;

    @Override
    public void onAttachedToEngine(@NonNull FlutterPluginBinding binding) {
        channel = new MethodChannel(binding.getBinaryMessenger(), "launch_review");
        channel.setMethodCallHandler(this);
    }

    @Override
    public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
        channel.setMethodCallHandler(null);
        channel = null;
    }

    @Override
    public void onAttachedToActivity(@NonNull ActivityPluginBinding binding) {
        activity = binding.getActivity();
    }

    @Override
    public void onDetachedFromActivityForConfigChanges() {
        activity = null;
    }

    @Override
    public void onReattachedToActivityForConfigChanges(@NonNull ActivityPluginBinding binding) {
        activity = binding.getActivity();
    }

    @Override
    public void onDetachedFromActivity() {
        activity = null;
    }

    @Override
    public void onMethodCall(MethodCall call, Result result) {
        if (call.method.equals("launch")) {
            if (activity == null) {
                result.error("NO_ACTIVITY", "LaunchReviewPlugin: activity is null", null);
                return;
            }

            String appId = call.argument("android_id");
            String toastMessage = call.argument("toast_message");
            boolean showToast = call.argument("show_toast");

            if (appId == null) {
                appId = activity.getPackageName();
            }

            if (toastMessage == null) {
                toastMessage = "Please Rate Application";
            }

            Intent rateIntent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("market://details?id=" + appId));
            boolean marketFound = false;

            List<ResolveInfo> otherApps = activity.getPackageManager()
                    .queryIntentActivities(rateIntent, 0);
            for (ResolveInfo otherApp : otherApps) {
                if (otherApp.activityInfo.applicationInfo.packageName.equals("com.android.vending")) {
                    ActivityInfo otherAppActivity = otherApp.activityInfo;
                    ComponentName componentName = new ComponentName(
                            otherAppActivity.applicationInfo.packageName,
                            otherAppActivity.name
                    );

                    rateIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                            Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED |
                            Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    rateIntent.setComponent(componentName);

                    if (showToast && activity != null) {
                        android.widget.Toast.makeText(activity, toastMessage, android.widget.Toast.LENGTH_SHORT).show();
                    }

                    activity.startActivity(rateIntent);
                    marketFound = true;
                    break;
                }
            }

            if (!marketFound) {
                try {
                    activity.startActivity(new Intent(Intent.ACTION_VIEW,
                            Uri.parse("market://details?id=" + appId)));
                } catch (ActivityNotFoundException e) {
                    activity.startActivity(new Intent(Intent.ACTION_VIEW,
                            Uri.parse("https://play.google.com/store/apps/details?id=" + appId)));
                }
            }

            result.success(null);
        } else {
            result.notImplemented();
        }
    }
}
