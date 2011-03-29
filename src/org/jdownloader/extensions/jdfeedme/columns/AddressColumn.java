package org.jdownloader.extensions.jdfeedme.columns;

import org.jdownloader.extensions.jdfeedme.JDFeedMeFeed;
import org.jdownloader.extensions.jdfeedme.JDFeedMeTableModel;

import jd.gui.swing.components.table.JDTextEditorTableColumn;

public class AddressColumn extends JDTextEditorTableColumn {

	private static final long serialVersionUID = 4030351646645222509L;
	
	JDFeedMeTableModel table;

    public AddressColumn(String address, JDFeedMeTableModel table) {
        super(address, table);
        this.table = table;
    }

    @Override
    public boolean isEditable(Object obj) {
        return true;
    }

    @Override
    public boolean isEnabled(Object obj) {
        return ((JDFeedMeFeed) obj).isEnabled();
    }

    @Override
    public boolean isSortable(Object obj) {
        return false;
    }

    @Override
    public void sort(Object obj, boolean sortingToggle) {
    }

    @Override
    protected String getStringValue(Object value) {
        return ((JDFeedMeFeed) value).getAddress();
    }

    @Override
    protected void setStringValue(String value, Object object) 
    {
    	JDFeedMeFeed feed = ((JDFeedMeFeed) object);
        feed.setAddress(value);
    }


}
