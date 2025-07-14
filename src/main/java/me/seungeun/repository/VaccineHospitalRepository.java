package me.seungeun.repository;

import me.seungeun.Entity.VaccineHospital;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VaccineHospitalRepository extends JpaRepository<VaccineHospital, Long> {
    List<VaccineHospital> findByNameContaining(String keyword);
}

