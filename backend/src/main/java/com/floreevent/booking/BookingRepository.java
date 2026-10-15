package com.floreevent.booking;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookingRepository extends JpaRepository<Booking, Long> {

	/**
	 * Exemplaires d'un article immobilises par les reservations dont la periode bloquee chevauche [from, to].
	 * La periode bloquee va de la veille du debut (preparation) au lendemain de la fin (nettoyage) : on compare donc
	 * le debut avec to + 1 jour (dayAfterTo) et la fin avec from - 1 jour (dayBeforeFrom).
	 */
	@Query("""
			select coalesce(sum(l.quantity), 0) from BookingLine l join l.booking b
			where l.item.id = :itemId and b.status in :statuses and b.id <> :excludedId
			  and b.startDate <= :dayAfterTo and b.endDate >= :dayBeforeFrom""")
	int reservedQuantity(@Param("itemId") Long itemId, @Param("statuses") Collection<BookingStatus> statuses,
			@Param("dayBeforeFrom") LocalDate dayBeforeFrom, @Param("dayAfterTo") LocalDate dayAfterTo,
			@Param("excludedId") long excludedId);

	List<Booking> findByCustomerIdOrderByCreatedAtDescIdDesc(Long customerId);

	@Query("select b from Booking b join fetch b.customer order by b.createdAt desc, b.id desc")
	List<Booking> findAllForTeam();
}
