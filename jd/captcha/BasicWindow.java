package jd.captcha;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

/**
 * Die Klasse dient als Window Basis Klasse.
 * 
 * @author coalado
 * 
 */
public class BasicWindow extends JFrame {
	/**
	 * 
	 */
	private static final long serialVersionUID = 8474181150357563979L;

	/**
	 * Gibt an ob beim Schließen des fensters das programm beendet werden sol
	 */
	public boolean exitSystem = true;

	/**
	 * Aktuelle X Position der Autopositionierung
	 */
	private static int screenPosX = 0;

	/**
	 * Aktuelle Y Position der Autopositionierung
	 */
	private static int screenPosY = 0;

	/**
	 * Owner. Owner der GUI
	 */
	public Object owner;

	public BasicWindow(Object owner) {
		this.owner = owner;
		initWindow();
	}

	public BasicWindow() {
		initWindow();
	}

	/**
	 * Gibt die default GridbagConstants zurück
	 * 
	 * @param x
	 * @param y
	 * @param width
	 * @param height
	 * @return
	 */
	public GridBagConstraints getGBC(int x, int y, int width, int height) {

		GridBagConstraints gbc = UTILITIES.getGBC(x, y, width, height);
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.weighty = 1;
		gbc.weightx = 1;

		return gbc;
	}

	/**
	 * Initialisiert das Fenster und setzt den WindowClosing Adapter
	 * 
	 */
	private void initWindow() {
		final BasicWindow _this = this;
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent event) {
				Window window = event.getWindow();
				_this.setVisible(true);
				window.setVisible(false);
				window.dispose();
				if (_this.exitSystem) {
					System.exit(0);
				}
			}

		});

		resizeWindow(100);
		setLocationByScreenPercent(50, 50);
		setBackground(Color.LIGHT_GRAY);
	}

	/**
	 * Gibt das Fenster wieder frei
	 * 
	 */
	public void destroy() {
		setVisible(false);
		dispose();
	}

	/**
	 * Prozentuales (im bezug aufd en Screen) setzend er größe
	 * 
	 * @param percent
	 *            in screenProzent
	 */
	public void resizeWindow(int percent) {
		Dimension screenSize = getToolkit().getScreenSize();
		setSize((screenSize.width * percent) / 100,
				(screenSize.height * percent) / 100);
	}

	/**
	 * packt das fenster neu
	 * 
	 */
	public void repack() {
		SwingUtilities.updateComponentTreeUI(this);
	}

	/**
	 * Prozentuales Positionsetzen des fensters (Mittelpunkt
	 * 
	 * @param width
	 *            in screenprozent
	 * @param height
	 *            in ScreenProzent
	 */
	public void setLocationByScreenPercent(int width, int height) {
		Dimension screenSize = getToolkit().getScreenSize();

		setLocation(((screenSize.width - getSize().width) * width) / 100,
				((screenSize.height - getSize().height) * height) / 100);
	}

	/**
	 * Zeigt ein Image in einem Neuen fenster an. Die fenster Positionieren sich
	 * von Links oben nach rechts uten von selbst
	 * 
	 * @param file
	 * @param title
	 * @return
	 */
	public static BasicWindow showImage(File file, String title) {
		Dimension screenSize = new JFrame().getToolkit().getScreenSize();
		Image img = UTILITIES.loadImage(file);
		BasicWindow w = new BasicWindow();
		ImageComponent ic = new ImageComponent(img);

		w.setSize(ic.getImageWidth() + 10, ic.getImageHeight() + 20);
		w.setLocation(screenPosX, screenPosY);
		screenPosY += ic.getImageHeight() + 40;
		if (screenPosY >= screenSize.height) {
			screenPosX += ic.getImageWidth() + 30;
			screenPosY = 0;
		}
		w.setTitle(title);
		w.setLayout(new GridBagLayout());
		w.add(ic, UTILITIES.getGBC(0, 0, 1, 1));
		w.setVisible(true);
		w.pack();
		w.repack();
		w.setAlwaysOnTop(true);
		return w;

	}

	/**
	 * Zeigt ein Image in einem neuen fenster an.Die fenster Positionieren sich
	 * von Links oben nach rechts uten von selbst
	 * 
	 * @param img
	 * @return BasicWindow Das neue fenster
	 */
	public static BasicWindow showImage(Image img) {

		return showImage(img, img.toString());
	}

	/**
	 * Zeigt ein image in einem Neuen fenster an. Das fenster positioniert sich
	 * im nächsten Freien bereich
	 * 
	 * @param img
	 * @param title
	 * @return BasicWindow das neue fenster
	 */
	public static BasicWindow showImage(Image img, String title) {
		Dimension screenSize = new JFrame().getToolkit().getScreenSize();

		BasicWindow w = new BasicWindow();
		ImageComponent ic = new ImageComponent(img);

		w.setSize(ic.getImageWidth() + 10, ic.getImageHeight() + 20);
		w.setLocation(screenPosX, screenPosY);
		screenPosY += ic.getImageHeight() + 40;
		if (screenPosY >= screenSize.height) {
			screenPosX += ic.getImageWidth() + 30;
			screenPosY = 0;
		}
		w.setTitle(title);
		w.setLayout(new GridBagLayout());
		w.add(ic, UTILITIES.getGBC(0, 0, 1, 1));
		w.setVisible(true);
		w.pack();
		w.repack();
		w.setAlwaysOnTop(true);
		return w;

	}

}