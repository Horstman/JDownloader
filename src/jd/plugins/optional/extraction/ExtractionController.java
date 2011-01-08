//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.plugins.optional.extraction;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.logging.Logger;

import jd.config.SubConfiguration;
import jd.controlling.DownloadWatchDog;
import jd.controlling.JDLogger;
import jd.controlling.PasswordListController;
import jd.controlling.ProgressController;
import jd.nutils.jobber.JDRunnable;
import jd.plugins.DownloadLink;
import jd.plugins.optional.jdunrar.JDUnrarConstants;
import jd.utils.locale.JDL;

/**
 * Responsible for the coorect procedure of the extraction process. Contains one
 * IExtraction instance.
 * 
 * @author botzi
 * 
 */
public class ExtractionController extends Thread implements JDRunnable {
    private ArrayList<ExtractionListener> listener = new ArrayList<ExtractionListener>();
    private ArrayList<String>             passwordList;
    private SubConfiguration              config   = null;
    private Exception                     exception;
    private boolean                       removeAfterExtraction;
    private ProgressController            progressController;
    private Archive                       archive;
    private IExtraction                   extractor;
    private Timer                         timer;
    private Logger                        logger;

    ExtractionController(Archive archiv, IExtraction extractor) {
        this.archive = archiv;
        this.extractor = extractor;

        extractor.setArchiv(archiv);
        extractor.setExtractionController(this);

        config = SubConfiguration.getConfig(JDL.L("plugins.optional.extraction.name", "Extraction"));

        logger = JDLogger.getLogger();
    }

    /**
     * Adds an listener to the current unpack process.
     * 
     * @param listener
     */
    void addExtractionListener(ExtractionListener listener) {
        this.removeExtractionListener(listener);
        this.listener.add(listener);
    }

    /**
     * Removes an listener from the current unpack process.
     * 
     * @param listener
     */
    private void removeExtractionListener(ExtractionListener listener) {
        this.listener.remove(listener);
    }

    /**
     * Checks if the extracted file(s) has enough space. Only works with Java 6
     * or higher.
     * 
     * @return True if it's enough space.
     */
    private boolean checkSize() {
        if (System.getProperty("java.version").contains("1.5")) { return true; }

        File f = archive.getExtractTo();

        while (!f.exists()) {
            f = f.getParentFile();

            if (f == null) return false;
        }

        long size = 1024L * 1024 * config.getIntegerProperty(ExtractionConstants.CONFIG_KEY_ADDITIONAL_SPACE, 512);

        for (DownloadLink dlink : DownloadWatchDog.getInstance().getRunningDownloads()) {
            size += dlink.getDownloadSize() - dlink.getDownloadCurrent();
        }

        if (f.getUsableSpace() < size + archive.getSize()) { return false; }

        return true;
    }

