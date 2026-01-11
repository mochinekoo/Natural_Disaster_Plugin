package mochineko.natural_disaster;

import mochineko.natural_disaster.api.P2PEEWAPI;
import mochineko.natural_disaster.api.P2PEarthquakeAPI;
import mochineko.natural_disaster.api.P2PTsunamiAPI;
import org.bukkit.plugin.java.JavaPlugin;

public final class Main extends JavaPlugin {

    @Override
    public void onEnable() {
        // Plugin startup logic
        P2PEarthquakeAPI.startMonitor();
        P2PEEWAPI.startMonitor();
        P2PTsunamiAPI.startMonitor();
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
