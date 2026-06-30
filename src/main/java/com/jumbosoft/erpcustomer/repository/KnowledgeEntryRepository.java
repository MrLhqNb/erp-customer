package com.jumbosoft.erpcustomer.repository;

import com.jumbosoft.erpcustomer.model.entity.KnowledgeEntry;
import com.jumbosoft.erpcustomer.model.enums.KnowledgeCategory;
import com.jumbosoft.erpcustomer.model.enums.KnowledgeStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface KnowledgeEntryRepository extends JpaRepository<KnowledgeEntry, Long>, JpaSpecificationExecutor<KnowledgeEntry> {

    List<KnowledgeEntry> findByStatus(KnowledgeStatus status);

    Page<KnowledgeEntry> findByStatus(KnowledgeStatus status, Pageable pageable);

    Page<KnowledgeEntry> findByCategoryAndStatus(KnowledgeCategory category, KnowledgeStatus status, Pageable pageable);

    @Query("SELECT k FROM KnowledgeEntry k WHERE k.status = 'ACTIVE' " +
           "AND (k.title LIKE %:keyword% OR k.tags LIKE %:keyword%)")
    Page<KnowledgeEntry> searchActive(@Param("keyword") String keyword, Pageable pageable);
}
