package io.tightloop.spor;

public final class DistanceUtil {
    private static final long EARTH_RADIUS = 6_378_136L;

    private DistanceUtil() {
    }

    public static double distanceInMeters(double lat1, double lat2, double lng1, double lng2, double alt1, double alt2) {
        double latDistance = Math.toRadians(lat2 - lat1);
        double lngDistance = Math.toRadians(lng2 - lng1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lngDistance / 2) * Math.sin(lngDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = Math.pow(EARTH_RADIUS * c, 2) + Math.pow(alt1 - alt2, 2);

        return Math.sqrt(distance);
    }
}
