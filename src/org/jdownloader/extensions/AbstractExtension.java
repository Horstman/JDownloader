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

package org.jdownloader.extensions;


 import org.jdownloader.translate.*;
import java.util.ArrayList;
import java.util.logging.Logger;

import javax.swing.ImageIcon;

import jd.config.ConfigContainer;
import jd.config.Property;
import jd.config.SubConfiguration;
import jd.controlling.JSonWrapper;
import jd.gui.swing.jdgui.menu.MenuAction;
import jd.plugins.AddonPanel;
import jd.plugins.PlugionOptionalConfig;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.Application;
import org.appwork.utils.IO;
import org.jdownloader.logging.LogController;

/**
 * Superclass for all extensions
 * 
 * @author thomas
 * 
 */
public abstract class AbstractExtension {

    public static final int ADDON_INTERFACE_VERSION = 8;

    private boolean         enabled                 = false;

    /**
     * true if the extension is currently running.
     * 
     * @return
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * start/stops the extension.
     * 
     * @param enabled
     * @throws StartException
     * @throws StopException
     */
    public void setEnabled(boolean enabled) throws StartException, StopException {
        if (enabled == this.enabled) return;
        if (enabled) {
            start();
            store.setEnabled(true);

        } else {
            store.setEnabled(false);
            if (getGUI() != null) {
                getGUI().setActive(false);
            }
            stop();
        }

        this.enabled = enabled;
    }

    /**
     * Returns the internal storage. Most of the configvalues are for internal
     * use only. This config only contains values which are valid for all
     * extensions
     * 
     * @return
     */
    public PlugionOptionalConfig getStore() {
        return store;
    }

    /**
     * use {@link #setEnabled(false)} to stop the extension.
     * 
     * @throws StopException
     */
    protected abstract void stop() throws StopException;

    protected abstract void start() throws StartException;

    protected Logger              logger;

    private String                name;

    private ConfigContainer       settings;

    private int                   version = -1;
    @Deprecated
    private JSonWrapper           classicConfig;

    private PlugionOptionalConfig store;

    public ConfigContainer getSettings() {
        return settings;
    }

    public String getName() {
        return name;
    }

    public Logger getLogger() {
        return logger;
    }

    /*
     * converts old dynamic getConfigName entries to static getID entries, WE
     * MUST USE STATIC getID to access db
     */
    @Deprecated
    public synchronized JSonWrapper getPluginConfig() {
        if (classicConfig != null) return classicConfig;
        classicConfig = JSonWrapper.get(this.getConfigID());
        if (SubConfiguration.hasConfig(getName())) {
            /* convert old to new */
            SubConfiguration oldConfig = SubConfiguration.getConfig(getName());
            if (oldConfig != null) {
                /* put old values into new db and delete old one then */
                oldConfig.copyTo(classicConfig);
                SubConfiguration.removeConfig(getName());
                classicConfig.save();
            }
        }
        return classicConfig;
    }

    /**
     * 
     * @param name
     *            name of this plugin. Until JD 2.* we should use null here to
     *            use the old defaultname. we used to sue this localized name as
     *            config key.
     * @throws
     * @throws StartException
     */
    public AbstractExtension(String name) {
        this.name = name == null ? JDL.L(getClass().getName(), getClass().getSimpleName()) : name;
        logger = createLogger(getClass());
        version = readVersion(getClass());
        store = createStore(getClass());
        logger.info("Loaded");

        if (JDUtilities.getConfiguration().hasProperty("OPTIONAL_PLUGIN2_" + getConfigID())) {
            Boolean ret = JDUtilities.getConfiguration().getBooleanProperty("OPTIONAL_PLUGIN2_" + getConfigID(), isDefaultEnabled());
            store.setEnabled(ret);
            JDUtilities.getConfiguration().setProperty("OPTIONAL_PLUGIN2_" + getConfigID(), Property.NULL);
            JDUtilities.getConfiguration().save();
            logger.finer("Convert old storage sys to new one");
        }

    }

    public static PlugionOptionalConfig createStore(Class<? extends AbstractExtension> class1) {
        return JsonConfig.create(Application.getResource("cfg/" + class1.getName()), PlugionOptionalConfig.class);
    }

    protected abstract void initExtension() throws StartException;

    /**
     * Reads the version.dat in the same directory as class1
     * 
     * @param class1
     * @return
     */
    public static int readVersion(Class<? extends AbstractExtension> class1) {

        try {
            return Integer.parseInt(IO.readURLToString(class1.getResource("version.dat")).trim());
        } catch (Throwable e) {
            return -1;
        }

    }

    @Deprecated
    public boolean hasSettings() {
        return settings.getEntries().size() > 0;
    }

    @Deprecated
    protected void initSettings(ConfigContainer config) {

    }

    public abstract AbstractConfigPanel getConfigPanel();

    public abstract boolean hasConfigPanel();

    private Logger createLogger(Class<? extends AbstractExtension> class1) {
        return LogController.getInstance().createLogger(class1);
    }

    public abstract String getConfigID();

    public abstract String getAuthor();

    public abstract String getDescription();

    public boolean isLinuxRunnable() {
        return true;
    }

    @Deprecated
    public String getIconKey() {
        return "gui.images.preferences";
    }

    public ImageIcon getIcon(int size) {
        return JDTheme.II(getIconKey(), size, size);
    }

    public boolean isWindowsRunnable() {
        return true;
    }

    public boolean isMacRunnable() {
        return true;
    }

    public abstract AddonPanel getGUI();

    public boolean isDefaultEnabled() {
        return false;
    }

    public int getVersion() {
        return version;
    }

    abstract public ArrayList<MenuAction> getMenuAction();

    public ExtensionGuiEnableAction getShowGuiAction() {
        return getGUI() != null ? getGUI().getEnabledAction() : null;

    }

    public void init() throws StartException {
        this.settings = new ConfigContainer(name);
        initExtension();
        initSettings(settings);

        if (store.isFreshInstall()) {
            store.setEnabled(this.isDefaultEnabled());
            store.setFreshInstall(false);
        }
        if (store.isEnabled()) {
            try {
                setEnabled(true);
            } catch (StopException e) {
                // cannot happen
            }
        }
    }

}