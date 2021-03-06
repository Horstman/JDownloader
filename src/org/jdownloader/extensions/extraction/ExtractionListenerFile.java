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

package org.jdownloader.extensions.extraction;

import java.io.File;
import java.util.logging.Logger;

import jd.controlling.ProgressController;
import jd.event.ControlEvent;
import jd.gui.UserIO;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.utils.JDUtilities;

import org.jdownloader.extensions.extraction.translate.T;

/**
 * Updates the Extractionprogess for archives, where the download is not in the
 * downloadlist.
 * 
 * @author botzi
 * 
 */
public class ExtractionListenerFile implements ExtractionListener {
    private Logger              logger;
    private ExtractionExtension ex;

    ExtractionListenerFile(ExtractionExtension ex) {
        this.ex = ex;
    }

    public void onExtractionEvent(int id, ExtractionController controller) {
        ProgressController pc = controller.getProgressController();
        switch (id) {
        case ExtractionConstants.WRAPPER_STARTED:
            pc.setStatusText(controller.getArchiv().getFirstDownloadLink().getFileOutput() + ": " + T._.plugins_optional_extraction_status_queued());
            break;
        case ExtractionConstants.WRAPPER_EXTRACTION_FAILED:
            if (controller.getException() != null) {
                pc.setStatusText(controller.getArchiv().getFirstDownloadLink().getFileOutput() + ": " + T._.plugins_optional_extraction_status_extractfailed() + ": " + controller.getException().getMessage());
            } else {
                pc.setStatusText(controller.getArchiv().getFirstDownloadLink().getFileOutput() + ": " + T._.plugins_optional_extraction_status_extractfailed());
            }

            for (File f : controller.getArchiv().getExtractedFiles()) {
                if (f.exists()) {
                    if (!f.delete()) {
                        logger.warning("Could not delete file " + f.getAbsolutePath());
                    }
                }
            }

            controller.getArchiv().setActive(false);
            ex.onFinished(controller);

            break;
        case ExtractionConstants.WRAPPER_PASSWORD_NEEDED_TO_CONTINUE:
            pc.setStatusText(controller.getArchiv().getFirstDownloadLink().getFileOutput() + ": " + T._.plugins_optional_extraction_status_extractfailedpass());

            if (ex.getSettings().isAskForUnknownPasswordsEnabled()) {
                String pass = UserIO.getInstance().requestInputDialog(0, T._.plugins_optional_extraction_askForPassword(controller.getArchiv().getFirstDownloadLink().getName()), "");
                if (pass == null || pass.length() == 0) {
                    ex.onFinished(controller);
                    break;
                }
                controller.getArchiv().setPassword(pass);
            }

            break;
        case ExtractionConstants.WRAPPER_PASSWORT_CRACKING:
            pc.setStatusText(controller.getArchiv().getFirstDownloadLink().getFileOutput() + ": " + T._.plugins_optional_extraction_status_crackingpass());
            pc.setRange(controller.getPasswordList().size());
            pc.setStatus(controller.getCrackProgress());
            break;
        case ExtractionConstants.WRAPPER_START_OPEN_ARCHIVE:
            pc.setStatusText(controller.getArchiv().getFirstDownloadLink().getFileOutput() + ": " + T._.plugins_optional_extraction_status_openingarchive());
            break;
        case ExtractionConstants.WRAPPER_OPEN_ARCHIVE_SUCCESS:
            ex.assignRealDownloadDir(controller);
            break;
        case ExtractionConstants.WRAPPER_PASSWORD_FOUND:
            pc.setStatusText(controller.getArchiv().getFirstDownloadLink().getFileOutput() + ": " + T._.plugins_optional_extraction_status_passfound());
            break;
        case ExtractionConstants.WRAPPER_ON_PROGRESS:
            pc.setStatusText(controller.getArchiv().getFirstDownloadLink().getFileOutput() + ": " + T._.plugins_optional_extraction_status_extracting());
            pc.setRange(controller.getArchiv().getSize());
            pc.setStatus(controller.getArchiv().getExtracted());
            break;
        case ExtractionConstants.WRAPPER_EXTRACTION_FAILED_CRC:
            pc.setStatusText(controller.getArchiv().getFirstDownloadLink().getFileOutput() + ": " + T._.plugins_optional_extraction_status_extractfailedcrc());

            for (File f : controller.getArchiv().getExtractedFiles()) {
                if (f.exists()) {
                    if (!f.delete()) {
                        logger.warning("Could not delete file " + f.getAbsolutePath());
                    }
                }
            }

            controller.getArchiv().setActive(false);
            ex.onFinished(controller);
            break;
        case ExtractionConstants.WRAPPER_FINISHED_SUCCESSFUL:
            File[] files = new File[controller.getArchiv().getExtractedFiles().size()];
            int i = 0;
            for (File f : controller.getArchiv().getExtractedFiles()) {
                files[i++] = f;
            }
            JDUtilities.getController().fireControlEvent(new ControlEvent(controller, ControlEvent.CONTROL_ON_FILEOUTPUT, files));

            pc.setStatusText(controller.getArchiv().getFirstDownloadLink().getFileOutput() + ": " + T._.plugins_optional_extraction_status_extractok());

            if (ex.getSettings().isDeleteInfoFilesAfterExtraction()) {
                File fileOutput = new File(controller.getArchiv().getFirstDownloadLink().getFileOutput());
                File infoFiles = new File(fileOutput.getParentFile(), fileOutput.getName().replaceFirst("(?i)(\\.pa?r?t?\\.?[0-9]+\\.rar|\\.rar)$", "") + ".info");
                if (infoFiles.exists() && infoFiles.delete()) {
                    logger.info(infoFiles.getName() + " removed");
                }
            }
            controller.getArchiv().setActive(false);
            ex.onFinished(controller);
            break;
        case ExtractionConstants.NOT_ENOUGH_SPACE:
            for (DownloadLink link : controller.getArchiv().getDownloadLinks()) {
                if (link == null) continue;

                link.getLinkStatus().setStatus(LinkStatus.FINISHED);
                link.getLinkStatus().setStatusText(T._.plugins_optional_extraction_status_notenoughspace());
                link.requestGuiUpdate();
            }

            ex.onFinished(controller);
            break;
        case ExtractionConstants.REMOVE_ARCHIVE_METADATA:
            ex.removeArchive(controller.getArchiv());
            break;
        case ExtractionConstants.WRAPPER_FILE_NOT_FOUND:
            pc.setStatus(LinkStatus.ERROR_DOWNLOAD_FAILED);
            pc.setStatusText(T._.plugins_optional_extraction_filenotfound());
            controller.getArchiv().setActive(false);
            ex.onFinished(controller);
            break;
        }
    }
}