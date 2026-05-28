package com.red404.util;

/**
 * HaversineUtil — Distance calculation & Zone bucketing (DSA: Grid Hashing)
 *
 * Zone System:
 *   Earth degrees ≈ 111 km per degree latitude.
 *   We bucket every ~5 km → bucket size = 5/111 ≈ 0.045 degrees.
 *   zone_key = "latBucket:lngBucket"
 *
 * This gives O(1) zone lookup and limits donor search to nearby grid cells.
 * We check the 3×3 neighbourhood (9 cells) covering ~15 km radius.
 */
public class HaversineUtil {

    private static final double EARTH_RADIUS_KM = 6371.0;
    private static final double BUCKET_SIZE_DEG = 0.045; // ≈ 5 km

    /** Haversine formula — returns distance in km */
    public static double distance(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                 + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                 * Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }

    /** Compute zone_key for a given coordinate */
    public static String zoneKey(double lat, double lon) {
        int latBucket = (int) Math.floor(lat / BUCKET_SIZE_DEG);
        int lonBucket = (int) Math.floor(lon / BUCKET_SIZE_DEG);
        return latBucket + ":" + lonBucket;
    }

    /**
     * Returns all 9 neighbouring zone keys (3×3 grid) including center.
     * Used by HospitalDAO to query donors in surrounding zones.
     */
    public static String[] neighbouringZones(double lat, double lon) {
        int latB = (int) Math.floor(lat / BUCKET_SIZE_DEG);
        int lonB = (int) Math.floor(lon / BUCKET_SIZE_DEG);
        String[] zones = new String[9];
        int idx = 0;
        for (int dLat = -1; dLat <= 1; dLat++) {
            for (int dLon = -1; dLon <= 1; dLon++) {
                zones[idx++] = (latB + dLat) + ":" + (lonB + dLon);
            }
        }
        return zones;
    }

    /** Quick check: is distance within threshold? */
    public static boolean isWithinRadius(double lat1, double lon1,
                                        double lat2, double lon2,
                                        double radiusKm) {
        return distance(lat1, lon1, lat2, lon2) <= radiusKm;
    }
}