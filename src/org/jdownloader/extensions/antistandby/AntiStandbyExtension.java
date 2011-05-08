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

package org.jdownloader.extensions.antistandby;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.ConfigGroup;
import jd.gui.swing.jdgui.actions.ToolBarAction;
import jd.gui.swing.jdgui.menu.MenuAction;
import jd.nutils.OSDetector;
import jd.plugins.AddonPanel;

import org.jdownloader.extensions.AbstractExtension;
import org.jdownloader.extensions.ExtensionConfigPanel;
import org.jdownloader.extensions.StartException;
import org.jdownloader.extensions.StopException;
import org.jdownloader.extensions.antistandby.translate.T;

public class AntiStandbyExtension extends AbstractExtension {

    private static final String CONFIG_MODE = "CONFIG_MODE2";
    private String[]            modes;
    private MenuAction          menuAction;
    private boolean             status;
    private JDAntiStandbyThread asthread    = null;

    public boolean isStatus() {
        return status;
    }

    public int getMode() {
        return getPluginConfig().getIntegerProperty(CONFIG_MODE, 0);
    }

    public ExtensionConfigPanel getConfigPanel() {
        return null;
    }

    public boolean hasConfigPanel() {
        return false;
    }

    public AntiStandbyExtension() throws StartException {
        super(T._.jd_plugins_optional_antistandby_jdantistandby());
        modes = new String[] { T._.gui_config_antistandby_whiledl(), T._.gui_config_antistandby_whilejd() };

    }

    @Override
    public ArrayList<MenuAction> getMenuAction() {
        ArrayList<MenuAction> menu = new ArrayList<MenuAction>();
        menu.add(menuAction);
        return menu;
    }

    @Override
    protected void stop() throws StopException {
        if (asthread != null) {
            asthread.setRunning(false);
            asthread = null;
        }
    }

    @Override
    protected void start() throws StartException {
        if (menuAction == null) menuAction = new MenuAction("jdantistandby", "gui.images.config.eventmanager") {

            private static final long serialVersionUID = -5269457972563036769L;

            @Override
            public void initDefaults() {
                this.setEnabled(true);
                this.setType(ToolBarAction.Types.TOGGLE);
                this.addPropertyChangeListener(new PropertyChangeListener() {
                    public void propertyChange(PropertyChangeEvent evt) {
                        if (evt.getPropertyName() == SELECTED_KEY) {
                            status = isSelected();
                        }
                    }
                });
            }

        };

        switch (OSDetector.getID()) {
        case OSDetector.OS_WINDOWS_2003:
        case OSDetector.OS_WINDOWS_VISTA:
        case OSDetector.OS_WINDOWS_XP:
        case OSDetector.OS_WINDOWS_7:
        case OSDetector.OS_WINDOWS_2000:
        case OSDetector.OS_WINDOWS_NT:
            asthread = new JDAntiStandbyThread(this);
            asthread.start();

        default:
            logger.fine("JDAntiStandby: System is not supported (" + OSDetector.getOSString() + ")");
        }

    }

    @Override
    protected void initSettings(ConfigContainer config) {
        config.setGroup(new ConfigGroup(getName(), "gui.images.preferences"));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_COMBOBOX_INDEX, getPluginConfig(), CONFIG_MODE, modes, T._.gui_config_antistandby_mode()).setDefaultValue(0));

    }

    @Override
    public String getConfigID() {
        return "jdantistandby";
    }

    @Override
    public String getAuthor() {
        return null;
    }

    @Override
    public String getDescription() {
        return T._.jd_plugins_optional_antistandby_jdantistandby_description();
    }

    @Override
    public AddonPanel getGUI() {
        return null;
    }

    @Override
    protected void initExtension() throws StartException {
    }

}