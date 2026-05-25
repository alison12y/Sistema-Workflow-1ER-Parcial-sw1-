import { Component } from '@angular/core';
import { PlaceholderComponent } from '../placeholder/placeholder.component';

@Component({
  selector: 'app-roles',
  standalone: true,
  imports: [PlaceholderComponent],
  template: `<app-placeholder title="Roles" description="Configuración de roles y permisos." />`,
})
export class RolesComponent {}
