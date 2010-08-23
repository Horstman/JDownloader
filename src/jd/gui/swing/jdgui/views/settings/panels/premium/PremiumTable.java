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

package jd.gui.swing.jdgui.views.settings.panels.premium;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;

import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import jd.controlling.AccountController;
import jd.gui.swing.GuiRunnable;
import jd.gui.swing.components.table.JDRowHighlighter;
import jd.gui.swing.components.table.JDTable;
import jd.gui.swing.jdgui.actions.ActionController;
import jd.nutils.OSDetector;
import jd.plugins.Account;

public class PremiumTable extends JDTable implements MouseListener, KeyListener {

    private static final long serialVersionUID = 9049514723238421532L;
    private Premium panel;

    public PremiumTable(Premium panel) {
        super(new PremiumJTableModel("premiumview"));
        this.panel = panel;
        addMouseListener(this);
        addKeyListener(this);
        addHighlighter();
    }

    private void addHighlighter() {
        this.addJDRowHighlighter(new JDRowHighlighter(UIManager.getColor("TableHeader.background")) {
            @Override
            public boolean doHighlight(Object obj) {
                return !(obj instanceof Account);
            }
        });
    }

    public ArrayList<Account> getSelectedAccounts() {
        int[] rows = getSelectedRows();
        ArrayList<Account> ret = new ArrayList<Account>();
        for (int row : rows) {
            Object element = getValueAt(row, 0);
            if (element != null && element instanceof Account) {
                ret.add((Account) element);
            }
        }
        return ret;
    }

    public ArrayList<Account> getAllSelectedAccounts() {
        ArrayList<Account> accs = getSelectedAccounts();
        ArrayList<HostAccounts> ha = getSelectedHostAccounts();
        for (HostAccounts hostAccount : ha) {
            for (Account acc : AccountController.getInstance().getAllAccounts(hostAccount.getHost())) {
                if (!accs.contains(acc)) accs.add(acc);
            }
        }
        return accs;
    }

    public ArrayList<HostAccounts> getSelectedHostAccounts() {
        int[] rows = getSelectedRows();
        ArrayList<HostAccounts> ret = new ArrayList<HostAccounts>();
        for (int row : rows) {
            Object element = this.getModel().getValueAt(row, 0);
            if (element != null && element instanceof HostAccounts) {
                ret.add((HostAccounts) element);
            }
        }
        return ret;
    }

    public void fireTableChanged() {
        new GuiRunnable<Object>() {
            @Override
            public Object runSave() {
                final Rectangle viewRect = panel.getScrollPane().getViewport().getViewRect();
                int[] rows = getSelectedRows();
                final ArrayList<Object> selected = new ArrayList<Object>();
                for (int row : rows) {
                    Object elem = getValueAt(row, 0);
                    if (elem != null) selected.add(elem);
                }
                getJDTableModel().refreshModel();
                getJDTableModel().fireTableStructureChanged();
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        for (Object obj : selected) {
                            int row = getJDTableModel().getRowforObject(obj);
                            if (row != -1) addRowSelectionInterval(row, row);
                        }
                        scrollRectToVisible(viewRect);
                    }
                });
                return null;
            }
        }.start();
    }

    public void mouseClicked(MouseEvent e) {
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }

    public void mousePressed(MouseEvent e) {
        /* nicht auf headerclicks reagieren */
        if (e.getSource() != this) return;
        Point point = e.getPoint();
        int row = rowAtPoint(point);

        if (this.getModel().getValueAt(row, 0) == null) {
            clearSelection();
            return;
        }

        if (e.isPopupTrigger() || e.getButton() == MouseEvent.BUTTON3) {
            if (!isRowSelected(row)) {
                clearSelection();
                addRowSelectionInterval(row, row);
            }

            JPopupMenu popup = new JPopupMenu();
            popup.add(ActionController.getToolBarAction("action.premiumview.refreshacc"));
            popup.show(this, point.x, point.y);
        }
    }

    public void mouseReleased(MouseEvent e) {
    }

    public void keyPressed(KeyEvent e) {
    }

    public void keyReleased(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_DELETE || (OSDetector.isMac() && e.getKeyCode() == KeyEvent.VK_BACK_SPACE)) {
            ActionController.getToolBarAction("action.premiumview.removeacc").actionPerformed(null);
        }
    }

    public void keyTyped(KeyEvent e) {
    }

}
