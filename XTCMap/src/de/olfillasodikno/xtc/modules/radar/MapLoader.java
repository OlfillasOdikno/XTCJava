package de.olfillasodikno.xtc.modules.radar;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;

import javafx.embed.swing.SwingFXUtils;

public class MapLoader {
	public static RadarMap loadMap(File dir, String name) {
		File textFile = new File(dir, name + ".txt");
		if (!textFile.exists() || !textFile.canRead()) {
			return null;
		}

		int x = 0;
		int y = 0;
		float scale = 0;
		String mapTexture = null;

		try (BufferedReader br = new BufferedReader(new FileReader(textFile))) {
			String l;
			while ((l = br.readLine()) != null) {
				if (l.contains("\"pos_x\"")) {
					x = Integer.parseInt(l.split("\"pos_x\"")[1].trim().split("\"")[1].split("\"")[0]);
				}
				if (l.contains("\"pos_y\"")) {
					y = Integer.parseInt(l.split("\"pos_y\"")[1].trim().split("\"")[1].split("\"")[0]);
				}
				if (l.contains("\"scale\"")) {
					scale = Float.parseFloat(l.split("\"scale\"")[1].trim().split("\"")[1].split("\"")[0]);
				}
				if (l.contains("material")) {
					mapTexture = l.split("\"material\"")[1].trim().split("\"")[1] + "_radar.dds";
					mapTexture = mapTexture.replace("overviews/", "");
				}
			}
		} catch (IOException ex) {
			ex.printStackTrace();
			return null;
		}
		if (mapTexture == null) {
			System.out.println("MapTexture not specified");
			return null;
		}
		File mapFile = new File(dir, mapTexture);
		if (!mapFile.exists() || !mapFile.canRead()) {
			System.out.println(mapFile.getAbsolutePath());
			System.out.println("MapTexture not exist");
			return null;
		}
		byte[] buffer = null;
		try (FileInputStream fis = new FileInputStream(mapFile)) {
			buffer = new byte[fis.available()];
			fis.read(buffer);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		int[] pixels = DDSReader.read(buffer, DDSReader.ARGB, 0);
		int width = DDSReader.getWidth(buffer);
		int height = DDSReader.getHeight(buffer);
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		image.setRGB(0, 0, width, height, pixels, 0, width);
		return new RadarMap(name, SwingFXUtils.toFXImage(image, null), x, y, scale);
	}

}
