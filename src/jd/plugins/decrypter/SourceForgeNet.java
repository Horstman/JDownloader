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

import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.utils.locale.JDL;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "sourceforge.net" }, urls = { "http://[\\w\\.]*?(sourceforge\\.net/projects/(.*?/files/extras/.*?/download|.+)|downloads\\.sourceforge\\.net/.+)" }, flags = { 0 })
public class SourceForgeNet extends PluginForDecrypt {

    public SourceForgeNet(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(true);
        // Test if we already hav a direct link here
        URLConnectionAdapter con = br.openGetConnection(parameter);
        if (con.getContentType().contains("html")) {
            br.followConnection();
            if (br.containsHTML("(Error 404|The page you were looking for cannot be found|could not be found or is not available)")) throw new DecrypterException(JDL.L("plugins.decrypt.errormsg.unavailable", "Perhaps wrong URL or the download is not available anymore."));
            String link = null;
            if (parameter.contains("/files/extras/") || parameter.contains("prdownloads.sourceforge.net")) {
                link = br.getRegex("Please use this <a href=\"(.*?)\"").getMatch(0);
                if (link == null) link = br.getRegex("\"(http://downloads\\.sourceforge\\.net/project/.*?/extras/.*?/.*?use_mirror=.*?)\"").getMatch(0);
            } else {
                String continuelink = br.getRegex("\\}\" href=\"(http://sourceforge\\.net/projects/.*?/download)\"").getMatch(0);
                if (continuelink != null) br.getPage(Encoding.htmlDecode(continuelink));
                link = br.getRegex("Please use this <a href=\"(http://downloads\\.sourceforge\\.net/project/.*?)\"").getMatch(0);
            }
            if (link == null) return null;
            br.setFollowRedirects(false);
            link = Encoding.htmlDecode(link);
            String finallink = null;
            boolean failed = true;
            for (int i = 0; i <= 5; i++) {
                br.getPage(link);
                finallink = br.getRedirectLocation();
                if (finallink == null) return null;
                con = br.openGetConnection(finallink);
                if (con.getContentType().contains("html")) {
                    logger.info("finallink is no file, continuing...");
                    continue;
                }
                failed = false;
                break;
            }
            if (failed) logger.warning("The finallink is no file!!");
            decryptedLinks.add(createDownloadlink("directhttp://" + finallink));
            con.disconnect();
        } else {
            con.disconnect();
            decryptedLinks.add(createDownloadlink("directhttp://" + parameter));
        }
        return decryptedLinks;
    }
}
