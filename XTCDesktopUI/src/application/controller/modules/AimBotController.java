package application.controller.modules;

import java.awt.AWTException;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Robot;
import java.net.URL;
import java.util.ResourceBundle;

import de.olfillasodikno.xtc.modules.aimbot.AimbotModule;
import de.olfillasodikno.xtc.modules.aimbot.MouseMoveImpl;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.VBox;

public class AimBotController implements Initializable {

	@FXML
	public VBox root;

	@FXML
	public ToggleButton switchBtn;

	@FXML
	public Button testBtn;

	@FXML
	public Label facX;

	@FXML
	public Label facY;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		AimbotModule.setImpl(new WindowsMousImpl());
		switchBtn.setDisable(true);

		testBtn.setOnAction(e -> {
			Thread thread = new Thread(() -> {
				int length = 5;
				for (int i = 0; i < length; i++) {
					String text = String.format("%d", length - i);
					Platform.runLater(() -> {
						testBtn.setText(text);
					});
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e1) {
						e1.printStackTrace();
					}
				}
				AimbotModule.INSTANCE.setTest(true);
				Platform.runLater(() -> {
					testBtn.setText("Test");
				});
			});
			thread.start();
		});

		AimbotModule.setTestResultListener(() -> {
			Platform.runLater(() -> {
				facX.setText("Factor X: " + AimbotModule.INSTANCE.getFacPitch());
				facY.setText("Factor Y: " + AimbotModule.INSTANCE.getFacYaw());
				switchBtn.setDisable(false);
			});
		});

		switchBtn.selectedProperty().addListener(c -> {
			if (switchBtn.isSelected()) {
				AimbotModule.INSTANCE.setEnabled(true);
				switchBtn.setText("Turn Off");
			} else {
				AimbotModule.INSTANCE.setEnabled(false);
				switchBtn.setText("Turn On");
			}
		});
	}

	public class WindowsMousImpl implements MouseMoveImpl {
		private Robot robot;

		public WindowsMousImpl() {
			try {
				robot = new Robot();
			} catch (AWTException e) {
				e.printStackTrace();
			}
		}

		@Override
		public void moveMouse(int dY, int dX) {
			if (robot != null) {
				Point point = MouseInfo.getPointerInfo().getLocation();
				robot.mouseMove(point.x + dX, point.y + dY);
			}
		}

	}
}
