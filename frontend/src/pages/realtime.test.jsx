import React from 'react';
import { act, render, screen, waitFor } from '@testing-library/react';
import { beforeEach, expect, it, vi } from 'vitest';

const session = vi.hoisted(() => ({ user: { role: 'MEMBER' } }));
vi.mock('../context/AuthContext', () => ({ useAuth: () => session }));
vi.mock('../components/Navbar', () => ({ default: () => <header /> }));
vi.mock('../services/api', () => ({ default: { get: vi.fn() } }));
vi.mock('../services/websocket', () => ({
  wsService: { subscribe: vi.fn(), unsubscribe: vi.fn() },
}));

import api from '../services/api';
import { wsService } from '../services/websocket';
import Tasks from './Tasks';
import TaskCommentDrawer from '../components/TaskCommentDrawer';

beforeEach(() => {
  localStorage.clear();
  vi.clearAllMocks();
  session.user = { role: 'MEMBER' };
});

it('subscribes to loaded task and project IDs, refreshes, and cleans up when IDs change or unmount', async () => {
  let tasks = [{ id: 1, projectId: 3, title: 'Task one', status: 'TODO' }];
  api.get.mockImplementation((url) => Promise.resolve({ data:
    url === '/tasks' ? tasks : url === '/projects' ? [{ id: 1 }, { id: 2 }] : null,
  }));
  const { unmount } = render(<Tasks />);
  await screen.findByText('Task one');
  for (const id of [1, 2, 3]) {
    expect(wsService.subscribe).toHaveBeenCalledWith(`/topic/project/${id}`, expect.any(Function));
  }
  const refresh = wsService.subscribe.mock.calls.find(([topic]) => topic === '/topic/project/3')[1];
  tasks = [{ id: 2, projectId: 4, title: 'Task two', status: 'TODO' }];
  await act(async () => { await refresh('DELETE:1'); });
  expect(await screen.findByText('Task two')).toBeInTheDocument();
  expect(screen.queryByText('Task one')).not.toBeInTheDocument();
  expect(wsService.unsubscribe).toHaveBeenCalledWith('/topic/project/3');
  expect(wsService.subscribe).toHaveBeenCalledWith('/topic/project/4', expect.any(Function));
  const count = wsService.subscribe.mock.calls.length;
  const refreshAgain = wsService.subscribe.mock.calls.find(([topic]) => topic === '/topic/project/4')[1];
  await act(async () => { await refreshAgain({ id: 2 }); });
  expect(wsService.subscribe).toHaveBeenCalledTimes(count);
  wsService.unsubscribe.mockClear();
  unmount();
  expect(wsService.unsubscribe.mock.calls.map(([topic]) => topic)).toEqual([
    '/topic/project/1', '/topic/project/2', '/topic/project/4',
  ]);
});

it.each(['OWNER', 'ADMIN', 'MEMBER'])('shows only the open task comments for %s and unsubscribes', async (role) => {
  session.user.role = role;
  api.get.mockResolvedValue({ data: [] });
  const { unmount } = render(<TaskCommentDrawer task={{ id: 7, projectId: 2, title: 'Task' }} />);
  await screen.findByText(/No comments yet/);
  expect(wsService.subscribe).toHaveBeenCalledWith('/topic/project/2/comments', expect.any(Function));
  const receive = wsService.subscribe.mock.calls[0][1];
  act(() => {
    receive({ id: 1, taskId: 8, content: 'Other task' });
    receive({ id: 2, taskId: 7, content: 'Current task' });
    receive({ id: 2, taskId: 7, content: 'Current task' });
  });
  expect(screen.queryByText('Other task')).not.toBeInTheDocument();
  expect(screen.getAllByText('Current task')).toHaveLength(1);
  unmount();
  expect(wsService.unsubscribe).toHaveBeenCalledWith('/topic/project/2/comments');
});

it('does not subscribe clients to either internal feed', async () => {
  session.user.role = 'CLIENT';
  api.get.mockImplementation((url) => Promise.resolve({ data:
    url === '/projects' ? [{ id: 1 }] : url === '/time-entries/active' ? null : [],
  }));
  render(<Tasks />);
  render(<TaskCommentDrawer task={{ id: 7, projectId: 1, title: 'Task' }} />);
  await screen.findByText(/No comments yet/);
  await waitFor(() => expect(api.get).toHaveBeenCalledWith('/projects'));
  expect(wsService.subscribe).not.toHaveBeenCalled();
});
