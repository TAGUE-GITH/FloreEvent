package com.floreevent.prestation;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PrestationRepository extends JpaRepository<Prestation, Long> {

	List<Prestation> findAllByOrderByNameAsc();

	List<Prestation> findByActiveTrueOrderByNameAsc();

	boolean existsByNameIgnoreCase(String name);

	boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
}
