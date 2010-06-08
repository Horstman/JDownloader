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

package jd.gui.swing.jdgui.views.settings;

import java.util.ArrayList;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.ConfigGroup;
import jd.config.SubConfiguration;
import jd.config.ConfigEntry.PropertyType;
import jd.controlling.JDLogger;
import jd.gui.UserIO;
import jd.gui.swing.Factory;
import jd.gui.swing.jdgui.JDGui;
import jd.gui.swing.jdgui.interfaces.SwitchPanel;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;
import net.miginfocom.swing.MigLayout;

public abstract class ConfigPanel extends SwitchPanel {

    private static final long serialVersionUID = 3383448498625377495L;

    private ArrayList<GUIConfigEntry> entries = new ArrayList<GUIConfigEntry>();

    protected Logger logger = JDLogger.getLogger();

    protected JPanel panel;

    private ConfigGroup currentGroup;

    public ConfigPanel() {
        this.setLayout(new MigLayout("ins 0", "[fill,grow]", "[fill,grow]"));
        this.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        panel = new JPanel(new MigLayout("ins 5, wrap 2", "[fill,grow 10]10[fill,grow]"));
    }

    public void init() {
        for (ConfigEntry cfgEntry : setupContainer().getEntries()) {
            addConfigEntry(cfgEntry);
        }

        this.add(panel);
        this.load();
    }

    protected abstract ConfigContainer setupContainer();

    /**
     * Overwrite this, when no ConfigGroups Headers should be displayed
     */
    protected boolean showGroups() {
        return true;
    }

    private void addConfigEntry(ConfigEntry entry) {
        GUIConfigEntry guiEntry = new GUIConfigEntry(entry);

        ConfigGroup group = showGroups() ? entry.getGroup() : null;
        if (currentGroup != group) {
            if (group != null) {
                panel.add(Factory.createHeader(group), "spanx");
            } else {
                panel.add(new JSeparator(), "spanx, gapbottom 15, gaptop 15");
            }
            currentGroup = group;
        }

        String gapLeft = (group == null) ? "" : "gapleft 37,";
        if (guiEntry.getDecoration() != null) {
            switch (entry.getType()) {
            case ConfigContainer.TYPE_TEXTAREA:
            case ConfigContainer.TYPE_LISTCONTROLLED:
                panel.add(guiEntry.getDecoration(), gapLeft + "spanx");
                break;
            case ConfigContainer.TYPE_COMPONENT:
                panel.add(guiEntry.getDecoration(), gapLeft + "spanx," + guiEntry.getConfigEntry().getConstraints());
                break;
            default:
                panel.add(guiEntry.getDecoration(), gapLeft + (guiEntry.getInput() == null ? "spanx" : ""));
                break;
            }
        }

        if (guiEntry.getInput() != null) {
            switch (entry.getType()) {
            case ConfigContainer.TYPE_BUTTON:
                panel.add(guiEntry.getInput(), (guiEntry.getDecoration() == null ? gapLeft + "spanx," : "") + "wmax 250");
                break;
            case ConfigContainer.TYPE_TEXTAREA:
            case ConfigContainer.TYPE_LISTCONTROLLED:
                panel.add(new JScrollPane(guiEntry.getInput()), gapLeft + "spanx, growy, pushy");
                break;
            default:
                panel.add(guiEntry.getInput(), guiEntry.getDecoration() == null ? gapLeft + "spanx" : "");
                break;
            }
        }

        entries.add(guiEntry);
    }

    public final void load() {
        this.loadConfigEntries();
        this.loadSpecial();
    }

    private final void loadConfigEntries() {
        for (GUIConfigEntry akt : entries) {
            akt.load();
        }
    }

    public final void save() {
        this.saveConfigEntries();
        this.saveSpecial();
    }

    /**
     * Should be overwritten to do special loading.
     */
    protected void loadSpecial() {
    }

    /**
     * Should be overwritten to do special saving.
     */
    protected void saveSpecial() {
    }

    @Override
    public void onShow() {
        load();
    }

    @Override
    public void onHide() {
        PropertyType changes = hasChanges();
        this.save();
        if (changes == PropertyType.NEEDS_RESTART) {
            if (!JDGui.getInstance().isExitRequested()) {
                int answer = UserIO.getInstance().requestConfirmDialog(0, JDL.L("jd.gui.swing.jdgui.settings.ConfigPanel.restartquestion.title", "Restart required!"), JDL.L("jd.gui.swing.jdgui.settings.ConfigPanel.restartquestion", "This option needs a JDownloader restart."), null, JDL.L("jd.gui.swing.jdgui.settings.ConfigPanel.restartquestion.ok", "Restart NOW!"), null);

                if (UserIO.isOK(answer)) JDUtilities.restartJD(false);
            }
        }
    }

    public PropertyType hasChanges() {
        PropertyType ret = PropertyType.NONE;
        Object old;
        synchronized (entries) {
            for (GUIConfigEntry akt : entries) {
                if (akt.getConfigEntry().getPropertyInstance() != null && akt.getConfigEntry().getPropertyName() != null) {
                    old = akt.getConfigEntry().getPropertyInstance().getProperty(akt.getConfigEntry().getPropertyName());
                    if (old == null && akt.getText() != null) {
                        ret = ret.getMax(akt.getConfigEntry().getPropertyType());
                    } else if (old != null && !old.equals(akt.getText())) {
                        ret = ret.getMax(akt.getConfigEntry().getPropertyType());
                    }
                }
            }
        }
        return ret;
    }

    /**
     * Saves the ConfigEntries in THIS panel.
     */
    private final void saveConfigEntries() {
        ArrayList<SubConfiguration> subs = new ArrayList<SubConfiguration>();
        for (GUIConfigEntry akt : entries) {
            if (akt.getConfigEntry().getPropertyInstance() instanceof SubConfiguration && !subs.contains(akt.getConfigEntry().getPropertyInstance())) {
                subs.add((SubConfiguration) akt.getConfigEntry().getPropertyInstance());
            }
            akt.save();
        }

        for (SubConfiguration subConfiguration : subs) {
            subConfiguration.save();
        }
    }

    public static String getTitle() {
        return "No Title set!";
    }

    public static String getIconKey() {
        return "gui.images.taskpanes.configuration";
    }

}
