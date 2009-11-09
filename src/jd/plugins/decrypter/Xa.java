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
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.utils.locale.JDL;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "ex.ua" }, urls = { "http://www\\.ex\\.ua/((view|get)/[0-9]+|(view/[0-9]+\\?r=[0-9]+|view/[0-9]+\\?r=[0-9]+,[0-9]+))" }, flags = { 0 })
public class Xa extends PluginForDecrypt {

    public Xa(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(false);
        br.getPage(parameter);
        if (br.getRedirectLocation() != null && br.getRedirectLocation().equals("http://www.ex.ua/")) throw new DecrypterException(JDL.L("plugins.decrypt.errormsg.unavailable", "Perhaps wrong URL or the download is not available anymore."));
        String fpName = br.getRegex("<title>(.*?)@ EX\\.UA</title>").getMatch(0);
        if (parameter.contains("/view/")) {
            String[] links = br.getRegex("'(/get/[0-9]+)'").getColumn(0);
            if (links.length == 0) return null;
            progress.setRange(links.length);
            for (String cryptedlink : links) {
                br.getPage("http://www.ex.ua" + cryptedlink);
                String finallink = br.getRedirectLocation();
                if (finallink == null) return null;
                decryptedLinks.add(createDownloadlink("directhttp://" + finallink));
                progress.increase(1);
            }
            if (fpName != null) {
                FilePackage fp = FilePackage.getInstance();
                fp.setName(fpName.trim());
                fp.addLinks(decryptedLinks);
            }

        } else {
            String finallink = br.getRedirectLocation();
            if (finallink == null) return null;
            decryptedLinks.add(createDownloadlink("directhttp://" + finallink));
        }
        return decryptedLinks;
    }
}