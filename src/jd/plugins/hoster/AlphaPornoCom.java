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

package jd.plugins.hoster;

import java.io.IOException;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

import org.appwork.utils.Hash;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "alphaporno.com" }, urls = { "http://(www\\.)?alphaporno\\.com/videos/[a-z0-9\\-]+" }, flags = { 0 })
public class AlphaPornoCom extends PluginForHost {

    public AlphaPornoCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    private String DLLINK = null;

    @Override
    public String getAGBLink() {
        return "http://www.alphaporno.com/";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(false);
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML("(<h2>Sorry, this video is no longer available|<title></title>)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        boolean pluginBroken = true;
        if (pluginBroken) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        String magic = getLink();
        String filename = br.getRegex("<h2><span></span>(.*?)</h2>").getMatch(0);
        if (filename == null) filename = br.getRegex("<title>(.*?)</title>").getMatch(0);
        DLLINK = br.getRegex("video_url: encodeURIComponent\\(\\'(http://.*?)\\'\\)").getMatch(0);
        DLLINK += "?time=This var is unknown, we have to find out how that works!&ahv=" + magic;
        if (filename == null || DLLINK == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        filename = filename.trim();
        downloadLink.setFinalFileName(Encoding.htmlDecode(filename) + ".flv");
        Browser br2 = br.cloneBrowser();
        // In case the link redirects to the finallink
        br2.setFollowRedirects(true);
        URLConnectionAdapter con = null;
        try {
            con = br2.openGetConnection(DLLINK);
            if (!con.getContentType().contains("html"))
                downloadLink.setDownloadSize(con.getLongContentLength());
            else
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            return AvailableStatus.TRUE;
        } finally {
            try {
                con.disconnect();
            } catch (Throwable e) {
            }
        }
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, DLLINK, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private String getLink() {
        int one = 668;
        int two = 501;
        int var1 = (one + two) * 2;
        String var2 = "http://www.alphaporno.com/get_file/1/d13a3cd18ab6b7ad88f409f2b6b30767/7000/7274/7274.flv/";
        String var3 = var2.substring(7);
        var3 = var3.substring(0, var3.indexOf("/", 0));
        if (var3.indexOf("www.") == 0) {
            var3 = var3.substring(4);
        }
        if (var3.indexOf(".") != var3.lastIndexOf(".")) {
            var3 = var3.substring((var3.indexOf(".") + 1));
        }
        String finalfinalfinal = Hash.getMD5(var1 + var3 + var1);
        return finalfinalfinal;
    }

    // {
    // var _loc_3:* = param2.substring(7);
    // _loc_3 = _loc_3.substring(0, _loc_3.indexOf("/", 0));
    // if (_loc_3.indexOf("www.") == 0)
    // {
    // _loc_3 = _loc_3.substring(4);
    // }
    // if (_loc_3.indexOf(".") != _loc_3.lastIndexOf("."))
    // {
    // _loc_3 = _loc_3.substring((_loc_3.indexOf(".") + 1));
    // }
    // trace("B.init(): domain=" + _loc_3);
    // lc = MD5.hash(_loc_3 + param1);
    // lrc = MD5.hash(param1 + _loc_3);
    // ahv = MD5.hash(param1 + _loc_3 + param1);
    // return;
    //    

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}
