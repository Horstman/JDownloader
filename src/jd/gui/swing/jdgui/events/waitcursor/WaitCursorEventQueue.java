package jd.gui.swing.jdgui.events.waitcursor;

import java.awt.AWTEvent;
import java.awt.EventQueue;
import java.awt.Toolkit;
import java.awt.event.MouseEvent;

import jd.gui.swing.jdgui.events.ContextMenu;

/**
 * @author Based on <a
 *         href="http://www.javaspecialists.eu/archive/Issue075.html">The Java
 *         Specialists' Newsletter</a>
 */
public class WaitCursorEventQueue extends EventQueue implements DelayTimerCallback {

    public static void initEventQueue() {
        Toolkit.getDefaultToolkit().getSystemEventQueue().push(new WaitCursorEventQueue());
    }

    private static final int DELAY = 70;

    private final DelayTimer waitTimer;
    private final CursorManager cursorManager;
    private final ContextMenu contextMenu;

    private WaitCursorEventQueue() {
        this.waitTimer = new DelayTimer(this, DELAY);
        this.cursorManager = new CursorManager(waitTimer);
        this.contextMenu = new ContextMenu();
    }

    public void close() {
        waitTimer.quit();
        pop();
    }

    @Override
    protected void dispatchEvent(AWTEvent event) {
        cursorManager.push(event.getSource());
        waitTimer.startTimer();
        try {
            super.dispatchEvent(event);

            if (event instanceof MouseEvent) contextMenu.dispatchMouseEvent((MouseEvent) event);
        } finally {
            waitTimer.stopTimer();
            cursorManager.pop();
        }
    }

    @Override
    public AWTEvent getNextEvent() throws InterruptedException {
        /*
         * started by pop(), this catches modal dialogs closing that do work
         * afterwards
         */
        waitTimer.stopTimer();
        return super.getNextEvent();
    }

    public void trigger() {
        cursorManager.setCursor();
    }

}
