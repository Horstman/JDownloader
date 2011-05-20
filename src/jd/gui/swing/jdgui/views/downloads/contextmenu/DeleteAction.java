package jd.gui.swing.jdgui.views.downloads.contextmenu;

import java.awt.event.ActionEvent;
import java.util.ArrayList;

import jd.gui.UserIO;
import jd.gui.swing.jdgui.interfaces.ContextMenuAction;
import jd.plugins.DownloadLink;

import org.jdownloader.gui.translate._GUI;

public class DeleteAction extends ContextMenuAction {

    private static final long             serialVersionUID = -5721724901676405104L;

    private final ArrayList<DownloadLink> links;

    public DeleteAction(ArrayList<DownloadLink> links) {
        this.links = links;

        init();
    }

    @Override
    protected String getIcon() {
        return "delete";
    }

    @Override
    protected String getName() {
        return _GUI._.gui_table_contextmenu_deletelist2() + " (" + links.size() + ")";
    }

    public void actionPerformed(ActionEvent e) {
        if (links.size() > 0 && UserIO.isOK(UserIO.getInstance().requestConfirmDialog(UserIO.DONT_SHOW_AGAIN | UserIO.DONT_SHOW_AGAIN_IGNORES_CANCEL, _GUI._.gui_downloadlist_delete() + " (" + _GUI._.gui_downloadlist_delete_size_packagev2(links.size()) + ")"))) {
            for (DownloadLink link : links) {
                link.setEnabled(false);
                link.deleteFile(true, false);
                link.getFilePackage().remove(link);
            }
        }
    }
}