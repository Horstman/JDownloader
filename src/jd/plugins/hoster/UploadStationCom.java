//jDownloader - Downloadmanager
//Copyright (C) 2010  JD-Team support@jdownloader.org
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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.RandomUserAgent;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "uploadstation.com" }, urls = { "http://(www\\.)?uploadstation\\.com/file/[A-Za-z0-9]+" }, flags = { 2 })
public class UploadStationCom extends PluginForHost {

    public UploadStationCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://uploadstation.com/premium.php");
    }

    @Override
    public String getAGBLink() {
        return "http://www.uploadstation.com/toc.php";
    }

    private static final String FILEOFFLINE = "(<h1>File not available</h1>|<b>The file could not be found\\. Please check the download link)";
    public static String        agent       = RandomUserAgent.generate();
    private boolean             ispremium   = false;

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.br.getHeaders().put("User-Agent", agent);
        this.checkLinks(new DownloadLink[] { link });
        if (!link.isAvailabilityStatusChecked()) {
            link.setAvailableStatus(AvailableStatus.UNCHECKABLE);
        } else if (!link.isAvailable()) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        return link.getAvailableStatus();
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        // Works like fileserve.com, they use the same scripts
        requestFileInformation(downloadLink);
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML(FILEOFFLINE)) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        this.handleErrors(br, downloadLink);
        final String fileId = this.br.getRegex("uploadstation\\.com/file/([a-zA-Z0-9]+)").getMatch(0);
        this.br.setFollowRedirects(false);
        // Not needed(yet)
        // String captchaJSPage =
        // this.br.getRegex("\"(/landing/[A-Za-z0-9]+/download_captcha\\.js\\?(\\d+)?)\"").getMatch(0);
        // if (captchaJSPage == null) {
        // logger.warning("captchaJSPage is null...");
        // throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        // }
        // captchaJSPage = "http://uploadstation.com" + captchaJSPage;
        final Browser br2 = this.br.cloneBrowser();
        br2.setCustomCharset("utf-8");
        // br2.getPage(captchaJSPage);
        br2.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        br2.postPage(downloadLink.getDownloadURL(), "checkDownload=check");
        if (!br2.containsHTML("success\":\"showCaptcha\"")) {
            handleCaptchaErrors(br2, downloadLink);
            handleErrors(br2, downloadLink);
            logger.info("There seems to be an error, no captcha is shown!");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        Boolean failed = true;
        for (int i = 0; i <= 10; i++) {
            final String id = this.br.getRegex("var reCAPTCHA_publickey=\\'(.*?)\\';").getMatch(0);
            if ((!br.containsHTML("api\\.recaptcha\\.net") && !br.containsHTML("\"javascript:Recaptcha\\.reload")) || id == null) {
                if (br.containsHTML("blogspot\\.com")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 5 * 60 * 1000l);
                handleCaptchaErrors(br2, downloadLink);
                logger.warning("id or fileId is null or the browser doesn't contain the reCaptcha text...");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final Form reCaptchaForm = new Form();
            reCaptchaForm.setMethod(Form.MethodType.POST);
            reCaptchaForm.setAction("http://www.uploadstation.com/checkReCaptcha.php");
            reCaptchaForm.put("recaptcha_shortencode_field", fileId);
            final PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
            final jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(this.br);
            rc.setForm(reCaptchaForm);
            rc.setId(id);
            rc.load();
            final File cf = rc.downloadCaptcha(this.getLocalCaptchaFile());
            final String c = this.getCaptchaCode(cf, downloadLink);
            if (c == null || c.length() == 0) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Recaptcha failed");
            rc.getForm().put("recaptcha_response_field", c);
            rc.getForm().put("recaptcha_challenge_field", rc.getChallenge());
            br2.submitForm(rc.getForm());
            if (br2.containsHTML("incorrect-captcha-sol")) {
                handleCaptchaErrors(br2, downloadLink);
                this.br.getPage(downloadLink.getDownloadURL());
                continue;
            }
            failed = false;
            break;
        }
        if (failed) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        handleCaptchaErrors(br2, downloadLink);
        handleErrors(br2, downloadLink);
        this.br.postPage(downloadLink.getDownloadURL(), "downloadLink=wait");
        // Ticket Time
        if (!this.br.getHttpConnection().isOK()) {
            logger.warning("The connection is not okay...");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        String reconTime = br.toString();
        int tt = 10;
        if (reconTime.length() < 500) {
            reconTime = new Regex(reconTime, ".*?(\\d+).*?").getMatch(0);
            logger.info("Waittime detected, waiting " + reconTime + " seconds from now on...");
            tt = Integer.parseInt(reconTime.trim());
        } else {
            logger.warning("Couldn't find dynamic waittime");
            logger.warning(br.toString());
        }
        this.sleep(tt * 1001, downloadLink);
        br2.postPage(downloadLink.getDownloadURL(), "downloadLink=show");
        this.br.postPage(downloadLink.getDownloadURL(), "download=normal");
        final String dllink = this.br.getRedirectLocation();
        if (dllink == null) {
            this.handleErrors(br, downloadLink);
            logger.warning("dllink is null...");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        this.dl = jd.plugins.BrowserAdapter.openDownload(this.br, downloadLink, dllink, false, 1);
        if (this.dl.getConnection().getResponseCode() == 404) {
            logger.info("got a 404 error...");
            this.br.followConnection();
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (this.dl.getConnection().getContentType().contains("html")) {
            logger.info("The finallink doesn't seem to be a file...");
            this.br.followConnection();
            this.handleErrors(br, downloadLink);
            logger.warning("Unexpected error at the last step...");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.setFilenameFix(true);
        this.dl.startDownload();
    }

    private void login(Account account) throws Exception {
        this.setBrowserExclusive();
        br.postPage("http://uploadstation.com/login.php", "loginUserName=" + Encoding.urlEncode(account.getUser()) + "&loginUserPassword=" + Encoding.urlEncode(account.getPass()) + "&autoLogin=on&recaptcha_response_field=&recaptcha_challenge_field=&recaptcha_shortencode_field=&loginFormSubmit=Login");
        if (br.getCookie("http://uploadstation.com/", "cookie") == null) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        try {
            login(account);
        } catch (PluginException e) {
            account.setValid(false);
            return ai;
        }
        br.getPage("http://uploadstation.com/dashboard.php");
        String filesUploaded = br.getRegex(">Files Uploaded</div>[\t\n\r ]+<div class=\"box_des\"><span>(\\d+) </span>").getMatch(0);
        if (filesUploaded != null) ai.setFilesNum(Integer.parseInt(filesUploaded));
        account.setValid(true);
        ai.setUnlimitedTraffic();
        String expire = br.getRegex("Expiry date: (\\d+\\-\\d+\\-\\d+)").getMatch(0);
        if (expire == null) {
            // ai.setExpired(true);
            this.ispremium = false;
            ai.setStatus("Free User");
            account.setMaxSimultanDownloads(1);
            account.setValid(true);
        } else {
            this.ispremium = true;
            account.setMaxSimultanDownloads(-1);
            ai.setValidUntil(TimeFormatter.getMilliSeconds(expire, "yyyy-MM-dd", null));
            ai.setStatus("Premium User");
        }
        return ai;
    }

    @Override
    public void handlePremium(DownloadLink link, Account account) throws Exception {
        requestFileInformation(link);
        login(account);

        if (this.ispremium) {
            br.setFollowRedirects(false);
            br.getPage(link.getDownloadURL());
            String dllink = br.getRedirectLocation();
            if (dllink == null) {
                if (br.containsHTML(FILEOFFLINE)) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                logger.warning("Final downloadlink (String is \"dllink\") regex didn't match!");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 0);
            if (dl.getConnection().getContentType().contains("html")) {
                logger.warning("The final dllink seems not to be a file!");
                br.followConnection();
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dl.startDownload();
        } else
            this.handleFree(link);
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    public boolean checkLinks(final DownloadLink[] urls) {
        // Works nearly 100% like the fileserve.com linkcheck
        if (urls == null || urls.length == 0) { return false; }
        try {
            Browser checkbr = new Browser();
            checkbr.setCustomCharset("utf-8");
            final ArrayList<DownloadLink> links = new ArrayList<DownloadLink>();
            int index = 0;
            final StringBuilder sb = new StringBuilder();
            while (true) {
                sb.delete(0, sb.capacity());
                sb.append("urls=");
                links.clear();
                while (true) {
                    /*
                     * we test 100 links at once - its tested with 500 links,
                     * probably we could test even more at the same time...
                     */
                    if (index == urls.length || links.size() > 100) {
                        break;
                    }
                    links.add(urls[index]);
                    index++;
                }
                int c = 0;
                for (final DownloadLink dl : links) {
                    /*
                     * append fake filename, because api will not report
                     * anything else
                     */
                    if (c > 0) {
                        sb.append("%0D%0A");
                    }
                    sb.append(Encoding.urlEncode(dl.getDownloadURL()));
                    c++;
                }
                checkbr.postPage("http://www.uploadstation.com/check-links.php", sb.toString());
                for (final DownloadLink dl : links) {
                    final String linkpart = new Regex(dl.getDownloadURL(), "(uploadstation\\.com/file/.+)").getMatch(0);
                    if (linkpart == null) {
                        logger.warning("Uploadstation availablecheck is broken!");
                        return false;
                    }
                    final String regexForThisLink = "(<td>http://(www\\.)" + linkpart + "([\r\n\t]+)?</td>[\r\n\t ]+<td>.*?</td>[\r\n\t ]+<td>.*?</td>[\r\n\t ]+<td>(Available|Not available)(\\&nbsp;)?(<img|</td>))";
                    final String theData = checkbr.getRegex(regexForThisLink).getMatch(0);
                    if (theData == null) {
                        logger.warning("Uploadstation availablecheck is broken!");
                        return false;
                    }
                    final Regex linkinformation = new Regex(theData, "<td>http://(www\\.)?" + linkpart + "([\r\n\t]+)?</td>[\r\n\t ]+<td>(.*?)</td>[\r\n\t ]+<td>(.*?)</td>[\r\n\t ]+<td>(Available|Not available)(\\&nbsp;)?(<img|</td>)");
                    final String status = linkinformation.getMatch(4);
                    String filename = linkinformation.getMatch(2);
                    String filesize = linkinformation.getMatch(3);
                    if (filename == null || filesize == null) {
                        logger.warning("Uploadstation availablecheck is broken!");
                        dl.setAvailable(false);
                    } else if (!status.equals("Available") || filename.equals("--") || filesize.equals("--")) {
                        filename = linkpart;
                        dl.setAvailable(false);
                    } else {
                        dl.setAvailable(true);
                    }
                    dl.setName(filename);
                    if (filesize != null) {
                        if (filesize.contains(",") && filesize.contains(".")) {
                            /* workaround for 1.000,00 MB bug */
                            filesize = filesize.replaceFirst("\\.", "");
                        }
                        dl.setDownloadSize(SizeFormatter.getSize(filesize));
                    }
                }
                if (index == urls.length) {
                    break;
                }
            }
        } catch (final Exception e) {
            return false;
        }
        return true;
    }

    private void handleErrors(Browser br2, DownloadLink link) throws PluginException, IOException {
        logger.info("Handling errors...");
        boolean limitReached = false;
        if (br2.containsHTML("\"fail\":\"timeLimit\"")) {
            br2.postPage(link.getDownloadURL(), "checkDownload=showError&errorType=timeLimit");
            limitReached = true;
        }
        String waittime = br2.getRegex("<h1>You need to wait (\\d+) seconds to download next file").getMatch(0);
        if (waittime == null && limitReached) waittime = "300";
        if (waittime != null) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, Integer.parseInt(waittime) * 1001l);
        if (br.containsHTML("To remove download restriction, please choose your suitable plan as below</h1>")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 30 * 60 * 1000l);
    }

    private void handleCaptchaErrors(Browser br2, DownloadLink downloadLink) throws IOException, PluginException {
        // Handles captcha errors and additionsl limits
        logger.info("Checking captcha errors...");
        if (br.containsHTML("No htmlCode read")) {
            logger.info("Unexpected captcha error happened");
            throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        }
        String fail = br2.getRegex("\"(fail|error)\":\"(.*?)\"").getMatch(1);
        String waittime = br2.getRegex("\"(waitTime|msg)\":(\\d+)").getMatch(1);
        if (fail != null && waittime != null) {
            if (fail.equals("captcha-fail") || fail.equals("captchaFail")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Too many wrong captcha attempts!", 10 * 60 * 1000l);
            br2.postPage(downloadLink.getDownloadURL(), "checkDownload=showError&errorType=" + fail + "&waitTime=" + waittime);
            // Just an additional check
            if (br2.containsHTML("Please retry later\\.<") || br2.containsHTML(">Your IP has failed the captcha too many times")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Too many wrong captcha attempts!", 10 * 60 * 1000l);
        }
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}