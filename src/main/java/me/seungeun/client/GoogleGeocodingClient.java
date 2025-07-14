package me.seungeun.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.seungeun.dto.RegionInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Component
@RequiredArgsConstructor
public class GoogleGeocodingClient {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${google.api.key}")
    private String apiKey;

    public RegionInfo getRegionInfo(double lat, double lng) {
        try {
            String url = UriComponentsBuilder
                    .fromHttpUrl("https://maps.googleapis.com/maps/api/geocode/json")
                    .queryParam("latlng", lat + "," + lng)
                    .queryParam("key", apiKey)
                    .toUriString();

            String response = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(response);
            JsonNode results = root.path("results");

            String si = null;
            String gu = null;

            for (JsonNode result : results) {
                for (JsonNode component : result.path("address_components")) {
                    JsonNode types = component.path("types");

                    if (types.isArray()) {
                        for (JsonNode type : types) {
                            if ("administrative_area_level_1".equals(type.asText())) {
                                si = component.path("long_name").asText();
                            }
                            if ("administrative_area_level_2".equals(type.asText())) {
                                gu = component.path("long_name").asText();
                            }
                        }
                    }
                }

                if (si != null && gu != null) break;
            }

            log.info("Resolved address: 시={}, 구={}", si, gu);
            return new RegionInfo(si, gu);

        } catch (Exception e) {
            log.error("Failed to get region info from Geocoding API", e);
            return new RegionInfo(null, null);
        }
    }
}
