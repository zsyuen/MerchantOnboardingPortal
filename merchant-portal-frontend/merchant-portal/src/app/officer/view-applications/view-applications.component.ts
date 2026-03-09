import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { PortalService } from '../../services/portal.service';

@Component({
  selector: 'app-view-application',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './view-applications.component.html',
  styleUrls: ['./view-applications.component.css']
})
export class ViewApplicationComponent implements OnInit {
  application: any;
  isLoading = true;
  errorMsg = '';
  uploadsUrl = 'http://localhost:8080/uploads/';

  constructor(
    private route: ActivatedRoute,
    private svc: PortalService
  ) { }

  ngOnInit(): void {
    const appId = this.route.snapshot.paramMap.get('id');
    if (appId) {
      this.svc.getApplicationById(appId).subscribe({
        next: (data) => {
          this.application = data;
          this.isLoading = false;
        },
        error: (err) => {
          this.errorMsg = 'Could not load application details.';
          this.isLoading = false;
          console.error(err);
        }
      });
    }
  }

  /** Handles both ISO string ("2025-09-17") and Java array ([2025,9,17]) date formats */
  formatDate(val: any): string {
    if (!val) return '-';
    let date: Date;
    if (Array.isArray(val)) {
      const [y, m, d] = val;
      date = new Date(y, m - 1, d);
    } else {
      date = new Date(val);
    }
    if (isNaN(date.getTime())) return '-';
    const dd = String(date.getDate()).padStart(2, '0');
    const mm = String(date.getMonth() + 1).padStart(2, '0');
    const yyyy = date.getFullYear();
    return `${dd}-${mm}-${yyyy}`;
  }

  updateStatus(newStatus: string): void {
    if (!this.application) return;

    this.svc.updateApplicationStatus(this.application.id, newStatus).subscribe({
      next: (updatedApplication) => {
        this.application.status = updatedApplication.status;
        alert(`Application has been ${newStatus.toLowerCase()}!`);
      },
      error: (err) => {
        console.error('Failed to update status:', err);
        alert('Failed to update status. Please try again.');
      }
    });
  }
}