package com.voyanta.destination.dao.repository;


import com.voyanta.destination.dao.entity.Destination;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA Repository for the Destination entity. Besides the standard CRUD
 * methods it loads only the destinations marked as featured, which are shown to visitors
 * and used as the fallback list for recommendations.
 */
public interface DestinationRepository extends JpaRepository<Destination, UUID> {
    List<Destination> findByFeaturedTrue();
    java.util.Optional<Destination> findFirstByNameIgnoreCaseOrderByCreatedAtAsc(String name);
}