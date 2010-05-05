//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
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

package jd.gui.swing.jdgui.views.settings.panels.reconnect;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.ConfigGroup;
import jd.config.Configuration;
import jd.config.SubConfiguration;
import jd.config.ConfigEntry.PropertyType;
import jd.controlling.JDLogger;
import jd.controlling.ProgressController;
import jd.controlling.reconnect.BatchReconnect;
import jd.controlling.reconnect.ExternReconnect;
import jd.controlling.reconnect.ReconnectMethod;
import jd.controlling.reconnect.Reconnecter;
import jd.gui.swing.Factory;
import jd.gui.swing.GuiRunnable;
import jd.gui.swing.jdgui.interfaces.SwitchPanelEvent;
import jd.gui.swing.jdgui.views.settings.ConfigPanel;
import jd.gui.swing.jdgui.views.settings.GUIConfigEntry;
import jd.nrouter.IPCheck;
import jd.nutils.Formatter;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;
import net.miginfocom.swing.MigLayout;

public class MethodSelection extends ConfigPanel implements ActionListener {

    private static final String JDL_PREFIX = "jd.gui.swing.jdgui.settings.panels.reconnect.MethodSelection.";
    private static final long serialVersionUID = 3383448498625377495L;

    private JButton btn;

    private Configuration configuration;

    private JTabbedPane tabbed;

    private JLabel currentip;

    private JLabel success;

    private JLabel beforeIPLabel;

    private JLabel beforeIP;

    private JLabel message;

    private JLabel timeLabel;

    private JLabel time;

    private JPanel method;

    public static String getTitle() {
        return JDL.L(JDL_PREFIX + "reconnect.title", "Reconnection");
    }

