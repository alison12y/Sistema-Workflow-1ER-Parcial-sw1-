import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { BitacoraService } from '../../services/bitacora.service';
import {
  BITACORA_ACTION_LABELS,
  BITACORA_MODULES,
  BitacoraEntry,
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
  filteredEntries: BitacoraEntry[] = [];
  loading = true;
  searchTerm = '';
  selectedModule = '';
  message = '';
  error = '';

  readonly modules = ['', ...BITACORA_MODULES];

  ngOnInit(): void {
    this.load(false);
  }

  load(showSuccessMessage = true): void {
    this.loading = true;
    this.error = '';
    this.message = '';

    const request =
      this.selectedModule.trim().length > 0
        ? this.bitacoraService.getByModule(this.selectedModule.trim())
        : this.bitacoraService.getAll();

    request.subscribe({
      next: (data) => {
        this.entries = data;
        this.applyFilters();
        this.loading = false;
        if (showSuccessMessage) {
          this.message = 'Bitácora actualizada correctamente';
        }
      },
      error: (err: HttpErrorResponse) => {
        this.entries = [];
        this.filteredEntries = [];
        this.loading = false;
        if (err.status === 401) {
          this.error = 'Su sesión expiró. Inicie sesión nuevamente';
        } else {
          this.error = 'No se pudo cargar la bitácora';
        }
      },
    });
  }

  onSearchChange(): void {
    this.applyFilters();
  }

  onModuleChange(): void {
    this.load(false);
  }

  refresh(): void {
    this.load(true);
  }

  actionLabel(action: string): string {
    return BITACORA_ACTION_LABELS[action] ?? action.replace(/_/g, ' ').toLowerCase();
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

  private applyFilters(): void {
    const term = this.searchTerm.trim().toLowerCase();
    if (!term) {
      this.filteredEntries = [...this.entries];
      return;
    }

    this.filteredEntries = this.entries.filter((entry) => {
      const haystack = [
        entry.username,
        entry.module,
        entry.action,
        this.actionLabel(entry.action),
        entry.description,
      ]
        .join(' ')
        .toLowerCase();
      return haystack.includes(term);
    });
  }
}
