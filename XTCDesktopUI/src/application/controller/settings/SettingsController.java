package application.controller.settings;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URL;
import java.util.ResourceBundle;

import org.pcap4j.core.PcapNativeException;
import org.pcap4j.core.PcapNetworkInterface;
import org.pcap4j.core.Pcaps;

import de.olfillasodikno.xtc.PassiveSniffer;
import de.olfillasodikno.xtc.SniffImpl;
import de.olfillasodikno.xtc.manager.CoreManager;
import de.olfillasodikno.xtc.modules.ModuleLoader;
import de.olfillasodikno.xtc.modules.radar.RadarModule;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class SettingsController implements Initializable {

	@FXML
	private ToggleGroup sniffImpl;

	@FXML
	private ToggleButton startButton;

	@FXML
	private Button resetButton;

	@FXML
	private RadioButton passiveImpl;

	@FXML
	private RadioButton proxyImpl;

	private ToggleGroup devicesGroup;
	private VBox devicesBtns;
	private VBox proxyStuff;
	private HBox srcStuff;
	private HBox dstStuff;

	private TextField srcAddr;
	private TextField srcPort;
	
	private TextField dstAddr;
	private TextField dstPort;
	
	@FXML
	private VBox root;

	@Override
	public void initialize(URL arg0, ResourceBundle arg1) {
		startButton.setDisable(true);
		devicesGroup = new ToggleGroup();
		devicesBtns = new VBox();
		
		proxyStuff = new VBox();
		srcStuff = new HBox();
		dstStuff = new HBox();
		
		srcAddr = new TextField();
		srcPort = new TextField();
		srcStuff.getChildren().addAll(new Label("Src: "),srcAddr,srcPort);
		
		dstAddr = new TextField();
		dstPort = new TextField();
		dstStuff.getChildren().addAll(new Label("Dst: "),dstAddr,dstPort);

		
		proxyStuff.getChildren().addAll(srcStuff,dstStuff);
		
		proxyStuff.setVisible(false);
		root.getChildren().add(proxyStuff);
		
		root.getChildren().add(devicesBtns);
		sniffImpl.selectedToggleProperty().addListener(c -> {
			if (sniffImpl.getSelectedToggle() != null) {
				startButton.setDisable(false);
			}
			if (sniffImpl.getSelectedToggle() == passiveImpl) {
				try {
					PrintStream old = System.out;
					System.setOut(new PrintStream(new OutputStream() {
						@Override
						public void write(int b) throws IOException {
						}
					}));
					Pcaps.findAllDevs().forEach(d -> {
						DeviceBtn btn = new DeviceBtn(d);
						btn.setToggleGroup(devicesGroup);
						devicesBtns.getChildren().add(btn);
					});
					System.setOut(old);
					devicesBtns.setVisible(true);
				} catch (PcapNativeException e) {
					e.printStackTrace();
				}
			} else if (sniffImpl.getSelectedToggle() == proxyImpl) {
				devicesBtns.getChildren().clear();
				devicesGroup.getToggles().clear();
				
				proxyStuff.setVisible(true);
			} else {
				devicesBtns.getChildren().clear();
				devicesGroup.getToggles().clear();
			}
		});
		startButton.selectedProperty().addListener(c -> {
			if (startButton.isSelected()) {
				sniffImpl.getToggles().forEach(t -> {
					if (t instanceof RadioButton) {
						((RadioButton) t).setDisable(true);
					}
				});
				devicesGroup.getToggles().forEach(t -> {
					if (t instanceof RadioButton) {
						((RadioButton) t).setDisable(true);
					}
				});
				onStart();
				startButton.setText("Stop");
			} else {
				sniffImpl.getToggles().forEach(t -> {
					if (t instanceof RadioButton) {
						((RadioButton) t).setDisable(false);
					}
				});
				devicesGroup.getToggles().forEach(t -> {
					if (t instanceof RadioButton) {
						((RadioButton) t).setDisable(false);
					}
				});
				onStop();
				startButton.setText("Start");
			}
		});
		resetButton.setOnAction(e -> {
			if (CoreManager.INSTANCE != null && CoreManager.INSTANCE.getImpl() != null) {
				CoreManager.INSTANCE.getImpl().stop();
			}
			CoreManager.INSTANCE = null;
			startButton.setText("Start");
			startButton.setSelected(false);
			startButton.setDisable(true);

			devicesGroup.getSelectedToggle().setSelected(false);
			devicesBtns.getChildren().clear();
			devicesGroup.getToggles().clear();
			devicesBtns.setVisible(false);
			sniffImpl.getSelectedToggle().setSelected(false);
		});
	}

	private void onStop() {
		if (CoreManager.INSTANCE == null) {
			return;
		}
		SniffImpl impl = CoreManager.INSTANCE.getImpl();
		if (impl != null) {
			impl.stop();
		}
	}

	private void onStart() {
		if (CoreManager.INSTANCE == null) {
			new CoreManager();
			ModuleLoader.addModule(RadarModule.class);
			ModuleLoader.loadModules();
		}
		if (sniffImpl.getSelectedToggle() == passiveImpl) {
			if (devicesGroup.getSelectedToggle() != null) {
				CoreManager.INSTANCE
						.setImpl(new PassiveSniffer(((DeviceBtn) devicesGroup.getSelectedToggle()).getIface()));
			} else {
				System.out.println("No device selected...");
			}
		}
	}

	private class DeviceBtn extends RadioButton {
		private PcapNetworkInterface iface;

		public DeviceBtn(PcapNetworkInterface iface) {
			this.iface = iface;
			this.setText(iface.getDescription());
		}

		public PcapNetworkInterface getIface() {
			return iface;
		}
	}

}
