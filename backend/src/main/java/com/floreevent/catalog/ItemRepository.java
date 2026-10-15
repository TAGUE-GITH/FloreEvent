package com.floreevent.catalog;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface ItemRepository extends JpaRepository<Item, Long> {

	boolean existsByReference(String reference);

	boolean existsByReferenceAndIdNot(String reference, Long id);

	long countByCategoryId(Long categoryId);

	/**
	 * Recherche par nom ou reference, filtre facultatif par categorie. Aucun parametre n'est jamais null :
	 * PostgreSQL doit pouvoir deviner le type de chaque parametre (« % » pour tout, allCategories pour ignorer le filtre).
	 */
	@Query("""
			select i from Item i join fetch i.category
			where (lower(i.name) like :pattern or lower(i.reference) like :pattern)
			  and (:allCategories = true or i.category.id = :categoryId)
			order by i.category.name, i.name""")
	List<Item> search(@Param("pattern") String pattern, @Param("allCategories") boolean allCategories,
			@Param("categoryId") long categoryId);

	/** R2 : verrouille l'article jusqu'a la fin de la transaction (SELECT ... FOR UPDATE). */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select i from Item i where i.id = :id")
	Optional<Item> findByIdForUpdate(@Param("id") Long id);
}
