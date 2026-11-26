import { calculateCommercials } from './taxMath.js';

/**
 * Maps the proposal form onto the fields the backend's AiContractRequest expects.
 * The backend checks that each supplied fact appears in the AI draft verbatim, so the
 * strings stay short and concrete. Empty strings mean "not specified".
 */
export function buildAiContractRequest(formData, agencySettings, paymentTerms) {
  const budget = String(formData.totalBudget || '').trim();
  const budgetGuidance = budget
    ? `${formData.currency || 'INR'} ${budget} (${formData.pricingType || 'Fixed Price'}, before GST)`
    : '';

  let gstStatus = '';
  if (budget) {
    const { taxType, gstRate } = calculateCommercials({
      budget,
      clientCountry: formData.clientCountry,
      clientState: formData.clientState,
      agencyState: agencySettings.state,
      isGstRegistered: agencySettings.isGstRegistered,
      startDate: formData.startDate,
      paymentMilestones: [],
    });
    if (taxType === 'None') gstStatus = 'No GST charged';
    else if (taxType === 'Export') gstStatus = 'Export of services, zero-rated under LUT';
    else gstStatus = `${taxType} @${Math.round(gstRate * 100)}% extra`;
  }

  const jurisdiction = agencySettings.state ? `Courts of ${agencySettings.state}, India` : '';
  const timeline = String(formData.timeline || '').trim()
    || (formData.startDate ? `Starting ${formData.startDate}` : '');

  return {
    clientName: formData.clientName,
    projectScope: formData.projectScope,
    deliverables: formData.deliverables,
    paymentTerms,
    timeline,
    budgetGuidance,
    gstStatus,
    jurisdiction,
  };
}
