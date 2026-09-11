import { Injectable, signal } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class ToastService {
  message = signal('');
  type = signal<'success' | 'danger'> ('success');
  show(message: string, type: 'success' | 'danger' = 'success') {
    this.message.set(message); this.type.set(type);
    window.setTimeout(() => this.message.set(''), 3500);
  }
}
