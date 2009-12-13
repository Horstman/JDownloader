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

package jd.event;

import java.awt.AWTEvent;

/**
 * Diese Klasse realisiert Ereignisse, die zum Steuern des Programmes dienen
 * 
 * @author astaldo
 */
public class ControlEvent extends AWTEvent {

    private static final long serialVersionUID = 1639354503246054870L;

    /**
     * Alle Downloads wurden bearbeitet. Und der download wird angehalten
     */
    public static final int CONTROL_ALL_DOWNLOADS_FINISHED = 1;

    /**
     * A Reconnect was requested and will be performed.
     */
    public static final int CONTROL_BEFORE_RECONNECT = 2;

    /**
     * The requested Reconnect was performed.
     */
    public static final int CONTROL_AFTER_RECONNECT = 3;

    /**
     * Ein PLugin wird beendet. Source: ist jeweils das PLugin Parameter:
     * Decrypter: decrypted Links Vector
     */
    public static final int CONTROL_PLUGIN_INACTIVE = 4;

    /**
     * Ein Plugin fängt an zu arbeiten. Source ist das PLugin selbst Parameter:
     * Decrypter: encryptedLinks
     */
    public static final int CONTROL_PLUGIN_ACTIVE = 5;

    /**
     * Wird aufgerufen sobald der Downloadvorgang komplett gestoppt ist
     */
    public static final int CONTROL_DOWNLOAD_STOP = 6;

    /**
     * Gibt an, dass der Downloadvorgang gestartet wurde
     */
    public static final int CONTROL_DOWNLOAD_START = 13;

    /**
     * wird verschickt wenn das Kontextmenü der Downloadlinks geöffnet wird
     * (oder package); soiu7rce: link/packlage parameter:menuitem arraylist
     */
    public static final int CONTROL_LINKLIST_CONTEXT_MENU = 22;

    /**
     * Gibt an dass ein plugin, eine INteraction etc. einen Forschritt gemacht
     * haben. Das entsprechende Event wird aus der ProgressController klasse
     * ausgelöst
     */
    public static final int CONTROL_ON_PROGRESS = 24;

    /**
     * Wird vom Controller vor dem beeenden des Programms aufgerufen
     */
    public static final int CONTROL_SYSTEM_EXIT = 26;

    public static final int CONTROL_JDPROPERTY_CHANGED = 27;

    public static final int CONTROL_INTERACTION_CALL = 28;

    public static final int CONTROL_LOG_OCCURED = 29;

    public static final int CONTROL_INIT_COMPLETE = 30;

    /**
     * Wird verwendet wenn eine datei verarbeitet wurde.z.B. eine datei entpackt
     * wurde. Andere plugins und addons können dieses event abrufen und
     * entscheiden wie die files weiterverareitet werden sollen. Die files
     * werden als File[] parameter übergeben
     */
    public static final int CONTROL_ON_FILEOUTPUT = 33;

    /**
     * Sammelt über DataBox.java daten ein
     */
    public static final int CONTROL_COLLECT_DATA = 34;

    /**
     * prepareShutDown is complete
     */
    public static final int CONTROL_SYSTEM_SHUTDOWN_PREPARED = 261;

    /**
     * can be used to write reconnect addons. the call must set
     * messagebox.setProperty(Reconnecter.VERIFY_IP_AGAIN,true);
     * 
     * to force the reconnector to check the ip again. if this ip check
     * succeeds, the internal reconnect settings are skipped
     */
    public static final int CONTROL_RECONNECT_REQUEST = 262;

    /**
     * Die ID des Ereignisses
     */
    private int controlID;

    /**
     * Ein optionaler Parameter
     */
    private Object parameter;

    public ControlEvent(Object source, int controlID) {
        this(source, controlID, null);
    }

    public ControlEvent(Object source, int controlID, Object parameter) {
        super(source, controlID);
        this.controlID = controlID;
        this.parameter = parameter;
    }

    @Override
    public int getID() {
        return controlID;
    }

    public Object getParameter() {
        return parameter;
    }

    @Override
    public String toString() {
        return "[source:" + source + ", controlID:" + controlID + ", parameter:" + parameter + "]";
    }
}
