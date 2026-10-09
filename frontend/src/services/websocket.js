import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

class WebSocketService {
  constructor() {
    this.client = null;
    this.connected = false;
    this.subscriptions = {};
  }

  connect(onConnectCallback) {
    if (this.client && this.connected) {
      if (onConnectCallback) onConnectCallback();
      return;
    }

    const socketUrl = import.meta.env.VITE_WS_URL || getDefaultSocketUrl();

    this.client = new Client({
      webSocketFactory: () => new SockJS(socketUrl),
      connectHeaders: (() => {
        const token = localStorage.getItem('token');
        return token ? { Authorization: `Bearer ${token}` } : {};
      })(),
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
      onConnect: () => {
        this.connected = true;
        console.log('STOMP WebSocket Connected Successfully');
        if (onConnectCallback) onConnectCallback();
      },
      onStompError: (frame) => {
        console.error('STOMP Error:', frame.headers['message']);
      },
    });

    this.client.activate();
  }

  subscribe(topic, callback) {
    if (!this.client || !this.connected) {
      this.connect(() => {
        this._doSubscribe(topic, callback);
      });
    } else {
      this._doSubscribe(topic, callback);
    }
  }

  _doSubscribe(topic, callback) {
    if (this.subscriptions[topic]) {
      this.subscriptions[topic].unsubscribe();
    }

    const sub = this.client.subscribe(topic, (message) => {
      try {
        const payload = JSON.parse(message.body);
        callback(payload);
      } catch (e) {
        callback(message.body);
      }
    });

    this.subscriptions[topic] = sub;
  }

  unsubscribe(topic) {
    if (this.subscriptions[topic]) {
      this.subscriptions[topic].unsubscribe();
      delete this.subscriptions[topic];
    }
  }

  disconnect() {
    if (this.client) {
      this.client.deactivate();
      this.connected = false;
    }
  }
}

function getDefaultSocketUrl() {
  const apiBaseUrl = import.meta.env.VITE_API_BASE_URL;
  if (apiBaseUrl && /^https?:\/\//i.test(apiBaseUrl)) {
    return `${new URL(apiBaseUrl).origin}/ws`;
  }

  if (['localhost', '127.0.0.1'].includes(window.location.hostname)) {
    return `${window.location.protocol}//${window.location.hostname}:8080/ws`;
  }

  return `${window.location.origin}/ws`;
}

export const wsService = new WebSocketService();
