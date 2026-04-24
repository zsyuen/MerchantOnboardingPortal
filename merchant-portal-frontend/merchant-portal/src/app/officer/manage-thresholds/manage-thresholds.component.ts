import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Title } from '@angular/platform-browser';
import { PortalService } from '../../services/portal.service';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-manage-thresholds',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './manage-thresholds.component.html',
  styleUrls: ['./manage-thresholds.component.css']
})
export class ManageThresholdsComponent implements OnInit {
  readonly DEFAULT_HIGH   = 0.70;
  readonly DEFAULT_MEDIUM = 0.55;

  thresholdHigh   = this.DEFAULT_HIGH;
  thresholdMedium = this.DEFAULT_MEDIUM;

  savedHigh   = this.DEFAULT_HIGH;
  savedMedium = this.DEFAULT_MEDIUM;

  isSaving   = false;
  successMsg = '';
  errorMsg   = '';

  isReviewer = false;

  constructor(private svc: PortalService, private titleService: Title, private authService: AuthService) {}

  ngOnInit(): void {
    this.titleService.setTitle('Bank Officer Portal');
    this.isReviewer = this.authService.isReviewer();
    this.svc.getFacialThresholds().subscribe({
      next: (data: any) => {
        this.thresholdHigh   = data.thresholdHigh;
        this.thresholdMedium = data.thresholdMedium;
        this.savedHigh       = data.thresholdHigh;
        this.savedMedium     = data.thresholdMedium;
      },
      error: (err: any) => console.error('Failed to load thresholds', err)
    });
  }

  get isValid(): boolean {
    return +this.thresholdMedium < +this.thresholdHigh;
  }

  get isDirty(): boolean {
    return +this.thresholdHigh !== +this.savedHigh ||
           +this.thresholdMedium !== +this.savedMedium;
  }

  onThresholdChange(): void {
    this.successMsg = '';
    this.errorMsg   = '';
  }

  resetToDefaults(): void {
    this.thresholdHigh   = this.DEFAULT_HIGH;
    this.thresholdMedium = this.DEFAULT_MEDIUM;
    this.successMsg = '';
    this.errorMsg   = '';
  }

  save(): void {
    if (!this.isValid) return;
    this.successMsg = '';
    this.errorMsg   = '';
    this.isSaving   = true;

    this.svc.updateFacialThresholds(this.thresholdHigh, this.thresholdMedium).subscribe({
      next: () => {
        this.successMsg  = 'Thresholds updated successfully!';
        this.savedHigh   = this.thresholdHigh;
        this.savedMedium = this.thresholdMedium;
        this.isSaving    = false;
      },
      error: (err: any) => {
        this.errorMsg = err.error?.message || 'Failed to update thresholds.';
        this.isSaving = false;
      }
    });
  }
}
