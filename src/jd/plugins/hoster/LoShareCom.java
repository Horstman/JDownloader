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

package jd.plugins.hoster;

import java.io.IOException;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "loshare.com" }, urls = { "http://[\\w\\.]*?oshar(e|a)\\.com/download\\.php\\?id=[a-zA-Z0-9_-]+" }, flags = { 0 })
public class LoShareCom extends PluginForHost {

    public LoShareCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://upload.loshare.com/rules.php";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("File was deleted")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("File name</span>:(.*?)<br").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("\"Click this to report for(.*?)\"").getMatch(0);
            if (filename == null) {
                filename = br.getRegex("Title:</span>(.*?)<br").getMatch(0);
            }
        }
        String filesize = br.getRegex("File size:</span>(.*?)</p>").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        link.setName(filename.trim());
        link.setDownloadSize(Regex.getSize(filesize.trim()));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        String tmpmin = br.getRegex("You can wait for the start of downloading.*?(\\d+).*?minute").getMatch(0);
        if (tmpmin == null) tmpmin = br.getRegex("download will be available by.*?(\\d+).*?minute").getMatch(0);
        if (tmpmin != null) {
            int minutes = 0;
            if (tmpmin != null) minutes = Integer.parseInt(tmpmin);
            int waittime = (60 * minutes) * 1000;
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, null, waittime);
        }
        br.setFollowRedirects(false);
        Form dlform = br.getFormbyProperty("id", "slowdownload");
        if (dlform == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        br.submitForm(dlform);
        String rticketData = br.getRegex("data' value='(.*?)'>").getMatch(0);
        Browser br2 = br.cloneBrowser();
        br2.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        br2.postPage("http://loshare.com/ajax.php", "act=rticket&data=" + rticketData);
        System.out.print(br2.toString());
        String getdlData = br2.getRegex("text\":\"(.*?)\"").getMatch(0);
        sleep(30 * 1001l, downloadLink);
        br2.postPage("http://loshare.com/ajax.php", "act=getdl&data=" + getdlData);
        String dllink = br.getRegex("url\":\"(http.*?)\"").getMatch(0);
        System.out.print(br.toString());
        dllink = dllink.replace("\\", "");
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, false, 1);
        dl.startDownload();
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}