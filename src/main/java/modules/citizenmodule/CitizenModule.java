package modules.citizenmodule;

import com.centralcore.modules.Module;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

/**
 * modulo de base de datos de ciudadanos - demuestra la arquitectura de plugins
 *
 * completamente independiente del shell de la app, puede cargarse/descargarse dinamicamente
 */
public class CitizenModule implements Module {

    private Parent uiRoot;

    @Override
    public String getModuleId() {
        return "citizen_module";
    }

    @Override
    public String getName() {
        return "Citizen Database";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public String getDescription() {
        return "Manage city citizen records and profiles";
    }

    @Override
    public String getLogoPath() {
        return "images/logo.png";
    }

    @Override
    public void initialize() throws Exception {
        //carga el layout fxml para este modulo
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/citizenmodule/Main.fxml"));
        uiRoot = loader.load();
        System.out.println("citizen module initialized");
    }

    @Override
    public void shutdown() {
        //limpia recursos
        uiRoot = null;
        System.out.println("citizen module shut down");
    }

    @Override
    public Parent getMainUI() throws Exception {
        return uiRoot;
    }
}
