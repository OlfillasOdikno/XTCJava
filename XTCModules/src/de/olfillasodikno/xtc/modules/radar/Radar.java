package de.olfillasodikno.xtc.modules.radar;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import javax.sound.midi.Synthesizer;
import javax.swing.JPanel;

import de.olfillasodikno.xtc.proto.Entity;

public class Radar extends JPanel {

	private BufferedImage texture;

	private int posX;
	private int posY;

	private float scale;

	private float rotation;
	private float playerX;
	private float playerY;

	public Map<Integer, RadarEntity> entities = new HashMap<>();

	public BufferedImage getTexture() {
		return texture;
	}

	public void setRotation(float rotation) {
		this.rotation = rotation;
	}

	public void setPlayerXY(float playerX, float playerY) {
		this.playerX = playerX;
		this.playerY = playerY;
	}

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);

		Graphics2D g2d = (Graphics2D) g.create();

		int w = getWidth();
		int h = getHeight();

		g2d.setColor(Color.BLACK.brighter());
		g2d.fillRect(0, 0, w, h);

		float facX = (float) w / texture.getWidth();
		float facY = (float) h / texture.getHeight();

		float cX = (playerX - posX) * facX / scale;
		float cY = (posY - playerY) * facY / scale;

		g2d.translate(w / 2 - cX, h / 2 - cY);
		g2d.rotate(Math.toRadians(rotation), cX, cY);

		g2d.drawImage(texture, 0, 0, w, h, null);
		entities.values().forEach(e -> {
			e.paint(g2d, facX, facY, posX, posY, scale);
		});
		g2d.translate(-w / 2 + cX, -h / 2 + cY);

	}

	public void addEntity(Entity e) {
		entities.put(e.getId(), new RadarEntity());
	}

	public static Radar fromFile(File f) {
		Radar radar = new Radar();
		try (Stream<String> lines = Files.lines(f.toPath(), Charset.defaultCharset())) {
			lines.forEach(l -> {
				if (l.contains("\"pos_x\"")) {
					radar.posX = Integer.parseInt(l.split("\"pos_x\"")[1].trim().split("\"")[1].split("\"")[0]);
				}
				if (l.contains("\"pos_y\"")) {
					radar.posY = Integer.parseInt(l.split("\"pos_y\"")[1].trim().split("\"")[1].split("\"")[0]);
				}
				if (l.contains("\"scale\"")) {
					radar.scale = Float.parseFloat(l.split("\"scale\"")[1].trim().split("\"")[1].split("\"")[0]);
				}
				if (l.contains("material")) {
					String textureFile = l.split("\"material\"")[1].trim().split("\"")[1] + "_radar.dds";
					byte[] buffer;
					try (FileInputStream fis = new FileInputStream(new File(textureFile))) {
						buffer = new byte[fis.available()];
						fis.read(buffer);
					} catch (Exception e) {
						e.printStackTrace();
						return;
					}

					int[] pixels = DDSReader.read(buffer, DDSReader.ARGB, 0);
					int width = DDSReader.getWidth(buffer);
					int height = DDSReader.getHeight(buffer);
					BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
					image.setRGB(0, 0, width, height, pixels, 0, width);
					radar.texture = image;
				}
			});
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		return radar;
	}
}
