package org.jdownloader.update;

import java.io.File;
import java.io.IOException;

import org.appwork.shutdown.ShutdownEvent;
import org.appwork.utils.Application;

public class SilentUpdaterEvent extends ShutdownEvent {
    private static final SilentUpdaterEvent INSTANCE = new SilentUpdaterEvent();

    /**
     * get the only existing instance of SilentUpdaterEvent. This is a singleton
     * 
     * @return
     */
    public static SilentUpdaterEvent getInstance() {
        return SilentUpdaterEvent.INSTANCE;
    }

    /**
     * Create a new instance of SilentUpdaterEvent. This is a singleton class.
     * Access the only existing instance by using {@link #getInstance()}.
     */
    private SilentUpdaterEvent() {
        this.setHookPriority(Integer.MAX_VALUE);

    }

    @Override
    public void run() {

        final File root = Application.getResource(RestartController.JARNAME);
        if (!root.exists()) {
            System.err.println(root + " is missing");
            return;
        }

        final String tiny[] = new String[] { RestartController.JAVA_INTERPRETER, "-jar", RestartController.UPDATER_JARNAME, "-restart", " " };
        if (Application.getResource(RestartController.JARNAME).exists()) {
            System.out.println(Application.getResource(RestartController.JARNAME) + " exists");
        } else {
            System.err.println(Application.getResource(RestartController.JARNAME) + " is Missing");
        }

        /*
         * build complete call arguments for tinybootstrap
         */
        final StringBuilder sb = new StringBuilder();

        for (final String arg : tiny) {
            sb.append(arg + " ");
        }

        System.out.println("UpdaterCall: " + sb.toString());

        final ProcessBuilder pb = new ProcessBuilder(tiny);
        /*
         * needed because the root is different for jre/class version
         */
        File pbroot = null;
        if (Application.isJared(this.getClass())) {
            pbroot = new File(Application.getRoot()).getParentFile();
        } else {
            pbroot = new File(Application.getRoot());
        }
        System.out.println("Root: " + pbroot);
        pb.directory(pbroot);
        try {
            pb.start();
        } catch (final IOException e) {
        }
    }

}
