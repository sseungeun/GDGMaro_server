package me.seungeun.dto.googleplaces;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class GooglePlaceDetail {

    @JsonProperty("place_id")
    private String placeId;

    private String name;

    @JsonProperty("formatted_address")
    private String formatted_address;

    @JsonProperty("formatted_phone_number")
    private String formattedPhoneNumber;

    private Geometry geometry;

    @JsonProperty("opening_hours")
    private OpeningHours openingHours;

    @Data
    public static class OpeningHours {
        @JsonProperty("weekday_text")
        private List<String> weekdayText;
    }

    @Data
    public static class Geometry {
        private Location location;

        @Data
        public static class Location {
            private double lat;
            private double lng;
        }
    }
}
