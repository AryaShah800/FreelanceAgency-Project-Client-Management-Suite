package com.freelancesuite.service;

import com.freelancesuite.dto.PublishProposalRequest;
import com.freelancesuite.dto.ProposalListItemDto;
import com.freelancesuite.entity.Agency;
import com.freelancesuite.entity.Proposal;
import com.freelancesuite.repository.AgencyRepository;
import com.freelancesuite.repository.ProposalRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.List;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;

@Service
public class ProposalService {

    private final ProposalRepository proposalRepository;
    private final AgencyRepository agencyRepository;

    public ProposalService(ProposalRepository proposalRepository, AgencyRepository agencyRepository) {
        this.proposalRepository = proposalRepository;
        this.agencyRepository = agencyRepository;
    }

    @Transactional
    public Proposal publishProposal(PublishProposalRequest req, Long agencyId) {
        Agency agency = agencyRepository.findById(agencyId)
                .orElseThrow(() -> new IllegalArgumentException("Agency not found"));

        Proposal proposal = Proposal.builder()
                .agency(agency)
                .clientName(req.getClientName().trim())
                .clientEmail(req.getClientEmail().trim().toLowerCase(java.util.Locale.ROOT))
                .projectScope(req.getProjectScope())
                .deliverables(req.getDeliverables())
                .paymentTerms(req.getPaymentTerms())
                .estimatedBudget(req.getTotalBudget())
                .documentHtml(sanitizeProposalHtml(req.getDocumentHtml()))
                .paymentMilestonesJson(req.getPaymentMilestonesJson())
                .shareToken(UUID.randomUUID().toString())
                .expiresAt(LocalDateTime.now().plusDays(14))
                .isSigned(false)
                .isConvertedToProject(false)
                .build();

        return proposalRepository.save(proposal);
    }

    static String sanitizeProposalHtml(String html) {
        Safelist safelist = Safelist.relaxed()
                .removeTags("iframe", "object", "embed", "form", "input", "button", "style", "link", "meta")
                .removeAttributes(":all", "style", "class", "id")
                .addAttributes("table", "border", "cellpadding", "cellspacing")
                .addAttributes("td", "colspan", "rowspan")
                .addAttributes("th", "colspan", "rowspan");
        return Jsoup.clean(html, safelist);
    }

    @Transactional(readOnly = true)
    public List<ProposalListItemDto> listForAgency(Long agencyId) {
        return proposalRepository.findByAgencyIdOrderByCreatedAtDesc(agencyId).stream()
                .map(proposal -> new ProposalListItemDto(
                        proposal.getId(),
                        proposal.getClientName(),
                        proposal.getProjectScope(),
                        proposal.getEstimatedBudget(),
                        proposal.isSigned(),
                        proposal.getSignatureName(),
                        proposal.getSignedAt(),
                        proposal.isConvertedToProject(),
                        proposal.getCreatedProjectId(),
                        proposal.getCreatedAt(),
                        proposal.getExpiresAt(),
                        proposal.isRevoked(),
                        proposal.getShareToken()))
                .toList();
    }

    @Transactional
    public void revokeProposal(Long proposalId, Long agencyId) {
        Proposal proposal = proposalRepository.findByIdAndAgencyId(proposalId, agencyId)
                .orElseThrow(() -> new IllegalArgumentException("Proposal not found"));
        if (proposal.isSigned()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A signed proposal cannot be revoked.");
        }
        proposal.setRevoked(true);
        proposalRepository.save(proposal);
    }
}
