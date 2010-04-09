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

package jd.gui.swing.jdgui.views.log;

import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Icon;
import javax.swing.JButton;

import jd.gui.swing.jdgui.views.InfoPanel;
import jd.utils.JDTheme;
import jd.utils.locale.JDL;

public class LogInfoPanel extends InfoPanel implements ActionListener {

    private static final long serialVersionUID = -1910950245889164423L;
    private static final String JDL_PREFIX = "jd.gui.swing.jdgui.views.info.LogInfoPanel.";

    public static final int ACTION_SAVE = 1;
    public static final int ACTION_UPLOAD = 2;

    private JButton btnSave;
    private JButton btnUpload;

    public LogInfoPanel() {
        super("gui.images.taskpanes.log");

        btnSave = createButton(JDL.L(JDL_PREFIX + "save", "Save Log As"), JDTheme.II("gui.images.save", 16, 16));
        btnUpload = createButton(JDL.L(JDL_PREFIX + "upload", "Upload Log"), JDTheme.II("gui.images.upload", 16, 16));

        addComponent(btnSave, 1, 0);
        addComponent(btnUpload, 1, 1);
    }

    public JButton createButton(String string, Icon i) {
        final JButton bt = new JButton(string, i);
        bt.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        bt.setFocusPainted(false);
        bt.setHorizontalAlignment(JButton.LEFT);
        bt.setIconTextGap(5);
        bt.addActionListener(this);
        // bt.addMouseListener(new JDUnderlinedText(bt));
        return bt;
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == btnSave) {
            this.broadcastEvent(new ActionEvent(this, ACTION_SAVE, e.getActionCommand()));
        } else if (e.getSource() == btnUpload) {
            this.broadcastEvent(new ActionEvent(this, ACTION_UPLOAD, e.getActionCommand()));
        }
    }

}