    public MethodSelection() {
        super();
        this.configuration = JDUtilities.getConfiguration();
        initPanel();
        load();
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == btn) {
            save();

            JDLogger.addHeader("Reconnect Testing");

            final ProgressController progress = new ProgressController(JDL.L("gui.warning.reconnect.pleaseWait", "Bitte Warten...Reconnect läuft"), 100, "gui.images.reconnect");

            logger.info("Start Reconnect");
            message.setText(JDL.L("gui.warning.reconnect.running", "running..."));
            message.setEnabled(true);
            beforeIP.setText(currentip.getText());
            beforeIP.setEnabled(true);
            beforeIPLabel.setEnabled(true);
            currentip.setText("?");
            final long timel = System.currentTimeMillis();

            final Thread timer = new Thread() {
                @Override
                public void run() {
                    while (true) {
                        new GuiRunnable<Object>() {

                            @Override
                            public Object runSave() {
                                time.setText(Formatter.formatSeconds((System.currentTimeMillis() - timel) / 1000));
                                time.setEnabled(true);
                                timeLabel.setEnabled(true);
                                return null;
                            }

                        }.start();
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            return;
                        }
                        if (progress.isFinalizing()) break;
                    }
                }
            };
            timer.start();
            final int retries = JDUtilities.getConfiguration().getIntegerProperty(ReconnectMethod.PARAM_RETRIES, 5);
            progress.setStatus(30);
            new Thread() {
                @Override
                public void run() {
                    JDUtilities.getConfiguration().setProperty(ReconnectMethod.PARAM_RETRIES, 0);
                    if (Reconnecter.doManualReconnect()) {
                        if (SubConfiguration.getConfig("DOWNLOAD").getBooleanProperty(Configuration.PARAM_GLOBAL_IP_DISABLE, false)) {
                            progress.setStatusText(JDL.L("gui.warning.reconnectunknown", "Reconnect unknown"));
                        } else {
                            progress.setStatusText(JDL.L("gui.warning.reconnectSuccess", "Reconnect successfull"));
                        }
                        new GuiRunnable<Object>() {

                            @Override
                            public Object runSave() {
                                if (SubConfiguration.getConfig("DOWNLOAD").getBooleanProperty(Configuration.PARAM_GLOBAL_IP_DISABLE, false)) {
                                    message.setText(JDL.L("gui.warning.reconnectunknown", "Reconnect unknown"));
                                } else {
                                    message.setText(JDL.L("gui.warning.reconnectSuccess", "Reconnect successfull"));
                                }
                                success.setIcon(JDTheme.II("gui.images.selected", 32, 32));
                                success.setEnabled(true);
                                if (SubConfiguration.getConfig("DOWNLOAD").getBooleanProperty(Configuration.PARAM_GLOBAL_IP_DISABLE, false)) {
                                    currentip.setText("?");
                                } else {
                                    currentip.setText(IPCheck.getIPAddress());
                                }
                                return null;
                            }
                        }.start();
                    } else {
                        progress.setStatusText(JDL.L("gui.warning.reconnectFailed", "Reconnect failed!"));
                        progress.setColor(Color.RED);
                        new GuiRunnable<Object>() {

                            @Override
                            public Object runSave() {
                                message.setText(JDL.L("gui.warning.reconnectFailed", "Reconnect failed!"));
                                success.setIcon(JDTheme.II("gui.images.unselected", 32, 32));
                                success.setEnabled(true);
                                if (SubConfiguration.getConfig("DOWNLOAD").getBooleanProperty(Configuration.PARAM_GLOBAL_IP_DISABLE, false)) {
                                    currentip.setText("?");
                                } else {
                                    currentip.setText(IPCheck.getIPAddress());
                                }
                                return null;
                            }
                        }.start();
                    }
                    timer.interrupt();
                    progress.setStatus(100);
                    progress.doFinalize(5000);
                    JDUtilities.getConfiguration().setProperty(ReconnectMethod.PARAM_RETRIES, retries);
                }
            }.start();
        }
    }

    @Override
    public void initPanel() {

        /* 0=LiveHeader, 1=Extern, 2=Batch,3=CLR */

        method = new JPanel(new MigLayout("ins 0 0 0 0,wrap 2", "[fill,grow 10]10[fill,grow]"));

        tabbed = new JTabbedPane();
        method.add(tabbed, "spanx,pushy,growy");
        tabbed.setTabPlacement(SwingConstants.TOP);

        tabbed.addTab(JDL.L("modules.reconnect.types.liveheader", "LiveHeader/Curl"), new SubPanelLiveHeaderReconnect(configuration));
        tabbed.addTab(JDL.L("modules.reconnect.types.extern", "Extern"), getPanelFor(new ExternReconnect()));
        tabbed.addTab(JDL.L("modules.reconnect.types.batch", "Batch"), getPanelFor(new BatchReconnect()));
        tabbed.addTab(JDL.L("modules.reconnect.types.clr", "CLR Script"), new SubPanelCLRReconnect(configuration));

        method.add(Factory.createHeader(new ConfigGroup(JDL.L("gui.config.reconnect.test", "Showcase"), "gui.images.config.network_local")), "spanx,gaptop 15,gapleft 20,gapright 15");
        JPanel p = new JPanel(new MigLayout(" ins 0,wrap 7", "[]5[fill]5[align right]20[align right]20[align right]20[align right]20[align right]", "[][]"));
        method.add(p, "spanx,gapright 20,gapleft 54");
        btn = new JButton(JDL.L("gui.config.reconnect.showcase.reconnect", "Change IP"));
        btn.addActionListener(this);
        p.add(btn, "spany, aligny top");
        p.add(new JPanel(), "height 32!,spany,alignx left,pushx");
        p.add(timeLabel = new JLabel(JDL.L("gui.config.reconnect.showcase.time", "Reconnect duration")));
        p.add(time = new JLabel("---"));

        timeLabel.setEnabled(false);
        time.setEnabled(false);
        p.add(new JLabel(JDL.L("gui.config.reconnect.showcase.currentip", "Your current IP")));
        p.add(currentip = new JLabel("---"));

        success = new JLabel(JDTheme.II("gui.images.selected", 32, 32));
        success.setEnabled(false);
        p.add(success, "spany,alignx right");

        p.add(message = new JLabel(JDL.L("gui.config.reconnect.showcase.message.none", "Not tested yet")), "spanx 2");
        message.setEnabled(false);

        p.add(beforeIPLabel = new JLabel(JDL.L("gui.config.reconnect.showcase.lastip", "Ip before reconnect")));
        p.add(beforeIP = new JLabel("---"));
        beforeIPLabel.setEnabled(false);
        beforeIP.setEnabled(false);

        if (SubConfiguration.getConfig("DOWNLOAD").getBooleanProperty(Configuration.PARAM_GLOBAL_IP_DISABLE, false) == false) {
            new Thread() {
                @Override
                public void run() {
                    final String ip = IPCheck.getIPAddress();
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            currentip.setText(ip);
                        }
                    });
                }
            }.start();
        }

        setLayout(new MigLayout("ins 0,wrap 1", "[fill,grow 10]", "[fill,grow]"));

        this.add(method);
    }

    @Override
    public void loadSpecial() {
        tabbed.setSelectedIndex(configuration.getIntegerProperty(ReconnectMethod.PARAM_RECONNECT_TYPE, 0));
    }

    @Override
    public PropertyType hasChanges() {
        PropertyType ret = tabbed.getSelectedIndex() != configuration.getIntegerProperty(ReconnectMethod.PARAM_RECONNECT_TYPE, ReconnectMethod.LIVEHEADER) ? PropertyType.NORMAL : PropertyType.NONE;
        return PropertyType.getMax(ret, super.hasChanges(), ((ConfigPanel) tabbed.getSelectedComponent()).hasChanges());
    }

    @Override
    public void setHidden() {
        save();
        getBroadcaster().fireEvent(new SwitchPanelEvent(this, SwitchPanelEvent.ON_HIDE));
    }

    @Override
    public void saveSpecial() {
        configuration.setProperty(ReconnectMethod.PARAM_RECONNECT_TYPE, tabbed.getSelectedIndex());
        ((ConfigPanel) tabbed.getSelectedComponent()).save();
    }

    private static final ConfigPanel getPanelFor(final ReconnectMethod method) {
        ConfigPanel cp = new ConfigPanel() {

            private static final long serialVersionUID = 1086423194283483561L;

            @Override
            public void initPanel() {
                ConfigContainer container = method.getConfig();

                for (ConfigEntry cfgEntry : container.getEntries()) {
                    GUIConfigEntry ce = new GUIConfigEntry(cfgEntry);
                    if (ce != null) addGUIConfigEntry(ce);
                }

                add(panel);
            }

        };
        cp.initPanel();
        cp.setBorder(BorderFactory.createEmptyBorder(3, 0, 0, 0));
        return cp;
    }
}
