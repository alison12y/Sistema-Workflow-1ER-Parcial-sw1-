import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { BitacoraService } from '../../services/bitacora.service';
import {
  BITACORA_ACTION_LABELS,
  BITACORA_MODULES,
  BitacoraEntry,
  BitacoraFilter,
} from '../../models/bitacora.model';

@Component({
  selector: 'app-bitacora',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './bitacora.component.html',
  styleUrl: './bitacora.component.scss',
})
export class BitacoraComponent implements OnInit {
  private readonly bitacoraService = inject(BitacoraService);

  entries: BitacoraEntry[] = [];
  loading = true;
  message = '';
  error = '';

  filterUsername = '';
  filterModule = '';
  filterAction = '';
  filterDateFrom = '';
  filterDateTo = '';

  readonly modules = ['', ...BITACORA_MODULES];
  readonly actionOptions = Object.keys(BITACORA_ACTION_LABELS).sort();

  ngOnInit(): void {
    this.load(false);
  }

  load(showSuccessMessage = true): void {
    this.loading = true;
    this.error = '';
    this.message = '';

    this.bitacoraService.getAll(this.buildFilter()).subscribe({
      next: (data) => {
        this.entries = data;
        this.loading = false;
        if (showSuccessMessage) {
          this.message = 'Bitácora actualizada correctamente';
        }
      },
      error: (err: HttpErrorResponse) => {
        this.entries = [];
        this.loading = false;
        if (err.status === 401) {
          this.error = 'Su sesión expiró. Inicie sesión nuevamente';
        } else {
          this.error = 'No se pudo cargar la bitácora';
        }
      },
    });
  }

  applyFilters(): void {
    this.load(false);
  }

  clearFilters(): void {
    this.filterUsername = '';
    this.filterModule = '';
    this.filterAction = '';
    this.filterDateFrom = '';
    this.filterDateTo = '';
    this.load(false);
  }

  refresh(): void {
    this.load(true);
  }

  exportCsv(): void {
    this.bitacoraService.exportCsv(this.buildFilter()).subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        const anchor = document.createElement('a');
        anchor.href = url;
        anchor.download = `bitacora-${new Date().toISOString().slice(0, 10)}.csv`;
        anchor.click();
        URL.revokeObjectURL(url);
        this.message = 'Exportación CSV generada';
      },
      error: () => {
        this.error = 'No se pudo exportar la bitácora';
      },
    });
  }

  actionLabel(action: string): string {
    return BITACORA_ACTION_LABELS[action] ?? action.replace(/_/g, ' ').toLowerCase();
  }

  displayUser(entry: BitacoraEntry): string {
    return entry.fullName?.trim() || entry.username || '—';
  }

  resultadoLabel(entry: BitacoraEntry): string {
    const value = (entry.resultado ?? 'EXITO').toUpperCase();
    return value === 'ERROR' ? 'Error' : 'Éxito';
  }

  formatDate(value: string | null | undefined): string {
    if (!value) {
      return '—';
    }
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) {
      return '—';
    }
    const day = String(date.getDate()).padStart(2, '0');
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const year = date.getFullYear();
    const hours = String(date.getHours()).padStart(2, '0');
    const minutes = String(date.getMinutes()).padStart(2, '0');
    return `${day}/${month}/${year} ${hours}:${minutes}`;
  }

  private buildFilter(): BitacoraFilter {
    const filter: BitacoraFilter = {};
    if (this.filterUsername.trim()) {
      filter.username = this.filterUsername.trim();
    }
    if (this.filterModule.trim()) {
      filter.module = this.filterModule.trim();
    }
    if (this.filterAction.trim()) {
      filter.action = this.filterAction.trim();
    }
    if (this.filterDateFrom.trim()) {
      filter.dateFrom = this.toIsoDateTime(this.filterDateFrom, false);
    }
    if (this.filterDateTo.trim()) {
      filter.dateTo = this.toIsoDateTime(this.filterDateTo, true);
    }
    return filter;
  }

  private toIsoDateTime(dateValue: string, endOfDay: boolean): string {
    const time = endOfDay ? 'T23:59:59' : 'T00:00:00';
    return `${dateValue}${time}`;
  }
}
