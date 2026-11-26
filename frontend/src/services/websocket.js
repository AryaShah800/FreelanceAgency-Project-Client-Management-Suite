import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

class WebSocketService {
  constructor() {
    this.client = null;
    this.connected = false;
    this.subscriptions = {};
    // Keep desired subscriptions across reconnects, including while connecting.
    this.callbacks = {};
  }

  connect() {
    if (this.client?.active) return;

    const socketUrl = import.meta.env.VITE_WS_URL || getDefaultSocketUrl();

    const client = new Client({
      webSocketFactory: () => new SockJS(socketUrl),
      beforeConnect: () => {
        const token = localStorage.getItem('token');
        client.connectHeaders = token ? { Authorization: `Bearer ${token}` } : {};
      },
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
      onConnect: () => {
        if (this.client !== client) return;
        this.connected = true;
        console.log('STOMP WebSocket Connected Successfully');
        this.subscriptions = {};
        Object.entries(this.callbacks).forEach(([topic, callback]) => {
          this._doSubscribe(topic, callback);
        });
      },
      onWebSocketClose: () => {
        if (this.client !== client) return;
        this.connected = false;
        this.subscriptions = {};
      },
      onStompError: (frame) => {
        console.error('STOMP Error:', frame.headers['message']);
      },
    });

    this.client = client;
    client.activate();
  }

  subscribe(topic, callback) {
    this.callbacks[topic] = callback;
    if (!this.client || !this.connected) {
      this.connect();
    } else {
      this._doSubscribe(topic, callback);
    }
  }

  _doSubscribe(topic, callback) {
    if (this.subscriptions[topic]) {
      this.subscriptions[topic].unsubscribe();
    }

    const sub = this.client.subscribe(topic, (message) => {
      if (this.callbacks[topic] !== callback) return;
      let payload;
      try {
        payload = JSON.parse(message.body);
      } catch (e) {
        payload = message.body;
      }
      callback(payload);
    });

    this.subscriptions[topic] = sub;
  }

  unsubscribe(topic) {
    delete this.callbacks[topic];
    if (this.subscriptions[topic]) {
      this.subscriptions[topic].unsubscribe();
      delete this.subscriptions[topic];
    }
  }

  disconnect() {
    const client = this.client;
    this.client = null;
    this.connected = false;
    this.subscriptions = {};
    this.callbacks = {};
    if (client) client.deactivate();
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
