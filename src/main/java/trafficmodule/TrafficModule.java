package trafficmodule;

import com.centralcore.modules.Module;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

/**
 * traffic management module - demonstrates the plugin architecture
 * modulo de gestion de trafico - demuestra la arquitectura de plugins
 *
 * completely independent from the app shell, can be loaded/unloaded dynamically
 * completamente independiente del shell de la app, puede cargarse/descargarse dinamicamente
 */
public class TrafficModule implements Module {

    private Parent uiRoot;

    @Override
    public String getModuleId() {
        return "traffic_module";
    }

    @Override
    public String getName() {
        return "Traffic Management";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public String getDescription() {
        return "Track vehicles and traffic incidents in the city";
    }

    @Override
    public String getLogoPath() {
        return "images/logo.png";
    }

    @Override
    public void initialize() throws Exception {
        //load the fxml layout for this module
        //carga el layout fxml para este modulo
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/trafficmodule/Main.fxml"));
        uiRoot = loader.load();
        System.out.println("traffic module initialized");
    }

    @Override
    public void shutdown() {
        //cleanup resources
        //limpia recursos
        uiRoot = null;
        System.out.println("traffic module shut down");
    }

    @Override
    public Parent getMainUI() throws Exception {
        return uiRoot;
    }
}
