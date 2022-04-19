package application.controller.modules;

import java.net.URL;
import java.util.ResourceBundle;

import de.olfillasodikno.xtc.modules.ModuleLoader;
import de.olfillasodikno.xtc.modules.radar.RadarModule;
import de.olfillasodikno.xtc.modules.radar.RadarView;
import javafx.animation.AnimationTimer;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.canvas.Canvas;

public class MapController implements Initializable {

	@FXML
	public Canvas canvas;
	
	@Override
	public void initialize(URL location, ResourceBundle resources) {
		ModuleLoader.addModule(RadarModule.class);

		RadarView v = new RadarView();
		RadarModule.setRadarView(v);
		AnimationTimer timer = new AnimationTimer() {
			@Override
			public void handle(long now) {
				v.onDraw(canvas);
			}
		};
		timer.start();
		canvas.setOnScroll(e -> {
			v.setScale((float) (v.getScale() + e.getDeltaY() / e.getMultiplierY()));
		});
	}
}
