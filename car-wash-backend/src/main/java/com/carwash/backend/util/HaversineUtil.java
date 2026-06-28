package com.carwash.backend.util;

/**
 * Pure-Java Haversine distance calculator.
 *
 * <p>Used by the valet geofencing feature to determine whether a customer's
 * pick-up location falls within the configurable service radius of a shop branch.
 *
 * <p>Formula reference: https://en.wikipedia.org/wiki/Haversine_formula
 */
public final class HaversineUtil {

    /** Earth's mean radius in kilometres. */
    private static final double EARTH_RADIUS_KM = 6371.0;

    private HaversineUtil() {
        // Utility class — do not instantiate.
    }

    /**
     * Calculates the great-circle distance between two points on the Earth's
     * surface using the Haversine formula.
     *
     * @param lat1 latitude of point 1 in decimal degrees
     * @param lon1 longitude of point 1 in decimal degrees
     * @param lat2 latitude of point 2 in decimal degrees
     * @param lon2 longitude of point 2 in decimal degrees
     * @return distance in kilometres
     */
    public static double distanceKm(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS_KM * c;
    }
}

