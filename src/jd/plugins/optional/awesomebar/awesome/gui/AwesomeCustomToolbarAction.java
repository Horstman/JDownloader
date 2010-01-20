package jd.plugins.optional.awesomebar.awesome.gui;

import javax.swing.JPanel;

import jd.gui.swing.jdgui.actions.CustomToolbarAction;
import jd.gui.swing.jdgui.components.toolbar.ToolBar;
import jd.plugins.optional.awesomebar.Awesomebar;


public class AwesomeCustomToolbarAction extends CustomToolbarAction {
    
    private final Awesomebar awesomebar;
    private final JPanel awesomePanel;
    
    public AwesomeCustomToolbarAction(Awesomebar awesomebar) {
        super("addons.awesomebar");
        this.awesomebar = awesomebar;
        
        awesomePanel = this.awesomebar.getToolbarPanel();
  }

    private static final long serialVersionUID = 5484555948469924227L;

    
    // the addon has to get disabled, not the CustomToolbarAction
    public boolean force() {
        return true;
    }
    
    @Override
    public void addTo(Object toolBar) {
        // Toolbaractions might be used by other components, too
        // it's up to the custom action to implement them
        if (toolBar instanceof ToolBar) {
            ToolBar tb = (ToolBar) toolBar;
            tb.add(this.awesomebar.getToolbarPanel(), "");
        }        
    }

    @Override
    public void init() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void initDefaults() {
        // TODO Auto-generated method stub
        
    }

    public JPanel getAwesomePanel() {
        return awesomePanel;
    }
    
}