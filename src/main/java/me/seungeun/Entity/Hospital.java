package me.seungeun.Entity;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.List;

@Entity
@Table(name = "hospital")
public class Hospital {
    @Id
    private String placeId; // Google Place ID

    private String name;
    private String nameTranslated;

    private String address;
    private String addressTranslated;

    private String phone;
    private String weekday;

    private double lat;
    private double lng;

    @ElementCollection
    private List<String> vaccines;

    @ElementCollection
    private List<String> vaccinesTranslated;
}
