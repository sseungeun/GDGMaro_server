package me.seungeun.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.seungeun.cache.VaccineHospitalCacheService;
import me.seungeun.cache.VaccineHospitalCacheService.VaccineInfo;
import me.seungeun.dto.HospitalDto;
import me.seungeun.dto.googleplaces.GooglePlace;
import me.seungeun.dto.googleplaces.GooglePlaceDetail;
import me.seungeun.dto.googleplaces.GooglePlacesResponse;
import me.seungeun.dto.googleplaces.GooglePlaceDetailResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class GooglePlaceClient {

    // Google API key injected from application properties
    @Value("${google.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate;
    private final VaccineHospitalCacheService vaccineHospitalCacheService;

    // Mapping of some English hospital names to their Korean equivalents for matching with cached data
    private static final Map<String, String> hospitalNameMap = Map.ofEntries(
            Map.entry("Severance Hospital", "연세대학교 세브란스병원"),
            Map.entry("Seoul Red Cross Hospital", "서울적십자병원"),
            Map.entry("Seoul National University Dental Hospital", "서울대학교치과병원"),
            Map.entry("Samsung Medical Center", "삼성서울병원"),
            Map.entry("Asan Medical Center", "서울아산병원"),
            Map.entry("Kangbuk Samsung Hospital", "강북삼성병원")
    );

    /**
     * Retrieves a list of nearby hospitals around the specified latitude and longitude
     * using the Google Places API. For each hospital, fetches detailed information
     * and filters only those with available vaccine information.
     *
     * @param lat Latitude coordinate
     * @param lng Longitude coordinate
     * @return List of HospitalDto containing hospital and vaccine info
     */
    public List<HospitalDto> getNearbyHospitals(double lat, double lng) {

        // Build the Google Places Nearby Search API URL
        String url = UriComponentsBuilder.fromHttpUrl("https://maps.googleapis.com/maps/api/place/nearbysearch/json")
                .queryParam("location", lat + "," + lng)
                .queryParam("radius", 5000)  // Search radius in meters
                .queryParam("type", "hospital")
                .queryParam("key", apiKey)
                .queryParam("language", "ko")
                .toUriString();

        // Fetch hospital search results from Google Places API
        GooglePlacesResponse response = restTemplate.getForObject(url, GooglePlacesResponse.class);

        if (response == null || response.getResults() == null) {
            throw new RuntimeException("No nearby hospitals found.");
        }

        // For each hospital, fetch detailed info and filter those with vaccine data
        return response.getResults().stream()
                .map(place -> {
                    try {
                        return getPlaceDetails(place.getPlace_id());
                    } catch (Exception e) {
                        log.warn("Failed to fetch details for placeId {}: {}", place.getPlace_id(), e.getMessage());
                        return null;
                    }
                })
                .filter(hospital -> hospital != null && hospital.getVaccines() != null && !hospital.getVaccines().isEmpty())
                .collect(Collectors.toList());
    }

    /**
     * Retrieves detailed information for a specific hospital place by placeId
     * using the Google Places Details API.
     *
     * @param placeId Google Place ID
     * @return HospitalDto containing detailed hospital information
     */
    public HospitalDto getPlaceDetails(String placeId) {

        // Build Place Details API URL
        String url = UriComponentsBuilder.fromHttpUrl("https://maps.googleapis.com/maps/api/place/details/json")
                .queryParam("place_id", placeId)
                .queryParam("key", apiKey)
                .queryParam("language", "ko")
                .queryParam("fields", "place_id,name,formatted_address,formatted_phone_number,geometry,opening_hours")
                .toUriString();



        try {
            GooglePlaceDetailResponse response = restTemplate.getForObject(url, GooglePlaceDetailResponse.class);

            if (response == null || response.getResult() == null) {
                throw new RuntimeException("Hospital details not found.");
            }

            return convertToHospitalDto(response.getResult());
        } catch (Exception e) {
            throw new RuntimeException("Error fetching hospital details", e);
        }
    }

    // Normalize a name by converting to lowercase and removing whitespace
    private String normalize(String name) {
        if (name == null) return "";
        return name.toLowerCase().replaceAll("\\s+", "");
    }

    /**
     * Converts GooglePlace DTO to internal HospitalDto,
     * including mapping hospital names to Korean for matching cached vaccine data,
     * and extracting relevant fields.
     *
     * @param place GooglePlace object from API
     * @return HospitalDto containing mapped hospital and vaccine info
     */
    private HospitalDto convertToHospitalDto(GooglePlaceDetail place) {
        String mappedName = hospitalNameMap.getOrDefault(place.getName(), place.getName());
        mappedName = normalize(mappedName);

        VaccineInfo cached = vaccineHospitalCacheService.getBestMatchingHospital(mappedName);

        List<String> vaccines;
        if (cached != null && cached.getVaccines() != null) {
            vaccines = cached.getVaccines();
        } else {
            vaccines = List.of("No vaccine information available");
        }

        log.info("Vaccine info for hospital {}: {}", place.getName(), vaccines);

        String weekdayText = null;
        if (place.getOpeningHours() != null && place.getOpeningHours().getWeekdayText() != null) {
            weekdayText = String.join(", ", place.getOpeningHours().getWeekdayText());
        }



        return HospitalDto.builder()
                .placeId(place.getPlaceId())
                .name(place.getName())
                .address(place.getFormatted_address())
                .phone(place.getFormattedPhoneNumber())
                .lat(place.getGeometry().getLocation().getLat())
                .lng(place.getGeometry().getLocation().getLng())
                .weekday(weekdayText)
                .vaccines(vaccines)
                .build();
    }
}
