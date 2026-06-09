package com.centralcore.util;

import java.io.IOException;
import java.net.URL;

import com.centralcore.modules.Module;

import javafx.animation.FadeTransition;
import javafx.scene.shape.Rectangle;
import javafx.scene.Group;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.transform.Scale;
import javafx.stage.Stage;
import javafx.util.Duration;

//punto único de navegación, todos los cambios de pantalla pasan por aqui
public class SceneManager {

    private static Stage stage;
    private static StackPane mainShellContentPane;
    private static javafx.scene.layout.VBox sidebar;

    //escena y wrapper raíz reutilizada para evitar el flash entre pantallas
    private static Scene sharedScene;
    private static VBox sceneWrapper;

    //dimensiones de diseño — el layout siempre se calcula a este tamaño,
    //y un Scale transform lo adapta al tamaño real de la ventana
    private static final double DESIGN_W = 1380;
    private static final double DESIGN_H = 820;

    private static final String FXML_PATH = "/com/centralcore/fxml/";
    private static final String CSS_PATH  = "/com/centralcore/css/";

    public static void initialize(Stage primaryStage) {
        stage = primaryStage;
        buildSharedScene();
    }

    //construye la escena una sola vez, el contenido cambia, pero la scene no se recrea
    private static void buildSharedScene() {
        sceneWrapper = new VBox();
        sceneWrapper.setStyle(
                "-fx-spacing: 0; -fx-padding: 0; " +
                        "-fx-background-color: #1e2738; " +
                        "-fx-border-color: #3d5270; " +
                        "-fx-border-width: 1; " +
                        "-fx-border-radius: 8; " +
                        "-fx-background-radius: 8;"
        );

        //fijar el tamaño de diseño: el layout siempre se calcula a 1380x820
        sceneWrapper.setPrefWidth(DESIGN_W);
        sceneWrapper.setPrefHeight(DESIGN_H);
        sceneWrapper.setMaxWidth(DESIGN_W);
        sceneWrapper.setMaxHeight(DESIGN_H);

        //clip redondeado para que las esquinas se vean bien en modo ventana
        //al maximizar se elimina el clip para que ocupe toda la pantalla sin huecos
        applyRoundedClip(8);
        sceneWrapper.widthProperty().addListener((obs, o, n) -> applyRoundedClip(stage.isMaximized() ? 0 : 8));
        sceneWrapper.heightProperty().addListener((obs, o, n) -> applyRoundedClip(stage.isMaximized() ? 0 : 8));

        CustomTitleBar titleBar = new CustomTitleBar(stage);
        sceneWrapper.getChildren().add(titleBar);

        //viewport: contenedor transparente que ocupa toda la ventana real
        //el sceneWrapper (contenido de diseño) se escala dentro de este viewport
        StackPane viewport = new StackPane();
        viewport.setBackground(javafx.scene.layout.Background.EMPTY);

        //scale transform: adapta sceneWrapper (1380x820) al tamaño real de la ventana
        //xProperty y yProperty pueden diferir si la ventana tiene otro aspect ratio,
        //esto rellena la pantalla completamente; si prefieres barras negras y ratio fijo,
        //usa Bindings.min(viewport.widthProperty().divide(DESIGN_W), viewport.heightProperty().divide(DESIGN_H))
        //para ambas propiedades
        Scale scaleTransform = new Scale();
        scaleTransform.setPivotX(0);
        scaleTransform.setPivotY(0);
        scaleTransform.xProperty().bind(viewport.widthProperty().divide(DESIGN_W));
        scaleTransform.yProperty().bind(viewport.heightProperty().divide(DESIGN_H));
        sceneWrapper.getTransforms().add(scaleTransform);

        //group no es redimensionable, así StackPane no encoge sceneWrapper en el layout pass,
        //sin esto el Scale actúa sobre un wrapper ya reducido y el contenido queda
        //más pequeño que la ventana en vez de ajustarse a ella
        Group scalingGroup = new Group(sceneWrapper);
        StackPane.setAlignment(scalingGroup, javafx.geometry.Pos.TOP_LEFT);
        viewport.getChildren().add(scalingGroup);

        sharedScene = new Scene(viewport);
        //con undecorated no necesitamos fill transparente
        sharedScene.setFill(Color.TRANSPARENT);  //transparente requerido por StageStyle.TRANSPARENT

        URL globalCss = SceneManager.class.getResource(CSS_PATH + "global.css");
        if (globalCss != null) sharedScene.getStylesheets().add(globalCss.toExternalForm());

        stage.setScene(sharedScene);
    }

