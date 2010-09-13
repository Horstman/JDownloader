package jd.controlling.reconnect.ipcheck;

public class IP {
    /**
     * validates the adress, and returns an IP instance or throws an exception
     * in case of validation errors
     * 
     * @param ip
     * @return
     * @throws IPCheckException
     */
    public static IP getInstance(final String ip) throws IPCheckException {
        if (ip.matches("^\\d+\\.\\d+\\.\\d+\\.\\d+$")) {
            final String parts[] = ip.split("\\.");
            if (parts.length == 4) {
                /* filter private networks */
                final int n1 = Integer.parseInt(parts[0]);
                /* 10.0.0.0-10.255.255.255 */
                if (n1 == 10) { throw new InvalidIPRangeException(ip); }
                final int n2 = Integer.parseInt(parts[1]);
                /* 192.168.0.0 - 192.168.255.255 */
                if (n1 == 192 && n2 == 168) { throw new InvalidIPRangeException(ip); }
                /* 172.16.0.0 - 172.31.255.255 */
                if (n1 == 172 && n2 >= 16 && n2 <= 31) { throw new InvalidIPRangeException(ip); }
                final int n3 = Integer.parseInt(parts[2]);
                final int n4 = Integer.parseInt(parts[3]);
                /* fritzbox sends 0.0.0.0 while its offline */
                if (n1 == 0 && n2 == 0 && n3 == 0 && n4 == 0) { throw new InvalidIPException(ip); }
                if (n1 >= 0 && n1 <= 255 && n2 >= 0 && n2 <= 255 && n3 >= 0 && n3 <= 255 && n4 >= 0 && n4 <= 255) {

                    if (!IPAddress.validateIP(ip)) { throw new ForbiddenIPException(ip); }
                    return new IP(ip);
                } else {
                    throw new InvalidIPException(ip);
                }
            }
        }
        throw new InvalidIPException(ip);

    }

    protected String ip = null;

    private IP(final String ip) {
        this.ip = ip;

    }

    public boolean equals(final Object c) {
        if (c != null && c instanceof IP) {
            final IP ip = (IP) c;

            return ip.ip.equals(this.ip);

        }
        return false;
    }

    public int hashCode() {
        return this.ip.hashCode();
    }

    public String toString() {
        return this.ip;
    }

}