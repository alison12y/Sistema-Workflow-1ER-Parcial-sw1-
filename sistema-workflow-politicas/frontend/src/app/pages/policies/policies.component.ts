import { Component } from '@angular/core';
import { PlaceholderComponent } from '../placeholder/placeholder.component';

@Component({
  selector: 'app-policies',
  standalone: true,
  imports: [PlaceholderComponent],
  template: `<app-placeholder title="Políticas" description="Gestión de políticas de negocio y workflows." />`,
})
export class PoliciesComponent {}
