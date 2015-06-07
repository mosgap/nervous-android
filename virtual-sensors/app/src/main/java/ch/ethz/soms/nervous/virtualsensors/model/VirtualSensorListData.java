package ch.ethz.soms.nervous.virtualsensors.model;

import com.google.gson.Gson;

import android.content.Context;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class VirtualSensorListData {
    private static List<VirtualSensorModel> virtualSensorModels;

    public static VirtualSensorModel getVirtualSensor(Context context, String sensorId) {
        if(virtualSensorModels == null) {
            virtualSensorModels = getVirtualSensorList(context);
        }

        for(VirtualSensorModel vsm: virtualSensorModels) {
            if(sensorId.equals(vsm.getSensorId())) {
                return vsm;
            }
        }

        return null;
    }

    public static List<VirtualSensorModel> getVirtualSensorList(Context context) {
        try {
            String jsonStr = loadJSONFromAsset(context);

            Gson gson = new Gson();
            VirtualSensorListModel list = gson.fromJson(jsonStr, VirtualSensorListModel.class);
            virtualSensorModels = list.getVirtualSensorModels();
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<VirtualSensorModel>();
        }

        return virtualSensorModels;
    }

    public static String loadJSONFromAsset(Context context) {
        String json = null;
        try {
            InputStream is = context.getAssets().open("virtualSensors.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();

            json = new String(buffer, "UTF-8");
        } catch (IOException ex) {
            Toast.makeText(context, "Error parsing virtualSensors.json from assets folder.", Toast.LENGTH_LONG).show();
            ex.printStackTrace();
            return null;
        }

        return json;
    }
}