    //aplica un recorte redondeado al wrapper con el radio dado (0 = sin redondear)
    public static void applyRoundedClip(double radius) {
        if (sceneWrapper == null) return;
        double w = sceneWrapper.getWidth();
        double h = sceneWrapper.getHeight();
        if (w <= 0 || h <= 0) {
            //si el wrapper aún no tiene tamaño, esperar al primer layout
            sceneWrapper.layoutBoundsProperty().addListener(new javafx.beans.value.ChangeListener<javafx.geometry.Bounds>() {
                @Override
                public void changed(
                        javafx.beans.value.ObservableValue<? extends javafx.geometry.Bounds> obs2,
                        javafx.geometry.Bounds o2,
                        javafx.geometry.Bounds n2) {

                    sceneWrapper.layoutBoundsProperty().removeListener(this);
                    applyRoundedClip(radius);
                }
            });
            return;
        }
        if (radius <= 0) {
            sceneWrapper.setClip(null);
        } else {
            Rectangle clip = new Rectangle(w, h);
            clip.setArcWidth(radius * 2);
            clip.setArcHeight(radius * 2);
            sceneWrapper.setClip(clip);
        }
    }

    public static void setMainShellContentPane(StackPane contentPane) {
        mainShellContentPane = contentPane;
    }

    public static void setSidebar(javafx.scene.layout.VBox sidebarPane) {
        sidebar = sidebarPane;
    }

    public static void showWelcome() {
        loadView("Welcome.fxml", "welcome");
    }

    public static void showLogin() {
        loadView("Login.fxml", "auth");
    }

    public static void showMainShell() {
        loadView("MainShell.fxml", "main");
    }

