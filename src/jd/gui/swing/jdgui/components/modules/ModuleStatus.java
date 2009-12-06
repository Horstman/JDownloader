package jd.gui.swing.jdgui.components.modules;

import java.awt.Cursor;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import jd.controlling.ProgressController;
import jd.event.ControlEvent;
import jd.event.ControlListener;
import jd.utils.JDUtilities;
import net.miginfocom.swing.MigLayout;

public class ModuleStatus extends JPanel implements ControlListener, MouseListener {

    private static final long serialVersionUID = 1745881766942067472L;
    private static final int BARCOUNT = 15;
    private ArrayList<ProgressController> controllers = new ArrayList<ProgressController>();
    private ArrayList<ProgressController> addcontrollers = new ArrayList<ProgressController>();
    private ArrayList<ProgressController> removecontrollers = new ArrayList<ProgressController>();
    private ProgressCircle[] circles;
    private transient Thread updateThread = null;
    private volatile boolean updateThreadWaiting = false;
    private static final int updateThreadPause = 250;

    public ModuleStatus() {
        super(new MigLayout("ins 0", "[fill,grow,align right]", "[::20, center]"));
        circles = new ProgressCircle[BARCOUNT];

        setName("Module Statusbar");
        for (int i = 0; i < BARCOUNT; i++) {
            circles[i] = new ProgressCircle();
            circles[i].setOpaque(false);
            circles[i].addMouseListener(this);
            circles[i].setVisible(false);
            add(circles[i], "dock east, hidemode 3, hmax 20, gapleft 3");
        }
        setOpaque(false);

        updateThread = new Thread() {
            @Override
            public void run() {
                int activecontrollers = 0;
                while (true) {
                    activecontrollers = controllers.size();
                    updateControllers();
                    if (controllers.size() != 0 || activecontrollers != 0) {
                        /* only gui update if controllers changed */
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                update();
                            }
                        });
                    } else {
                        updateThreadWaiting = true;
                        synchronized (this) {
                            while (addcontrollers.size() == 0) {
                                try {
                                    wait();
                                } catch (InterruptedException e) {
                                }
                            }
                        }
                        updateThreadWaiting = false;
                    }
                    try {
                        /* 4 updates per second is enough */
                        sleep(updateThreadPause);
                    } catch (InterruptedException e) {
                    }
                }
            }
        };
        updateThread.start();
        JDUtilities.getController().addControlListener(this);
    }

    private void addController(ProgressController source) {
        synchronized (addcontrollers) {
            if (!addcontrollers.contains(source)) addcontrollers.add(0, source);
        }
    }

    private void removeController(ProgressController source) {
        synchronized (removecontrollers) {
            if (!removecontrollers.contains(source)) removecontrollers.add(0, source);
        }
    }

    private void updateControllers() {
        synchronized (controllers) {
            synchronized (addcontrollers) {
                for (ProgressController add : addcontrollers) {
                    if (!controllers.contains(add)) controllers.add(add);
                }
                addcontrollers.clear();
            }
            synchronized (removecontrollers) {
                controllers.removeAll(removecontrollers);
                removecontrollers.clear();
            }
        }
    }

    public void controlEvent(ControlEvent event) {
        if (event.getID() == ControlEvent.CONTROL_ON_PROGRESS && event.getSource() instanceof ProgressController) {
            ProgressController source = (ProgressController) event.getSource();
            if (!source.isFinished()) {
                addController(source);
                if (updateThreadWaiting) {
                    synchronized (updateThread) {
                        updateThread.notify();
                    }
                }
            } else {
                removeController(source);
            }
        }
    }

    private void update() {
        synchronized (controllers) {
            int i;
            for (i = 0; i < Math.min(BARCOUNT, controllers.size()); ++i) {
                circles[i].setController(controllers.get(i));
                if (controllers.get(i).isFinalizing()) {
                    if (controllers.get(i).isFinished()) {
                        controllers.get(i).setFinished();
                    } else {
                        controllers.get(i).increase(updateThreadPause);
                    }
                }
                if (controllers.get(i).isInterruptable()) {
                    circles[i].setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                    circles[i].removeMouseListener(this);
                    circles[i].addMouseListener(this);
                } else {
                    circles[i].setCursor(Cursor.getDefaultCursor());
                    circles[i].removeMouseListener(this);
                }
                circles[i].setVisible(true);
            }
            for (int j = i; j < BARCOUNT; ++j) {
                circles[j].setVisible(false);
                circles[j].removeMouseListener(this);
                /* to remove references */
                circles[j].setController(null);
            }
            revalidate();
            repaint();
        }
    }

    public void mouseClicked(MouseEvent e) {
        if (e.getSource() instanceof ProgressCircle) {
            ProgressController controller = ((ProgressCircle) e.getSource()).getController();
            if (controller != null && !controller.isAbort()) controller.fireCancelAction();
        }
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }

    public void mousePressed(MouseEvent e) {
    }

    public void mouseReleased(MouseEvent e) {
    }

}
