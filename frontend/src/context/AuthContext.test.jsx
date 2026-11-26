import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { beforeEach, expect, it, vi } from 'vitest';

vi.mock('../services/api', () => ({ default: { get: vi.fn() } }));
vi.mock('../services/websocket', () => ({ wsService: { disconnect: vi.fn() } }));

import api from '../services/api';
import { wsService } from '../services/websocket';
import { AuthProvider, useAuth } from './AuthContext';

function Session() {
  const { user, loading, logout } = useAuth();
  return <button disabled={loading} onClick={logout}>{user ? 'Logout' : 'Signed out'}</button>;
}

beforeEach(() => {
  localStorage.clear();
  vi.clearAllMocks();
});

it('disconnects the live session and clears authentication on logout', async () => {
  const user = { id: 1, role: 'MEMBER' };
  localStorage.setItem('user', JSON.stringify(user));
  localStorage.setItem('token', 'token');
  api.get.mockResolvedValue({ data: user });
  render(<AuthProvider><Session /></AuthProvider>);
  await waitFor(() => expect(screen.getByRole('button', { name: 'Logout' })).toBeEnabled());
  fireEvent.click(screen.getByRole('button', { name: 'Logout' }));
  expect(wsService.disconnect).toHaveBeenCalledOnce();
  expect(localStorage.getItem('token')).toBeNull();
  expect(localStorage.getItem('user')).toBeNull();
  expect(screen.getByRole('button', { name: 'Signed out' })).toBeInTheDocument();
});
