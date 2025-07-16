package me.seungeun.repository;

import me.seungeun.Entity.Hospital;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HospitalRepository extends JpaRepository<Hospital, String> {
    List<Hospital> findByLatBetweenAndLngBetween(double latMin, double latMax, double lngMin, double lngMax);
}

