package me.seungeun.util;

import me.seungeun.cache.VaccineHospitalCacheService;

import java.util.Comparator;
import java.util.List;

public class HospitalNameMatcher {

    public static String normalize(String name) {
        if (name == null) return "";
        return name.toLowerCase().replaceAll("\\s+", "");
    }

    public static int levenshtein(String a, String b) {
        int[] costs = new int[b.length() + 1];
        for (int j = 0; j < costs.length; j++) costs[j] = j;
        for (int i = 1; i <= a.length(); i++) {
            costs[0] = i;
            int nw = i - 1;
            for (int j = 1; j <= b.length(); j++) {
                int cj = Math.min(1 + Math.min(costs[j], costs[j - 1]),
                        a.charAt(i - 1) == b.charAt(j - 1) ? nw : nw + 1);
                nw = costs[j];
                costs[j] = cj;
            }
        }
        return costs[b.length()];
    }

    public static VaccineHospitalCacheService.VaccineInfo findClosestMatch(String googleName, List<VaccineHospitalCacheService.VaccineInfo> candidates) {
        String normalizedGoogle = normalize(googleName);
        return candidates.stream()
                .min(Comparator.comparingInt(info ->
                        levenshtein(normalizedGoogle, normalize(info.getCenterName()))))
                .orElse(null);
    }
}

