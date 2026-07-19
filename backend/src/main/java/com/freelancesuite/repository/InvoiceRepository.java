package com.freelancesuite.repository;

import com.freelancesuite.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);

    @Query("SELECT i FROM Invoice i WHERE i.project.client.agency.id = :agencyId")
    List<Invoice> findByAgencyId(@Param("agencyId") Long agencyId);

    List<Invoice> findByIsRecurringTrue();
}
