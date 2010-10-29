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

package jd.plugins.hoster;

import java.io.IOException;
import java.util.regex.Pattern;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import jd.PluginWrapper;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.locale.JDL;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "2shared.com" }, urls = { "http://[\\w\\.]*?2shared\\.com/(audio|file|video)/.*?/[a-zA-Z0-9._]+" }, flags = { 0 })
public class TwoSharedCom extends PluginForHost {

    private static final String MAINPAGE = "http://www.2shared.com";

    public TwoSharedCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.2shared.com/terms.jsp";
    }

    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("/audio/", "/file/"));
        link.setUrlDownload(link.getDownloadURL().replace("/video/", "/file/"));
    }
    
    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws PluginException, IOException {
        br.setCookiesExclusive(true);
        br.getPage(downloadLink.getDownloadURL());
        Form pwform = br.getForm(0);
        if (pwform != null) {
            String filename = br.getRegex("<td class=\"header\" align=\"center\">Download (.*?)</td>").getMatch(0);
            if (filename == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            downloadLink.setName(filename.trim());
            return AvailableStatus.TRUE;
        }
        String filesize = br.getRegex(Pattern.compile("<span class=.*?>File size:</span>(.*?)&nbsp; &nbsp;", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)).getMatch(0);
        String filename = br.getRegex("<title>2shared - download(.*?)</title>").getMatch(0);
        if (filesize == null || filename == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        downloadLink.setName(filename.trim());
        downloadLink.setDownloadSize(Regex.getSize(filesize.trim().replaceAll(",|\\.", "")));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        Form pwform = br.getForm(0);
        if (pwform != null) {
            String passCode;
            if (downloadLink.getStringProperty("pass", null) == null) {
                passCode = getUserInput(null, downloadLink);
            } else {
                passCode = downloadLink.getStringProperty("pass", null);
            }
            pwform.put("userPass2", passCode);
            br.submitForm(pwform);
            if (br.containsHTML("passError\\(\\);")) {
                downloadLink.setProperty("pass", null);
                throw new PluginException(LinkStatus.ERROR_CAPTCHA, JDL.L("plugins.hoster.2sharedcom.errors.wrongcaptcha", "Wrong captcha"));
            } else {
                downloadLink.setProperty("pass", passCode);
            }
        }
        String link = br.getRegex(Pattern.compile("\\$\\.get\\('(.*?)'", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)).getMatch(0);
        /* charalgo now works dynamically */
        String jsquery = br.getPage(MAINPAGE + br.getRegex("src=\"(.*?)\"").getMatch(0, 1));
        String charalgo = new Regex(jsquery, "var viw(.*)l2surl;\\}").getMatch(-1).replaceAll("M\\.url", "M");
        if (link == null || jsquery == null || charalgo == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        String result = decrypt(charalgo, link);
        if (result != null) link = result;
        link = br.getPage(MAINPAGE + link).trim();
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, link, true, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            if (dl.getConnection().getURL().getQuery().contains("MAX_IP")) {
                dl.getConnection().disconnect();
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, JDL.L("plugins.hoster.2sharedcom.errors.sessionlimit", "Session limit reached"), 10 * 60 * 1000l);
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }
    
    public String decrypt(String fun, String value) throws Exception {
        Object result = new Object();
        ScriptEngineManager manager = new ScriptEngineManager();
        ScriptEngine engine = manager.getEngineByName("javascript");
        Invocable inv = (Invocable) engine;
        try {
            engine.eval("function charalgo(M){" + fun + "return (M);}");
            result = inv.invokeFunction("charalgo", value);
        } catch (ScriptException e) {
            e.printStackTrace();
        }
        return (String) result;
    }

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
