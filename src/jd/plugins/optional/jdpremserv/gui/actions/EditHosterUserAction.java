package jd.plugins.optional.jdpremserv.gui.actions;

import java.awt.event.ActionEvent;
import java.util.HashMap;

import javax.swing.AbstractAction;

import jd.parser.Regex;
import jd.plugins.optional.jdpremserv.model.PremServHoster;
import jd.plugins.optional.jdpremserv.model.PremServUser;

import org.appwork.utils.formatter.SizeFormater;
import org.appwork.utils.swing.dialog.Dialog;

public class EditHosterUserAction extends AbstractAction {

    private static final long serialVersionUID = -9025882574366289273L;
    private PremServUser obj;

    public EditHosterUserAction(PremServUser obj) {
        super("Edit allowed Hosters");

        this.obj = obj;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    public void actionPerformed(ActionEvent arg0) {

        HashMap<String, PremServHoster> hosters = obj.getHosters();
        StringBuilder sb = new StringBuilder();
        for (PremServHoster hoster : hosters.values()) {
            sb.append(hoster.getDomain());
            sb.append(",");
            sb.append(SizeFormater.formatBytes(hoster.getTraffic()));
            sb.append("\r\n");
        }
        String ret = Dialog.getInstance().showInputDialog(Dialog.STYLE_LARGE | Dialog.STYLE_HIDE_ICON, "SetHoster for " + obj.getUsername(), "Format: domain.com,trafficpermonth\r\n", sb.toString(), null, null, null);
        if (ret == null) return;
        hosters = new HashMap<String, PremServHoster>();

        for (String s : Regex.getLines(ret)) {
            String[] p = s.split("\\,");

            if (p.length == 2) {
                PremServHoster h = new PremServHoster(p[0], SizeFormater.getSize(p[1].trim()));
                hosters.put(h.getDomain(), h);
            }
        }
        obj.setHosters(hosters);

    }
}
