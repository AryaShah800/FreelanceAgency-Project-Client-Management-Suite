import React from 'react';
import { render, screen, act, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';

vi.mock('../services/api', () => ({ default: { get: vi.fn() } }));
vi.mock('../context/AuthContext', () => ({
  useAuth: vi.fn(),
}));
vi.mock('../components/Navbar', () => ({ default: () => <header /> }));
vi.mock('../components/DashboardCharts', () => ({ default: () => <div data-testid="charts" /> }));

import api from '../services/api';
import { useAuth } from '../context/AuthContext';
import Dashboard from './Dashboard';
import Clients from './Clients';

const data = {
  '/projects': [{ id: 1, title: 'Website Redesign', clientName: 'Acme', budget: 50000 }],
  '/tasks': [{ id: 1, projectId: 1, status: 'DONE', title: 'T' }],
  '/invoices': [{ id: 1, status: 'PAID', totalAmount: 12345 }],
  '/clients': [{ id: 1, companyName: 'Acme Corp', email: 'a@acme.test', dealStage: 'LEAD' }],
  '/audit-events': [{ id: 1, actor: 'owner', timestamp: '2026-01-01T10:00:00', details: 'Did a thing' }],
};

const renderPage = (ui) => render(<MemoryRouter>{ui}</MemoryRouter>);

beforeEach(() => {
  vi.useFakeTimers({ shouldAdvanceTime: true });
  api.get.mockReset();
  useAuth.mockReturnValue({ user: { name: 'Demo', role: 'OWNER', agencyName: 'Demo Agency' }, logout: vi.fn() });
});
afterEach(() => vi.useRealTimers());

describe('Dashboard loading states', () => {
  it('shows client budget burn from project logged values, with missing values treated as zero', async () => {
    useAuth.mockReturnValue({ user: { name: 'Client', role: 'CLIENT' } });
    const clientData = {
      '/projects': [
        { id: 1, budget: 1000, loggedValue: 181.68 },
        { id: 2, budget: 500, loggedValue: 118.32 },
        { id: 3, budget: 500 },
      ],
      '/tasks': [{ id: 1, status: 'DONE', actualHours: 999 }],
      '/invoices': [],
    };
    api.get.mockImplementation((url) => Promise.resolve({ data: clientData[url] }));
    const { container } = renderPage(<Dashboard />);

    expect(await screen.findByText(/Logged Work Value:/)).toHaveTextContent('₹300');
    expect(screen.getByText('15% Consumed')).toBeInTheDocument();
    expect(container.querySelector('[style="width: 15%;"]')).toBeInTheDocument();
  });

  it('shows skeletons, not fake zeros, while loading', () => {
    api.get.mockReturnValue(new Promise(() => {}));
    renderPage(<Dashboard />);
    expect(screen.getByRole('status', { name: /loading project health/i })).toBeInTheDocument();
    expect(screen.queryByText(/₹0/)).not.toBeInTheDocument();
    expect(screen.queryByText(/Showing 0 of 0/)).not.toBeInTheDocument();
    expect(screen.queryByText(/No activity logged yet/)).not.toBeInTheDocument();
  });

  it('shows the waking-up note only after 5 seconds', () => {
    api.get.mockReturnValue(new Promise(() => {}));
    renderPage(<Dashboard />);
    expect(screen.queryByText(/waking up/i)).not.toBeInTheDocument();
    act(() => { vi.advanceTimersByTime(5100); });
    expect(screen.getByText(/waking up/i)).toBeInTheDocument();
  });

  it('renders real data once loaded and drops the note', async () => {
    api.get.mockImplementation((url) => Promise.resolve({ data: data[url] }));
    renderPage(<Dashboard />);
    expect(await screen.findByText('Website Redesign')).toBeInTheDocument();
    expect(screen.getByText(/₹12,345/)).toBeInTheDocument();
    expect(screen.getByText('Did a thing')).toBeInTheDocument();
    expect(screen.queryByText(/waking up/i)).not.toBeInTheDocument();
  });

  it('shows an error with retry instead of an empty workspace', async () => {
    api.get.mockRejectedValue(new Error('boom'));
    vi.spyOn(console, 'error').mockImplementation(() => {});
    renderPage(<Dashboard />);
    expect(await screen.findByRole('alert')).toHaveTextContent(/could not load the dashboard/i);
    expect(screen.getByRole('button', { name: /retry/i })).toBeInTheDocument();
    expect(screen.queryByText(/No activity logged yet/)).not.toBeInTheDocument();
  });
});

describe('Clients loading states', () => {
  it('shows skeleton then clients', async () => {
    let resolve;
    api.get.mockReturnValue(new Promise((r) => { resolve = r; }));
    renderPage(<Clients />);
    expect(screen.getAllByRole('status', { name: /loading clients/i }).length).toBeGreaterThan(0);
    await act(async () => { resolve({ data: data['/clients'] }); });
    await waitFor(() => expect(screen.getByText('Acme Corp')).toBeInTheDocument());
    expect(screen.queryByRole('status', { name: /loading clients/i })).not.toBeInTheDocument();
  });

  it('shows an error state on failure', async () => {
    api.get.mockRejectedValue(new Error('down'));
    vi.spyOn(console, 'error').mockImplementation(() => {});
    renderPage(<Clients />);
    expect(await screen.findByRole('alert')).toHaveTextContent(/could not load clients/i);
  });
});
