//jDownloader - Downloadmanager
//Copyright (C) 2009  JD-Team support@jdownloader.org
//
//This program is free software: you can redistribute it and/or modify
//it under the terms of the GNU General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//This program is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.plugins.decrypter;

import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.utils.locale.JDL;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "uploader.ro" }, urls = { "http://[\\w\\.]*?uploader\\.ro/(files/[0-9A-Z]{8}|redirect/[A-Z0-9]{8}/[0-9]+)" }, flags = { 0 })
public class UploaderRo extends PluginForDecrypt {

    public UploaderRo(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        String id = new Regex(parameter, "(files|redirect)/([0-9A-Z]{8})").getMatch(1);
        String statuslink = "http://uploader.ro/status.php?uid=" + id;
        br.getPage(statuslink);
        /* Error handling */
        if (!br.containsHTML("<td><img src=")) throw new DecrypterException(JDL.L("plugins.decrypt.errormsg.unavailable", "Perhaps wrong URL or the download is not available anymore."));
        if (parameter.contains("/redirect/")) {
            br.getPage(parameter);
            String finallink = br.getRegex("<frame name=\"main\" src=\"(.*?)\">").getMatch(0);
            if (finallink == null) return null;
            decryptedLinks.add(createDownloadlink(finallink));
        } else {
            String[] redirectLinks = br.getRegex("(/redirect/[0-9A-Z]{8}/\\d+)").getColumn(0);
            if (redirectLinks.length == 0) return null;
            progress.setRange(redirectLinks.length);
            for (String link : redirectLinks) {
                br.getPage(link);
                String dllink = br.getRegex("<frame name=\"main\" src=\"(.*?)\">").getMatch(0);
                if (dllink == null) return null;
                decryptedLinks.add(createDownloadlink(dllink));
                progress.increase(1);
            }
        }
        return decryptedLinks;
    }

}