package ch.ethz.soms.nervous.virtualsensors.model;


import java.util.List;

public class VirtualSensorListModel {
    public List<VirtualSensorModel> virtualSensorModels;

    public List<VirtualSensorModel> getVirtualSensorModels() {
        return virtualSensorModels;
    }

    public void setVirtualSensorModels(
        List<VirtualSensorModel> virtualSensorModels) {
        this.virtualSensorModels = virtualSensorModels;
    }
}
