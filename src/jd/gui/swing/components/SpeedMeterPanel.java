//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.gui.swing.components;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.BorderFactory;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.plaf.FontUIResource;

import jd.config.ConfigPropertyListener;
import jd.config.Configuration;
import jd.config.Property;
import jd.config.SubConfiguration;
import jd.controlling.DownloadWatchDog;
import jd.gui.swing.jdgui.GUIUtils;
import jd.gui.swing.jdgui.JDGuiConstants;
import jd.nutils.Formatter;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

public class SpeedMeterPanel extends JPanel implements ActionListener, MouseListener {

    private static final long serialVersionUID = 5571694800446993879L;
    private int i;
    private int[] cache;
    private transient Thread th;
    private int window;
    private boolean show;

    private static final int CAPACITY = 40;

    public SpeedMeterPanel() {
        i = 0;
        window = GUIUtils.getConfig().getIntegerProperty(JDGuiConstants.PARAM_SHOW_SPEEDMETER_WINDOWSIZE, 60);
        show = GUIUtils.getConfig().getBooleanProperty(JDGuiConstants.PARAM_SHOW_SPEEDMETER, true);
        cache = new int[CAPACITY];
        for (int x = 0; x < CAPACITY; x++) {
            cache[x] = 0;
        }

        setOpaque(false);
        setBorder(show ? BorderFactory.createEtchedBorder() : null);
        addMouseListener(this);

        JDUtilities.getController().addControlListener(new ConfigPropertyListener(Configuration.PARAM_DOWNLOAD_MAX_SPEED, JDGuiConstants.PARAM_SHOW_SPEEDMETER_WINDOWSIZE, JDGuiConstants.PARAM_SHOW_SPEEDMETER) {

            @Override
            public void onPropertyChanged(Property source, String key) {
                if (key == null) return;
                if (key.equalsIgnoreCase(Configuration.PARAM_DOWNLOAD_MAX_SPEED)) {
                    update();
                } else if (key.equalsIgnoreCase(JDGuiConstants.PARAM_SHOW_SPEEDMETER_WINDOWSIZE)) {
                    window = GUIUtils.getConfig().getIntegerProperty(JDGuiConstants.PARAM_SHOW_SPEEDMETER_WINDOWSIZE, 60);
                } else if (key.equalsIgnoreCase(JDGuiConstants.PARAM_SHOW_SPEEDMETER)) {
                    show = GUIUtils.getConfig().getBooleanProperty(JDGuiConstants.PARAM_SHOW_SPEEDMETER, true);
                    setBorder(show ? BorderFactory.createEtchedBorder() : null);
                }
            }

        });

    }

    public void start() {
        if (th != null) return;
        th = new Thread("Speedmeter updater") {

            @Override
            public void run() {
                long nextCacheEntry = 0;
                while (!this.isInterrupted()) {
                    update();
                    if (nextCacheEntry < System.currentTimeMillis()) {
                        cache[i] = (int) DownloadWatchDog.getInstance().getConnectionManager().getIncommingBandwidthUsage();
                        i++;
                        i = i % cache.length;
                        nextCacheEntry = System.currentTimeMillis() + ((window * 1000) / CAPACITY);
                    }
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        return;
                    }
                }
            }
        };
        th.start();

