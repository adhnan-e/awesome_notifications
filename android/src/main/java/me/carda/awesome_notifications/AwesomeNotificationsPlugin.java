package me.carda.awesome_notifications;

import android.content.Context;
import android.os.Build;

import io.flutter.Log;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

import androidx.annotation.NonNull;

import io.flutter.embedding.engine.plugins.FlutterPlugin;

import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.Registrar;

import me.carda.awesome_notifications.awesome_notifications_android_core.AwesomeNotifications;
import me.carda.awesome_notifications.awesome_notifications_android_core.observers.AwesomeEventListener;
import me.carda.awesome_notifications.awesome_notifications_android_core.Definitions;
import me.carda.awesome_notifications.awesome_notifications_android_core.decoders.BitmapResourceDecoder;
import me.carda.awesome_notifications.awesome_notifications_android_core.completion_handlers.BitmapCompletionHandler;
import me.carda.awesome_notifications.awesome_notifications_android_core.completion_handlers.PermissionCompletionHandler;
import me.carda.awesome_notifications.awesome_notifications_android_core.models.NotificationModel;
import me.carda.awesome_notifications.awesome_notifications_android_core.models.NotificationScheduleModel;
import me.carda.awesome_notifications.awesome_notifications_android_core.exceptions.AwesomeNotificationException;

import me.carda.awesome_notifications.awesome_notifications_android_core.models.NotificationChannelModel;

import me.carda.awesome_notifications.awesome_notifications_android_core.utils.BooleanUtils;
import me.carda.awesome_notifications.awesome_notifications_android_core.utils.DateUtils;
import me.carda.awesome_notifications.awesome_notifications_android_core.utils.ListUtils;
import me.carda.awesome_notifications.awesome_notifications_android_core.utils.MapUtils;
import me.carda.awesome_notifications.awesome_notifications_android_core.utils.StringUtils;


import static me.carda.awesome_notifications.awesome_notifications_android_core.Definitions.ACTION_HANDLE;

/**
 * AwesomeNotificationsPlugin
 **/
