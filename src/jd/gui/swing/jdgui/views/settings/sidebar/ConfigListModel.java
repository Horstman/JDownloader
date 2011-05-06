package jd.gui.swing.jdgui.views.settings.sidebar;

import javax.swing.DefaultListModel;

import jd.gui.swing.jdgui.views.settings.panels.AccountManagerSettings;
import jd.gui.swing.jdgui.views.settings.panels.BarrierFree;
import jd.gui.swing.jdgui.views.settings.panels.BasicAuthentication;
import jd.gui.swing.jdgui.views.settings.panels.ConfigPanelGeneral;
import jd.gui.swing.jdgui.views.settings.panels.DownloadControll;
import jd.gui.swing.jdgui.views.settings.panels.ReconnectSettings;
import jd.gui.swing.jdgui.views.settings.panels.addons.ExtensionManager;
import jd.gui.swing.jdgui.views.settings.panels.downloadandnetwork.ProxyConfig;
import jd.gui.swing.jdgui.views.settings.panels.hoster.ConfigPanelPlugin;

import org.jdownloader.extensions.AbstractExtension;
import org.jdownloader.extensions.ExtensionController;

public class ConfigListModel extends DefaultListModel {

    private static final long serialVersionUID = -204494527404304349L;

    public ConfigListModel() {
        super();
        fill();
    }

    private void fill() {
        removeAllElements();

        addElement(new ConfigPanelGeneral());

        addElement(new DownloadControll());

        // addElement(new ToolbarController());
        addElement(new jd.gui.swing.jdgui.views.settings.panels.Linkgrabber());
        addElement(new ReconnectSettings());
        addElement(new ProxyConfig());
        addElement(new AccountManagerSettings());
        addElement(new BasicAuthentication());

        // addElement(new Premium());
        addElement(new ConfigPanelPlugin());
        addElement(new ExtensionManager());

        addElement(new BarrierFree());
        for (final AbstractExtension plg : ExtensionController.getInstance().getEnabledExtensions()) {
            if ((!plg.hasSettings() && !plg.hasConfigPanel())) {
                continue;
            }
            if (plg.hasConfigPanel()) {
                addElement(plg.getConfigPanel());
            } else {
                addElement(AddonConfig.getInstance(plg.getSettings(), "", true));
            }
        }
    }

}
