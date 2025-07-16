package me.seungeun.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.seungeun.cache.VaccineHospitalCacheService;
import me.seungeun.cache.VaccineHospitalCacheService.VaccineInfo;
import me.seungeun.client.GooglePlaceClient;
import me.seungeun.dto.HospitalDto;
import me.seungeun.dto.PlaceIdRequestDto;
import me.seungeun.dto.publicdata.LocationRequestDto;
import me.seungeun.util.HospitalNameMatcher;
import me.seungeun.util.HospitalTranslator;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class HospitalSearchService {

    private final GooglePlaceClient googlePlaceClient;
    private final VaccineHospitalCacheService vaccineHospitalCacheService;
    private final HospitalTranslator hospitalTranslator;

    public List<HospitalDto> findNearbyHospitals(LocationRequestDto request) {
        return findNearbyHospitalsWithVaccines(request.getLat(), request.getLng(), request.getLanguage());
    }

    public List<HospitalDto> findTranslatedNearbyHospitals(LocationRequestDto request, String targetLang) {
        return findNearbyHospitalsWithVaccines(request.getLat(), request.getLng(), targetLang);
    }

    private List<HospitalDto> findNearbyHospitalsWithVaccines(double lat, double lng, String lang) {
        List<HospitalDto> googleHospitals = googlePlaceClient.getNearbyHospitals(lat, lng);
        List<VaccineInfo> vaccineHospitals = vaccineHospitalCacheService.fetchHospitalsByLocation(lat, lng);

        return googleHospitals.stream()
                .map(h -> mergeVaccineInfo(h, vaccineHospitals, lang))
                .limit(10)
                .collect(Collectors.toList());
    }

    private HospitalDto mergeVaccineInfo(HospitalDto hospital, List<VaccineInfo> vaccineHospitals, String lang) {
        try {
            VaccineInfo match = HospitalNameMatcher.findClosestMatch(hospital.getName(), vaccineHospitals);

            if (match != null && match.getVaccines() != null && !match.getVaccines().isEmpty()) {
                hospital.setVaccines(hospitalTranslator.translateVaccines(match.getVaccines(), lang));
            } else {
                hospital.setVaccines(List.of(hospitalTranslator.translate("병원에 문의해주세요", lang)));
            }

            Map<String, String> translated = hospitalTranslator.translateHospital(hospital, lang);
            return HospitalDto.from(hospital, translated);

        } catch (Exception e) {
            log.error("Error merging vaccine info for hospital: {}", hospital.getName(), e);
            return HospitalDto.from(hospital, Map.of("name", hospital.getName()));
        }
    }

    public HospitalDto getHospitalDetailByPlaceId(PlaceIdRequestDto request) {
        HospitalDto detail = googlePlaceClient.getPlaceDetails(request.getPlaceId());
        VaccineInfo cached = vaccineHospitalCacheService.getBestMatchingHospital(detail.getName());

        if (cached != null && cached.getVaccines() != null) {
            detail.setVaccines(hospitalTranslator.translateVaccines(cached.getVaccines(), request.getLanguage()));
        } else {
            detail.setVaccines(List.of(hospitalTranslator.translate("병원에 문의해주세요", request.getLanguage())));
        }

        return detail;
    }

    public HospitalDto getTranslatedHospitalDetail(PlaceIdRequestDto request, String lang) {
        HospitalDto detail = googlePlaceClient.getPlaceDetails(request.getPlaceId());

        try {
            double lat = detail.getLat();
            double lng = detail.getLng();

            // ✅ nearby와 동일하게 근처 병원 기반 캐시 목록 가져오기
            List<VaccineInfo> vaccineHospitals = vaccineHospitalCacheService.fetchHospitalsByLocation(lat, lng);

            // ✅ 유사도 기반 이름 매칭
            VaccineInfo matched = HospitalNameMatcher.findClosestMatch(detail.getName(), vaccineHospitals);

            if (matched != null && matched.getVaccines() != null && !matched.getVaccines().isEmpty()) {
                detail.setVaccines(hospitalTranslator.translateVaccines(matched.getVaccines(), lang));
            } else {
                detail.setVaccines(List.of(hospitalTranslator.translate("병원에 문의해주세요", lang)));
            }

            Map<String, String> translated = hospitalTranslator.translateHospital(detail, lang);
            return HospitalDto.from(detail, translated);

        } catch (Exception e) {
            log.error("Hospital detailed translation failed: {}", detail.getName(), e);
            return HospitalDto.from(detail, Map.of("name", detail.getName()));
        }
    }
}
