package me.seungeun.Entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "vaccine_hospitals")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VaccineHospital {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String address;
    private String tel;
    private double lat;
    private double lng;

    @Column(length = 1000)
    private String vaccines;  // 예: 화이자,모더나,노바백스
}
