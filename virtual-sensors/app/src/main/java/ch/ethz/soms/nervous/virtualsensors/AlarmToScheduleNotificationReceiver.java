package ch.ethz.soms.nervous.virtualsensors;

import com.google.gson.Gson;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.preference.PreferenceManager;
import android.util.Log;

import java.util.Calendar;

import ch.ethz.soms.nervous.virtualsensors.model.VirtualSensorListData;
import ch.ethz.soms.nervous.virtualsensors.model.VirtualSensorModel;

/**
 * BroadcastReceiver to handle the periodic alarm intents to run the start notification service. Consider using
 * WakefulBroadcastReceiver instead of BroadcastReceiver, if there is a need to guarantee that the CPU will stay awake while
 * running the service.
 */
public class AlarmToScheduleNotificationReceiver extends BroadcastReceiver {

    private static final String TAG = AlarmToScheduleNotificationReceiver.class.getName();

    public static void initAllAlarms(Context context) {
        Log.d(TAG, "initAllAlarms");
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        boolean enableAll = preferences.getBoolean(VirtualSensorSettingsActivity.VIRTUAL_SENSORS_MAIN_CHECKBOX_KEY, false);

        if(enableAll) {
            startAllAlarms(context);
        } else {
            disableAllAlarms(context);
        }
    }

    public static void startAllAlarms(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);

        Log.d(TAG, "startAllAlarms");
        //Init the alarm to start the notification sending service periodically with the interval.
        for (VirtualSensorModel vsm : VirtualSensorListData.getVirtualSensorList(context)) {
            long persistedIntervalInMinutes = Long.valueOf(preferences.getString(vsm.getSensorId(), "-1"));
            //Start or cancel new alarm with the given name
            AlarmToScheduleNotificationReceiver.setAlarm(context, persistedIntervalInMinutes, vsm);
        }

        // Enable {@code AlarmToScheduleNotificationReceiver} to automatically restart all the alarms when the
        // device is rebooted.
        ComponentName receiver = new ComponentName(context, InitAlarmsBootReceiver.class);
        PackageManager pm = context.getPackageManager();

        pm.setComponentEnabledSetting(receiver,
                                      PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                                      PackageManager.DONT_KILL_APP);
    }

    public static void disableAllAlarms(Context context) {
        Log.d(TAG, "disableAllAlarms");
        for (VirtualSensorModel vsm : VirtualSensorListData.getVirtualSensorList(context)) {
            AlarmToScheduleNotificationReceiver.cancelAlarm(context, vsm.getSensorId());
        }

        // Disable {@code InitAlarmsBootReceiver} so that it doesn't automatically restart all the
        // alarms when the device is rebooted.
        ComponentName receiver = new ComponentName(context, InitAlarmsBootReceiver.class);
        PackageManager pm = context.getPackageManager();

        pm.setComponentEnabledSetting(receiver,
                                      PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                                      PackageManager.DONT_KILL_APP);
    }

    /**
     * Returns an alarm manager that will fire at this hour at 00 minutes and repeat with given interval.
     */
    public static AlarmManager setAlarm(Context context, long intervalInMinutes, String sensorId) {
        VirtualSensorModel vsm = VirtualSensorListData.getVirtualSensor(context, sensorId);

        return setAlarm(context, intervalInMinutes, vsm);
    }

    /**
     * Returns an alarm manager that will fire at this hour at 00 minutes and repeat with given interval.
     */
    public static AlarmManager setAlarm(Context context, long intervalInMinutes, VirtualSensorModel vsm) {
        try {
            if(vsm == null) {
                Log.e(TAG, "Creating alarm to start notification service, vsm = null.");
                return null;
            } else if (intervalInMinutes <= 0) {
                //Cancel previous alarms
                AlarmToScheduleNotificationReceiver.cancelAlarm(context, vsm.getSensorId());
            } else {
                Log.d(TAG,
                      "Creating alarm to start notification service, sensorId = " + vsm.getSensorId()
                      + ", interval = " + intervalInMinutes + " seconds.");

                Calendar calendar = Calendar.getInstance();
                calendar.setTimeInMillis(System.currentTimeMillis());
                calendar.add(Calendar.SECOND, 5);

                Intent intent = new Intent(context, AlarmToScheduleNotificationReceiver.class);
                intent.setAction(vsm.getSensorId());
                intent.putExtra(ScheduledNotificationService.VIRTUAL_SENSOR_JSON, new Gson().toJson(vsm));

                PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

                AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, //Use ELAPSED_REALTIME_WAKEUP for fixed hours
                                          calendar.getTimeInMillis(),
                                          intervalInMinutes * 60 * 1000,
                                          pendingIntent
                );

                return alarmManager;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public static void cancelAlarm(Context context, String sensorId) {
        Log.d(TAG, "Canceled the alarm to start notification service, sensorId  = " + sensorId);

        Intent intent = new Intent(context, AlarmToScheduleNotificationReceiver.class);
        intent.setAction(sensorId);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        // Cancel the alarm
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(pendingIntent);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String sensorId = intent.getAction();
        Log.d(TAG, "Alarm to start notification service received, sensorId = " + sensorId);

        Intent service = new Intent(context, ScheduledNotificationService.class);
        service.putExtra(ScheduledNotificationService.VIRTUAL_SENSOR_JSON,
                         intent.getStringExtra(ScheduledNotificationService.VIRTUAL_SENSOR_JSON));

        context.startService(service);
    }

}