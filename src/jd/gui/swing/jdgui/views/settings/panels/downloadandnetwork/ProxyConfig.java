package jd.gui.swing.jdgui.views.settings.panels.downloadandnetwork;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.ArrayList;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;
import javax.swing.ListSelectionModel;

import jd.controlling.proxy.ProxyInfo;
import jd.gui.swing.jdgui.views.settings.ConfigPanel;

import org.appwork.utils.swing.table.ExtTable;
import org.appwork.utils.swing.table.SelectionHighlighter;
import org.jdownloader.gui.settings.AbstractConfigPanel;
import org.jdownloader.images.NewTheme;
import org.jdownloader.translate._JDT;

public class ProxyConfig extends AbstractConfigPanel {

    public String getTitle() {
        return _JDT._.gui_settings_proxy_title();
    }

    private static final long   serialVersionUID = -521958649780869375L;

    private ExtTable<ProxyInfo> table;

    private JButton             btnAdd;

    private JButton             btnRemove;

    public ProxyConfig() {
        super();

        this.addHeader(getTitle(), NewTheme.I().getIcon("proxy", 32));
        this.addDescription(_JDT._.gui_settings_proxy_description());

        table = new ExtTable<ProxyInfo>(new ProxyTableModel(), "proxyTable") {
            /**
             * 
             */
            private static final long serialVersionUID = 7638505074419527640L;

            @Override
            protected boolean onShortcutDelete(final ArrayList<ProxyInfo> selectedObjects, final KeyEvent evt, final boolean direct) {
                new ProxyDeleteAction(selectedObjects).actionPerformed(null);
                return true;
            }
        };
        table.setSearchEnabled(true);
        table.addRowHighlighter(new SelectionHighlighter(null, new Color(200, 200, 200, 80)));
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        table.addMouseListener(new ProxyContextMenu(table));
        JScrollPane sp = new JScrollPane(table);
        this.add(sp, "gapleft 37,growx, pushx,spanx,pushy,growy");
        JToolBar toolbar = new JToolBar();
        toolbar.setFloatable(false);
        btnAdd = new JButton(_JDT._.basics_add(), NewTheme.I().getIcon("add", 20));
        btnRemove = new JButton(_JDT._.basics_remove(), NewTheme.I().getIcon("delete", 20));
        btnAdd.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                new ProxyAddAction().actionPerformed(e);
            }
        });
        btnRemove.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                new ProxyDeleteAction(table.getExtTableModel().getSelectedObjects()).actionPerformed(e);
            }
        });
        toolbar.add(btnAdd);
        toolbar.add(btnRemove);

        add(toolbar, "gapleft 37,growx,spanx");

    }

    @Override
    public ImageIcon getIcon() {
        return NewTheme.I().getIcon("proxy", ConfigPanel.ICON_SIZE);
    }

    @Override
    public void save() {
    }

    @Override
    public void updateContents() {
    }

}
