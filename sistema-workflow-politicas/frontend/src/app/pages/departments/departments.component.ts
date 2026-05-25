import { Component } from '@angular/core';
import { PlaceholderComponent } from '../placeholder/placeholder.component';

@Component({
  selector: 'app-departments',
  standalone: true,
  imports: [PlaceholderComponent],
  template: `<app-placeholder title="Departamentos" description="Organización por departamentos y responsables." />`,
})
export class DepartmentsComponent {}
