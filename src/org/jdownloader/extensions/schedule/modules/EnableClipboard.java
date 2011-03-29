package org.jdownloader.extensions.schedule.modules;

import org.jdownloader.extensions.schedule.SchedulerModule;
import org.jdownloader.extensions.schedule.SchedulerModuleInterface;

import jd.controlling.ClipboardHandler;
import jd.utils.locale.JDL;

@SchedulerModule
public class EnableClipboard implements SchedulerModuleInterface {

    private static final long serialVersionUID = 7131982059931117440L;

    public boolean checkParameter(String parameter) {
        return false;
    }

    public void execute(String parameter) {
        ClipboardHandler.getClipboard().setEnabled(true);
    }

    public String getTranslation() {
        return JDL.L("jd.plugins.optional.schedule.modules.enableClipboard", "Enable Clipboard Monitoring");
    }

    public boolean needParameter() {
        return false;
    }

}
