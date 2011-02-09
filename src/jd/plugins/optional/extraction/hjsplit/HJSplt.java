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

package jd.plugins.optional.extraction.hjsplit;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import jd.config.ConfigContainer;
import jd.config.SubConfiguration;
import jd.controlling.JDLogger;
import jd.nutils.io.FileSignatures;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.optional.extraction.Archive;
import jd.plugins.optional.extraction.DummyDownloadLink;
import jd.plugins.optional.extraction.ExtractionController;
import jd.plugins.optional.extraction.ExtractionControllerConstants;
import jd.plugins.optional.extraction.IExtraction;
import jd.plugins.optional.extraction.hjsplit.jaxe.JAxeJoiner;
import jd.plugins.optional.extraction.hjsplit.jaxe.JoinerFactory;
import jd.plugins.optional.extraction.hjsplit.jaxe.ProgressEvent;
import jd.plugins.optional.extraction.hjsplit.jaxe.ProgressEventListener;
import jd.utils.JDHexUtils;
import jd.utils.JDUtilities;

import org.appwork.utils.Regex;
import org.appwork.utils.formatter.StringFormatter;

public class HJSplt implements IExtraction {
    private static final String DUMMY_HOSTER = "dum.my";

    private Archive             archive;

    public HJSplt() {
    }

    public Archive buildArchive(DownloadLink link) {
        Archive a = new Archive();
        a.setExtractor(this);

        File file = new File(link.getFileOutput());

        file = this.getStartFile(file);

        ArrayList<File> files = getFileList(file);

        ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        a.setDownloadLinks(ret);

        if (files == null) return a;

        String startfile = getStartFile(new File(link.getFileOutput())).getAbsolutePath();

        for (File f : files) {
            DownloadLink l = JDUtilities.getController().getDownloadLinkByFileOutput(f.getAbsoluteFile(), LinkStatus.FINISHED);
            if (l == null) {
                l = buildDownloadLinkFromFile(f.getAbsolutePath());
            }

            if (startfile.equals(f.getAbsolutePath())) {
                a.setFirstDownloadLink(l);
            }

            ret.add(l);
        }

        a.setProtected(false);

        return a;
    }

    /**
     * Builds an Dummydownloadlink from an file.
     * 
     * @param file
     *            The file from the harddisk.
     * @return The Downloadlink.
     */
    private DownloadLink buildDownloadLinkFromFile(String file) {
        File file0 = new File(file);
        DummyDownloadLink link = new DummyDownloadLink(null, file0.getName(), DUMMY_HOSTER, "", true);
        link.setFile(file0);
        return link;
    }

    public Archive buildDummyArchive(String file) {
        File file0 = new File(file);
        DownloadLink link = JDUtilities.getController().getDownloadLinkByFileOutput(file0, LinkStatus.FINISHED);
        if (link == null) {
            link = buildDownloadLinkFromFile(file);
        }
        return buildArchive(link);
    }

    public boolean findPassword(String password) {
        return true;
    }

    public void extract() {
        File first = new File(archive.getFirstDownloadLink().getFileOutput());
        JAxeJoiner join = JoinerFactory.getJoiner(first);

        String cutKillerExt = getCutkillerExtension(first, archive.getDownloadLinks().size());
        join.setCutKiller(cutKillerExt);
        join.setProgressEventListener(new ProgressEventListener() {
            public void handleEvent(ProgressEvent pe) {
                archive.setExtracted(pe.getCurrent());
                archive.setSize(pe.getMax());
            }
        });
        join.overwriteExistingFile(archive.isOverwriteFiles());

        join.run();

        if (!join.wasSuccessfull()) {
            archive.setExitCode(ExtractionControllerConstants.EXIT_CODE_FATAL_ERROR);
            return;
        }

        archive.addExtractedFiles(getOutputFile(first));
    }

    public boolean checkCommand() {
        return true;
    }

    public int getCrackProgress() {
        return 100;
    }

    public boolean prepare() {
        return true;
    }

    public void initConfig(ConfigContainer config, SubConfiguration subConfig) {
    }

    public void setArchiv(Archive archive) {
        this.archive = archive;
    }

    public void setExtractionController(ExtractionController controller) {
    }

    public String getArchiveName(DownloadLink link) {
        return null;
    }

