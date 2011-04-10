package jd.gui.swing.jdgui.views.downloads.contextmenu;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.HashSet;

import jd.controlling.ClipboardHandler;
import jd.gui.swing.jdgui.interfaces.ContextMenuAction;
import jd.plugins.DownloadLink;

import org.jdownloader.gui.translate.T;

public class CopyPasswordAction extends ContextMenuAction {
    private static final long             serialVersionUID = -6747711277011715259L;

    private final ArrayList<DownloadLink> links;
    private final String                  password;

    public CopyPasswordAction(ArrayList<DownloadLink> links) {
        this.links = links;
        this.password = getPasswordSelectedLinks(links);

        init();
    }

    @Override
    protected String getIcon() {
        return "gui.icons.copy";
    }

    @Override
    protected String getName() {
        return T._.gui_table_contextmenu_copyPassword() + " (" + links.size() + ")";
    }

    @Override
    public boolean isEnabled() {
        return password.length() != 0;
    }

    public void actionPerformed(ActionEvent e) {
        ClipboardHandler.getClipboard().copyTextToClipboard(password);
    }

    public static String getPasswordSelectedLinks(ArrayList<DownloadLink> links) {
        HashSet<String> list = new HashSet<String>();
        StringBuilder sb = new StringBuilder("");
        String pw;
        for (DownloadLink link : links) {
            pw = link.getFilePackage().getPassword();
            if (!list.contains(pw) && pw.length() > 0) {
                if (list.size() > 0) sb.append("\r\n");
                list.add(pw);
                sb.append(pw);
            }

            pw = link.getStringProperty("pass", null);
            if (pw != null) {
                if (!list.contains(pw) && pw.length() > 0) {
                    if (list.size() > 0) sb.append("\r\n");
                    list.add(pw);
                    sb.append(pw);
                }
            }
        }
        return sb.toString();
    }

}