package ch.ethz.soms.nervous.virtualsensors;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;

import ch.ethz.soms.nervous.virtualsensors.model.VirtualSensorListData;
import ch.ethz.soms.nervous.virtualsensors.model.VirtualSensorModel;

/**
 * A {@link PreferenceActivity} that presents a set of application settings. On handset devices, settings are presented as a
 * single list. On tablets, settings are split by category, with category headers shown to the left of the list of settings. <p>
 * See <a href="http://developer.android.com/design/patterns/settings.html"> Android Design: Settings</a> for design guidelines
 * and the <a href="http://developer.android.com/guide/topics/ui/settings.html">Settings API Guide</a> for more information on
 * developing a Settings UI.
 */
public class VirtualSensorSettingsActivity extends PreferenceActivity {
    public static final String TAG = VirtualSensorSettingsActivity.class.getName();
    public static final String VIRTUAL_SENSORS_MAIN_CHECKBOX_KEY = "virtual_sensors_main_checkbox";
    public static final String VIRTUAL_SENSORS_SOUND_CHECKBOX_KEY = "virtual_sensors_sound_checkbox";
    public static final String VIRTUAL_SENSORS_VIBRATE_CHECKBOX_KEY = "virtual_sensors_vibrate_checkbox";

    /**
     * Determines whether to always show the simplified settings UI, where settings are presented in a single list. When false,
     * settings are shown as a master/detail two-pane view on tablets. When true, a single pane is shown on tablets.
     */
    private static final boolean ALWAYS_SIMPLE_PREFS = true;

    /**
     * A preference value change listener that updates the preference's summary to reflect its new value.
     */
    private static Preference.OnPreferenceChangeListener
        sBindPreferenceSummaryToValueListener =
        new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object value) {
                String stringValue = value.toString();

                if (preference instanceof MultiLineListPreference) {
                    // For list preferences, look up the correct display value in
                    // the preference's 'entries' list.
                    MultiLineListPreference listPreference = (MultiLineListPreference) preference;
                    int index = listPreference.findIndexOfValue(stringValue);

                    // Set the summary to reflect the new value.
                    preference.setSummary(
                        index >= 0
                        ? listPreference.getEntries()[index]
                        : null);

                    //Update the interval change to notification alarm scheduler
                    AlarmToScheduleNotificationReceiver.setAlarm(preference.getContext(), Long.valueOf(stringValue), preference.getKey());
                } else if(preference.getKey().equals(VIRTUAL_SENSORS_MAIN_CHECKBOX_KEY)) {
                    //Init all the scheduled notifications with the existing preferences
                    if((boolean) value) {
                        AlarmToScheduleNotificationReceiver.startAllAlarms(preference.getContext());
                    } else {
                        AlarmToScheduleNotificationReceiver.disableAllAlarms(preference.getContext());
                    }
                } else {
                    // For all other preferences, set the summary to the value's
                    // simple string representation.
                    preference.setSummary(stringValue);
                }
                return true;
            }
        };

    /**
     * Helper method to determine if the device has an extra-large screen. For example, 10" tablets are extra-large.
     */
    private static boolean isXLargeTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE;
    }

    /**
     * Determines whether the simplified settings UI should be shown. This is true if this is forced via {@link
     * #ALWAYS_SIMPLE_PREFS}, or the device doesn't have newer APIs like {@link PreferenceFragment}, or the device doesn't have an
     * extra-large screen. In these cases, a single-pane "simplified" settings UI should be shown.
     */
    private static boolean isSimplePreferences(Context context) {
        return ALWAYS_SIMPLE_PREFS
               || Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB
               || !isXLargeTablet(context);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        //Show the settings UI
        setupSimplePreferencesScreen();

        //Triggers the notifications to start with default values.
        initOnPreferenceChangeListener();
    }

    //Triggers the notifications to start with default values.
    private void initOnPreferenceChangeListener() {
        // Set the listener to watch for value changes.
        Preference allNotificationsPreference = findPreference(VIRTUAL_SENSORS_MAIN_CHECKBOX_KEY);
        allNotificationsPreference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

        //Check if dialog as been open once
        SharedPreferences sharedPref = this.getPreferences(Context.MODE_PRIVATE);
        boolean isInitialised = sharedPref.getBoolean("virtual_sensors_initialised", false);

        if(!isInitialised) {
            //Set initialised true
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putBoolean("virtual_sensors_initialised", true);
            editor.commit();

            // Trigger the listener immediately with the preference's current value.
            sBindPreferenceSummaryToValueListener.onPreferenceChange(allNotificationsPreference,
                                                                     PreferenceManager
                                                                         .getDefaultSharedPreferences(
                                                                             allNotificationsPreference.getContext())
                                                                         .getBoolean(allNotificationsPreference.getKey(), false));
        }
    }

    /**
     * Shows the settings UI and triggers the notifications to start with default values.
     */
    private void setupSimplePreferencesScreen() {
        if (!isSimplePreferences(this)) {
            return;
        }

        // In the simplified UI, fragments are not used at all and we instead
        // use the older PreferenceActivity APIs.

        // Add 'General' preferences.
        addPreferencesFromResource(R.xml.pref_general);

        // Add preferences, and a corresponding header.
        PreferenceCategory fakeHeader = new PreferenceCategory(this);
        fakeHeader.setTitle(R.string.pref_header_main);
        getPreferenceScreen().addPreference(fakeHeader);

        //Dynamically add preferences here
        for (VirtualSensorModel vsm : VirtualSensorListData.getVirtualSensorList(this)) {
            // Create the new ListPref
            MultiLineListPreference pref = new MultiLineListPreference(this);

            // Set intervals
            pref.setEntries(vsm.getIntervalTypes());
            pref.setEntryValues(vsm.getIntervalValues());
            pref.setDefaultValue(vsm.getDefaultInterval()[1]);
            String value =  PreferenceManager
                .getDefaultSharedPreferences(this).getString(vsm.getSensorId(), vsm.getDefaultInterval()[1]);
            pref.setSummary(vsm.getIntervalType(value));

            pref.setTitle(vsm.getMessage());
            pref.setKey(vsm.getSensorId());
            pref.setPersistent(true);

            // Add the pref into list
            getPreferenceScreen().addPreference(pref);

            // Bind the summaries of EditText/List/Dialog/Ringtone preferences to
            // their values. When their values change, their summaries are updated
            // to reflect the new value, per the Android Design guidelines.
            pref.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

            // Turn all sensors on/off
            pref.setDependency(VIRTUAL_SENSORS_MAIN_CHECKBOX_KEY);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onIsMultiPane() {
        return isXLargeTablet(this) && !isSimplePreferences(this);
    }

    @Override
    protected boolean isValidFragment(String fragmentName) {
        return true;
    }

}
