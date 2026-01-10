package mochineko.natural_disaster.status;

import mochineko.natural_disaster.api.DisasterAPI;
import mochineko.natural_disaster.api.P2PEarthquakeAPI;

import java.lang.reflect.Method;

public enum DisasterType {
    EARTHQUAKE(P2PEarthquakeAPI.class);

    private Class<? extends DisasterAPI> disasterClass;

    DisasterType(Class<? extends DisasterAPI> disasterClass) {
        this.disasterClass = disasterClass;
    }

    public Class<? extends DisasterAPI> getDisasterClass() {
        return disasterClass;
    }
}
