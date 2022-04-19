package application;
	
import java.io.IOException;

import application.controller.RootController;
import de.codecentric.centerdevice.javafxsvg.SvgImageLoaderFactory;
import de.codecentric.centerdevice.javafxsvg.dimension.PrimitiveDimensionProvider;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import javafx.stage.StageStyle;


public class Main extends Application {
	@Override
	public void start(Stage primaryStage) {
		this.primaryStage = primaryStage;
		initRootLayout();
	}
	
	private Stage primaryStage;
	private BorderPane rootLayout;
	private RootController rootController;
	
	private void initRootLayout() {
		try {
			primaryStage.initStyle(StageStyle.TRANSPARENT);

			FXMLLoader loader = new FXMLLoader();
			loader.setLocation(Main.class.getResource("view/RootLayout.fxml"));
			rootLayout = loader.load();
			rootController = loader.getController();
			rootController.setStage(primaryStage);
			Scene scene = new Scene(rootLayout);
			scene.getStylesheets().add("application/view/style.css");
			primaryStage.setScene(scene);
			primaryStage.minWidthProperty().bind(rootLayout.minWidthProperty().add(31));
			primaryStage.show();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		SvgImageLoaderFactory.install(new PrimitiveDimensionProvider());
		launch(args);
	}
}
