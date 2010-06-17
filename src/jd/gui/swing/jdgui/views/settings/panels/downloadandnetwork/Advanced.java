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

package jd.gui.swing.jdgui.views.settings.panels.downloadandnetwork;

import javax.swing.SwingUtilities;

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.ConfigGroup;
import jd.config.Configuration;
import jd.config.SubConfiguration;
import jd.controlling.ByteBufferController;
import jd.gui.UserIO;
import jd.gui.swing.jdgui.views.settings.ConfigPanel;
import jd.utils.locale.JDL;

public class Advanced extends ConfigPanel {

    private static final long serialVersionUID = -8421603124342250902L;
    private static final String JDL_PREFIX = "jd.gui.swing.jdgui.settings.panels.downloadandnetwork.Advanced.";

    public static String getTitle() {
        return JDL.L(JDL_PREFIX + "download.advanced.title", "Advanced");
    }

    public static String getIconKey() {
        return "gui.images.preferences";
    }

    private SubConfiguration config;

    public Advanced() {
        super();

        config = SubConfiguration.getConfig("DOWNLOAD");

        init();
    }

    @Override
    protected ConfigContainer setupContainer() {

        ConfigEntry ce, cond;

        ConfigContainer container = new ConfigContainer();

        container.setGroup(new ConfigGroup(JDL.L("gui.config.download.ipcheck", "Reconnection IP-Check"), "gui.images.network"));

        container.addEntry(cond = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, config, Configuration.PARAM_GLOBAL_IP_DISABLE, JDL.L("gui.config.download.ipcheck.disable", "Disable IP-Check")) {
            private static final long serialVersionUID = 1L;
            /**
             * assures that the user sees the warning only once
             */
            private boolean warned = true;

            /**
             * This method gets called when the user clicks the checkbox. It
             * gets also invoked at startup not only on user IO.
             */
            @Override
            public void valueChanged(Object newValue) {
                // get Current Databasevalue
                super.valueChanged(newValue);
                // Only show the warning if the newValue differs from the
                // database stored one
                if (newValue == Boolean.TRUE && !warned) {
                    warned = true;
                    SwingUtilities.invokeLater(new Runnable() {

                        public void run() {
                            UserIO.getInstance().requestMessageDialog(UserIO.ICON_WARNING, JDL.L("jd.gui.swing.jdgui.settings.panels.downloadandnetwork.advanced.ipcheckdisable.warning.title", "IP-Check disabled!"), JDL.L("jd.gui.swing.jdgui.settings.panels.downloadandnetwork.advanced.ipcheckdisable.warning.message", "You disabled the IP-Check. This will increase the reconnection times dramatically!\r\n\r\nSeveral further modules like Reconnect Recorder are disabled."));
                        }

                    });

                } else if (newValue == Boolean.FALSE) {
                    warned = false;
                }

            }
        });
        cond.setDefaultValue(false);

        container.addEntry(cond = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, config, Configuration.PARAM_GLOBAL_IP_BALANCE, JDL.L("gui.config.download.ipcheck.balance", "Use balanced IP-Check")));
        cond.setDefaultValue(true);

        container.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, config, Configuration.PARAM_GLOBAL_IP_CHECK_SITE, JDL.L("gui.config.download.ipcheck.website", "Check IP online")));
        ce.setDefaultValue(JDL.L("gui.config.download.ipcheck.website.default", "Please enter Website for IPCheck here"));
        ce.setEnabledCondidtion(cond, false);

        container.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, config, Configuration.PARAM_GLOBAL_IP_PATTERN, JDL.L("gui.config.download.ipcheck.regex", "IP Filter RegEx")));
        ce.setDefaultValue(JDL.L("gui.config.download.ipcheck.regex.default", "Please enter Regex for IPCheck here"));
        ce.setEnabledCondidtion(cond, false);

        container.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, config, Configuration.PARAM_GLOBAL_IP_MASK, JDL.L("gui.config.download.ipcheck.mask", "Allowed IPs")));
        ce.setDefaultValue("\\b(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?).)" + "{3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\b");
        ce.setEnabledCondidtion(cond, false);

        container.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_SPINNER, config, "EXTERNAL_IP_CHECK_INTERVAL2", JDL.L("gui.config.download.ipcheck.externalinterval2", "External IP Check Interval [min]"), 10, 240, 10));
        ce.setDefaultValue(10);
        ce.setEnabledCondidtion(cond, false);

        container.setGroup(new ConfigGroup(JDL.L("gui.config.download.write", "File writing"), "gui.images.save"));

        container.addEntry(cond = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, config, Configuration.PARAM_DO_CRC, JDL.L("gui.config.download.crc", "SFV/CRC check when possible")));
        cond.setDefaultValue(true);

        // // TODO!
        // container.addEntry(ce = new
        // ConfigEntry(ConfigContainer.TYPE_CHECKBOX, config,
        // Configuration.PARAM_REDOWNLOAD_AFTER_CRC_ERROR, JDL.L(JDL_PREFIX +
        // "redownloadAfterCRCError", "Redownload file when check fails")));
        // ce.setDefaultValue(false);
        // ce.setEnabledCondidtion(cond, true);
        //
        // container.addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));

        container.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_SPINNER, config, ByteBufferController.MAXBUFFERSIZE, JDL.L("gui.config.download.buffersize2", "Max. Buffersize[KB]"), 500, 2000, 100));
        ce.setDefaultValue(500);
        return container;
    }
}
