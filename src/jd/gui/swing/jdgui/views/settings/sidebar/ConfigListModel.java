package jd.gui.swing.jdgui.views.settings.sidebar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import javax.swing.DefaultListModel;

import jd.gui.swing.jdgui.views.settings.panels.AccountManagerSettings;
import jd.gui.swing.jdgui.views.settings.panels.BarrierFree;
import jd.gui.swing.jdgui.views.settings.panels.BasicAuthentication;
import jd.gui.swing.jdgui.views.settings.panels.ConfigPanelGeneral;
import jd.gui.swing.jdgui.views.settings.panels.DownloadControll;
import jd.gui.swing.jdgui.views.settings.panels.ReconnectSettings;
import jd.gui.swing.jdgui.views.settings.panels.downloadandnetwork.ProxyConfig;
import jd.gui.swing.jdgui.views.settings.panels.hoster.ConfigPanelPlugin;

import org.appwork.storage.config.ConfigEventListener;
import org.appwork.storage.config.ConfigInterface;
import org.appwork.utils.os.CrossSystem;
import org.jdownloader.extensions.AbstractExtensionWrapper;
import org.jdownloader.extensions.ExtensionController;

public class ConfigListModel extends DefaultListModel implements ConfigEventListener {

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
        // addElement(new ExtensionManager());

        addElement(new BarrierFree());
        boolean first = true;
        ArrayList<AbstractExtensionWrapper> pluginsOptional = ExtensionController.getInstance().getExtensions();
        Collections.sort(pluginsOptional, new Comparator<AbstractExtensionWrapper>() {

            public int compare(AbstractExtensionWrapper o1, AbstractExtensionWrapper o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });
        for (final AbstractExtensionWrapper plg : pluginsOptional) {
            if (CrossSystem.isWindows() && !plg.isWindowsRunnable()) continue;
            if (CrossSystem.isLinux() && !plg.isLinuxRunnable()) continue;
            if (CrossSystem.isMac() && !plg.isMacRunnable()) continue;

            if (first) {
                addElement(new ExtensionHeader());
            }
            first = false;
            addElement(plg);
            plg.getStore().addListener(this);

        }
    }

    public void onConfigValueModified(ConfigInterface config, String key, Object newValue) {
        this.fireContentsChanged(this, 0, this.getSize() - 1);
    }
}
