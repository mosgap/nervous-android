package ch.ethz.soms.nervous.virtualsensors;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * This BroadcastReceiver automatically (re)starts the alarm when the device is rebooted. This receiver is set to be disabled
 * (android:enabled="false") in the application's manifest file. When the user sets the alarm, the receiver is enabled. When the
 * user cancels the alarm, the receiver is disabled, so that rebooting the device will not trigger this receiver.
 */

public class InitAlarmsBootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals("android.intent.action.BOOT_COMPLETED")) {
            AlarmToScheduleNotificationReceiver.initAllAlarms(context);
        }
    }
}