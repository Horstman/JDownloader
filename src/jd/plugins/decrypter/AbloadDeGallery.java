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
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "abload.de" }, urls = { "http://[\\w\\.]*?abload\\.de/(gallery\\.php\\?key=[A-Za-z0-9]+|browseGallery\\.php\\?gal=[A-Za-z0-9]+\\&img=.+)" }, flags = { 0 })
public class AbloadDeGallery extends PluginForDecrypt {

    public AbloadDeGallery(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String DIRECTLINKREGEX  = "um die Originalgröße anzuzeigen\\.</div><img src=\"(http://.*?)\"";
    private static final String DIRECTLINKREGEX2 = "\"(http://(www\\.)?abload\\.de/img/.*?)\"";

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.getPage(parameter);
        if (!parameter.contains("browseGallery.php?gal=")) {
            String[] links = br.getRegex("class=\"image\"><a href=\"(/.*?)\"").getColumn(0);
            if (links == null || links.length == 0) links = br.getRegex("\"(/browseGallery\\.php\\?gal=.*img=.*?)\"").getColumn(0);
            if (links == null || links.length == 0) return null;
            progress.setRange(links.length);
            for (String singlePictureLink : links) {
                singlePictureLink = "http://www.abload.de" + Encoding.htmlDecode(singlePictureLink);
                br.getPage(singlePictureLink);
                String finallink = br.getRegex(DIRECTLINKREGEX).getMatch(0);
                if (finallink == null) finallink = br.getRegex(DIRECTLINKREGEX2).getMatch(0);
                if (finallink == null) return null;
                decryptedLinks.add(createDownloadlink("directhttp://" + finallink));
                progress.increase(1);
            }
        } else {
            String finallink = br.getRegex(DIRECTLINKREGEX).getMatch(0);
            if (finallink == null) finallink = br.getRegex(DIRECTLINKREGEX2).getMatch(0);
            if (finallink == null) return null;
            decryptedLinks.add(createDownloadlink("directhttp://" + finallink));
        }
        return decryptedLinks;
    }

}
