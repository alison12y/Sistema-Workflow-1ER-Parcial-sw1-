import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { AuthService } from '../../services/auth.service';

interface DemoCredential {
  role: string;
  username: string;
  password: string;
}

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss',
})
export class LoginComponent {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  /** Solo referencia visual; no rellena el formulario ni altera el flujo de autenticación. */
  readonly demoAccounts: DemoCredential[] = [
    { role: 'Administrador', username: 'sistema.admin', password: 'Admin.Sistema2024!' },
    { role: 'Diseñador del Workflow', username: 'carlos.mendoza', password: 'Carlos.M2024!' },
    { role: 'Funcionario', username: 'ana.rodriguez', password: 'Ana.R2024!' },
  ];

  demoSectionOpen = false;

  username = '';
  password = '';
  loading = false;
  error = '';

  toggleDemoSection(): void {
    this.demoSectionOpen = !this.demoSectionOpen;
  }

  onSubmit(): void {
    this.error = '';
    if (!this.username.trim() || !this.password) {
      this.error = 'Ingrese usuario y contraseña';
      return;
    }
    this.loading = true;
    this.auth.login({ username: this.username.trim(), password: this.password }).subscribe({
      next: () => {
        this.loading = false;
        this.router.navigate(['/dashboard']);
      },
      error: (err: HttpErrorResponse) => {
        this.loading = false;
        if (err.status === 0) {
          this.error = 'No se pudo conectar con el servidor. Verifique que el backend esté activo.';
        } else {
          this.error = err.error?.message || 'Usuario o contraseña incorrectos';
        }
      },
    });
  }
}
