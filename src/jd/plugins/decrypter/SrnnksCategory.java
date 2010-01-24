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

package jd.plugins.decrypter;

import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.gui.UserIO;
import jd.http.Browser;
import jd.parser.html.HTMLParser;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision: 10324 $", interfaceVersion = 2, names = { "serienjunkies.org" }, urls = { "http://[\\w\\.]*?serienjunkies\\.org/\\?(cat|p)=\\d+" }, flags = { 0 })
public class SrnnksCategory extends PluginForDecrypt {

    public SrnnksCategory(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink parameter, final ProgressController progress) throws Exception {
        Browser.setRequestIntervalLimitGlobal("serienjunkies.org", 400);
        Browser.setRequestIntervalLimitGlobal("download.serienjunkies.org", 400);
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        if (!UserIO.isOK(UserIO.getInstance().requestConfirmDialog(UserIO.DONT_SHOW_AGAIN, "Kategorie Decrypter!\r\nWillst du wirklich eine ganze Kategorie hinzufügen?"))) return ret;

        br.getPage(parameter.getCryptedUrl());
        if (br.containsHTML("<FRAME SRC")) {
            // progress.setStatusText("Lade Downloadseitenframe");
            br.getPage(br.getRegex("<FRAME SRC=\"(.*?)\"").getMatch(0));
        }
        if(br.containsHTML("Error 503")){
            UserIO.getInstance().requestMessageDialog("Serienjunkies ist überlastet. Bitte versuch es später nocheinmal!");
            return ret;
        }
        String[] ids = br.getRegex("<a href=\"http://serienjunkies.org/\\?p=(\\d+)\" .*?>(.*?)</a></h2>").getColumn(0);

        String[] names = br.getRegex("<a href=\"http://serienjunkies.org/\\?p=(\\d+)\" .*?>(.*?)</a></h2>").getColumn(1);

        int res = UserIO.getInstance().requestComboDialog(0, "Bitte Kategorie auswählen", "Bitte die gewünschte Staffel auswählen", names, 0, null, null, null, null);
        if (res < 0) return ret;
        br.getPage("http://serienjunkies.org/?p=" + ids[res]);
        ArrayList<String> mirrors = new ArrayList<String>();
        for (String m : br.getRegex("hier</a> \\| (.*?)<").getColumn(0)) {
            if (m.trim().length() > 0 && !mirrors.contains(m)) {
                mirrors.add(m);
            }
        }
        res = UserIO.getInstance().requestComboDialog(0, "Bitte Mirror auswählen", "Bitte den gewünschten Anbieter aus.", mirrors.toArray(new String[] {}), 0, null, null, null, null);
        if (res < 0) return ret;

        String[] urls = br.getRegex("</strong> <a href=\"([^<]*?)\" target=\"_blank\">hier</a> \\| " + mirrors.get(res) + "<br />").getColumn(0);
        StringBuilder sb = new StringBuilder();
        for (String url : urls) {
            sb.append(url);
            sb.append("\r\n");

        }
        String linklist = UserIO.getInstance().requestInputDialog(UserIO.STYLE_LARGE | UserIO.NO_COUNTDOWN, "Entferne ungewollte Links", sb.toString());

        urls = HTMLParser.getHttpLinks(linklist, null);
        for (String url : urls) {
            ret.add(this.createDownloadlink(url));
        }
        if (UserIO.isOK(UserIO.getInstance().requestConfirmDialog(0, "Jetzt " + ret.size() + " Links Decrypten? Für Jeden Link muss ein Captcha eingegeben werden!"))) {
            return ret;
        } else {
            return new ArrayList<DownloadLink>();
        }

    }

}
