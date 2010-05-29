//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
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

package jd.plugins.optional.customizer;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;

import jd.parser.Regex;
import jd.utils.JDUtilities;

public class CustomizeSetting implements Serializable, Comparable<CustomizeSetting> {

    private static ArrayList<CustomizeSetting> SETTINGS = null;

    public static CustomizeSetting getFirstMatch(String fileName) {
        ArrayList<CustomizeSetting> settings = new ArrayList<CustomizeSetting>(SETTINGS);
        Collections.sort(settings);
        for (CustomizeSetting setting : settings) {
            if (setting.isEnabled() && setting.matches(fileName)) return setting;
        }
        return null;
    }

    public static void setSettings(ArrayList<CustomizeSetting> settings) {
        SETTINGS = settings;
    }

    public static ArrayList<CustomizeSetting> getSettings() {
        return SETTINGS;
    }

    private static final long serialVersionUID = 3295935612660256840L;

    private String name;

    private boolean enabled = true;

    private String regex;

    private String packageName;

    private String downloadDir = JDUtilities.getDefaultDownloadDirectory();

    private boolean extract = true;

    private String password;

    private boolean useSubDirectory = false;

    private int dlpriority = 0;

    private transient int matchCount = 0;

    public CustomizeSetting(String name) {
        setName(name);
    }

    public boolean matches(String fileName) {
        if (regex == null || regex.length() == 0) return false;
        try {
            return new Regex(fileName, regex).matches();
        } catch (Exception e) {
            return false;
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getRegex() {
        return regex;
    }

    public void setRegex(String regex) {
        this.regex = regex;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getDownloadDir() {
        return downloadDir;
    }

    public void setDownloadDir(String downloadDir) {
        this.downloadDir = downloadDir;
    }

    public boolean isPostProcessing() {
        return extract;
    }

    public void setPostProcessing(boolean extract) {
        this.extract = extract;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isUseSubDirectory() {
        return useSubDirectory;
    }

    public void setUseSubDirectory(boolean useSubDirectory) {
        this.useSubDirectory = useSubDirectory;
    }

    public int getDLPriority() {
        return dlpriority;
    }

    public void setDLPriority(int dlpriority) {
        this.dlpriority = dlpriority;
    }

    public String getMatchCount() {
        return String.valueOf(matchCount);
    }

    public void incMatchCount() {
        ++matchCount;
    }

    public int compareTo(CustomizeSetting o) {
        if (o.getDLPriority() == getDLPriority()) return 0;
        return o.getDLPriority() > getDLPriority() ? 1 : -1;
    }

}
