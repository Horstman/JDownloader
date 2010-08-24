//    jDownloader - Downloadmanager
//    Copyright (C) 2010  JD-Team support@jdownloader.org
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

package jd.plugins.optional.folderwatch;

import jd.gui.swing.components.table.JDTableModel;
import jd.plugins.optional.folderwatch.columns.FilenameColumn;
import jd.plugins.optional.folderwatch.columns.FilepathColumn;
import jd.plugins.optional.folderwatch.columns.FiletypeColumn;
import jd.plugins.optional.folderwatch.columns.ImportdateColumn;
import jd.utils.locale.JDL;

public class FolderWatchTableModel extends JDTableModel {

    private static final long serialVersionUID = 5047870839332563506L;
    private static final String JDL_PREFIX = "jd.plugins.optional.folderwatch.gui.FolderWatchTableModel.";

    public FolderWatchTableModel(String configname) {
        super(configname);
    }

    @Override
    protected void initColumns() {
        this.addColumn(new FilenameColumn(JDL.L(JDL_PREFIX + "filename", "Filename"), this));
        this.addColumn(new FiletypeColumn(JDL.L(JDL_PREFIX + "filetype", "Container type"), this));
        this.addColumn(new FilepathColumn(JDL.L(JDL_PREFIX + "filepath", "Path"), this));
        this.addColumn(new ImportdateColumn(JDL.L(JDL_PREFIX + "importdate", "Import date"), this));
    }

    @Override
    public void refreshModel() {
        synchronized (list) {
            list.clear();
            list.addAll(JDFolderWatch.history);
        }
    }
}
