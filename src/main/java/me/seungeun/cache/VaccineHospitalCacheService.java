package me.seungeun.cache;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.seungeun.client.GoogleGeocodingClient;
import me.seungeun.dto.RegionInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.InputStream;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class VaccineHospitalCacheService {

    private final GoogleGeocodingClient googleGeocodingClient;
    private final RestTemplate restTemplate = new RestTemplate();
    private final Map<String, VaccineInfo> vaccineHospitalMap = new HashMap<>();
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${publicdata.service.key}")
    private String serviceKey;

    private static final Map<String, String> brtcCdMap = Map.of(
            "서울특별시", "1100000000",
            "부산광역시", "2600000000"
    );

    private static final Map<String, String> sggCdMap = Map.of(
            "종로구", "11110",
            "중구", "11140",
            "강남구", "11680"
    );

    private String normalize(String name) {
        return Optional.ofNullable(name)
                .orElse("")
                .toLowerCase()
                .replaceAll("\\s+", "")
                .replaceAll("[^가-힣a-z0-9]", "")
                .replaceAll("의원|병원|한의원|보건소", "");
    }

    @PostConstruct
    public void init() {
        initializeCache(37.5665, 126.9780); // 서울 광화문 기준 초기 캐시
    }

    public void initializeCache(double lat, double lng) {
        List<VaccineInfo> vaccineInfos = fetchHospitalsByLocation(lat, lng);
        vaccineHospitalMap.clear();
        vaccineInfos.forEach(info ->
                vaccineHospitalMap.put(normalize(info.getCenterName()), info));
        log.info("✅ 백신 병원 캐시 초기화 완료 - {}개 병원", vaccineHospitalMap.size());
    }

    public VaccineInfo getBestMatchingHospital(String hospitalName) {
        String normalizedName = normalize(hospitalName);
        return vaccineHospitalMap.entrySet().stream()
                .map(entry -> Map.entry(entry.getKey(), levenshtein(normalizedName, entry.getKey())))
                .filter(e -> e.getValue() <= 3)
                .min(Map.Entry.comparingByValue())
                .map(e -> vaccineHospitalMap.get(e.getKey()))
                .orElse(null);
    }

    public List<VaccineInfo> fetchHospitalsByLocation(double lat, double lng) {
        try {
            RegionInfo region = googleGeocodingClient.getRegionInfo(lat, lng);
            String brtcCd = brtcCdMap.get(region.getSi());
            String sggCd = sggCdMap.get(region.getGu());

            if (brtcCd == null || sggCd == null) {
                log.warn("⚠️ 행정코드 매핑 실패: {}, {}", region.getSi(), region.getGu());
                return List.of();
            }

            String url = UriComponentsBuilder.fromHttpUrl("https://apis.data.go.kr/1790387/orglist3/getOrgList3")
                    .queryParam("serviceKey", serviceKey)
                    .queryParam("numOfRows", 100)
                    .queryParam("pageNo", 1)
                    .queryParam("returnType", "json")
                    .queryParam("brtcCd", brtcCd)
                    .queryParam("sggCd", sggCd)
                    .toUriString();

            log.info("📡 공공데이터 API 호출 URL: {}", url);

            String responseStr = restTemplate.getForObject(url, String.class);
            if (responseStr != null && responseStr.trim().startsWith("<")) {
                log.error("❌ 응답이 XML 형식입니다. (API 키 또는 요청 파라미터 확인)");
                return loadMockVaccineInfo();
            }

            VaccineApiResponse parsed = objectMapper.readValue(responseStr, VaccineApiResponse.class);
            return parsed.getData();

        } catch (Exception e) {
            log.error("❌ 백신 병원 정보 조회 실패", e);
            return loadMockVaccineInfo();
        }
    }

    private List<VaccineInfo> loadMockVaccineInfo() {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("mock/vaccine_sample.json")) {
            if (inputStream == null) {
                log.warn("❗ mock/vaccine_sample.json 파일이 없습니다.");
                return List.of();
            }
            List<VaccineInfo> list = objectMapper.readValue(
                    inputStream,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, VaccineInfo.class)
            );
            log.info("📦 Mock 데이터 로딩 완료: {}개 병원", list.size());
            return list;
        } catch (Exception e) {
            log.error("❌ Mock 데이터 로딩 중 오류", e);
            return List.of();
        }
    }

    public static int levenshtein(String a, String b) {
        int[] dp = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) dp[j] = j;

        for (int i = 1; i <= a.length(); i++) {
            int prev = dp[0];
            dp[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                int temp = dp[j];
                dp[j] = Math.min(1 + Math.min(dp[j], dp[j - 1]),
                        a.charAt(i - 1) == b.charAt(j - 1) ? prev : prev + 1);
                prev = temp;
            }
        }
        return dp[b.length()];
    }

    // 응답 JSON 구조에 맞는 DTO
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class VaccineApiResponse {
        private List<VaccineInfo> data;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class VaccineInfo {
        private String centerName;
        private String address;
        private String tel;

        @JsonProperty("vaccine")
        private List<String> vaccines;
        private double lat;
        private double lng;
    }
}