    public boolean isArchivSupported(String file) {
        switch (getArchiveType(new File(file))) {
        case NONE:
            return false;
        case NORMAL:
            if (file.matches("(?i).*\\.7z\\.\\d+$")) {
                return false;
            } else {
                Archive a = buildDummyArchive(file);
                if (a.getFirstDownloadLink() == null || a.getDownloadLinks().size() <= 1) {
                    return false;
                } else {
                    return true;
                }
            }
        default:
            return true;
        }
    }

    public boolean isArchivSupportedFileFilter(String file) {
        return isStartVolume(new File(file));
    }

    public void setConfig(SubConfiguration config) {
    }

    public void close() {
    }

    private static enum ARCHIV_TYPE {
        NONE, NORMAL, UNIX, CUTKILLER, XTREMSPLIT
    }

    /**
     * Gibt die zu entpackende Datei zurück.
     * 
     * @param file
     * @return
     */
    private File getOutputFile(File file) {
        ARCHIV_TYPE type = getArchiveType(file);
        switch (type) {
        case XTREMSPLIT:
            /* maybe parse header here */
            return new File(file.getParentFile(), file.getName().replaceFirst("\\.\\d+\\.xtm$", ""));
        case UNIX:
            return new File(file.getParentFile(), file.getName().replaceFirst("\\.a.$", ""));
        case NORMAL:
            return new File(file.getParentFile(), file.getName().replaceFirst("\\.[\\d]+($|\\.[^\\d]*$)", ""));
        default:
            return null;
        }
    }

    /**
     * Gibt alle files die zum Archiv von file gehören zurück
     * 
     * @param file
     * @return
     */
    private ArrayList<File> getFileList(File file) {

        File startFile = getStartFile(file);
        if (startFile == null || !startFile.exists() || !startFile.isFile()) return null;
        ARCHIV_TYPE type = getArchiveType(file);

        switch (type) {
        case UNIX:
            return validateUnixType(startFile);
        case NORMAL:
            return validateNormalType(startFile);
        default:
            return null;
        }
    }

    /**
     * Validiert typ normal (siehe validateArchiv)
     * 
     * @param file
     * @return
     */
    private ArrayList<File> validateNormalType(File file) {
        final String matcher = file.getName().replaceAll("\\[|\\]|\\(|\\)|\\?", ".").replaceFirst("\\.[\\d]+($|\\.[^\\d]*$)", "\\\\.[\\\\d]+$1");
        ArrayList<DownloadLink> missing = JDUtilities.getController().getDownloadLinksByNamePattern(matcher);
        if (missing == null) return null;
        for (DownloadLink miss : missing) {
            /* do not continue if we have an unfinished file here */
            if (!miss.getLinkStatus().isFinished()) return null;
            File par1 = new File(miss.getFileOutput()).getParentFile();
            File par2 = file.getParentFile();
            if (par1.equals(par2)) {

                if (!new File(miss.getFileOutput()).exists()) { return null; }
            }
        }
        File[] files = file.getParentFile().listFiles(new java.io.FileFilter() {
            public boolean accept(File pathname) {
                if (pathname.isFile() && pathname.getName().matches(matcher)) { return true; }
                return false;
            }
        });
        int c = 1;
        ArrayList<File> ret = new ArrayList<File>();
        for (int i = 0; i < files.length; i++) {
            String volume = StringFormatter.fillString(c + "", "0", "", 3);
            File newFile;
            if ((newFile = new File(file.getParentFile(), file.getName().replaceFirst("\\.[\\d]+($|\\.[^\\d]*$)", "\\." + volume + "$1"))).exists()) {
                c++;
                ret.add(newFile);
            } else {
                return null;
            }
        }
        /*
         * securitycheck for missing file on disk but in downloadlist, will
         * check for next possible filename
         */
        String volume = StringFormatter.fillString(c + "", "0", "", 3);
        if (JDUtilities.getController().getDownloadLinkByFileOutput(new File(file.getParentFile(), file.getName().replaceFirst("\\.[\\d]+($|\\.[^\\d]*$)", "\\." + volume + "$1")), null) != null) return null;
        return ret;
    }

