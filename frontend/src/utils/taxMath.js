export function calculateCommercials({
  budget,
  clientCountry,
  clientState,
  agencyState,
  isGstRegistered,
  startDate,
  paymentMilestones
}) {
  const b = parseFloat(budget) || 0;
  const isIndian = clientCountry === 'IN';
  let gstRate = 0;
  let taxType = 'None';

  if (isIndian && isGstRegistered) {
    gstRate = 0.18;
    if ((clientState || '').trim().toLowerCase() === (agencyState || '').trim().toLowerCase()) {
      taxType = 'CGST/SGST';
    } else {
      taxType = 'IGST';
    }
  } else if (!isIndian && isGstRegistered) {
    taxType = 'Export';
  }

  const gstAmount = b * gstRate;
  const grandTotal = b + gstAmount;

  let remainingTotal = grandTotal;
  let remainingBase = b;
  let baseDate = new Date();
  if (startDate) {
    baseDate = new Date(startDate);
  }

  const invoices = paymentMilestones.map((m, idx) => {
    const pct = parseFloat(m.percentage) || 0;
    const days = parseInt(m.daysFromStart, 10) || 0;
    let trancheTotal = 0;
    if (idx === paymentMilestones.length - 1) {
      trancheTotal = remainingTotal;
    } else {
      trancheTotal = Math.round(grandTotal * (pct / 100) * 100) / 100;
      remainingTotal = Math.round((remainingTotal - trancheTotal) * 100) / 100;
    }

    let trancheBase;
    if (idx === paymentMilestones.length - 1) {
      trancheBase = remainingBase;
    } else {
      trancheBase = Math.round(b * (pct / 100) * 100) / 100;
      remainingBase = Math.round((remainingBase - trancheBase) * 100) / 100;
    }
    const trancheTax = Math.round((trancheTotal - trancheBase) * 100) / 100;
    const isIntraState = taxType === 'CGST/SGST';
    const cgst = isIntraState ? Math.round(trancheTax / 2 * 100) / 100 : 0;

    const dueDateObj = new Date(baseDate);
    dueDateObj.setDate(dueDateObj.getDate() + days);
    const dueDateIso = `${dueDateObj.getFullYear()}-${String(dueDateObj.getMonth() + 1).padStart(2, '0')}-${String(dueDateObj.getDate()).padStart(2, '0')}`;
    const dueDateStr = dueDateObj.toLocaleDateString('en-IN', { year: 'numeric', month: 'short', day: 'numeric' });

    return {
      label: m.label,
      percentage: pct,
      invoiceSubtotal: trancheBase,
      cgst,
      sgst: isIntraState ? Math.round((trancheTax - cgst) * 100) / 100 : 0,
      igst: taxType === 'IGST' ? trancheTax : 0,
      invoiceTotal: trancheTotal,
      dueDate: dueDateStr,
      dueDateIso
    };
  });

  return {
    baseBudget: b,
    taxType,
    gstRate,
    gstAmount,
    grandTotal,
    invoices
  };
}