        if (show) fadeIn();
    }

    public void stop() {
        if (!show) return;
        if (th != null) {
            th.interrupt();
            th = null;
        }

        if (show) fadeOut();
    }

    public synchronized void update() {
        repaint();
    }

    @Override
    public void paintComponent(Graphics g) {
        if (!show) {
            super.paintComponent(g);
            return;
        }
        ((Graphics2D) g).setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity));
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setStroke(new BasicStroke(1));

        Color col1 = new Color(0x7CD622);
        Color col2 = new Color(0x339933);

        int id = i;
        int limit = SubConfiguration.getConfig("DOWNLOAD").getIntegerProperty(Configuration.PARAM_DOWNLOAD_MAX_SPEED, 0) * 1024;
        int max = Math.max(10, limit);
        for (int element : cache) {
            max = Math.max(element, max);
        }
        int width = getWidth();
        int height = getHeight();

        Polygon poly = new Polygon();

        poly.addPoint(0, height);

        for (int x = 0; x < CAPACITY; x++) {
            poly.addPoint((x * width) / (CAPACITY - 1), height - (int) (height * cache[id] * 0.9) / max);
            id++;
            id = id % cache.length;
        }
        poly.addPoint(width, height);

        ((Graphics2D) g).setPaint(new GradientPaint(width / 2, 0, col1, width / 2, height, col2.darker()));

        g2.fill(poly);

        FontUIResource f = (FontUIResource) UIManager.getDefaults().get("Panel.font");

        g2.setFont(f);

        String txt = Formatter.formatReadable(DownloadWatchDog.getInstance().getConnectionManager().getIncommingBandwidthUsage()) + "/s";
        FontMetrics fmetrics = g2.getFontMetrics();

        int len = fmetrics.stringWidth(txt);
        Color fontCol = getForeground();
        if (limit > 0) {
            int limitpx = height - (int) (height * limit * 0.9) / max;
            g2.setColor(Color.RED);
            g2.drawLine(0, limitpx, width, limitpx);
            if (limitpx > height / 2) {
                g2.drawString((DownloadWatchDog.getInstance().isPaused() ? JDL.L("gui.speedmeter.pause", "pause") + " " : "") + Formatter.formatReadable(limit) + "/s", 5, limitpx - 4);
                g2.setColor(fontCol);
                g2.drawString(Formatter.formatReadable(DownloadWatchDog.getInstance().getConnectionManager().getIncommingBandwidthUsage()) + "/s", width - len - 5, limitpx - 4);
            } else {
                g2.drawString((DownloadWatchDog.getInstance().isPaused() ? JDL.L("gui.speedmeter.pause", "pause") + " " : "") + Formatter.formatReadable(limit) + "/s", 5, limitpx + 12);
                g2.setColor(fontCol);
                g2.drawString(Formatter.formatReadable(DownloadWatchDog.getInstance().getConnectionManager().getIncommingBandwidthUsage()) + "/s", width - len - 5, limitpx + 12);
            }
        } else {
            g2.setColor(fontCol);
            g2.drawString(Formatter.formatReadable(DownloadWatchDog.getInstance().getConnectionManager().getIncommingBandwidthUsage()) + "/s", width - len - 5, 12);
        }
    }

    private float opacity = 0f;
    private float fadeSteps = .1f;
    private Timer fadeTimer;

    public void fadeIn() {
        if (fadeTimer != null) {
            fadeTimer.stop();
            fadeTimer = null;
        }
        this.setVisible(true);

        fadeSteps = .1f;
        fadeTimer = new Timer(75, this);
        fadeTimer.setInitialDelay(0);
        fadeTimer.start();

    }

    public void fadeOut() {
        if (fadeTimer != null) {
            fadeTimer.stop();
            fadeTimer = null;
        }
        fadeSteps = -.1f;
        fadeTimer = new Timer(75, this);
        fadeTimer.setInitialDelay(0);
        fadeTimer.start();

    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == fadeTimer) {
            opacity += fadeSteps;
            if (opacity > 1) {
                opacity = 1;
                fadeTimer.stop();
                fadeTimer = null;
            } else if (opacity < 0) {
                opacity = 0;
                fadeTimer.stop();
                fadeTimer = null;
            }

            update();
        } else if (e.getSource() instanceof JMenuItem) {
            GUIUtils.getConfig().setProperty(JDGuiConstants.PARAM_SHOW_SPEEDMETER, !show);
            GUIUtils.getConfig().save();
        }
    }

    public void mouseClicked(MouseEvent e) {
        if (e.isPopupTrigger() || e.getButton() == MouseEvent.BUTTON3) {
            if (DownloadWatchDog.getInstance().getDownloadStatus() != DownloadWatchDog.STATE.RUNNING) return;
            JMenuItem mi = new JMenuItem(show ? JDL.L("gui.speedmeter.hide", "Hide Speedmeter") : JDL.L("gui.speedmeter.show", "Show Speedmeter"));
            mi.addActionListener(this);

            JPopupMenu popup = new JPopupMenu();

            popup.add(mi);
            popup.show(this, e.getPoint().x, e.getPoint().y);
        }
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }

    public void mousePressed(MouseEvent e) {
    }

    public void mouseReleased(MouseEvent e) {
    }

}