    public static void showModule(Module module) {
        if (mainShellContentPane == null) {
            System.err.println("panel de contenido del shell principal no registrado");
            return;
        }

        if (sidebar != null) {
            sidebar.setManaged(false);
            sidebar.setVisible(false);
        }

        try {
            Parent moduleUI = module.getMainUI();
            HBox headerBar = createModuleHeader(module);

            VBox moduleWrapper = new VBox();
            moduleWrapper.setStyle("-fx-spacing: 0; -fx-padding: 0;");
            moduleWrapper.setMaxWidth(Double.MAX_VALUE);
            moduleWrapper.setMaxHeight(Double.MAX_VALUE);
            StackPane.setAlignment(moduleWrapper, javafx.geometry.Pos.TOP_LEFT);
            moduleWrapper.prefWidthProperty().bind(mainShellContentPane.widthProperty());
            moduleWrapper.prefHeightProperty().bind(mainShellContentPane.heightProperty());
            moduleWrapper.getChildren().addAll(headerBar, moduleUI);
            VBox.setVgrow(moduleUI, Priority.ALWAYS);

            mainShellContentPane.getChildren().setAll(moduleWrapper);
            System.out.println("modulo cargado: " + module.getName());

        } catch (Exception e) {
            System.err.println("error al cargar ui del modulo: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static HBox createModuleHeader(Module module) {
        HBox header = new HBox();
        header.setStyle(
                "-fx-padding: 5 15; " +
                        "-fx-background-color: #34495e; " +
                        "-fx-border-color: #2c3e50; " +
                        "-fx-border-width: 0 0 1 0; " +
                        "-fx-alignment: center-left; " +
                        "-fx-spacing: 10;"
        );
        header.setPrefHeight(24);

        javafx.scene.control.Button btnBack = new javafx.scene.control.Button(TranslationManager.get("btn.back"));
        btnBack.setStyle(
                "-fx-font-size: 10; -fx-padding: 4 8; -fx-cursor: hand; " +
                        "-fx-text-fill: #ecf0f1; -fx-background-color: #2c3e50; " +
                        "-fx-border-color: #1a252f; -fx-border-width: 1;"
        );
        btnBack.setOnMouseEntered(e -> btnBack.setStyle(
                btnBack.getStyle().replace("#2c3e50", "#1a252f")));
        btnBack.setOnMouseExited(e -> btnBack.setStyle(
                btnBack.getStyle().replace("#1a252f", "#2c3e50")));
        btnBack.setOnAction(e -> showModulesView());

        javafx.scene.control.Label lblName = new javafx.scene.control.Label(module.getName());
        lblName.setStyle("-fx-font-size: 13; -fx-font-weight: bold; -fx-text-fill: #ecf0f1;");

        HBox spacerL = new HBox();
        HBox spacerR = new HBox();
        HBox.setHgrow(spacerL, Priority.ALWAYS);
        HBox.setHgrow(spacerR, Priority.ALWAYS);

        header.getChildren().addAll(btnBack, spacerL, lblName, spacerR);
        return header;
    }

    private static void showModulesView() {
        if (sidebar != null) {
            sidebar.setManaged(true);
            sidebar.setVisible(true);
        }
        if (mainShellContentPane == null) return;
        try {
            URL fxmlUrl = SceneManager.class.getResource(FXML_PATH + "ModulesView.fxml");
            if (fxmlUrl != null) {
                FXMLLoader loader = new FXMLLoader(fxmlUrl);
                Parent modulesView = loader.load();
                mainShellContentPane.getChildren().setAll(modulesView);
                System.out.println("volvió a la vista de módulos");
            }
        } catch (IOException e) {
            System.err.println("error al cargar vista de módulos: " + e.getMessage());
        }
    }

    //carga una vista y la intercambia con fade para evitar el flash entre pantallas
    private static void loadView(String fxmlFile, String cssName) {
        try {
            URL fxmlUrl = SceneManager.class.getResource(FXML_PATH + fxmlFile);
            if (fxmlUrl == null) {
                System.err.println("fxml no encontrado: " + FXML_PATH + fxmlFile);
                return;
            }

            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent root = loader.load();

            //quitar css de pantalla anterior y añadir el nuevo antes del swap
            sharedScene.getStylesheets().removeIf(s ->
                    !s.contains("global.css")
            );
            URL specificCss = SceneManager.class.getResource(CSS_PATH + cssName + ".css");
            if (specificCss != null) sharedScene.getStylesheets().add(specificCss.toExternalForm());

            //crossfade: insertar nuevo contenido a opacidad 0 antes de hacer fadeOut
            //asi el wrapper nunca queda visible entre pantallas
            if (sceneWrapper.getChildren().size() > 1) {
                Parent oldRoot = (Parent) sceneWrapper.getChildren().get(1);

                //insertar nuevo por debajo del viejo (índice 1), el viejo sube a 2
                root.setOpacity(0.0);
                VBox.setVgrow(root, Priority.ALWAYS);
                sceneWrapper.getChildren().add(1, root);

                //fade out del viejo y fade in del nuevo simultáneamente
                FadeTransition fadeOut = new FadeTransition(Duration.millis(120), oldRoot);
                fadeOut.setFromValue(1.0);
                fadeOut.setToValue(0.0);

                FadeTransition fadeIn = new FadeTransition(Duration.millis(120), root);
                fadeIn.setFromValue(0.0);
                fadeIn.setToValue(1.0);

                fadeOut.setOnFinished(ev -> sceneWrapper.getChildren().remove(oldRoot));

                fadeOut.play();
                fadeIn.play();
            } else {
                //primera carga, sin animación
                sceneWrapper.getChildren().add(root);
                VBox.setVgrow(root, Priority.ALWAYS);
            }

        } catch (IOException e) {
            System.err.println("error cargando vista: " + fxmlFile + " - " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static Stage getStage() {
        return stage;
    }

    //expone el wrapper raíz para que CustomTitleBar pueda actualizar el radio al maximizar
    public static VBox getSceneWrapper() {
        return sceneWrapper;
    }
}