package de.olfillasodikno.xtc.modules.radar;

import java.awt.EventQueue;

import javax.swing.JFrame;

public class Viewer extends JFrame {

	public Radar radar;
	
	
	public Viewer() {
		setTitle("Radar");
		setLocationRelativeTo(null);
		setDefaultCloseOperation(EXIT_ON_CLOSE);
	}
	
	public void setRadar(Radar radar) {
		getContentPane().removeAll();
		this.radar = radar;
		getContentPane().add(radar);
	}
	
	
	public static Viewer create() {
		Viewer ex = new Viewer();
		EventQueue.invokeLater(() -> {
			ex.setVisible(true);
		});
		return ex;
	}

}
