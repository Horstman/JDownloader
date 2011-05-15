package org.jdownloader.gui.views.downloads.columns;

import javax.swing.ImageIcon;
import javax.swing.JComponent;

import jd.gui.swing.jdgui.components.StatusLabel;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PackageLinkNode;
import jd.plugins.PluginForHost;
import jd.utils.JDTheme;

import org.appwork.utils.swing.table.ExtColumn;
import org.appwork.utils.swing.table.ExtTable;
import org.jdownloader.gui.translate._GUI;

public class HosterIconColumn extends ExtColumn<PackageLinkNode> {

    private static final long serialVersionUID = 8856394990447477484L;
    private StatusLabel       statuspanel      = null;
    private DownloadLink      dLink            = null;
    private int               counter          = 0;
    private ImageIcon         imgResume;
    private ImageIcon         imgPremium;
    private String            strResume;
    private String            strPremium;
    private ImageIcon         imgMissing;
    private String            strMissing;

    public HosterIconColumn() {
        super("HosterIcon", null);
        statuspanel = new StatusLabel();
        statuspanel.setBorder(null);
        imgResume = JDTheme.II("gui.images.resume", 16, 16);
        imgMissing = JDTheme.II("gui.images.bad", 16, 16);
        imgPremium = JDTheme.II("gui.images.premium", 16, 16);
        strResume = _GUI._.jd_gui_swing_jdgui_views_downloadview_TableRenderer_resume();
        strPremium = _GUI._.jd_gui_swing_jdgui_views_downloadview_TableRenderer_premium();
        strMissing = _GUI._.jd_gui_swing_jdgui_views_downloadview_TableRenderer_missing();
    }

    @Override
    public Object getCellEditorValue() {
        return null;
    }

    @Override
    public boolean isEditable(PackageLinkNode obj) {
        return false;
    }

    @Override
    public boolean isEnabled(PackageLinkNode obj) {
        return false;
    }

    @Override
    public boolean isSortable(PackageLinkNode obj) {
        return false;
    }

    @Override
    public void setValue(Object value, PackageLinkNode object) {
    }

    public JComponent getRendererComponent(ExtTable<PackageLinkNode> table, PackageLinkNode value, boolean isSelected, boolean hasFocus, int row, int column) {
        if (value instanceof FilePackage) {
            statuspanel.setText("FilePackage", null);
        } else {
            dLink = (DownloadLink) value;
            counter = 0;
            if (dLink.getTransferStatus().usesPremium()) {
                statuspanel.setIcon(counter, imgPremium, null, strPremium);
                counter++;
            }
            if (dLink.getTransferStatus().supportsResume()) {
                statuspanel.setIcon(counter, imgResume, null, strResume);
                counter++;
            }
            if (dLink.getDefaultPlugin() == null) {
                statuspanel.setIcon(counter, imgMissing, null, strMissing);
                counter++;
            } else {
                PluginForHost plg = dLink.getLivePlugin();
                if (plg == null) plg = dLink.getDefaultPlugin();
                String s = plg.getSessionInfo();
                statuspanel.setText(s, null);
                statuspanel.setIcon(-1, plg.getHosterIconScaled(), null, dLink.getDownloadLinkInfo().getLoadingFrom());
            }
            statuspanel.clearIcons(counter);
        }
        return statuspanel;
    }

}