    @Override
    public void run() {
        try {
            fireEvent(ExtractionConstants.WRAPPER_START_OPEN_ARCHIVE);
            logger.info("Start unpacking of " + archive.getFirstDownloadLink().getFileOutput());
            if (extractor.prepare()) {
                if (!checkSize()) {
                    fireEvent(ExtractionConstants.NOT_ENOUGH_SPACE);
                    logger.info("Not enough space for unpacking of " + archive.getFirstDownloadLink().getFileOutput());
                    return;
                }

                if (archive.isProtected() && archive.getPassword().equals("")) {
                    fireEvent(ExtractionConstants.WRAPPER_CRACK_PASSWORD);
                    logger.info("Start password finding for " + archive.getFirstDownloadLink().getFileOutput());

                    for (String password : passwordList) {
                        if (password == null || password.equals("")) continue;

                        if (extractor.findPassword(password)) {
                            break;
                        }
                    }
                    if (archive.getPassword().equals("")) {
                        fireEvent(ExtractionConstants.WRAPPER_PASSWORD_NEEDED_TO_CONTINUE);
                        logger.info("Found no password in passwordlist " + archive.getFirstDownloadLink().getFileOutput());

                        if (!extractor.findPassword(archive.getPassword())) {
                            fireEvent(JDUnrarConstants.WRAPPER_EXTRACTION_FAILED);
                            logger.info("No password found for " + archive.getFirstDownloadLink().getFileOutput());
                            return;
                        }
                        PasswordListController.getInstance().addPassword(archive.getPassword(), true);
                    }

                    fireEvent(ExtractionConstants.WRAPPER_PASSWORD_FOUND);
                    logger.info("Found password for " + archive.getFirstDownloadLink().getFileOutput());
                }
                fireEvent(ExtractionConstants.WRAPPER_OPEN_ARCHIVE_SUCCESS);

                if (!archive.getExtractTo().exists()) {
                    if (!archive.getExtractTo().mkdirs()) {
                        JDLogger.getLogger().warning("Could not create subpath");
                        fireEvent(ExtractionConstants.WRAPPER_EXTRACTION_FAILED);
                    }
                }

                logger.info("Execute unpacking of " + archive.getFirstDownloadLink().getFileOutput());

                UpdateDisplay update = new UpdateDisplay(this);
                timer.schedule(update, 0, 1000);
                extractor.extract();
                update.cancel();

                extractor.close();

                switch (archive.getExitCode()) {
                case ExtractionControllerConstants.EXIT_CODE_SUCCESS:
                    logger.info("Unpacking successfull for " + archive.getFirstDownloadLink().getFileOutput());
                    if (!archive.getGotInterrupted() && removeAfterExtraction) {
                        removeArchiveFiles();
                    }
                    fireEvent(ExtractionConstants.WRAPPER_FINISHED_SUCCESSFULL);
                    break;
                case ExtractionControllerConstants.EXIT_CODE_CRC_ERROR:
                    JDLogger.getLogger().warning("A CRC error occurred when unpacking");
                    fireEvent(ExtractionConstants.WRAPPER_EXTRACTION_FAILED_CRC);
                    break;
                case ExtractionControllerConstants.EXIT_CODE_USER_BREAK:
                    JDLogger.getLogger().info(" User interrupted extraction");
                    fireEvent(ExtractionConstants.WRAPPER_EXTRACTION_FAILED);
                    break;
                case ExtractionControllerConstants.EXIT_CODE_CREATE_ERROR:
                    JDLogger.getLogger().warning("Could not create Outputfile");
                    fireEvent(ExtractionConstants.WRAPPER_EXTRACTION_FAILED);
                    break;
                case ExtractionControllerConstants.EXIT_CODE_MEMORY_ERROR:
                    JDLogger.getLogger().warning("Not enough memory for operation");
                    fireEvent(ExtractionConstants.WRAPPER_EXTRACTION_FAILED);
                    break;
                case ExtractionControllerConstants.EXIT_CODE_USER_ERROR:
                    JDLogger.getLogger().warning("Command line option error");
                    fireEvent(ExtractionConstants.WRAPPER_EXTRACTION_FAILED);
                    break;
                case ExtractionControllerConstants.EXIT_CODE_OPEN_ERROR:
                    JDLogger.getLogger().warning("Open file error");
                    fireEvent(ExtractionConstants.WRAPPER_EXTRACTION_FAILED);
                    break;
                case ExtractionControllerConstants.EXIT_CODE_WRITE_ERROR:
                    JDLogger.getLogger().warning("Write to disk error");
                    this.exception = new ExtractionException("Write to disk error");
                    fireEvent(ExtractionConstants.WRAPPER_EXTRACTION_FAILED);
                    break;
                case ExtractionControllerConstants.EXIT_CODE_LOCKED_ARCHIVE:
                    JDLogger.getLogger().warning("Attempt to modify an archive previously locked");
                    fireEvent(ExtractionConstants.WRAPPER_EXTRACTION_FAILED);
                    break;
                case ExtractionControllerConstants.EXIT_CODE_FATAL_ERROR:
                    JDLogger.getLogger().warning("A fatal error occurred");
                    fireEvent(ExtractionConstants.WRAPPER_EXTRACTION_FAILED);
                    break;
                case ExtractionControllerConstants.EXIT_CODE_WARNING:
                    JDLogger.getLogger().warning("Non fatal error(s) occurred");
                    fireEvent(ExtractionConstants.WRAPPER_EXTRACTION_FAILED);
                    break;
                default:
                    JDLogger.getLogger().warning("Unknown Error");
                    fireEvent(ExtractionConstants.WRAPPER_EXTRACTION_FAILED);
                    break;
                }
                return;
            } else {
                fireEvent(ExtractionConstants.WRAPPER_EXTRACTION_FAILED);
            }
        } catch (Exception e) {
            extractor.close();
            this.exception = e;
            JDLogger.exception(e);
            fireEvent(ExtractionConstants.WRAPPER_EXTRACTION_FAILED);
        } finally {
            fireEvent(ExtractionConstants.REMOVE_ARCHIVE_METADATA);
        }
    }

    /**
     * Deletes the archive files.
     */
    private void removeArchiveFiles() {
        for (DownloadLink link : archive.getDownloadLinks()) {
            if (!new File(link.getFileOutput()).delete()) {
                JDLogger.getLogger().warning("Could not delete archive: " + link.getFileOutput());
            }
        }
    }

    /**
     * Returns a thrown exception.
     * 
     * @return The thrown exception.
     */
    Exception getException() {
        return exception;
    }

    /**
     * 
     * Returns the current password finding process.
     * 
     * @return
     */
    int getCrackProgress() {
        return extractor.getCrackProgress();
    }

    /**
     * Fires an event.
     * 
     * @param status
     */
    public void fireEvent(int status) {
        for (ExtractionListener listener : this.listener) {
            listener.onExtractionEvent(status, this);
        }
    }

    /**
     * Sets the password list for extraction.
     * 
     * @param passwordList
     */
    void setPasswordList(ArrayList<String> passwordList) {
        this.passwordList = passwordList;
    }

    /**
     * Gets the passwordlist
     * 
     * @return
     */
    public ArrayList<String> getPasswordList() {
        return passwordList;
    }

    /**
     * Returns the ProgressController
     * 
     * @return
     */
    ProgressController getProgressController() {
        return progressController;
    }

    /**
     * Should the archives be deleted after extracting.
     * 
     * @param setProperty
     */
    void setRemoveAfterExtract(boolean setProperty) {
        this.removeAfterExtraction = setProperty;
    }

    /**
     * Sets the ProgressController.
     * 
     * @param progress
     */
    void setProgressController(ProgressController progress) {
        progressController = progress;
    }

    /**
     * Starts the extracting progress.
     */
    public void go() throws Exception {
        run();
    }

    /**
     * Returns the {@link Archive}.
     * 
     * @return
     */
    public Archive getArchiv() {
        return archive;
    }

    /**
     * Returns the Configuration.
     * 
     * @return
     */
    public SubConfiguration getConfig() {
        return config;
    }

    /**
     * Returns the extracted files.
     * 
     * @return
     */
    List<String> getPostProcessingFiles() {
        return extractor.filesForPostProcessing();
    }

    /**
     * Sets a exeption that occurs during unpacking.
     * 
     * @param e
     */
    public void setExeption(Exception e) {
        exception = e;
    }

    /**
     * Returns the extractor of this controller.
     * 
     * @return
     */
    IExtraction getExtractor() {
        return extractor;
    }

    /**
     * Sets the timer for updating the display;
     * 
     * @param timer
     */
    void setTimer(Timer timer) {
        this.timer = timer;
    }
}