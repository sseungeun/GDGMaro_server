package me.seungeun.util;

import lombok.RequiredArgsConstructor;
import me.seungeun.client.TranslateClient;
import me.seungeun.dto.HospitalDto;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HospitalTranslator {

    private final TranslateClient translateClient;

    public List<String> translateVaccines(List<String> vaccines, String lang) {
        if (vaccines == null || vaccines.isEmpty()) return List.of();
        return vaccines.stream()
                .map(v -> translateClient.translate(v, lang))
                .collect(Collectors.toList());
    }

    public Map<String, String> translateHospital(HospitalDto dto, String lang) {
        return translateClient.translateAll(dto, lang);
    }

    public String translate(String text, String lang) {
        return translateClient.translate(text, lang);
    }

}

