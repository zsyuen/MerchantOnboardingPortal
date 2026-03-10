import { Component, ElementRef, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import QRCode from 'qrcode';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css']
})
export class LoginComponent {

  @ViewChild('qrCanvas') qrCanvas!: ElementRef<HTMLCanvasElement>;
  loginForm: FormGroup;
  totpForm: FormGroup;
  errorMsg = '';
  revokedMsg = '';
  submitting = false;
  loginStep: 'CREDENTIALS' | 'TOTP_SETUP' | 'TOTP_VERIFY' = 'CREDENTIALS';
  secretKey: string = '';
  tempUsername: string = '';

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router
  ) {
    this.loginForm = this.fb.group({
      username: ['', Validators.required],
      password: ['', Validators.required]
    });

    this.totpForm = this.fb.group({
      totpCode: ['', [Validators.required, Validators.pattern(/^\d{6}$/)]]
    });
  }

  // Helper method to clear lingering Bootstrap modal backdrops
  private cleanupModal(): void {
    document.body.classList.remove('modal-open');
    const backdrops = document.querySelectorAll('.modal-backdrop');
    backdrops.forEach((backdrop) => backdrop.remove());
    document.body.style.overflow = 'auto';
  }

  onLogin(): void {
    if (this.loginForm.invalid) {
      return;
    }

    this.submitting = true;
    this.errorMsg = '';

    this.authService.login(this.loginForm.value).subscribe({
      next: (response) => {
        console.log('Login Response:', response);
        // CASE 1: Login Complete (e.g. 2FA not enabled globally, or specific logic)
        if (response.token) {
          this.finalizeLogin(response);
          return;
        }

        /// CASE 2: Mandatory Setup Required (User has no secret key yet)
        if (response.action === 'SETUP_REQUIRED') {
          // Use username from form if backend doesn't return it
          this.tempUsername = response.username || this.loginForm.value.username;
          // Save the temporary token if provided by backend
          if (response.tempToken) {
            console.log('Saving temp token');
            this.authService.saveTempToken(response.tempToken);
          } else {
            console.warn('No tempToken in response');
          }
          this.initiateSetup(this.tempUsername);
          return;
        }

        // CASE 3: Verification Required (User has secret key, needs to prove it)
        if (response.action === 'CODE_REQUIRED') {
          this.loginStep = 'TOTP_VERIFY';
          this.submitting = false;
        }
      },
      error: (err) => {
        // Account revoked - show specific message and stay on login screen
        if (err.status === 403) {
          alert('Your account has been revoked. Please contact the reviewer.');
          this.errorMsg = '';
        // Sometimes 401 is used for "Code Required" depending on backend implementation
        } else if (err.status === 401 && err.error?.action === 'CODE_REQUIRED') {
          this.tempUsername = this.loginForm.value.username;
          this.loginStep = 'TOTP_VERIFY';
        } else {
          alert(err.error?.error || err.error?.message || 'Invalid username or password.');
        }
        this.submitting = false;
      }
    });
  }

  // Fetch the secret and Generate QR Code
  initiateSetup(username: string): void {
    this.authService.setupTotp(username).subscribe({ // Assuming setupTotp handles the username internally or via token
      next: (res) => {
        console.log('Setup response:', res);
        console.log('Secret:', res.secret);
        this.secretKey = res.secret;
        this.loginStep = 'TOTP_SETUP';
        this.submitting = false;

        // Wait for Angular to render the canvas, then draw the QR code
        setTimeout(() => {
          this.generateQRCode(res.otpAuthUrl);
        }, 0);
      },
      error: (err) => {
        console.error('Setup TOTP Error:', err);
        this.errorMsg = err.error?.message || err.message || "Could not start 2FA setup.";
        this.submitting = false;
      }
    });
  }

  generateQRCode(url: string): void {
    if (this.qrCanvas && this.qrCanvas.nativeElement) {
      QRCode.toCanvas(this.qrCanvas.nativeElement, url, { width: 200, margin: 1 }, (error) => {
        if (error) console.error(error);
      });
    }
  }

  onVerifyTotp(): void {
    if (this.totpForm.invalid) {
      return;
    }
    this.submitting = true;
    this.errorMsg = '';

    const code = this.totpForm.value.totpCode;

    if (this.loginStep === 'TOTP_SETUP') {
      const verifyPayload = {
        username: this.tempUsername,
        code: code
      };

      this.authService.verifyTotp(verifyPayload).subscribe({
        next: (response) => {
          console.log('Verification successful:', response);

          if (response.token) {
            this.finalizeLogin(response);
          } else {
            this.authService.clearTempToken();
            alert("Setup Successful! Please log in again.");
            this.resetToLogin();
          }
        },
        error: (err) => {
          console.error('Verification error:', err);
          alert(err.error?.error || err.error?.message || 'Invalid code.');
          this.submitting = false;
        }
      });
      return;
    }

    // Normal login with 2FA code
    const loginPayload = {
      username: this.tempUsername || this.loginForm.value.username,
      password: this.loginForm.value.password,
      code: parseInt(code, 10)
    };

    this.authService.login(loginPayload).subscribe({
      next: (res) => {
        console.log('Login successful:', res);
        this.finalizeLogin(res);
      },
      error: (err) => {
        console.error('Login error:', err);
        alert(err.error?.error || err.error?.message || 'Invalid 2FA Code.');
        this.submitting = false;
        this.totpForm.reset();
      }
    });
  }

  finalizeLogin(response: any): void {
    this.authService.saveToken(response.token);
    this.authService.saveRole(response.role);

    // Fetch and store permissions from backend
    this.authService.fetchAndStorePermissions().subscribe({
      next: () => {
        const username = this.tempUsername || this.loginForm.value.username;
        alert(`Login successful, welcome ${username}!`);
        this.cleanupModal();
        this.router.navigate(['/officer/dashboard']);
      },
      error: () => {
        // Proceed even if permissions fetch fails
        const username = this.tempUsername || this.loginForm.value.username;
        alert(`Login successful, welcome ${username}!`);
        this.cleanupModal();
        this.router.navigate(['/officer/dashboard']);
      }
    });
  }

  resetToLogin(): void {
    this.loginStep = 'CREDENTIALS';
    this.submitting = false;
    this.errorMsg = '';
    this.loginForm.reset();
    this.totpForm.reset();
    this.secretKey = '';
  }

  goBack(): void {
    if (this.loginStep === 'TOTP_SETUP') {
      // Clear temp token and any stale auth, then reset to login screen
      this.authService.clearTempToken();
      localStorage.removeItem('auth-token');
      localStorage.removeItem('auth-role');
      localStorage.removeItem('auth-permissions');
      this.resetToLogin();
      return;
    }
    this.resetToLogin();
  }
}
