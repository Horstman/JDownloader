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

package jd.plugins.decrypter;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "share-links.biz" }, urls = { "http://[\\w\\.]*?share-links\\.biz/_[0-9a-z]+" }, flags = { 0 })
public class ShrLnksBz extends PluginForDecrypt {

    public ShrLnksBz(PluginWrapper wrapper) {
        super(wrapper);
    }

    // @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(false);
        br.getPage(parameter);

        /* Error handling */
        if (br.containsHTML("Der Inhalt konnte nicht gefunden werden")) throw new DecrypterException(JDL.L("plugins.decrypt.errormsg.unavailable", "Perhaps wrong URL or the download is not available anymore."));

        if (br.containsHTML("(/captcha/|captcha_container|\"Captcha\"|id=\"captcha\")")) {
            logger.warning("The user tried do add a captcha protected link but this plugin has no captcha handling!");
            throw new DecrypterException(JDL.L("plugins.decrypt.errormsg.unavailable", "Plugin unvollständig: Das Plugin kann noch nicht mit Captchaeingaben umgehen!"));
        }
        // Folderpassword+Captcha handling
        if (br.containsHTML("id=\"folderpass\"")) {
            for (int i = 0; i <= 3; i++) {
                Form captchaForm = br.getForm(0);
                if (captchaForm == null) return null;
                String passCode = Plugin.getUserInput("Password?", param);
                captchaForm.put("pass", passCode);
                br.submitForm(captchaForm);
                if (br.containsHTML("Das eingegebene Passwort ist falsch")) continue;
                break;
            }
            if (br.containsHTML("Das eingegebene Passwort ist falsch")) {
                logger.warning("Wrong password!");
                throw new DecrypterException(DecrypterException.PASSWORD);
            }
            if (br.containsHTML("Sicherheitscode ist falsch")) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        }

        // container handling
        if (br.containsHTML("'dlc'")) {
            decryptedLinks = loadcontainer(br, "dlc");
            if (decryptedLinks != null && decryptedLinks.size() > 0) return decryptedLinks;
        }

        if (br.containsHTML("'rsdf'")) {
            decryptedLinks = loadcontainer(br, "rsdf");
            if (decryptedLinks != null && decryptedLinks.size() > 0) return decryptedLinks;
        }

        if (br.containsHTML("'ccf'")) {
            decryptedLinks = loadcontainer(br, "ccf");
            if (decryptedLinks != null && decryptedLinks.size() > 0) return decryptedLinks;
        }
        if (br.containsHTML("'cnl'")) {
            decryptedLinks = loadcontainer(br, "cnl");
            if (decryptedLinks != null && decryptedLinks.size() > 0) return decryptedLinks;

        } else {
            logger.warning("The user tried to add a link without containers but the plugin can't handle such links!");
            throw new DecrypterException(JDL.L("plugins.decrypt.errormsg.unavailable", "Perhaps wrong URL or the download is not available anymore."));
        }
        /* File package handling */
        String[] links = br.getRegex("decrypt\\.gif\".*?_get\\('(.*?)'").getColumn(0);
        if (links == null || links.length == 0) return null;
        progress.setRange(links.length);
        for (String link : links) {
            link = "http://share-links.biz/get/lnk/" + link;
            br.getPage(link);
            System.out.print(br.toString());
            String clink0 = br.getRegex("unescape\\(\"(.*?)\"").getMatch(0);
            if (clink0 == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            clink0 = Encoding.htmlDecode(clink0);
            String frm = br.getRegex("\"(http://share-links\\.biz/get/frm/.*?)\"").getMatch(0);
            String cmm = br.getRegex("\"(http://share-links\\.biz/get/cmm/.*?)\"").getMatch(0);
            br.getPage(cmm);
            System.out.print(br.toString());
            br.getPage(frm);
            System.out.print(br.toString());
            DownloadLink dl = createDownloadlink(cmm);
            decryptedLinks.add(dl);
            progress.increase(1);
        }
        return decryptedLinks;
    }

    // by jiaz
    private ArrayList<DownloadLink> loadcontainer(Browser br, String format) throws IOException, PluginException {
        Browser brc = br.cloneBrowser();
        String dlclinks = null;
        if (format.matches("dlc")) {
            dlclinks = br.getRegex("dlc container.*?onclick=\"javascript:_get\\('(.*?)'.*?'dlc'\\)").getMatch(0);
        }
        if (format.matches("ccf")) {
            dlclinks = br.getRegex("ccf container.*?onclick=\"javascript:_get\\('(.*?)'.*?'ccf'\\)").getMatch(0);
        }
        if (format.matches("rsdf")) {
            dlclinks = br.getRegex("rsdf container.*?onclick=\"javascript:_get\\('(.*?)'.*?'rsdf'\\)").getMatch(0);
        }
        if (dlclinks == null) return null;
        dlclinks = "http://share-links.biz/get/" + format + "/" + dlclinks;
        String test = Encoding.htmlDecode(dlclinks);
        File file = null;
        URLConnectionAdapter con = brc.openGetConnection(dlclinks);
        if (con.getResponseCode() == 200) {
            file = JDUtilities.getResourceFile("tmp/sharelinks/" + test.replaceAll("(http://share-links.biz/|/|\\?)", "") + "." + format);
            if (file == null) return null;
            file.deleteOnExit();
            brc.downloadConnection(file, con);
        } else {
            con.disconnect();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }

        if (file != null && file.exists() && file.length() > 100) {
            ArrayList<DownloadLink> decryptedLinks = JDUtilities.getController().getContainerLinks(file);
            if (decryptedLinks.size() > 0) return decryptedLinks;
        } else {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        return null;
    }

}