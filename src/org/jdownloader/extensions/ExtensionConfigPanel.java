package org.jdownloader.extensions;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ImageIcon;

import org.appwork.storage.config.ConfigEventListener;
import org.appwork.storage.config.ConfigInterface;
import org.appwork.utils.swing.dialog.Dialog;
import org.jdownloader.gui.settings.AbstractConfigPanel;

public abstract class ExtensionConfigPanel<T extends AbstractExtension> extends AbstractConfigPanel implements ConfigEventListener {

    private static final long serialVersionUID = 1L;

    private T                 extension;

    private Header            header;

    public ExtensionConfigPanel(T plg, boolean clean) {
        super();
        this.extension = plg;
        plg.getStore().addListener(this);
        if (!clean) {
            header = new Header(plg.getName(), plg.getIcon(32), new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    try {
                        extension.setEnabled(header.isHeaderEnabled());

                    } catch (Exception e1) {
                        e1.printStackTrace();
                        Dialog.getInstance().showExceptionDialog("Error", e1.getMessage(), e1);
                    }
                }
            });
            add(header, "spanx,growx,pushx");
            header.setEnabled(plg.isEnabled());
            if (plg.getDescription() != null) {
                addDescription(plg.getDescription());
            }
        }

    }

    public void onConfigValueModified(ConfigInterface config, String key, Object newValue) {
        if ("enabled".equals(key)) {
            header.setHeaderEnabled((Boolean) newValue);
        }
    }

    public ExtensionConfigPanel(T plg) {
        this(plg, false);

    }

    @Override
    public ImageIcon getIcon() {
        return extension.getIcon(32);
    }

    @Override
    public String getTitle() {
        return extension.getName();
    }

    @Override
    protected void onShow() {
    }

    @Override
    protected void onHide() {
    }

    public T getExtension() {
        return extension;
    }
}
