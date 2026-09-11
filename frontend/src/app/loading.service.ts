import { Injectable, signal } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class LoadingService {
  readonly active = signal(false);
  private pending = 0;
  private showTimer?: ReturnType<typeof setTimeout>;

  start() {
    this.pending += 1;
    if (this.pending === 1) {
      this.showTimer = setTimeout(() => {
        if (this.pending > 0) this.active.set(true);
      }, 180);
    }
  }

  stop() {
    this.pending = Math.max(0, this.pending - 1);
    if (this.pending === 0) {
      if (this.showTimer) clearTimeout(this.showTimer);
      this.showTimer = undefined;
      this.active.set(false);
    }
  }
}