    /**
     * Validiert das archiv auf 2 arten 1. wird ind er downloadliste nach
     * passenden unfertigen archiven gesucht 2. wird das archiv durchnummeriert
     * und geprüft ob es lücken/fehlende files gibts siehe (validateArchiv)
     * 
     * @param file
     * @return
     */
    private ArrayList<File> validateUnixType(File file) {

        final String matcher = file.getName().replaceAll("\\[|\\]|\\(|\\)|\\?", ".").replaceFirst("\\.a.($|\\..*)", "\\\\.a.$1");
        ArrayList<DownloadLink> missing = JDUtilities.getController().getDownloadLinksByNamePattern(matcher);
        if (missing == null) return null;
        for (DownloadLink miss : missing) {
            /* do not continue if we have an unfinished file here */
            if (!miss.getLinkStatus().isFinished()) return null;
            if (new File(miss.getFileOutput()).exists() && new File(miss.getFileOutput()).getParentFile().equals(file.getParentFile())) continue;
            return null;
        }
        File[] files = file.getParentFile().listFiles(new java.io.FileFilter() {
            public boolean accept(File pathname) {
                if (pathname.isFile() && pathname.getName().matches(matcher)) { return true; }
                return false;
            }
        });
        ArrayList<File> ret = new ArrayList<File>();
        char c = 'a';
        for (int i = 0; i < files.length; i++) {
            File newFile;
            if ((newFile = new File(file.getParentFile(), file.getName().replaceFirst("\\.a.($|\\..*)", "\\.a" + c + "$1"))).exists()) {
                ret.add(newFile);
                c++;
            } else {
                return null;
            }
        }
        /*
         * securitycheck for missing file on disk but in downloadlist , will
         * check for next possible filename
         */
        if (JDUtilities.getController().getDownloadLinkByFileOutput(new File(file.getParentFile(), file.getName().replaceFirst("\\.a.($|\\..*)", "\\.a" + c + "$1")), null) != null) return null;
        return ret;
    }

    /**
     * Sucht den Dateinamen und den Pfad der des Startvolumes heraus
     * 
     * @param file
     * @return
     */
    private File getStartFile(File file) {
        ARCHIV_TYPE type = getArchiveType(file);
        switch (type) {
        case XTREMSPLIT:
            return new File(file.getParentFile(), file.getName().replaceFirst("\\.\\d+\\.xtm$", ".001.xtm"));
        case UNIX:
            return new File(file.getParentFile(), file.getName().replaceFirst("\\.a.$", ".aa"));
        case NORMAL:
            return new File(file.getParentFile(), file.getName().replaceFirst("\\.[\\d]+($|\\.[^\\d]*$)", ".001$1"));
        default:
            return null;
        }
    }

    /**
     * Gibt zurück ob es sich bei der Datei um ein hjsplit Startvolume handelt.
     * 
     * @param file
     * @return
     */
    private boolean isStartVolume(File file) {
        if (file.getName().matches(".*\\.aa$")) return true;
        if (file.getName().matches(".*\\.001\\.xtm$")) return true;
        if (file.getName().matches(".*\\.001(\\.[^\\d]*)?$")) return true;
        if (file.getName().matches(".*\\.001$")) return true;
        return false;
    }

    /**
     * Gibt den Archivtyp zurück. möglich sind: ARCHIVE_TYPE_7Z (bad)
     * ARCHIVE_TYPE_NONE (bad) ARCHIVE_TYPE_UNIX ARCHIVE_TYPE_NORMAL
     * 
     * @param file
     * @return
     */
    private ARCHIV_TYPE getArchiveType(File file) {
        String name = file.getName();
        if (name.matches(".*\\.a.$")) return ARCHIV_TYPE.UNIX;
        if (name.matches(".*\\.\\d+\\.xtm$")) return ARCHIV_TYPE.XTREMSPLIT;
        /* eg. bla.001.rar */
        if (name.matches(".*\\.[\\d]+($|\\.[^\\d]{1,5}$)")) return ARCHIV_TYPE.NORMAL;
        return ARCHIV_TYPE.NONE;
    }

    /**
     * returns String with fileextension if we find a valid cutkiller fileheader
     * returns null if no cutkiller fileheader found
     * 
     * @param file
     * @return
     */
    private String getCutkillerExtension(File file, int filecount) {
        File startFile = getStartFile(file);
        if (startFile == null) return null;
        String sig = null;
        try {
            sig = JDHexUtils.toString(FileSignatures.readFileSignature(startFile));
        } catch (IOException e) {
            JDLogger.exception(e);
            return null;
        }
        if (new Regex(sig, "[\\w]{3}  \\d+").matches()) {
            String count = new Regex(sig, ".*?  (\\d+)").getMatch(0);
            if (count == null) return null;
            if (filecount != Integer.parseInt(count)) return null;
            String ext = new Regex(sig, "(.*?) ").getMatch(0);
            return ext;
        }
        return null;
    }

    public List<String> checkComplete(Archive archive) {
        return new ArrayList<String>();
    }
}