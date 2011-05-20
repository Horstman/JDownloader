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

package jd.controlling.reconnect.plugins.liveheader.recorder;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import jd.config.Configuration;
import jd.config.SubConfiguration;
import jd.controlling.JDLogger;
import jd.controlling.reconnect.ReconnectPluginController;
import jd.controlling.reconnect.ipcheck.IPController;
import jd.controlling.reconnect.plugins.liveheader.LiveHeaderReconnect;
import jd.controlling.reconnect.plugins.liveheader.translate.T;
import jd.gui.UserIO;
import jd.nutils.JDFlags;
import jd.utils.JDUtilities;
import net.miginfocom.swing.MigLayout;

import org.appwork.utils.Regex;
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.swing.dialog.AbstractDialog;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.jdownloader.images.NewTheme;

public class Gui extends AbstractDialog<Object> {

    public class JDRRInfoPopup extends AbstractDialog<Object> {

        public class RRStatus extends JLabel {

            private static final long serialVersionUID = -3280613281656283625L;

            private final ImageIcon   imageProgress;

            private final ImageIcon   imageBad;

            private final ImageIcon   imageGood;

            private final String      strProgress;

            private final String      strBad;

            private final String      strGood;

            public RRStatus() {
                this.imageProgress = NewTheme.I().getIcon("record", 32);
                this.imageBad = NewTheme.I().getIcon("false", 32);
                this.imageGood = NewTheme.I().getIcon("true", 32);
                this.strProgress = T._.jd_router_reconnectrecorder_Gui_icon_progress();
                this.strBad = T._.jd_router_reconnectrecorder_Gui_icon_bad();
                this.strGood = T._.jd_router_reconnectrecorder_Gui_icon_good();
                this.setStatus(0);
            }

            public void setStatus(final int state) {
                if (state == 0) {
                    this.setIcon(this.imageProgress);
                    this.setText(this.strProgress);
                } else if (state == 1) {
                    this.setIcon(this.imageGood);
                    this.setText(this.strGood);
                } else {
                    this.setIcon(this.imageBad);
                    this.setText(this.strBad);
                }
            }
        }

        private static final long serialVersionUID = 1L;
        private long              reconnect_timer  = 0;

        private RRStatus          statusicon;

        public JDRRInfoPopup() {
            super(UserIO.NO_ICON | UserIO.NO_OK_OPTION, T._.gui_config_jdrr_status_title(), null, null, T._.gui_btn_abort());

        }

