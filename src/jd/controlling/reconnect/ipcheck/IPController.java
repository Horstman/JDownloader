package jd.controlling.reconnect.ipcheck;

import java.util.ArrayList;

import jd.config.Configuration;
import jd.config.SubConfiguration;
import jd.controlling.JDLogger;
import jd.controlling.reconnect.ReconnectPluginController;

import com.sun.istack.internal.Nullable;

public class IPController extends ArrayList<IPConnectionState> {
    /**
     * 
     */
    private static final long         serialVersionUID = -6856149094542337379L;
    private static final IPController INSTANCE         = new IPController();

    public static IPController getInstance() {
        return IPController.INSTANCE;
    }

    /**
     * true, if the current ip has no use. we need a new one
     */
    private boolean                          invalidated  = false;
    private IPConnectionState                latestConnectionState;
    private Object                           LOCK         = new Object();
    /**
     * blacklist for not working ip check providers
     */
    private final ArrayList<IPCheckProvider> badProviders = new ArrayList<IPCheckProvider>();
    private IPConnectionState                invalidState = null;

    private IPController() {
    }

    /**
     * forces the class to get a new IP
     * 
     * @return
     */
    public IP fetchIP() {
        IPConnectionState newIP = null;
        IPCheckProvider icp = null;
        while (true) {
            try {
                icp = this.getIPCheckProvider();
                newIP = new IPConnectionState(icp.getExternalIP());
                break;
            } catch (final InvalidProviderException e) {
                // IP check provider is bad.
                this.badProviders.add(icp);
            } catch (final IPCheckException e) {
                JDLogger.getLogger().info(e.getMessage());
                newIP = new IPConnectionState(e);
                break;
            }
        }
        add(newIP);
        return this.latestConnectionState.getExternalIp();
    }

    @Override
    public boolean add(IPConnectionState state) {
        if (state == null) return false;
        synchronized (LOCK) {
            if (this.size() > 0) {
                final IPConnectionState entry = this.get(this.size() - 1);
                /* new and current state are equal */
                if (entry.equalsLog(state)) return false;
            }
            /* new IPConnectionState reached */
            this.latestConnectionState = state;
            return super.add(state);
        }
    }

    public boolean changedIP() {
        if (invalidState == null || invalidState.isOffline()) return false;
        /* we dont have any previous states, we cannot check if ip changed */
        if (this.size() == 0) return false;
        synchronized (LOCK) {
            /* fetch current ip */
            fetchIP();
            IPConnectionState current = latestConnectionState;
            /* currently offline = no ip change */
            if (current.isOffline()) return false;
            for (int index = this.size() - 1; index >= 0; index--) {
                if (this.get(index) == invalidState) {
                    /*
                     * we reached the element we began with, so check changed IP
                     * and then stop
                     */
                    if (!invalidState.equalsLog(current)) return true;
                    return false;
                }
                /*
                 * we found a state that was online and had different ip that
                 * new state, so ip changed
                 */
                if (this.get(index).isOnline() && !this.get(index).equalsLog(current)) return true;
            }
        }
        return false;
    }

    /**
     * returns the latest log ebtry, or fetches the ip to get one
     * 
     * @return
     */
    private synchronized IPConnectionState getCurrentLog() {
        if (this.latestConnectionState == null) {
            this.fetchIP();
        }
        return this.latestConnectionState;
    }

    /**
     * Returns the current external IP. fetches new ip if if is marked as
     * invalid, or is null
     * 
     * 
     * @return
     */
    @Nullable
    public IP getIP() {
        return this.getCurrentLog().getExternalIp();
    }

    /**
     * finds the best ipcheck provider
     * 
     * @return
     */
    private IPCheckProvider getIPCheckProvider() {
        IPCheckProvider p = ReconnectPluginController.getInstance().getActivePlugin().getIPCheckProvider();
        if (p == null || this.badProviders.contains(p)) {
            if (SubConfiguration.getConfig("DOWNLOAD").getBooleanProperty(Configuration.PARAM_GLOBAL_IP_BALANCE, true)) {
                p = BalancedWebIPCheck.getInstance();
            } else {
                p = CustomWebIpCheck.getInstance();
            }
        }
        return p;
    }

    /**
     * Tells the IpController, that the current ip is "BAD". We need a new one<br>
     * 
     * @see #validate()
     * @see #validate(int, int)
     */
    public void invalidate() {
        if (this.invalidated == true) return;
        this.invalidated = true;
        invalidState = this.getCurrentLog();
    }

    /**
     * returns true if the current ip is "Bad" and we need a new one
     * 
     * @return
     */
    public boolean isInvalidated() {
        return this.invalidated;
    }

    /**
     * gets the latest connection state and validates if we have a new ip.<br>
     * 
     * @see #validate(int, int) for more details.<br>
     * 
     *      This method only does one single Check.
     */
    public boolean validate() {
        if (!this.invalidated) { return true; }
        if (SubConfiguration.getConfig("DOWNLOAD").getBooleanProperty(Configuration.PARAM_GLOBAL_IP_DISABLE, false)) {
            // IP check disabled. each validate request is successfull
            return !(this.invalidated = false);
        }
        return !(this.invalidated = !changedIP());
    }

    /**
     * check for max waitForIPTime seconds in an interval of ipCheckInterval if
     * the ip is valid.<br>
     * Call {@link #invalidate()} to invalidate the current state. AFterwards a
     * reconnect can get a new ip.<br>
     * this method gets the new connectionstate and validates it
     * 
     * 
     * @param waitForIPTime
     * @param ipCheckInterval
     * @return
     * @throws InterruptedException
     */
    public boolean validate(final int waitForIPTime, final int ipCheckInterval) throws InterruptedException {
        if (!this.invalidated) { return true; }
        if (SubConfiguration.getConfig("DOWNLOAD").getBooleanProperty(Configuration.PARAM_GLOBAL_IP_DISABLE, false)) {
            Thread.sleep(waitForIPTime);
            // IP check disabled. each validate request is successfull
            return !(this.invalidated = false);
        }
        final long endTime = System.currentTimeMillis() + waitForIPTime * 1000;
        while (System.currentTimeMillis() < endTime) {
            /* ip change detected then we can stop */
            if (!(this.invalidated = !changedIP())) { return true; }
            if (this.latestConnectionState.getCause() != null) {
                try {
                    throw this.latestConnectionState.getCause();
                } catch (final ForbiddenIPException e) {
                    // forbidden IP.. no need to wait
                    return !(this.invalidated = true);

                } catch (final Throwable e) {
                    // nothing
                }
            }
            Thread.sleep(Math.min(1, ipCheckInterval) * 1000);
        }
        return !this.isInvalidated();
    }
}
