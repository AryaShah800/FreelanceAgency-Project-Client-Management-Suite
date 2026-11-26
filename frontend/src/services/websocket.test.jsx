import { beforeEach, afterEach, describe, expect, it, vi } from 'vitest';

vi.mock('@stomp/stompjs', () => ({
  Client: vi.fn(function (config) {
    Object.assign(this, config);
    this.active = false;
    this.activate = vi.fn(() => { this.active = true; });
    this.deactivate = vi.fn(() => { this.active = false; });
    this.subscribe = vi.fn(() => ({ unsubscribe: vi.fn() }));
  }),
}));
vi.mock('sockjs-client', () => ({ default: vi.fn() }));

import { Client } from '@stomp/stompjs';
import { wsService } from './websocket';

beforeEach(() => {
  localStorage.clear();
  vi.clearAllMocks();
});
afterEach(() => wsService.disconnect());

describe('WebSocket session lifecycle', () => {
  it('reads the current token before every connection and clears missing credentials', () => {
    localStorage.setItem('token', 'first');
    wsService.connect();
    const client = wsService.client;
    client.beforeConnect();
    expect(client.connectHeaders).toEqual({ Authorization: 'Bearer first' });
    localStorage.setItem('token', 'renewed');
    client.beforeConnect();
    expect(client.connectHeaders).toEqual({ Authorization: 'Bearer renewed' });
    localStorage.removeItem('token');
    client.beforeConnect();
    expect(client.connectHeaders).toEqual({});
  });

  it('shares a pending connection and restores only current subscriptions after reconnect', () => {
    wsService.subscribe('/topic/project/1', vi.fn());
    wsService.subscribe('/topic/project/2', vi.fn());
    const client = wsService.client;
    expect(Client).toHaveBeenCalledTimes(1);
    wsService.unsubscribe('/topic/project/1');
    client.onConnect();
    expect(client.subscribe).toHaveBeenCalledTimes(1);
    expect(client.subscribe).toHaveBeenLastCalledWith('/topic/project/2', expect.any(Function));
    client.onWebSocketClose();
    wsService.subscribe('/topic/project/3', vi.fn());
    wsService.unsubscribe('/topic/project/2');
    client.onConnect();
    expect(Client).toHaveBeenCalledTimes(1);
    expect(client.subscribe).toHaveBeenCalledTimes(2);
    expect(client.subscribe).toHaveBeenLastCalledWith('/topic/project/3', expect.any(Function));
  });

  it('unsubscribes active feeds and drops queued subscriptions on logout', () => {
    const callback = vi.fn();
    wsService.subscribe('/topic/project/1', callback);
    const client = wsService.client;
    client.onConnect();
    const subscription = client.subscribe.mock.results[0].value;
    const receive = client.subscribe.mock.calls[0][1];
    receive({ body: 'DELETE:7' });
    expect(callback).toHaveBeenCalledWith('DELETE:7');
    wsService.unsubscribe('/topic/project/1');
    expect(subscription.unsubscribe).toHaveBeenCalledOnce();
    wsService.subscribe('/topic/project/2', vi.fn());
    wsService.disconnect();
    client.onConnect();
    receive({ body: '{"id":7}' });
    expect(callback).toHaveBeenCalledTimes(1);
    expect(client.deactivate).toHaveBeenCalledOnce();
    expect(wsService.client).toBeNull();
    expect(wsService.connected).toBe(false);
    expect(wsService.callbacks).toEqual({});
    expect(wsService.subscriptions).toEqual({});
  });
});
