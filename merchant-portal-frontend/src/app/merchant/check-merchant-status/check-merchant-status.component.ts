import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { PortalService } from '../../services/portal.service';
import { Title } from '@angular/platform-browser';

@Component({
  selector: 'app-merchant-status',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './check-merchant-status.component.html',
  styleUrls: ['./check-merchant-status.component.css']
})
export class MerchantStatusComponent implements OnInit {
  refId: string = '';
  result: any = null;
  errorMsg: string = '';

  constructor(private svc: PortalService, private titleService: Title) { }

  ngOnInit(): void {
    this.titleService.setTitle('Merchant Onboarding Portal');
  }

  checkStatus(): void {
    this.result = null;
    this.errorMsg = '';

    if (!this.refId || this.refId.trim() === '') {
      alert('Please enter a Reference ID to search.');
      return;
    }

    this.svc.getApplicationByRef(this.refId).subscribe({
      next: (res: any) => {
        this.result = res;
      },
      error: (err: any) => {
        console.error('Failed to fetch status:', err);
        this.errorMsg = 'Application not found.';
      }
    });
  }
}