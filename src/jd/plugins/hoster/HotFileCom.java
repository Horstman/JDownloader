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
//

package jd.plugins.hoster;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.http.Browser;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.http.RandomUserAgent;
import jd.nutils.JDHash;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "hotfile.com" }, urls = { "http://[\\w\\.]*?hotfile\\.com/dl/\\d+/[0-9a-zA-Z]+/(.*?/|.+)" }, flags = { 2 })
public class HotFileCom extends PluginForHost {
    private String              ua   = RandomUserAgent.generate();
    private static final Object LOCK = new Object();

    public HotFileCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://hotfile.com/register.html?reff=274657");
        setConfigElements();
    }

    private static final String UNLIMITEDMAXCON = "UNLIMITEDMAXCON";
    private static final String TRY_IWL_BYPASS  = "TRY_IWL_BYPASS";

    private void setConfigElements() {
        ConfigEntry cond = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), TRY_IWL_BYPASS, JDL.L("plugins.hoster.HotFileCom.TRYIWLBYPASS", "Try IWL-Filter list bypass?")).setDefaultValue(false);
        config.addEntry(cond);
        cond = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), UNLIMITEDMAXCON, JDL.L("plugins.hoster.HotFileCom.SetUnlimitedConnectionsForPremium", "Allow more than 5 connections per file for premium (default maximum = 5). Enabling this can cause errors!!")).setDefaultValue(false);
        config.addEntry(cond);
    }

    @Override
    public String getAGBLink() {
        return "http://hotfile.com/terms-of-service.html";
    }

    private HashMap<String, String> callAPI(Browser br, String action, Account account, HashMap<String, String> addParams) throws Exception {
        if (action == null || action.length() == 0) return null;
        Browser tbr = br;
        if (tbr == null) {
            tbr = new Browser();
        }
        tbr.setDebug(true);
        LinkedHashMap<String, String> post = new LinkedHashMap<String, String>();
        post.put("action", action);
        if (account != null) {
            /* do not remove */
            String pwMD5 = JDHash.getMD5(account.getPass().trim());
            post.put("passwordmd5", pwMD5);
            post.put("username", Encoding.urlEncode(account.getUser().trim()));
            // post.put("password",
            // Encoding.urlEncode(account.getPass().trim()));
        }
        if (addParams != null) {
            for (String param : addParams.keySet()) {
                post.put(param, addParams.get(param));
            }
        }
        tbr.postPage("http://api.hotfile.com", post);

        HashMap<String, String> ret = new HashMap<String, String>();
        ret.put("httpresponse", tbr.toString());
        String vars[][] = tbr.getRegex("(.*?)=(.*?)(&|$)").getMatches();
        for (String var[] : vars) {
            ret.put(var[0] != null ? var[0].trim() : null, var[1]);
        }
        return ret;
    }

    public AccountInfo fetchAccountInfoWebsite(Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        synchronized (LOCK) {
            this.setBrowserExclusive();
            try {
                loginWebsite(account);
            } catch (PluginException e) {
                account.setProperty("cookies", null);
                account.setValid(false);
                return ai;
            }
            String validUntil[] = br.getRegex("Premium until.*?>(.*?)<.*?>(\\d+:\\d+:\\d+)").getRow(0);
            if (validUntil == null || validUntil[0] == null || validUntil[1] == null) {
                logger.severe("HotFileDebug: " + br.toString());
                account.setProperty("cookies", null);
                account.setValid(false);
            } else {
                String valid = validUntil[0].trim() + " " + validUntil[1].trim() + " CDT";
                ai.setValidUntil(Regex.getMilliSeconds(valid, "yyyy-MM-dd HH:mm:ss zzz", null));
                account.setValid(true);
            }
        }
        return ai;
    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        if (account.getUser().trim().equalsIgnoreCase("cookie")) {
            account.setValid(false);
            ai.setStatus("Cookie login no longer possible! API does not support it!");
            return ai;
        }
        if (getPluginConfig().getBooleanProperty(TRY_IWL_BYPASS, false)) return fetchAccountInfoWebsite(account);
        HashMap<String, String> info = callAPI(null, "getuserinfo", account, null);
        String rawAnswer = info.get("httpresponse");
        logger.severe("HotFileDebug: " + rawAnswer);
        if (rawAnswer != null && rawAnswer.startsWith(".too many failed")) {
            /* fallback to normal website */
            logger.severe("api reports: too many failed logins(check logins)! using website fallback!");
            return fetchAccountInfoWebsite(account);
        }
        if (!info.containsKey("is_premium") || !"1".equalsIgnoreCase(info.get("is_premium"))) {
            account.setValid(false);
            if (info.get("httpresponse").contains("invalid username")) {
                ai.setStatus("invalid username or password");
            } else {
                ai.setStatus("No Premium Account");
            }
            return ai;
        }
        String validUntil = info.get("premium_until");
        if (validUntil == null) {
            account.setValid(false);
        } else {
            account.setValid(true);
            validUntil = validUntil.replaceAll(":|T", "");
            validUntil = validUntil.replaceFirst("-", "");
            validUntil = validUntil.replaceFirst("-", "");
            ai.setValidUntil(Regex.getMilliSeconds(validUntil, "yyyyMMddHHmmssZ", null));
            ai.setStatus("Premium");
        }
        return ai;
    }

    @Override
    public void handlePremium(DownloadLink downloadLink, Account account) throws Exception {
        checkLinks(new DownloadLink[] { downloadLink });
        if (downloadLink.isAvailabilityStatusChecked() && !downloadLink.isAvailable()) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        if (getPluginConfig().getBooleanProperty(TRY_IWL_BYPASS, false)) {
            logger.severe("trying iwl-bypass");
            handlePremiumWebsite(downloadLink, account);
            return;
        }
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("link", Encoding.urlEncode(downloadLink.getDownloadURL() + "\n\r"));
        params.put("alllinks", "1");
        HashMap<String, String> info = callAPI(null, "getdirectdownloadlink", account, params);
        logger.severe("HotFileDebug: " + info.get("httpresponse"));
        if (info.get("httpresponse").contains("file not found")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        if (info.get("httpresponse").contains("premium required")) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
        String finalUrls = info.get("httpresponse").trim();
        if (finalUrls == null || finalUrls.startsWith(".")) {
            if (finalUrls != null) {
                if (finalUrls.startsWith(".too many failed")) {
                    logger.severe("api reports: too many failed logins(check logins)! using website fallback!");
                    handlePremiumWebsite(downloadLink, account);
                    return;
                }
                if (finalUrls.startsWith(".ip blocked")) {
                    logger.severe("api reports: ip blocked! using website fallback!");
                    handlePremiumWebsite(downloadLink, account);
                    return;
                }
                if (finalUrls.startsWith(".server that hosts the file is temporarily")) { throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server temporarily unavailable", 30 * 60 * 1000l); }
            }
            logger.severe(finalUrls);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        String dlUrls[] = Regex.getLines(finalUrls);
        StringBuilder errorSb = new StringBuilder("");
        if (dlUrls == null || dlUrls.length == 0) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        boolean contentHeader = false;
        for (String url : dlUrls) {
            if (!url.startsWith("http")) {
                errorSb.append(url + "\n\r");
                continue;
            }
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, url, true, getPluginConfig().getBooleanProperty(UNLIMITEDMAXCON, false) == true ? 0 : -5);
            if (!dl.getConnection().isContentDisposition()) {
                br.followConnection();
                errorSb.append(br.toString() + "\n\r");
            } else {
                contentHeader = true;
                break;
            }
        }
        if (contentHeader) {
            /* filename workaround , MAYBE no longer needed because of api */
            String urlFileName = Plugin.getFileNameFromURL(new URL(br.getURL()));
            urlFileName = Encoding.htmlDecode(urlFileName);
            downloadLink.setFinalFileName(urlFileName);
            dl.startDownload();
        } else {
            logger.info("APIDebug:" + errorSb.toString());
            /* try website workaround */
            handlePremiumWebsite(downloadLink, account);
        }
    }

    private void prepareBrowser(Browser br) {
        if (br == null) return;
        br.setCookie("http://hotfile.com", "lang", "en");
        br.getHeaders().put("User-Agent", ua);
        br.getHeaders().put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        br.getHeaders().put("Accept-Language", "en-us,de;q=0.7,en;q=0.3");
        br.getHeaders().put("Pragma", null);
        br.getHeaders().put("Cache-Control", null);
    }

    private void submit(Browser br, Form form) throws Exception {
        br.getHeaders().setDominant(true);
        br.setCookie("http://hotfile.com", "lang", "en");
        br.getHeaders().put("User-Agent", ua);
        br.getHeaders().put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        br.getHeaders().put("Accept-Language", "en-us,de;q=0.7,en;q=0.3");
        br.getHeaders().put("Accept-Encoding", null);
        br.getHeaders().put("Accept-Charset", "ISO-8859-1,utf-8;q=0.7,*;q=0.7");
        br.getHeaders().put("Connection", "close");
        br.getHeaders().put("Referer", br.getHeaders().get("Referer"));
        br.getHeaders().put("Cookie", br.getHeaders().get("Cookie"));
        br.getHeaders().put("Content-Type", "application/x-www-form-urlencoded");
        br.submitForm(form);
        br.getHeaders().put("Content-Type", null);
        br.getHeaders().put("Accept-Encoding", "gzip");
    }

    @SuppressWarnings("unchecked")
    public void loginWebsite(Account account) throws IOException, PluginException {
        this.setBrowserExclusive();
        synchronized (LOCK) {
            br.setDebug(true);
            br.getHeaders().put("User-Agent", ua);
            br.setCookie("http://hotfile.com", "lang", "en");
            Object ret = account.getProperty("cookies", null);
            if (ret != null && ret instanceof HashMap<?, ?> && getPluginConfig().getBooleanProperty(TRY_IWL_BYPASS, false)) {
                logger.info("Use cookie login");
                /* use saved cookies */
                HashMap<String, String> cookies = (HashMap<String, String>) ret;
                for (String key : cookies.keySet()) {
                    br.setCookie("http://hotfile.com/", key, cookies.get(key));
                }
                br.setFollowRedirects(true);
                br.getPage("http://hotfile.com/");
                br.setFollowRedirects(false);
                String isPremium = br.getRegex("Account:.*?label.*?centerSide[^/]*?>(Premium)<").getMatch(0);
                if (isPremium == null) {
                    account.setProperty("cookies", null);
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
            } else {
                /* normal login */
                br.setFollowRedirects(true);
                br.getPage("http://hotfile.com/");
                br.postPage("http://hotfile.com/login.php", "returnto=%2F&user=" + Encoding.urlEncode(account.getUser()) + "&pass=" + Encoding.urlEncode(account.getPass()));
                Form form = br.getForm(0);
                if (form != null && form.containsHTML("<td>Username:")) {
                    account.setProperty("cookies", null);
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                if (br.getCookie("http://hotfile.com/", "auth") == null) {
                    account.setProperty("cookies", null);
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                String isPremium = br.getRegex("Account:.*?label.*?centerSide[^/]*?>(Premium)<").getMatch(0);
                if (isPremium == null) {
                    account.setProperty("cookies", null);
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                HashMap<String, String> cookies = new HashMap<String, String>();
                Cookies add = br.getCookies("http://hotfile.com/");
                for (Cookie c : add.getCookies()) {
                    cookies.put(c.getKey(), c.getValue());
                }
                account.setProperty("cookies", cookies);
                br.setFollowRedirects(false);
            }
        }
    }

    public void handlePremiumWebsite(DownloadLink downloadLink, Account account) throws Exception {
        loginWebsite(account);
        String finalUrl = null;
        br.getPage(downloadLink.getDownloadURL());
        if (br.getRedirectLocation() != null) {
            finalUrl = br.getRedirectLocation();
        } else {
            if (br.containsHTML("span>Free</span")) throw new PluginException(LinkStatus.ERROR_PREMIUM, "ISP blocked by Hotfile, Premium not possible", PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
            finalUrl = br.getRegex("<h3 style='margin-top: 20px'><a href=\"(.*?hotfile.*?)\">Click here to download</a></h3>").getMatch(0);
            if (finalUrl == null) finalUrl = br.getRegex("table id=\"download_file\".*?<a href=\"(.*?)\"").getMatch(0);/* polish */
        }
        br.setFollowRedirects(true);
        if (finalUrl == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        // Set the meximum connections per file
        boolean maxChunks = getPluginConfig().getBooleanProperty(UNLIMITEDMAXCON, false);
        int maxcon = -5;
        if (maxChunks) maxcon = 0;
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, finalUrl, true, maxcon);
        if (!dl.getConnection().isContentDisposition()) {
            br.followConnection();
            finalUrl = br.getRegex("<h3 style='margin-top: 20px'><a href=\"(.*?hotfile.*?)\">Click here to download</a></h3>").getMatch(0);
            if (finalUrl == null) finalUrl = br.getRegex("table id=\"download_file\".*?<a href=\"(.*?)\"").getMatch(0);/* polish */
            if (finalUrl == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, finalUrl, true, maxcon);
        }
        if (!dl.getConnection().isContentDisposition()) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        /* filename workaround */
        String urlFileName = Plugin.getFileNameFromURL(new URL(br.getURL()));
        urlFileName = Encoding.htmlDecode(urlFileName);
        downloadLink.setFinalFileName(urlFileName);
        dl.startDownload();
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink parameter) throws Exception {
        this.setBrowserExclusive();
        br.setDebug(true);
        /* workaround as server does not send correct encoding information */
        br.setCustomCharset("UTF-8");
        String lastDl = parameter.getDownloadURL().replaceFirst("http://.*?/", "/");
        br.setCookie("http://hotfile.com", "lastdl", Encoding.urlEncode(lastDl));
        prepareBrowser(br);
        br.getPage(parameter.getDownloadURL());
        Browser cl = new Browser();
        cl.setDebug(true);
        cl.setCookie("http://hotfile.com", "lastdl", br.getCookie("http://hotfile.com", "lastdl"));
        prepareBrowser(cl);
        cl.getHeaders().put("Referer", "http://hotfile.com/styles/structure.css");
        Browser.download(this.getLocalCaptchaFile(), cl.openGetConnection("http://hotfile.com/i/blank.gif"));
        if (br.getRedirectLocation() != null) br.getPage(br.getRedirectLocation());
        String filename = br.getRegex("Downloading <b>(.+?)</b>").getMatch(0);
        if (filename == null) {
            /* polish users get this */
            filename = br.getRegex("Downloading:</strong>(.*?)<").getMatch(0);
        }
        String filesize = br.getRegex("<span class=\"size\">(.*?)</span>").getMatch(0);
        if (filesize == null) {
            /* polish users get this */
            filesize = br.getRegex("Downloading:</strong>.*?span.*?strong>(.*?)<").getMatch(0);
        }
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        parameter.setName(filename.trim());
        parameter.setDownloadSize(Regex.getSize(filesize.trim()));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink link) throws Exception {
        /*
         * for free users we dont use api filecheck, cause we have to call
         * website anyway
         */
        requestFileInformation(link);
        if (br.containsHTML("You are currently downloading")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 5 * 60 * 1000l);
        if (br.containsHTML("starthtimer\\(\\)")) {
            String waittime = br.getRegex("starthtimer\\(\\).*?timerend=.*?\\+(\\d+);").getMatch(0);
            if (Long.parseLong(waittime.trim()) > 0) { throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, Long.parseLong(waittime.trim())); }
        }
        Form[] forms = br.getForms();
        Form form = forms[1];
        long sleeptime = 0;
        try {
            sleeptime = Long.parseLong(br.getRegex("timerend=d\\.getTime\\(\\)\\+(\\d+);").getMatch(0)) + 1;
            // for debugging purposes
            logger.info("Regexed waittime is " + sleeptime + " seconds");
        } catch (Exception e) {
            logger.info("WaittimeRegex broken");
            logger.info(br.toString());
            sleeptime = 60 * 1000l;
        }
        // Reconnect if the waittime is too big!
        if (sleeptime > 100 * 1000l) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, sleeptime);
        this.sleep(sleeptime, link);
        submit(br, form);
        // captcha
        if (!br.containsHTML("Click here to download")) {
            PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
            jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br);
            rc.parse();
            rc.load();
            File cf = rc.downloadCaptcha(getLocalCaptchaFile());
            String c = getCaptchaCode(cf, link);
            rc.setCode(c);
            if (!br.containsHTML("Click here to download")) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        }
        String dl_url = br.getRegex("<h3 style='margin-top: 20px'><a href=\"(.*?)\">Click here to download</a>").getMatch(0);
        if (dl_url == null) dl_url = br.getRegex("table id=\"download_file\".*?<a href=\"(.*?)\"").getMatch(0);/* polish */
        if (dl_url == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        br.setFollowRedirects(true);
        br.setDebug(true);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dl_url, false, 1);
        if (!dl.getConnection().isContentDisposition()) {
            br.followConnection();
            if (br.containsHTML("Invalid link")) {
                String newLink = br.getRegex("href=\"(http://.*?)\"").getMatch(0);
                if (newLink != null) {
                    /* set new downloadlink */
                    logger.warning("invalid link -> use new link");
                    link.setUrlDownload(newLink.trim());
                    throw new PluginException(LinkStatus.ERROR_RETRY);
                }
            }
            if (br.containsHTML("You are currently downloading")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 5 * 60 * 1000l);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        /* filename workaround */
        String urlFileName = Plugin.getFileNameFromURL(new URL(br.getURL()));
        urlFileName = Encoding.htmlDecode(urlFileName);
        link.setFinalFileName(urlFileName);
        dl.startDownload();
    }

    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("pl.hotfile.com", "hotfile.com"));
    }

    @Override
    public boolean checkLinks(DownloadLink[] urls) {
        if (urls == null || urls.length == 0) { return false; }
        try {
            StringBuilder sbIDS = new StringBuilder();
            StringBuilder sbKEYS = new StringBuilder();
            HashMap<String, String> params = new HashMap<String, String>();
            ArrayList<DownloadLink> links = new ArrayList<DownloadLink>();
            int index = 0;
            while (true) {
                links.clear();
                params.clear();
                while (true) {
                    if (index == urls.length || links.size() > 25) break;
                    links.add(urls[index]);
                    index++;
                }
                sbIDS.delete(0, sbIDS.capacity());
                sbKEYS.delete(0, sbKEYS.capacity());
                sbIDS.append("");
                sbKEYS.append("");
                int c = 0;
                for (DownloadLink dl : links) {
                    if (c > 0) {
                        sbIDS.append(",");
                        sbKEYS.append(",");
                    }
                    String id = new Regex(dl.getDownloadURL(), "/dl/(\\d+)/").getMatch(0);
                    String key = new Regex(dl.getDownloadURL(), "/dl/\\d+/([0-9a-zA-Z]+)").getMatch(0);
                    sbIDS.append(id);
                    sbKEYS.append(key);
                    c++;
                }
                /* we want id,status,name, size , md5 and sha1 info */
                params.put("fields", "id,status,name,size,md5,sha1");
                params.put("ids", sbIDS.toString());
                params.put("keys", sbKEYS.toString());
                HashMap<String, String> info = callAPI(null, "checklinks", null, params);
                String response = info.get("httpresponse");
                for (DownloadLink dl : links) {
                    String id = new Regex(dl.getDownloadURL(), "/dl/(\\d+)").getMatch(0);
                    String[] dat = new Regex(response, id + ",(\\d+),(.*?),(\\d+),(.*?),(.*?)(\n|$)").getRow(0);
                    if (dat != null) {
                        dl.setName(dat[1].trim());
                        dl.setDownloadSize(Long.parseLong(dat[2].trim()));
                        dl.setMD5Hash(dat[3].trim());
                        // SHA1 hashes seems to be wrong sometimes
                        // dl.setSha1Hash(dat[4].trim());
                        if ("1".equalsIgnoreCase(dat[0])) {
                            dl.setAvailable(true);
                        } else if ("0".equalsIgnoreCase(dat[0])) {
                            dl.setAvailable(false);
                        } else if ("2".equalsIgnoreCase(dat[0])) {
                            dl.setAvailable(true);
                            dl.getLinkStatus().setStatusText("HotLink");
                        } else {
                            dl.setAvailable(false);
                            dl.getLinkStatus().setStatusText("Unknown FileStatus " + dat[0]);
                        }
                    } else {
                        dl.setAvailable(false);
                    }
                }
                if (index == urls.length) break;
            }
        } catch (Throwable e) {
            return false;
        }
        return true;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}
