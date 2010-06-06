package jd.gui.swing.jdgui.views.downloads.contextmenu;

import java.awt.event.ActionEvent;
import java.io.File;
import java.util.ArrayList;

import jd.gui.UserIO;
import jd.gui.swing.jdgui.interfaces.ContextMenuAction;
import jd.nutils.io.JDFileFilter;
import jd.plugins.DownloadLink;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

public class CreateDLCAction extends ContextMenuAction {

    private static final long serialVersionUID = 7244681674979415222L;

    private final ArrayList<DownloadLink> links;

    public CreateDLCAction(ArrayList<DownloadLink> links) {
        this.links = links;

        init();
    }

    @Override
    protected String getIcon() {
        return "gui.images.dlc";
    }

    @Override
    protected String getName() {
        return JDL.L("gui.table.contextmenu.dlc", "Create DLC") + " (" + links.size() + ")";
    }

    public void actionPerformed(ActionEvent e) {
        File[] files = UserIO.getInstance().requestFileChooser("_LOADSAVEDLC", null, null, new JDFileFilter(null, ".dlc", true), null, null, UserIO.SAVE_DIALOG);
        if (files == null) return;

        JDUtilities.getController().saveDLC(files[0], links);
    }

}
