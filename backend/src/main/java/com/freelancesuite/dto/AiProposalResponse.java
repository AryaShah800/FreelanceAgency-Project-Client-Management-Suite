package com.freelancesuite.dto;

import java.time.LocalDateTime;
import java.util.List;

public class AiProposalResponse {
    private String title;
    private String executiveSummary;
    private List<ProposalSection> sections;
    private List<String> reviewNotes;
    private LocalDateTime generatedAt;

    public AiProposalResponse() {}
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getExecutiveSummary() { return executiveSummary; }
    public void setExecutiveSummary(String executiveSummary) { this.executiveSummary = executiveSummary; }
    public List<ProposalSection> getSections() { return sections; }
    public void setSections(List<ProposalSection> sections) { this.sections = sections; }
    public List<String> getReviewNotes() { return reviewNotes; }
    public void setReviewNotes(List<String> reviewNotes) { this.reviewNotes = reviewNotes; }
    public LocalDateTime getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt; }
}
