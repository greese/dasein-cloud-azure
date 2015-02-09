package org.dasein.cloud.azure;

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
}