public class AwesomeNotificationsPlugin
        implements FlutterPlugin, MethodCallHandler, AwesomeEventListener {

    private static final String TAG = "AwesomeNotificationsPlugin";

    private MethodChannel pluginChannel;
    private AwesomeNotifications awesomeNotifications;

    // https://flutter.dev/docs/development/packages-and-plugins/plugin-api-migration
    // FOR OLDER FLUTTER VERSIONS (1.11 releases and bellow)
    public static void registerWith(Registrar registrar) {

        AwesomeNotificationsPlugin awesomeNotificationsPlugin
                = new AwesomeNotificationsPlugin();

        awesomeNotificationsPlugin.AttachAwesomeNotificationsPlugin(
                registrar.context(),
                new MethodChannel(
                        registrar.messenger(),
                        Definitions.CHANNEL_FLUTTER_PLUGIN
                ));
    }

    // FOR NEWER FLUTTER VERSIONS (1.12 releases and above)
    @Override
    public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {

        AttachAwesomeNotificationsPlugin(
                flutterPluginBinding.getApplicationContext(),
                new MethodChannel(
                    flutterPluginBinding.getBinaryMessenger(),
                    Definitions.CHANNEL_FLUTTER_PLUGIN
                ));

        if (AwesomeNotifications.debug)
            Log.d(TAG, "Awesome Notifications attached to engine for Android " + Build.VERSION.SDK_INT);
    }

    @Override
    public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
        detachAwesomeNotificationsPlugin(
                binding.getApplicationContext());

        if (AwesomeNotifications.debug)
            Log.d(TAG, "Awesome Notifications plugin detached from engine");
    }

    private void AttachAwesomeNotificationsPlugin(Context applicationContext, MethodChannel channel) {
        pluginChannel = channel;
        pluginChannel.setMethodCallHandler(this);

        awesomeNotifications = new AwesomeNotifications(applicationContext);
        awesomeNotifications.subscribeOnAwesomeNotificationEvents(this);
    }

    private void detachAwesomeNotificationsPlugin(Context applicationContext) {

        pluginChannel.setMethodCallHandler(null);
        pluginChannel = null;

        awesomeNotifications
                .unsubscribeOnAwesomeNotificationEvents(this)
                .dispose();

        awesomeNotifications = null;

        if (AwesomeNotifications.debug)
            Log.d(TAG, "Awesome Notifications detached for Android " + Build.VERSION.SDK_INT);
    }

    @Override
    public void onNewAwesomeEvent(String eventType, Serializable content) {
        if (pluginChannel != null)
            pluginChannel.invokeMethod(eventType, content);
    }

    @Override
    public void onMethodCall(@NonNull final MethodCall call, @NonNull final Result result) {

        try {

            switch (call.method) {

                case Definitions.CHANNEL_METHOD_INITIALIZE:
                    channelMethodInitialize(call, result);
                    return;

                case Definitions.CHANNEL_METHOD_SET_ACTION_HANDLE:
                    channelMethodSetActionHandle(call, result);
                    return;

                case Definitions.CHANNEL_METHOD_GET_DRAWABLE_DATA:
                    channelMethodGetDrawableData(call, result);
                    return;

                case Definitions.CHANNEL_METHOD_IS_NOTIFICATION_ALLOWED:
                    channelIsNotificationAllowed(call, result);
                    return;

                case Definitions.CHANNEL_METHOD_SHOW_NOTIFICATION_PAGE:
                    channelShowNotificationPage(call, result);
                    return;

                case Definitions.CHANNEL_METHOD_SHOW_ALARM_PAGE:
                    channelShowAlarmPage(call, result);
                    return;

                case Definitions.CHANNEL_METHOD_SHOW_GLOBAL_DND_PAGE:
                    channelShowGlobalDndPage(call, result);
                    return;

                case Definitions.CHANNEL_METHOD_CHECK_PERMISSIONS:
                    channelMethodCheckPermissions(call, result);
                    return;

                case Definitions.CHANNEL_METHOD_SHOULD_SHOW_RATIONALE:
                    channelMethodShouldShowRationale(call, result);
                    return;

                case Definitions.CHANNEL_METHOD_REQUEST_NOTIFICATIONS:
                    channelRequestUserPermissions(call, result);
                    return;

                case Definitions.CHANNEL_METHOD_CREATE_NOTIFICATION:
                    channelMethodCreateNotification(call, result);
                    return;

                case Definitions.CHANNEL_METHOD_LIST_ALL_SCHEDULES:
                    channelMethodListAllSchedules(call, result);
                    return;

                case Definitions.CHANNEL_METHOD_GET_NEXT_DATE:
                    channelMethodGetNextDate(call, result);
                    return;

                case Definitions.CHANNEL_METHOD_GET_LOCAL_TIMEZONE_IDENTIFIER:
                    channelMethodGetLocalTimeZone(call, result);
                    return;

                case Definitions.CHANNEL_METHOD_GET_UTC_TIMEZONE_IDENTIFIER:
                    channelMethodGetUtcTimeZone(call, result);
                    return;

                case Definitions.CHANNEL_METHOD_SET_NOTIFICATION_CHANNEL:
                    channelMethodSetChannel(call, result);
                    return;

                case Definitions.CHANNEL_METHOD_REMOVE_NOTIFICATION_CHANNEL:
                    channelMethodRemoveChannel(call, result);
                    return;

                case Definitions.CHANNEL_METHOD_GET_BADGE_COUNT:
                    channelMethodGetBadgeCounter(call, result);
                    return;

                case Definitions.CHANNEL_METHOD_SET_BADGE_COUNT:
                    channelMethodSetBadgeCounter(call, result);
                    return;

                case Definitions.CHANNEL_METHOD_INCREMENT_BADGE_COUNT:
                    channelMethodIncrementBadge(call, result);
                    return;

                case Definitions.CHANNEL_METHOD_DECREMENT_BADGE_COUNT:
                    channelMethodDecrementBadge(call, result);
                    return;

                case Definitions.CHANNEL_METHOD_RESET_BADGE:
                    channelMethodResetBadge(call, result);
                    return;

                case Definitions.CHANNEL_METHOD_DISMISS_NOTIFICATION:
                    channelMethodDismissNotification(call, result);
                    return;

                case Definitions.CHANNEL_METHOD_CANCEL_NOTIFICATION:
                    channelMethodCancelNotification(call, result);
                    return;

                case Definitions.CHANNEL_METHOD_CANCEL_SCHEDULE:
                    channelMethodCancelSchedule(call, result);
                    return;

                case Definitions.CHANNEL_METHOD_DISMISS_NOTIFICATIONS_BY_CHANNEL_KEY:
                    channelMethodDismissNotificationsByChannelKey(call, result);
                    return;

                case Definitions.CHANNEL_METHOD_CANCEL_SCHEDULES_BY_CHANNEL_KEY:
                    channelMethodCancelSchedulesByChannelKey(call, result);
                    return;

                case Definitions.CHANNEL_METHOD_CANCEL_NOTIFICATIONS_BY_CHANNEL_KEY:
                    channelMethodCancelNotificationsByChannelKey(call, result);
                    return;

                case Definitions.CHANNEL_METHOD_DISMISS_NOTIFICATIONS_BY_GROUP_KEY:
                    channelMethodDismissNotificationsByGroupKey(call, result);
                    return;

                case Definitions.CHANNEL_METHOD_CANCEL_SCHEDULES_BY_GROUP_KEY:
                    channelMethodCancelSchedulesByGroupKey(call, result);
                    return;

                case Definitions.CHANNEL_METHOD_CANCEL_NOTIFICATIONS_BY_GROUP_KEY:
                    channelMethodCancelNotificationsByGroupKey(call, result);
                    return;

                case Definitions.CHANNEL_METHOD_DISMISS_ALL_NOTIFICATIONS:
                    channelMethodDismissAllNotifications(call, result);
                    return;

                case Definitions.CHANNEL_METHOD_CANCEL_ALL_SCHEDULES:
                    channelMethodCancelAllSchedules(call, result);
                    return;

                case Definitions.CHANNEL_METHOD_CANCEL_ALL_NOTIFICATIONS:
                    channelMethodCancelAllNotifications(call, result);
                    return;

                case Definitions.CHANNEL_METHOD_START_FOREGROUND:
                    channelMethodStartForeground(call, result);
                    return;

                case Definitions.CHANNEL_METHOD_STOP_FOREGROUND:
                    channelMethodStopForeground(call, result);
                    return;

                default:
                    result.notImplemented();
            }

        } catch (Exception e) {
            if (AwesomeNotifications.debug)
                Log.d(TAG, String.format("%s", e.getMessage()));

            result.error(call.method, e.getMessage(), e);
            e.printStackTrace();
        }
    }

    private void channelMethodStartForeground(@NonNull final MethodCall call, @NonNull final Result result) throws Exception {

        Map<String, Object> notificationData = call.<Map<String, Object>>argument("notificationData");
        Integer startType = call.<Integer>argument("startType");
        Boolean hasForegroundServiceType = call.<Boolean>argument("hasForegroundServiceType");
        Integer foregroundServiceType = call.<Integer>argument("foregroundServiceType");

        NotificationModel notificationModel = new NotificationModel().fromMap(notificationData);

        if(notificationModel == null)
            throw new AwesomeNotificationException("Foreground notification is invalid");

        if(startType == null)
            throw new AwesomeNotificationException("Foreground start type is required");

        if(hasForegroundServiceType == null)
            throw new AwesomeNotificationException("hasForegroundServiceType is required");

        if(foregroundServiceType == null)
            throw new AwesomeNotificationException("foregroundServiceType is required");

        awesomeNotifications.startForegroundService(
                    notificationModel,
                    startType,
                    hasForegroundServiceType,
                    foregroundServiceType
                );
    }

    private void channelMethodStopForeground(@NonNull final MethodCall call, @NonNull final Result result) {
        awesomeNotifications.stopForegroundService();
        result.success(null);
    }

    private void channelMethodGetDrawableData(@NonNull final MethodCall call, @NonNull final Result result) throws Exception {

        String bitmapReference = call.arguments();

        awesomeNotifications
                .getDrawableData(
                    bitmapReference,
                    new BitmapCompletionHandler() {
                        @Override
                        public void handle(byte[] byteArray, Exception exception) {
                            if(exception != null){
                                result.error(
                                        BitmapResourceDecoder.TAG,
                                        exception.getMessage(),
                                        exception);
                            }
                            else {
                                result.success(byteArray);
                            }
                        }
                    });
    }

    @SuppressWarnings("unchecked")
    private void channelMethodSetChannel(@NonNull final MethodCall call, @NonNull final Result result) throws Exception {

        Map<String, Object> channelData = MapUtils.extractArgument(call.arguments(), Map.class).orNull();
        if (channelData == null)
            throw new AwesomeNotificationException("Channel is invalid");

        NotificationChannelModel channelModel = new NotificationChannelModel().fromMap(channelData);
        if (channelModel == null)
            throw new AwesomeNotificationException("Channel is invalid");

        boolean forceUpdate = BooleanUtils.getValue((Boolean) channelData.get(Definitions.CHANNEL_FORCE_UPDATE));

        boolean channelSaved =
                awesomeNotifications
                        .setChannel(channelModel, forceUpdate);

        result.success(channelSaved);
    }

    private void channelMethodRemoveChannel(@NonNull final MethodCall call, @NonNull final Result result) throws Exception {
        String channelKey = call.arguments();

        if (StringUtils.isNullOrEmpty(channelKey))
            throw new AwesomeNotificationException("Empty channel key");

        boolean removed =
                awesomeNotifications
                        .removeChannel(channelKey);

        if (AwesomeNotifications.debug)
            Log.d(TAG, removed ?
                    "Channel removed" :
                    "Channel '" + channelKey + "' not found");

        result.success(removed);
    }

    private void channelMethodGetBadgeCounter(@NonNull final MethodCall call, @NonNull final Result result) throws Exception {
        Integer badgeCount =
                awesomeNotifications
                        .getGlobalBadgeCounter();

        result.success(badgeCount);
    }

    private void channelMethodSetBadgeCounter(@NonNull final MethodCall call, @NonNull final Result result) throws Exception {

        int count = MapUtils.extractArgument(call.arguments(), Integer.class).or(-1);
        if (count < 0)
            throw new AwesomeNotificationException("Invalid Badge value");

        awesomeNotifications.setGlobalBadgeCounter(count);
        result.success(true);
    }

    private void channelMethodResetBadge(@NonNull final MethodCall call, @NonNull final Result result) throws Exception {
        awesomeNotifications.resetGlobalBadgeCounter();
        result.success(null);
    }

    private void channelMethodIncrementBadge(@NonNull final MethodCall call, @NonNull final Result result) throws Exception {
        int badgeCount = awesomeNotifications.incrementGlobalBadgeCounter();
        result.success(badgeCount);
    }

    private void channelMethodDecrementBadge(@NonNull final MethodCall call, @NonNull final Result result) throws Exception {
        int badgeCount = awesomeNotifications.decrementGlobalBadgeCounter();
        result.success(badgeCount);
    }

    private void channelMethodDismissNotification(@NonNull final MethodCall call, @NonNull final Result result) throws Exception {

        Integer notificationId = call.arguments();
        if (notificationId == null || notificationId < 0)
            throw new AwesomeNotificationException("Invalid id value");

        boolean dismissed = awesomeNotifications.dismissNotification(notificationId);

        if (AwesomeNotifications.debug)
            Log.d(TAG, dismissed ?
                    "Notification " + notificationId + " dismissed" :
                    "Notification " + notificationId + " was not found");

        result.success(dismissed);
    }

    private void channelMethodCancelSchedule(@NonNull final MethodCall call, @NonNull final Result result) throws Exception {

        Integer notificationId = call.arguments();
        if (notificationId == null || notificationId < 0)
            throw new AwesomeNotificationException("Invalid id value");

        boolean canceled = awesomeNotifications.cancelSchedule(notificationId);

        if (AwesomeNotifications.debug)
            Log.d(TAG, canceled ?
                    "Schedule " + notificationId + " cancelled" :
                    "Schedule " + notificationId + " was not found");

        result.success(canceled);
    }

    private void channelMethodCancelNotification(@NonNull final MethodCall call, @NonNull final Result result) throws Exception {

        Integer notificationId = call.arguments();
        if (notificationId == null || notificationId < 0)
            throw new AwesomeNotificationException("Invalid id value");

        boolean canceled = awesomeNotifications.cancelNotification(notificationId);

        if (AwesomeNotifications.debug)
            Log.d(TAG, canceled ?
                    "Notification " + notificationId + " cancelled" :
                    "Notification " + notificationId + " was not found");

        result.success(canceled);
    }

    private void channelMethodDismissNotificationsByChannelKey(@NonNull final MethodCall call, @NonNull final Result result) throws Exception {

        String channelKey = call.arguments();
        if (StringUtils.isNullOrEmpty(channelKey))
            throw new AwesomeNotificationException("Invalid channelKey value");

        boolean dismissed = awesomeNotifications.dismissNotificationsByChannelKey(channelKey);

        if(AwesomeNotifications.debug)
            Log.d(TAG, dismissed ?
                    "Notifications from channel " + channelKey + " dismissed" :
                    "Notifications from channel " + channelKey + " not found");

        result.success(dismissed);
    }

    private void channelMethodCancelSchedulesByChannelKey(@NonNull final MethodCall call, @NonNull final Result result) throws Exception {

        String channelKey = call.arguments();
        if (StringUtils.isNullOrEmpty(channelKey))
            throw new AwesomeNotificationException("Invalid channelKey value");

        boolean canceled = awesomeNotifications.cancelSchedulesByChannelKey(channelKey);

        if(AwesomeNotifications.debug)
            Log.d(TAG, canceled ?
                    "Scheduled Notifications from channel " + channelKey + " canceled" :
                    "Scheduled Notifications from channel " + channelKey + " not found");

        result.success(canceled);
    }

    private void channelMethodCancelNotificationsByChannelKey(@NonNull final MethodCall call, @NonNull final Result result) throws Exception {

        String channelKey = call.arguments();
        if (StringUtils.isNullOrEmpty(channelKey))
            throw new AwesomeNotificationException("Invalid channelKey value");

        boolean canceled = awesomeNotifications.cancelNotificationsByChannelKey(channelKey);

        if(AwesomeNotifications.debug)
            Log.d(TAG, canceled ?
                    "Notifications and schedules from channel " + channelKey + " canceled" :
                    "Notifications and schedules from channel " + channelKey + " not found");

        result.success(canceled);
    }

    private void channelMethodDismissNotificationsByGroupKey(@NonNull final MethodCall call, @NonNull final Result result) throws Exception {

        String groupKey = call.arguments();
        if (StringUtils.isNullOrEmpty(groupKey))
            throw new AwesomeNotificationException("Invalid groupKey value");

        boolean dismissed = awesomeNotifications.dismissNotificationsByGroupKey(groupKey);

        if(AwesomeNotifications.debug)
            Log.d(TAG, dismissed ?
                    "Notifications from group " + groupKey + " dismissed" :
                    "Notifications from group " + groupKey + " not found");

        result.success(dismissed);
    }

    private void channelMethodCancelSchedulesByGroupKey(@NonNull final MethodCall call, @NonNull final Result result) throws Exception {

        String groupKey = call.arguments();
        if (StringUtils.isNullOrEmpty(groupKey))
            throw new AwesomeNotificationException("Invalid groupKey value");

        boolean canceled = awesomeNotifications.cancelSchedulesByGroupKey(groupKey);

        if(AwesomeNotifications.debug)
            Log.d(TAG, canceled ?
                    "Scheduled Notifications from group " + groupKey + " canceled" :
                    "Scheduled Notifications from group " + groupKey + " not found");

        result.success(canceled);
    }

    private void channelMethodCancelNotificationsByGroupKey(@NonNull final MethodCall call, @NonNull final Result result) throws Exception {

        String groupKey = call.arguments();
        if (StringUtils.isNullOrEmpty(groupKey))
            throw new AwesomeNotificationException("Invalid groupKey value");

        boolean canceled = awesomeNotifications.cancelNotificationsByGroupKey(groupKey);

        if(AwesomeNotifications.debug)
            Log.d(TAG, canceled ?
                    "Notifications and schedules from group " + groupKey + " canceled" :
                    "Notifications and schedules from group " + groupKey + " not found to be");

        result.success(canceled);
    }

    private void channelMethodDismissAllNotifications(@NonNull final MethodCall call, @NonNull final Result result) throws Exception {

        awesomeNotifications.dismissAllNotifications();

        if (AwesomeNotifications.debug)
            Log.d(TAG, "All notifications was dismissed");

        result.success(true);
    }

    private void channelMethodCancelAllSchedules(@NonNull final MethodCall call, @NonNull final Result result) throws Exception {

        awesomeNotifications.cancelAllSchedules();

        if (AwesomeNotifications.debug)
            Log.d(TAG, "All notifications scheduled was cancelled");

        result.success(true);
    }

    private void channelMethodCancelAllNotifications(@NonNull final MethodCall call, @NonNull final Result result) throws Exception {

        awesomeNotifications.cancelAllNotifications();

        if (AwesomeNotifications.debug)
            Log.d(TAG, "All notifications was cancelled");

        result.success(true);
    }

    private void channelMethodListAllSchedules(@NonNull final MethodCall call, @NonNull final Result result) throws Exception {
        List<NotificationModel> activeSchedules =
                awesomeNotifications.listAllPendingSchedules();

        List<Map<String, Object>> listSerialized = new ArrayList<>();

        if (activeSchedules != null)
            for (NotificationModel notificationModel : activeSchedules) {
                Map<String, Object> serialized = notificationModel.toMap();
                listSerialized.add(serialized);
            }

        result.success(listSerialized);
    }

    @SuppressWarnings("unchecked")
    private void channelMethodGetNextDate(@NonNull final MethodCall call, @NonNull final Result result) throws Exception {

        Map<String, Object> data = MapUtils.extractArgument(call.arguments(), Map.class).orNull();
        if (data == null)
            throw new AwesomeNotificationException("Schedule data is invalid");

        Map<String, Object> scheduleData = (Map<String, Object>) data.get(Definitions.NOTIFICATION_MODEL_SCHEDULE);
        if (scheduleData == null)
            throw new AwesomeNotificationException("Schedule data is invalid");

        NotificationScheduleModel scheduleModel = NotificationScheduleModel.getScheduleModelFromMap(scheduleData);
        if (scheduleModel == null)
            throw new AwesomeNotificationException("Schedule data is invalid");

        String fixedDateString = (String) data.get(Definitions.NOTIFICATION_INITIAL_FIXED_DATE);
        Date fixedDate = null;
        if (!StringUtils.isNullOrEmpty(fixedDateString))
            fixedDate = DateUtils.stringToDate(fixedDateString, scheduleModel.timeZone);

        Calendar nextValidDate =
                awesomeNotifications
                        .getNextValidDate(scheduleModel, fixedDate);

        String finalValidDateString = null;
        if (nextValidDate != null)
            finalValidDateString = DateUtils
                    .dateToString(
                            nextValidDate.getTime(),
                            scheduleModel.timeZone);

        result.success(finalValidDateString);
    }

    private void channelMethodGetLocalTimeZone(@NonNull final MethodCall call, @NonNull final Result result) throws Exception {
        result.success(
                awesomeNotifications
                        .getLocalTimeZone());
    }

    private void channelMethodGetUtcTimeZone(@NonNull final MethodCall call, @NonNull final Result result) throws Exception {
        result.success(
                awesomeNotifications
                        .getUtcTimeZone());
    }

    private void channelIsNotificationAllowed(@NonNull final MethodCall call, @NonNull final Result result) throws Exception {
        result.success(
                awesomeNotifications
                        .areNotificationsGloballyAllowed());
    }

    private void channelShowNotificationPage(@NonNull final MethodCall call, @NonNull final Result result) throws Exception {
        String channelKey = call.arguments();

        awesomeNotifications
                .showNotificationPage(
                    channelKey,
                    new PermissionCompletionHandler() {
                        @Override
                        public void handle(List<String> missingPermissions) {
                            result.success(missingPermissions);
                        }
                    }
                );
    }

    private void channelShowAlarmPage(@NonNull final MethodCall call, @NonNull final Result result) throws Exception {

        awesomeNotifications
                .showPreciseAlarmPage(
                    new PermissionCompletionHandler() {
                        @Override
                        public void handle(List<String> missingPermissions) {
                            result.success(missingPermissions);
                        }
                    }
                );
    }

    private void channelShowGlobalDndPage(@NonNull final MethodCall call, @NonNull final Result result) throws Exception {

        awesomeNotifications
                .showDnDGlobalOverridingPage(
                        new PermissionCompletionHandler() {
                            @Override
                            public void handle(List<String> missingPermissions) {
                                result.success(missingPermissions);
                            }
                        }
                );
    }

    @SuppressWarnings("unchecked")
    private void channelMethodCheckPermissions(@NonNull final MethodCall call, @NonNull final Result result) throws Exception {

        Map<String, Object> parameters = MapUtils.extractArgument(call.arguments(), Map.class).orNull();
        if(parameters == null)
            throw new AwesomeNotificationException("Parameters are required");

        String channelKey = (String) parameters.get(Definitions.NOTIFICATION_CHANNEL_KEY);

        List<String> permissions = (List<String>) parameters.get(Definitions.NOTIFICATION_PERMISSIONS);
        if(ListUtils.isNullOrEmpty(permissions))
            throw new AwesomeNotificationException("Permission list is required");

        permissions = awesomeNotifications
                        .arePermissionsAllowed(
                                channelKey,
                                permissions);

        result.success(permissions);
    }

    @SuppressWarnings("unchecked")
    private void channelMethodShouldShowRationale(@NonNull final MethodCall call, @NonNull final Result result) throws Exception {

        Map<String, Object> parameters = MapUtils.extractArgument(call.arguments(), Map.class).orNull();
        if(parameters == null)
            throw new AwesomeNotificationException("Parameters are required");

        String channelKey = (String) parameters.get(Definitions.NOTIFICATION_CHANNEL_KEY);
        List<String> permissions = (List<String>) parameters.get(Definitions.NOTIFICATION_PERMISSIONS);

        if(permissions == null)
            throw new AwesomeNotificationException("Permission list is required");

        if(permissions.isEmpty())
            throw new AwesomeNotificationException("Permission list cannot be empty");

        permissions = awesomeNotifications.shouldShowRationale(
                        channelKey,
                        permissions);

        result.success(permissions);
    }

    @SuppressWarnings("unchecked")
    private void channelRequestUserPermissions(@NonNull final MethodCall call, @NonNull final Result result) throws Exception {

        Map<String, Object> parameters = MapUtils.extractArgument(call.arguments(), Map.class).orNull();
        if(parameters == null)
            throw new AwesomeNotificationException("Parameters are required");

        if(!parameters.containsKey(Definitions.NOTIFICATION_PERMISSIONS))
            throw new AwesomeNotificationException("Permission list is required");

        String channelKey = (String) parameters.get(Definitions.NOTIFICATION_CHANNEL_KEY);
        List<String> permissions = (List<String>) parameters.get(Definitions.NOTIFICATION_PERMISSIONS);

        if(ListUtils.isNullOrEmpty(permissions))
            throw new AwesomeNotificationException("Permission list is required");

        awesomeNotifications
                .requestUserPermissions(
                    channelKey,
                    permissions,
                    new PermissionCompletionHandler() {
                        @Override
                        public void handle(List<String> missingPermissions) {
                            result.success(missingPermissions);
                        }
                    });
    }

    private void channelMethodCreateNotification(@NonNull final MethodCall call, @NonNull final Result result) throws Exception {

        Map<String, Object> pushData = call.arguments();
        NotificationModel notificationModel = new NotificationModel().fromMap(pushData);

        if (notificationModel == null)
            throw new AwesomeNotificationException("Invalid parameters");

        boolean created = awesomeNotifications.createNotification(notificationModel);
        result.success(created);
    }

    @SuppressWarnings("unchecked")
    private void channelMethodInitialize(@NonNull final MethodCall call, @NonNull final Result result) throws Exception {
        Map<String, Object> platformParameters = call.arguments();

        String defaultIconPath = (String) platformParameters.get(Definitions.INITIALIZE_DEFAULT_ICON);

        List<Object> channelsData = (List<Object>) platformParameters.get(Definitions.INITIALIZE_CHANNELS);
        List<Object> channelGroupsData = (List<Object>) platformParameters.get(Definitions.INITIALIZE_CHANNEL_GROUPS);

        Boolean debug = (Boolean) platformParameters.get(Definitions.INITIALIZE_DEBUG_MODE);
        debug = debug != null && debug;

        Object callbackDartObj = platformParameters.get(Definitions.DART_BG_HANDLE);
        long dartCallback = callbackDartObj == null ? 0L :(Long) callbackDartObj;

        awesomeNotifications.initialize(
                defaultIconPath,
                channelsData,
                channelGroupsData,
                dartCallback,
                debug);

        if (AwesomeNotifications.debug)
            Log.d(TAG, "Awesome Notifications Flutter plugin initialized");

        result.success(true);
    }

    @SuppressWarnings("unchecked")
    private void channelMethodSetActionHandle(@NonNull MethodCall call, Result result) throws Exception {
        Map<String, Object> platformParameters = call.arguments();
        Object callbackActionObj = platformParameters.get(ACTION_HANDLE);

        long silentCallback = callbackActionObj == null ? 0L : (Long) callbackActionObj;

        awesomeNotifications.SetActionHandle(silentCallback);

        boolean success = silentCallback != 0L;
        if(!success)
            Log.e(TAG, "Attention: there is no valid static method to receive notification action data in background");

        result.success(success);
    }
}
