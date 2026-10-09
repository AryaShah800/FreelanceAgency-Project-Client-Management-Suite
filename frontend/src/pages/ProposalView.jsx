import React from 'react';
import { Navigate, useParams } from 'react-router-dom';

export default function ProposalView() {
  const { token } = useParams();
  return <Navigate replace to={`/portal/share/${encodeURIComponent(token || '')}`} />;
}
