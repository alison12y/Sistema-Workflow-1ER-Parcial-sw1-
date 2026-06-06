import { AsyncPipe } from '@angular/common';
import { Component, OnDestroy, OnInit, inject } from '@angular/core';
import { Subscription } from 'rxjs';
import { ConnectivityService } from '../../../core/offline/connectivity.service';

@Component({
  selector: 'app-connectivity-banner',
  standalone: true,
  imports: [AsyncPipe],
  template: `
    @if ((connectivity.isOnline$ | async) === false) {
      <div class="connectivity-banner offline" role="status">
        <i class="material-icons">cloud_off</i>
        <span>Modo sin conexión — los cambios se guardarán localmente</span>
        @if (pendingCount > 0) {
          <strong>{{ pendingCount }} elemento(s) pendiente(s) de sincronizar</strong>
        }
      </div>
    } @else if (pendingCount > 0) {
      <div class="connectivity-banner syncing" role="status">
        <i class="material-icons">sync</i>
        <span>{{ pendingCount }} elemento(s) pendiente(s) de sincronizar</span>
      </div>
    } @else {
      <div class="connectivity-banner online" role="status">
        <i class="material-icons">cloud_done</i>
        <span>En línea</span>
      </div>
    }
  `,
  styles: `
    .connectivity-banner {
      display: flex;
      align-items: center;
      gap: 0.5rem;
      padding: 0.45rem 1rem;
      font-size: 0.82rem;
      border-bottom: 1px solid var(--color-border);

      .material-icons {
        font-size: 1.1rem;
      }

      strong {
        margin-left: auto;
        font-size: 0.78rem;
      }
    }

    .online {
      background: #ecfdf5;
      color: #065f46;
    }

    .offline {
      background: #fef3c7;
      color: #92400e;
    }

    .syncing {
      background: #eff6ff;
      color: #1e40af;
    }

    @media (min-width: 769px) {
      .online {
        display: none;
      }
    }
  `,
})
export class ConnectivityBannerComponent implements OnInit, OnDestroy {
  readonly connectivity = inject(ConnectivityService);
  pendingCount = 0;
  private sub: Subscription | null = null;

  ngOnInit(): void {
    this.sub = this.connectivity.pendingCount$.subscribe((count) => {
      this.pendingCount = count;
    });
  }

  ngOnDestroy(): void {
    this.sub?.unsubscribe();
  }
}
