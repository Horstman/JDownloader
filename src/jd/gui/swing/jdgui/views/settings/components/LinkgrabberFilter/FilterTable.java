package jd.gui.swing.jdgui.views.settings.components.LinkgrabberFilter;

import java.awt.Color;
import java.awt.Dimension;

import javax.swing.JLabel;
import javax.swing.ListSelectionModel;

import jd.gui.swing.laf.LookAndFeelController;

import org.appwork.utils.ColorUtils;
import org.appwork.utils.swing.table.AlternateHighlighter;
import org.appwork.utils.swing.table.ExtColumn;
import org.appwork.utils.swing.table.ExtComponentRowHighlighter;
import org.appwork.utils.swing.table.ExtTable;

public class FilterTable extends ExtTable<LinkFilter> {

    public FilterTable() {
        super(new FilterTableModel("FilterTable"));
        this.setSelectionBackground(Color.BLUE);
        this.setShowVerticalLines(false);
        this.setShowGrid(false);
        this.setShowHorizontalLines(false);
        this.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        this.setRowHeight(22);

        this.addMouseListener(new ContextMenuListener(this));

        int c = LookAndFeelController.getInstance().getLAFOptions().getPanelHeaderColor();
        Color b2;
        Color f2;
        if (c >= 0) {
            b2 = new Color(c);
            f2 = new Color(LookAndFeelController.getInstance().getLAFOptions().getPanelHeaderForegroundColor());
        } else {
            b2 = getForeground();
            f2 = getBackground();
        }
        this.setBackground(new Color(LookAndFeelController.getInstance().getLAFOptions().getPanelBackgroundColor()));
        // this.addRowHighlighter(new SelectionHighlighter(null, b2));
        this.getExtTableModel().addExtComponentRowHighlighter(new ExtComponentRowHighlighter<LinkFilter>(f2, b2, null) {

            @Override
            public boolean accept(ExtColumn<LinkFilter> column, LinkFilter value, boolean selected, boolean focus, int row) {
                return selected;
            }
        });
        this.addRowHighlighter(new AlternateHighlighter(null, ColorUtils.getAlphaInstance(new JLabel().getForeground(), 6)));
        this.setIntercellSpacing(new Dimension(0, 0));

    }
}
