import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators, AbstractControl } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { PortalService } from '../../services/portal.service';

interface AdminData {
  username: string;
  email: string;
  password: string;
  role: string;
}

@Component({
  selector: 'app-admin-register',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './register-admins.component.html',
  styleUrls: ['./register-admins.component.css']
})
export class AdminRegisterComponent {
  form: FormGroup;

  constructor(private fb: FormBuilder, private svc: PortalService, private router: Router) {
    this.form = this.fb.group({
      username: ['', Validators.required],
      email: ['', [Validators.required, Validators.email]],
      password: ['', Validators.required],
      confirmPassword: ['', Validators.required]
    }, { validators: this.passwordMatchValidator });
  }

  passwordMatchValidator(group: AbstractControl): { [key: string]: boolean } | null {
    const pass = group.get('password')?.value;
    const confirm = group.get('confirmPassword')?.value;
    return pass === confirm ? null : { mismatch: true };
  }

  onSubmit(): void {
    if (this.form.invalid) {
      alert('Please fix the form errors before submitting');
      return;
    }

    const data: AdminData = {
      username: this.form.value.username,
      email: this.form.value.email,
      password: this.form.value.password,
      role: 'admin'
    };

    this.svc.createAdmin(data).subscribe({
      next: (response) => {
        console.log('Admin registration successful:', response);
        alert('Admin registered successfully!');
        this.router.navigate(['/officer/dashboard']);
      },
      error: (error) => {
        console.error('Registration error:', error);
        alert('Registration failed: ' + (error.error?.message || error.message || 'Unknown error'));
      }
    });
  }
}
