//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
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

package jd.gui.swing.jdgui.menu.actions;


 import org.jdownloader.gui.translate.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;

import jd.controlling.CNL2;
import jd.controlling.ClipboardHandler;
import jd.controlling.DistributeData;
import jd.gui.UserIO;
import jd.gui.swing.jdgui.actions.ToolBarAction;
import jd.parser.html.HTMLParser;
import jd.utils.JDTheme;
import jd.utils.locale.JDL;

public class AddUrlAction extends ToolBarAction {

    private static final long serialVersionUID = -7185006215784212976L;

    public AddUrlAction() {
        super("action.addurl", "gui.images.url");
    }

    @Override
    public void onAction(ActionEvent e) {
        StringBuilder def = new StringBuilder();
        try {
            String newText = ClipboardHandler.getClipboard().getCurrentClipboardLinks();
            String[] links = HTMLParser.getHttpLinks(newText, null);
            ArrayList<String> pws = HTMLParser.findPasswords(newText);
            for (String l : links)
                def.append(l).append("\r\n");
            for (String pw : pws) {
                def.append("password: ").append(pw).append("\r\n");
            }
        } catch (Exception e2) {
        }
        String link = UserIO.getInstance().requestInputDialog(UserIO.NO_COUNTDOWN | UserIO.STYLE_LARGE, T._.gui_dialog_addurl_title(), T._.gui_dialog_addurl_message(), def.toString(), JDTheme.II("gui.images.taskpanes.linkgrabber", 32, 32), T._.gui_dialog_addurl_okoption_parse(), null);
        if (link == null || link.length() == 0) return;
        if (CNL2.checkText(link)) return;
        DistributeData tmp = new DistributeData(link, false);
        tmp.setDisableDeepEmergencyScan(false);
        tmp.start();
    }

    @Override
    public void initDefaults() {
    }

}