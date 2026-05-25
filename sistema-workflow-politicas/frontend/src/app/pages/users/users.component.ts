import { Component } from '@angular/core';
import { PlaceholderComponent } from '../placeholder/placeholder.component';

@Component({
  selector: 'app-users',
  standalone: true,
  imports: [PlaceholderComponent],
  template: `<app-placeholder title="Usuarios" description="Administración de usuarios del sistema." />`,
})
export class UsersComponent {}
