package org.dasein.cloud.azure;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by vmunthiu on 2/9/2015.
 */
public class IpUtils {

    public static class IpAddress {

        long ip;

        public IpAddress(long ip) {
            this.ip = ip;
        }

        public IpAddress(String dotted) {
            this.ip = fromDotted(dotted);
        }

        public static long fromDotted(String dotted) {
            String[] parts = dotted.split("\\.");
            long value =
                    Long.parseLong(parts[0]) << 24 |
                            Long.parseLong(parts[1]) << 16 |
                            Long.parseLong(parts[2]) << 8 |
                            Long.parseLong(parts[3]);
            return value & 0xFFFFFFFF;
        }

        public String toDotted() {
            return toDotted(this.ip);
        }

        public long toLong() {
            return this.ip;
        }

        public static String toDotted(long someip) {
            long a = (someip >> 24) & 0xFF;
            long b = (someip >> 16) & 0xFF;
            long c = (someip >> 8) & 0xFF;
            long d = someip & 0xFF;
            return String.format("%d.%d.%d.%d", a, b, c, d);
        }
    }


    public static class IpRange {

        IpAddress low, high;

        public IpRange(long low, long high) {
            this.low = new IpAddress(low);
            this.high = new IpAddress(high);
        }

        public IpRange(String low, String high) {
            this.low = new IpAddress(low);
            this.high = new IpAddress(high);
        }

        public IpRange(IpAddress low, IpAddress high) {
            this.low = low;
            this.high = high;
        }


        public IpRange(IpAddress ip, int significance) {
            long network = ip.toLong();
            long mask = (0xFFFFFFFFL << (32 - significance)) & 0xFFFFFFFFL;
            long broadcast = (0xFFFFFFFFL >> significance);

            long startIp = (network & mask) & 0xFFFFFFFF;
            long endIp = (network & mask) + broadcast;
            low = new IpAddress(startIp);
            high = new IpAddress(endIp);

        }

        public IpAddress getLow() {
            return low;
        }

        public IpAddress getHigh() {
            return high;
        }

        @Override
        public String toString() {
            return String.format("%s:%s", low.toDotted(), high.toDotted());
        }

        public static IpRange fromCidrString(String s) {
            String[] split = s.split("/");
            String ip = split[0];
            Integer significance = Integer.parseInt(split[1]);

            String[] quad = ip.split("\\.");
            String a = quad.length > 0 ? quad[0] : "0";
            String b = quad.length > 1 ? quad[1] : "0";
            String c = quad.length > 2 ? quad[2] : "0";
            String d = quad.length > 3 ? quad[3] : "0";
            IpAddress network = new IpAddress(String.format("%s.%s.%s.%s", a, b, c, d));
            return new IpRange(network, significance);
        }
    }

    public static class RangeToCidr {
        public static List<String> range2cidrlist( String startIp, String endIp ) {
            long start = ipToLong(startIp);
            long end = ipToLong(endIp);

            ArrayList<String> pairs = new ArrayList<String>();
            while ( end >= start ) {
                byte maxsize = 32;
                while ( maxsize > 0) {
                    long mask = CIDR2MASK[ maxsize -1 ];
                    long maskedBase = start & mask;

                    if ( maskedBase != start ) {
                        break;
                    }

                    maxsize--;
                }
                double x = Math.log( end - start + 1) / Math.log( 2 );
                byte maxdiff = (byte)( 32 - Math.floor( x ) );
                if ( maxsize < maxdiff) {
                    maxsize = maxdiff;
                }
                String ip = longToIP(start);
                pairs.add( ip + "/" + maxsize);
                start += Math.pow( 2, (32 - maxsize) );
            }
            return pairs;
        }

        public static final int[] CIDR2MASK = new int[] { 0x00000000, 0x80000000,
                0xC0000000, 0xE0000000, 0xF0000000, 0xF8000000, 0xFC000000,
                0xFE000000, 0xFF000000, 0xFF800000, 0xFFC00000, 0xFFE00000,
                0xFFF00000, 0xFFF80000, 0xFFFC0000, 0xFFFE0000, 0xFFFF0000,
                0xFFFF8000, 0xFFFFC000, 0xFFFFE000, 0xFFFFF000, 0xFFFFF800,
                0xFFFFFC00, 0xFFFFFE00, 0xFFFFFF00, 0xFFFFFF80, 0xFFFFFFC0,
                0xFFFFFFE0, 0xFFFFFFF0, 0xFFFFFFF8, 0xFFFFFFFC, 0xFFFFFFFE,
                0xFFFFFFFF };

        private static long ipToLong(String strIP) {
            long[] ip = new long[4];
            String[] ipSec = strIP.split("\\.");
            for (int k = 0; k < 4; k++) {
                ip[k] = Long.valueOf(ipSec[k]);
            }

            return (ip[0] << 24) + (ip[1] << 16) + (ip[2] << 8) + ip[3];
        }

        private static String longToIP(long longIP) {
            StringBuffer sb = new StringBuffer("");
            sb.append(String.valueOf(longIP >>> 24));
            sb.append(".");
            sb.append(String.valueOf((longIP & 0x00FFFFFF) >>> 16));
            sb.append(".");
            sb.append(String.valueOf((longIP & 0x0000FFFF) >>> 8));
            sb.append(".");
            sb.append(String.valueOf(longIP & 0x000000FF));

            return sb.toString();
        }
    }
}
