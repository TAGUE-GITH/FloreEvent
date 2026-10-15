package com.floreevent.catalog;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MaintenanceRepository extends JpaRepository<Maintenance, Long> {

	/**
	 * Exemplaires d'un article en reparation sur au moins un jour de [from, to]. Deux periodes se chevauchent
	 * quand chacune commence avant la fin de l'autre ; une reparation sans fin dure jusqu'a nouvel ordre.
	 */
	@Query("""
			select coalesce(sum(m.quantity), 0) from Maintenance m
			where m.item.id = :itemId and m.fromDate <= :to and (m.toDate is null or m.toDate >= :from)""")
	int quantityInRepair(@Param("itemId") Long itemId, @Param("from") LocalDate from, @Param("to") LocalDate to);

	/** Reparations en cours ou prevues (pas encore terminees), les plus anciennes d'abord. */
	@Query("""
			select m from Maintenance m join fetch m.item
			where m.toDate is null or m.toDate >= :today
			order by m.fromDate, m.id""")
	List<Maintenance> findOpen(@Param("today") LocalDate today);

	List<Maintenance> findByItemIdOrderByFromDateDescIdDesc(Long itemId);
}
