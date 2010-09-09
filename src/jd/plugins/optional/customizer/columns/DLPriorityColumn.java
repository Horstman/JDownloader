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

package jd.plugins.optional.customizer.columns;

import java.awt.Component;

import javax.swing.JComboBox;
import javax.swing.JTable;

import jd.gui.swing.jdgui.views.downloads.DownloadTable;
import jd.plugins.optional.customizer.CustomizeSetting;

import org.appwork.utils.swing.table.ExtColumn;
import org.appwork.utils.swing.table.ExtTableModel;
import org.jdesktop.swingx.renderer.JRendererLabel;

public class DLPriorityColumn extends ExtColumn<CustomizeSetting> {

    private static final long    serialVersionUID = 4640856288557573254L;
    private static String[]      prioDescs;
    private final JRendererLabel jlr;
    private final JComboBox      prio;

    public DLPriorityColumn(String name, ExtTableModel<CustomizeSetting> table) {
        super(name, table);

        jlr = new JRendererLabel();
        jlr.setBorder(null);

        prio = new JComboBox(prioDescs = DownloadTable.prioDescs);
        prio.setBorder(null);
    }

    @Override
    public Object getCellEditorValue() {
        return prio.getSelectedIndex();
    }

    @Override
    public boolean isEditable(CustomizeSetting obj) {
        return isEnabled(obj);
    }

    @Override
    public boolean isEnabled(CustomizeSetting obj) {
        return obj.isEnabled();
    }

    @Override
    public boolean isSortable(CustomizeSetting obj) {
        return false;
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        prio.setSelectedIndex(((CustomizeSetting) value).getDLPriority() + 1);
        return prio;
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        jlr.setText(prioDescs[((CustomizeSetting) value).getDLPriority() + 1]);
        return jlr;
    }

    @Override
    public void setValue(Object value, CustomizeSetting object) {
        object.setDLPriority((Integer) value - 1);
    }

}
