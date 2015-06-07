package ch.ethz.soms.nervous.virtualsensors;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import ch.ethz.soms.nervous.virtualsensors.model.VirtualSensorModel;

/**
 * A dialog activity to ask for user input and submit it to the virtual sensor handler. The activity gets intents with extra data
 * about the user response to the push notifications. The activity might not show if the user has already selected an option from
 * the notification and no need to input.
 */
public class VirtualSensorDialogActivity extends Activity {

    public static final String TAG = ScheduledNotificationService.class.getName();

    private VirtualSensorModel virtualSensorModel;
    private String userAction;
    private String userInput;

    private EditText editText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        if (intent != null) {
            handleIntent(intent);
        }
    }

    @Override
    public void onNewIntent(Intent newIntent) {
        this.setIntent(newIntent);
        if (newIntent != null) {
            handleIntent(newIntent);
        }
    }

    private void handleIntent(Intent intent) {
        try {
            String vsmJsonStr = intent.getStringExtra(ScheduledNotificationService.VIRTUAL_SENSOR_JSON);
            Gson gson = new Gson();
            virtualSensorModel = gson.fromJson(vsmJsonStr, VirtualSensorModel.class);
            userAction = intent.getStringExtra(ScheduledNotificationService.USER_ACTION);

            Log.d(TAG, "Handle user action on notification, sensorId = " + virtualSensorModel.getSensorId()
                       + ", userAction = " + userAction);

            if (virtualSensorModel.getSensorId() != null) {
                //Clear the notification
                NotificationManager notificationManager =
                    (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
                notificationManager.cancel(virtualSensorModel.getSensorId().hashCode());

                switch (userAction) {
                    case "_open":  //Show dialog to ask for input
                        setContentView(R.layout.activity_virtual_sensor_dialog);

                        // Show the question
                        TextView messageView = (TextView) findViewById(R.id.dialog_text_view_message);
                        messageView.setText(virtualSensorModel.getMessage());

                        editText = (EditText) findViewById(R.id.dialog_editText_input);
                        editText.addTextChangedListener(new TextWatcher() {
                            @Override
                            public void afterTextChanged(Editable mEdit) {
                                userInput = mEdit.toString();
                            }

                            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                            }

                            public void onTextChanged(CharSequence s, int start, int before, int count) {
                            }
                        });

                        if (virtualSensorModel.getOptionsType() == VirtualSensorModel.OptionsType.MANUAL_STRING_INPUT) {
                            // Hide the option radio buttons
                            ViewGroup radioButtonsLayout = (ViewGroup) findViewById(R.id.dialog_radio_group);
                            radioButtonsLayout.setVisibility(View.GONE);
                        } else if (virtualSensorModel.getOptions() != null && virtualSensorModel.getOptions().length > 0) {
                            final RadioGroup radioGroup = addRadioButtons(virtualSensorModel.getOptions());

                            if (virtualSensorModel.getOptionsType() == VirtualSensorModel.OptionsType.MULTYPLE_OPTIONS) {
                                // Hide the text input
                                editText.setVisibility(View.GONE);
                            } else {
                                editText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                                    @Override
                                    public void onFocusChange(View v, boolean hasFocus) {
                                        if (hasFocus) {
                                            radioGroup.clearCheck();
                                            userInput = editText.getText().toString();
                                        }
                                    }
                                });
                            }
                        }
                        break;
                    case "_delete": { //Report ignored notification
                        long timeStamp = System.currentTimeMillis();
                        deliverVirtualSensorData(virtualSensorModel.getSensorId(), "ignored", timeStamp);
                        finish();
                        break;
                    }
                    default: { //Report the user input made on the notification
                        long timeStamp = System.currentTimeMillis();
                        deliverVirtualSensorData(virtualSensorModel.getSensorId(), userAction, timeStamp);
                        finish();
                        break;
                    }
                }
            } else {
                Log.e(TAG, "sensorId == null");
            }
        } catch (JsonSyntaxException e) {
            //Fail silently
            e.printStackTrace();
        }
    }

    private RadioGroup addRadioButtons(final String[] options) {
        RadioGroup radioGroup = (RadioGroup) findViewById(R.id.dialog_radio_group);

        View.OnClickListener onClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                RadioButton button = (RadioButton) v;
                userInput = button.getText().toString();
                editText.clearFocus();
            }
        };

        for (int i = 0; i < options.length; i++) {
            RadioButton button = new RadioButton(this);
            button.setId(i + 100);
            button.setText(" " + options[i]);
            button.setTextColor(getResources().getColor(R.color.gray_nervous_dark));
            radioGroup.addView(button);

            button.setOnClickListener(onClickListener);
        }

        return radioGroup;
    }

    public void onActionIgnore(View v) {
        long timeStamp = System.currentTimeMillis();
        if (virtualSensorModel != null) {
            deliverVirtualSensorData(virtualSensorModel.getSensorId(), "ignored", timeStamp);
        }
        finish();
    }

    public void onActionSubmit(View v) {
        if (userInput != null && !userInput.isEmpty()) {
            long timeStamp = System.currentTimeMillis();
            deliverVirtualSensorData(virtualSensorModel.getSensorId(), userInput, timeStamp);
            finish();
        } else {
            Toast.makeText(this, getString(R.string.empty_response), Toast.LENGTH_LONG).show();
        }
    }

    public void onActionSettings(View v) {
        Intent intent = new Intent(this, VirtualSensorSettingsActivity.class);
        startActivity(intent);
    }

    /**
     * This is the exit method of this application which delivers the user responses to virtual sensor data handlers. For
     * integration within the same project, consider moving the implementation into the actual handler class. If the handling of
     * the virtual sensor notifications will be in a different application consider using a LocalBroadcastManager. More about
     * implicit/explicit broadcasting: http://codetheory.in/android-broadcast-receivers
     */
    public void deliverVirtualSensorData(String sensorId, String userInput, long timeStamp) {
        String response = sensorId + " : " + userInput + " : " + timeStamp;

        // TODO: Handle the data as needed.
        Toast.makeText(this, response, Toast.LENGTH_LONG).show();
        Log.i(TAG, sensorId + ": " + userInput + ": " + timeStamp);
    }

}
