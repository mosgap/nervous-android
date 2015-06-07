package ch.ethz.soms.nervous.virtualsensors;

import com.google.gson.Gson;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import ch.ethz.soms.nervous.virtualsensors.model.VirtualSensorModel;


/**
 * A service class that handles the scheduled alarm events to send virtual sensor push notifications. The push notifications are
 * created with a specific intents to deliver the user action to appropriate handlers.
 */
public class ScheduledNotificationService extends IntentService {

    public static final String TAG = ScheduledNotificationService.class.getName();
    public static final String VIRTUAL_SENSOR_JSON = "VIRTUAL_SENSOR_JSON";
    public static final String USER_ACTION = "USER_ACTION";

    private NotificationManager mNotificationManager;

    public ScheduledNotificationService() {
        super("ScheduledNotificationService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);

        Bundle extras = intent.getExtras();

        if (extras != null && !extras.isEmpty()) {  // has effect of unparcelling Bundle
            try {
                String vsmJsonStr = intent.getStringExtra(VIRTUAL_SENSOR_JSON);
                boolean isSound = preferences.getBoolean(VirtualSensorSettingsActivity.VIRTUAL_SENSORS_SOUND_CHECKBOX_KEY, false);
                boolean isVibrate = preferences.getBoolean(VirtualSensorSettingsActivity.VIRTUAL_SENSORS_VIBRATE_CHECKBOX_KEY, false);

                this.sendNotification(vsmJsonStr, isSound, isVibrate);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    private void sendNotification(String vsmJsonStr, boolean isSound, boolean isVibrate) throws Exception {
        Log.i(TAG, "Sending local notification");

        Gson gson = new Gson();
        VirtualSensorModel vsm = gson.fromJson(vsmJsonStr, VirtualSensorModel.class);

        mNotificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        PendingIntent contentPendingIntent = generateContentPendingIntent(vsmJsonStr);

        NotificationCompat.Builder mBuilder =
            new NotificationCompat.Builder(this)
                //.setAutoCancel(true)  //Cancel is done when the intent is handled.
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle(getString(R.string.title_notification))
                .setStyle(new NotificationCompat.BigTextStyle().bigText(vsm.getMessage()))
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setContentText(vsm.getMessage())
                .setDeleteIntent(generateDeletePendingIntent(vsmJsonStr))
                .setContentIntent(contentPendingIntent);

        //Add one or two notification actions if the sensor has multiple options or multiple options plus input.
        //Add the third "more" button if there are more then 2 inputs or of there is plus input.
        String[] options = vsm.getOptions();
        if (options != null
            && (vsm.getOptionsType() == VirtualSensorModel.OptionsType.MULTYPLE_OPTIONS
                || vsm.getOptionsType() == VirtualSensorModel.OptionsType.MULTYPLE_OPTIONS_PLUS_INPUT)) {

            if (options.length >= 1) { //add notification action #1
                String action = vsm.getOptions()[0];
                mBuilder.addAction(R.drawable.bullet, action, generateActionPendingIntent(action, vsmJsonStr));
            }
            if (options.length >= 2) { //add notification action #2
                String action = vsm.getOptions()[1];
                mBuilder.addAction(R.drawable.bullet, action, generateActionPendingIntent(action, vsmJsonStr));
            }
            if (options.length == 3 && vsm.getOptionsType() == VirtualSensorModel.OptionsType.MULTYPLE_OPTIONS) {
                String action = vsm.getOptions()[2];
                mBuilder.addAction(R.drawable.bullet, action, generateActionPendingIntent(action, vsmJsonStr));
            } else if (options.length >= 3) { // add action #3
                mBuilder.addAction(R.drawable.bullet, VirtualSensorModel.MORE_OPTIONS_STR, contentPendingIntent);
            }
        }

        //Add sound and vibrate
        if(isSound) {
            mBuilder.setSound(Settings.System.DEFAULT_NOTIFICATION_URI);
        }
        if(isVibrate) {
            mBuilder.setVibrate(new long[]{1000, 1000});
        }

        int notificationId = vsm.getSensorId().hashCode();
        mNotificationManager.notify(notificationId, mBuilder.build());
    }

    private PendingIntent generateContentPendingIntent(String vsmJsonStr) {
        Intent contentIntent = new Intent(this, VirtualSensorDialogActivity.class);
        contentIntent.putExtra(VIRTUAL_SENSOR_JSON, vsmJsonStr);
        contentIntent.putExtra(USER_ACTION, "_open");
        contentIntent
            .setAction("" + Math.random()); //To avoid matching the intents as the same ones defined by Intent.filterEquals.
        contentIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return PendingIntent.getActivity(this, 0, contentIntent, PendingIntent.FLAG_CANCEL_CURRENT);
    }

    private PendingIntent generateDeletePendingIntent(String vsmJsonStr) {
        Intent deleteIntent = new Intent(this, VirtualSensorDialogActivity.class);
        deleteIntent.putExtra(VIRTUAL_SENSOR_JSON, vsmJsonStr);
        deleteIntent.putExtra(USER_ACTION, "_delete");
        deleteIntent
            .setAction("" + Math.random()); //To avoid matching the intents as the same ones defined by Intent.filterEquals.
        deleteIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return PendingIntent.getActivity(this, 0, deleteIntent, PendingIntent.FLAG_CANCEL_CURRENT);
    }

    private PendingIntent generateActionPendingIntent(String userAction, String vsmJsonStr) {
        Intent actionIntent = new Intent(this, VirtualSensorDialogActivity.class);
        actionIntent.putExtra(USER_ACTION, userAction);
        actionIntent.putExtra(VIRTUAL_SENSOR_JSON, vsmJsonStr);
        actionIntent.setAction("" + Math.random()); //To avoid matching the intents as the same ones defined by Intent.filterEquals.
        actionIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return PendingIntent.getActivity(this, 0, actionIntent, PendingIntent.FLAG_CANCEL_CURRENT);
    }


}
