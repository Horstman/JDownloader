package jd.gui.swing.jdgui.views.settings.panels.pluginsettings;

import javax.swing.ImageIcon;

import jd.DecryptPluginWrapper;
import jd.HostPluginWrapper;

import org.jdownloader.gui.settings.AbstractConfigPanel;
import org.jdownloader.images.Theme;
import org.jdownloader.translate._JDT;

public class PluginSettings extends AbstractConfigPanel {

    private PluginSettingsPanel psp;

    public String getTitle() {
        return _JDT._.gui_settings_plugins_title();
    }

    public PluginSettings() {
        super();
        this.addHeader(getTitle(), Theme.getIcon("plugin", 32));
        this.addDescription(_JDT._.gui_settings_plugins_description(HostPluginWrapper.getHostWrapper().size() + DecryptPluginWrapper.getDecryptWrapper().size()));

        add(psp = new PluginSettingsPanel());

    }

    @Override
    public ImageIcon getIcon() {
        return Theme.getIcon("plugin", 32);
    }

    @Override
    public void save() {
        psp.setHidden();
    }

    @Override
    public void updateContents() {
        psp.setShown();
    }
}