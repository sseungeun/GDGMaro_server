package me.seungeun.dto.googleplaces;

import lombok.Data;

@Data
public class GooglePlaceDetailResponse {
    private GooglePlaceDetail result;
    private String status;

}
