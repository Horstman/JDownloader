package jd.gui.swing.jdgui.views.downloads.contextmenu;

import java.awt.event.ActionEvent;
import java.util.ArrayList;

import jd.controlling.JDLogger;
import jd.gui.swing.components.linkbutton.JLink;
import jd.gui.swing.jdgui.interfaces.ContextMenuAction;
import jd.plugins.DownloadLink;
import jd.utils.locale.JDL;

public class OpenInBrowserAction extends ContextMenuAction {

    private static final long serialVersionUID = 7911375550836173693L;

    private final ArrayList<DownloadLink> links;

    public OpenInBrowserAction(ArrayList<DownloadLink> links) {
        this.links = links;

        init();
    }

    @Override
    protected String getIcon() {
        return "gui.images.browse";
    }

    @Override
    protected String getName() {
        return JDL.L("gui.table.contextmenu.browselink", "Open in browser");
    }

    @Override
    public boolean isEnabled() {
        return links.size() == 1 && links.get(0).getLinkType() == DownloadLink.LINKTYPE_NORMAL;
    }

    public void actionPerformed(ActionEvent e) {
        try {
            JLink.openURL(links.get(0).getBrowserUrl());
        } catch (Exception e1) {
            JDLogger.exception(e1);
        }
    }

}
