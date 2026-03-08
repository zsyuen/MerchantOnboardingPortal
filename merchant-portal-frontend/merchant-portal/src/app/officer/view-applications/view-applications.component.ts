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