        public void closePopup() {

            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    JDRRInfoPopup.this.cancelButton.setEnabled(false);

                    if (IPController.getInstance().validate()) {
                        if (JDRRInfoPopup.this.reconnect_timer == 0) {
                            /*
                             * Reconnect fand innerhalb des Check-Intervalls
                             * statt
                             */
                            Gui.RECONNECT_DURATION = Gui.CHECK_INTERVAL;
                        } else {
                            Gui.RECONNECT_DURATION = System.currentTimeMillis() - JDRRInfoPopup.this.reconnect_timer;
                        }
                        JDLogger.getLogger().info("dauer: " + Gui.RECONNECT_DURATION);
                        JDRRInfoPopup.this.statusicon.setStatus(1);
                    } else {
                        JDRRInfoPopup.this.statusicon.setStatus(-1);
                    }
                    if (IPController.getInstance().validate()) {
                        Gui.this.save();
                    } else {
                        UserIO.getInstance().requestMessageDialog(T._.gui_config_jdrr_reconnectfaild());
                    }

                    JDRRInfoPopup.this.dispose();
                }
            });
        }

        @Override
        protected Object createReturnValue() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public JComponent layoutDialogContent() {
            return this.statusicon = new RRStatus();
        }

        @Override
        public void packed() {
            this.setMinimumSize(null);
            this.setResizable(false);
            this.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

            this.startCheck();
        }

        protected void setReturnmask(final boolean b) {
            ReconnectRecorder.stopServer();
            super.setReturnmask(b);
            if (!b) {
                this.closePopup();
            }
        }

        public void startCheck() {
            new Thread() {
                public void run() {
                    JDRRInfoPopup.this.statusicon.setStatus(0);
                    this.setName(T._.gui_config_jdrr_popup_title());
                    JDRRInfoPopup.this.reconnect_timer = 0;
                    while (ReconnectRecorder.running) {
                        try {
                            Thread.sleep(Gui.CHECK_INTERVAL);
                        } catch (final Exception e) {
                        }

                        if (!IPController.getInstance().validate() && JDRRInfoPopup.this.reconnect_timer == 0) {
                            JDRRInfoPopup.this.reconnect_timer = System.currentTimeMillis();
                        }
                        if (IPController.getInstance().validate()) {
                            JDRRInfoPopup.this.statusicon.setStatus(1);
                            if (ReconnectRecorder.running == true) {
                                ReconnectRecorder.stopServer();
                                JDRRInfoPopup.this.closePopup();
                            }
                            return;
                        }
                    }
                }
            }.start();
        }
    }

    private static final long serialVersionUID   = 1L;
    private JTextField        routerip;
    private JCheckBox         rawmode;
    public boolean            saved              = false;

    public String             ip                 = null;
    public String             methode            = null;
    public String             user               = null;
    public String             pass               = null;

    private static long       CHECK_INTERVAL     = 5000;

    private static long       RECONNECT_DURATION = 0;

    public Gui(final String ip) {
        super(UserIO.NO_COUNTDOWN | UserIO.NO_ICON, T._.gui_config_jdrr_title(), null, T._.gui_btn_start(), T._.gui_btn_cancel());
        this.ip = ip;

    }

    @Override
    protected void addButtons(final JPanel buttonBar) {
        final JButton help = new JButton(T._.gui_btn_help());
        help.addActionListener(new ActionListener() {

            public void actionPerformed(final ActionEvent e) {
                CrossSystem.openURLOrShowMessage("http://jdownloader.org/knowledge/wiki/reconnect/reconnect-recorder");
            }

        });
        buttonBar.add(help, "tag help, sizegroup confirms");
    }

    @Override
    protected Object createReturnValue() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public JComponent layoutDialogContent() {
        this.routerip = new JTextField(this.ip);

        this.rawmode = new JCheckBox(T._.gui_config_jdrr_rawmode());
        this.rawmode.setSelected(false);
        this.rawmode.setHorizontalTextPosition(SwingConstants.LEADING);

        final StringBuilder sb = new StringBuilder();
        sb.append("<html>");
        sb.append(T._.jd_nrouter_recorder_Gui_info1());
        sb.append("<br>");
        sb.append(T._.jd_nrouter_recorder_Gui_info2());
        sb.append("<br>");
        sb.append(T._.jd_nrouter_recorder_Gui_info3());
        sb.append("</html>");

        final JPanel panel = new JPanel(new MigLayout("wrap 3, ins 5", "[][grow]10[]"));
        panel.add(new JLabel(T._.gui_fengshuiconfig_routerip() + ":"));
        panel.add(this.routerip, "growx");
        panel.add(this.rawmode);
        panel.add(new JLabel(sb.toString()), "spanx,growx");
        return panel;
    }

    private void save() {
        final int ret = UserIO.getInstance().requestConfirmDialog(0, T._.gui_config_jdrr_success(), T._.gui_config_jdrr_savereconnect(), UserIO.getInstance().getIcon(UserIO.ICON_QUESTION), T._.gui_btn_yes(), T._.gui_btn_no());
        if (JDFlags.hasSomeFlags(ret, UserIO.RETURN_OK, UserIO.RETURN_COUNTDOWN_TIMEOUT)) {

            final StringBuilder b = new StringBuilder();
            final String br = System.getProperty("line.separator");
            for (final String element : ReconnectRecorder.steps) {
                b.append(element).append(br);
            }
            this.methode = b.toString().trim();

            if (ReconnectRecorder.AUTH != null) {
                this.user = new Regex(ReconnectRecorder.AUTH, "(.+?):").getMatch(0);
                this.pass = new Regex(ReconnectRecorder.AUTH, ".+?:(.+)").getMatch(0);

                ((LiveHeaderReconnect) ReconnectPluginController.getInstance().getPluginByID(LiveHeaderReconnect.ID)).setUser(this.user);
                ((LiveHeaderReconnect) ReconnectPluginController.getInstance().getPluginByID(LiveHeaderReconnect.ID)).setPassword(this.pass);
            }
            // TODO
            // btnCancel.setText(JDL.L("gui.btn_close", "Close"));

            ((LiveHeaderReconnect) ReconnectPluginController.getInstance().getPluginByID(LiveHeaderReconnect.ID)).setRouterIP(this.routerip.getText().trim());
            ((LiveHeaderReconnect) ReconnectPluginController.getInstance().getPluginByID(LiveHeaderReconnect.ID)).setScript(this.methode);
            ((LiveHeaderReconnect) ReconnectPluginController.getInstance().getPluginByID(LiveHeaderReconnect.ID)).setRouterName("Reconnect Recorder Method");

            ReconnectPluginController.getInstance().setActivePlugin(LiveHeaderReconnect.ID);
            if (Gui.RECONNECT_DURATION <= 2000) {
                Gui.RECONNECT_DURATION = 2000;
                /* minimum von 2 seks */
            }
            int aa = (int) (Gui.RECONNECT_DURATION / 1000 * 2);
            if (aa < 30) {
                aa = 30;
            }
            int ab = (int) (Gui.RECONNECT_DURATION / 1000 / 2);
            if (ab < 30) {
                ab = 5;
            }

            JDUtilities.getConfiguration().setProperty(Configuration.PARAM_IPCHECKWAITTIME, aa);
            JDUtilities.getConfiguration().setProperty(Configuration.PARAM_IPCHECKWAITTIME, ab);
            JDUtilities.getConfiguration().save();
            this.saved = true;
            this.dispose();
        }
    }

    protected void setReturnmask(final boolean b) {

        super.setReturnmask(b);
        if (b && !ReconnectRecorder.running) {
            if (this.routerip.getText() != null && !this.routerip.getText().matches("\\s*")) {
                String host = this.routerip.getText().trim();
                boolean startwithhttps = false;
                if (host.contains("https")) {
                    startwithhttps = true;
                }
                host = host.replaceAll("http://", "").replaceAll("https://", "");
                JDUtilities.getConfiguration().setProperty(Configuration.PARAM_HTTPSEND_IP, host);

                IPController.getInstance().invalidate();
                ReconnectRecorder.startServer(host, this.rawmode.isSelected());

                if (startwithhttps) {
                    CrossSystem.openURLOrShowMessage("http://localhost:" + (SubConfiguration.getConfig("ReconnectRecorder").getIntegerProperty(ReconnectRecorder.PROPERTY_PORT, 8972) + 1));
                } else {
                    CrossSystem.openURLOrShowMessage("http://localhost:" + SubConfiguration.getConfig("ReconnectRecorder").getIntegerProperty(ReconnectRecorder.PROPERTY_PORT, 8972));
                }
                try {
                    Dialog.getInstance().showDialog(new JDRRInfoPopup());
                } catch (DialogClosedException e) {
                    e.printStackTrace();
                } catch (DialogCanceledException e) {
                    e.printStackTrace();
                }
                return;
            }
        }

    }
}