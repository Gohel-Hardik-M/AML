import { Injectable, signal } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class ToastService {
  message = signal('');
  type = signal<'success' | 'danger'> ('success');
  show(message: string, type: 'success' | 'danger' = 'success') {
    let cleanMessage = message;
    if (typeof message === 'string') {
      const trimmed = message.trim();
      if (trimmed.startsWith('{') && trimmed.endsWith('}')) {
        try {
          const parsed = JSON.parse(trimmed);
          cleanMessage = parsed.message || cleanMessage;
        } catch {
          // keep original if not JSON
        }
      }
    }
    this.message.set(cleanMessage);
    this.type.set(type);
    window.setTimeout(() => this.message.set(''), 4500);
  }
}
