package jd.gui.swing.jdgui.views.downloads.contextmenu;

import java.awt.event.ActionEvent;
import java.io.File;

import jd.gui.swing.jdgui.interfaces.ContextMenuAction;
import jd.utils.locale.JDL;

import org.appwork.utils.os.CrossSystem;

public class OpenFileAction extends ContextMenuAction {

    private static final long serialVersionUID = 1901008532686173167L;

    private final File file;

    public OpenFileAction(File file) {
        this.file = file;

        init();
    }

    @Override
    protected String getIcon() {
        return "gui.images.load";
    }

    @Override
    protected String getName() {
        return JDL.L("gui.table.contextmenu.openfile", "Open File");
    }

    public void actionPerformed(ActionEvent e) {
        CrossSystem.openFile(file);
    }

}
