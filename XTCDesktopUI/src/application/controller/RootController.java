package application.controller;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.ResourceBundle;

import application.Main;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.ScrollPane;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Effect;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;

public class RootController implements Initializable {
	private double wndXOff, wndYOff;
	private Stage stage;

	@FXML
	private Circle aimBtn;
	@FXML
	private Circle mapBtn;
	@FXML
	private Circle multiBtn;
	@FXML
	private Circle prefBtn;

	private Circle selected;

	private ArrayList<Circle> btns;

	@FXML
	private ScrollPane modulePane;

	private Canvas mapCanvas;

	private VBox aimbot;

	private VBox settings;

	private Effect blur = new DropShadow(10, Color.BLACK);

	@Override
	public void initialize(URL arg0, ResourceBundle arg1) {
		btns = new ArrayList<>();

		initModules();
		FXMLLoader loader = new FXMLLoader();
		try {
			loader.setLocation(Main.class.getResource("view/modules/MapView.fxml"));
			mapCanvas = loader.load();
		} catch (IOException e) {
			e.printStackTrace();
		}
		loader = new FXMLLoader();
		try {
			loader.setLocation(Main.class.getResource("view/settings/SettingsView.fxml"));
			settings = loader.load();
		} catch (IOException e) {
			e.printStackTrace();
		}
		loader = new FXMLLoader();
		try {
			loader.setLocation(Main.class.getResource("view/modules/AimbotView.fxml"));
			aimbot = loader.load();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void updateEffect() {
		for (Circle c : btns) {
			if (c == selected) {
				c.setEffect(blur);
			} else {
				c.setEffect(null);
			}
		}
	}

	private void initModules() {
		btns.add(prefBtn);
		btns.add(mapBtn);
		btns.add(aimBtn);

		mapBtn.setOnMouseClicked(ev -> {
			if (mapCanvas != null) {
				mapCanvas.widthProperty().unbind();
				mapCanvas.heightProperty().unbind();
				modulePane.setContent(mapCanvas);
				mapCanvas.widthProperty().bind(modulePane.widthProperty());
				mapCanvas.heightProperty().bind(modulePane.heightProperty());

				selected = mapBtn;
				updateEffect();

			} else {
				System.out.println("map Canvas not loaded..");
			}
		});
		prefBtn.setOnMouseClicked(ev -> {
			if (settings != null) {
				modulePane.setContent(settings);

				selected = prefBtn;

				updateEffect();
			} else {
				System.out.println("settings not loaded..");
			}

		});

		aimBtn.setOnMouseClicked(ev -> {
			if (aimbot != null) {
				modulePane.setContent(aimbot);

				selected = aimBtn;
				updateEffect();
			} else {
				System.out.println("aimbot not loaded..");
			}

		});
	}

	@FXML
	public void initMoveWindow(MouseEvent ev) {
		wndXOff = ev.getSceneX();
		wndYOff = ev.getSceneY();
	}

	@FXML
	public void moveWindow(MouseEvent ev) {
		stage.setX(ev.getScreenX() - wndXOff);
		stage.setY(ev.getScreenY() - wndYOff);
	}

	@FXML
	public void close() {
		stage.close();
	}

	@FXML
	public void minimize() {
		stage.setIconified(true);
	}

	@FXML
	public void maximize() {
		stage.setMaximized(!stage.isMaximized());
	}

	public void setStage(Stage stage) {
		this.stage = stage;
	}

}
