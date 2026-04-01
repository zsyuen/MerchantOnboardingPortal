import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { PortalService } from '../../services/portal.service';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink, FormsModule],
  templateUrl: './view-dashboard.component.html',
  styleUrls: ['./view-dashboard.component.css']
})
export class DashboardComponent implements OnInit {
  applications: any[] = [];
  filteredApplications: any[] = [];
  
  isLoading = true;
  errorMsg = '';
  isReviewer = false;
  
  // Search and Filter
  searchTerm = '';
  statusFilter = 'Pending';
  confidenceFilter = 'All';

  // Dynamic Stats
  pendingTotal = 0;
  lowCount = 0;
  mediumCount = 0;
  highCount = 0;

  constructor(
    private svc: PortalService, 
    private router: Router,
    private authService: AuthService
  ) { }

  ngOnInit() {
    console.log('Stored role:', this.authService.getRole());
    this.isReviewer = this.authService.isReviewer(); 
    console.log('isReviewer:', this.isReviewer);

    this.svc.getApplications().subscribe({
      next: (data: any[]) => {
        this.applications = data;
        this.filteredApplications = data;
        this.calculateStats();
        this.isLoading = false;
      },
      error: (err) => {
        console.error('Error loading applications', err);
        this.errorMsg = 'Failed to load applications.';
        this.isLoading = false;
      }
    });
  }

  // 1. Calculate the dynamic stats for the top cards
  calculateStats() {
    this.pendingTotal = 0;
    this.lowCount = 0;
    this.mediumCount = 0;
    this.highCount = 0;

    this.applications.forEach(app => {
      // Only count actionable applications matching "Pending" status
      if (app.status && app.status.toLowerCase() === 'pending') {
        this.pendingTotal++;
        const confidence = app.confidenceLevel ? app.confidenceLevel.toLowerCase() : '';
        if (confidence === 'low') this.lowCount++;
        else if (confidence === 'medium') this.mediumCount++;
        else if (confidence === 'high') this.highCount++;
      }
    });
  }

  // 2. Filter by status, confidence, and Search
  filterApplications() {
    let tempApps = [...this.applications];

    if (this.statusFilter && this.statusFilter !== 'All') {
      tempApps = tempApps.filter(app => app.status && app.status.toLowerCase() === this.statusFilter.toLowerCase());
    }

    if (this.confidenceFilter && this.confidenceFilter !== 'All') {
      tempApps = tempApps.filter(app => app.confidenceLevel && app.confidenceLevel.toLowerCase() === this.confidenceFilter.toLowerCase());
    }

    // Apply Search Term
    if (this.searchTerm) {
      const term = this.searchTerm.toLowerCase();
      tempApps = tempApps.filter(app => 
        (app.companyName && app.companyName.toLowerCase().includes(term)) || 
        (app.referenceId && app.referenceId.toLowerCase().includes(term))
      );
    }

    // Apply Sorting: Pending always at the top,
    // then sort chronologically from oldest to newest
    tempApps.sort((a, b) => {
      const statusA = a.status ? a.status.toLowerCase() : '';
      const statusB = b.status ? b.status.toLowerCase() : '';
      
      // If one is pending and the other isn't, pending moves precisely to the top
      if (statusA === 'pending' && statusB !== 'pending') return -1;
      if (statusA !== 'pending' && statusB === 'pending') return 1;

      // Otherwise, parse their respective dates to sort oldest to newest (ascending)
      const dateA = a.submissionDate ? new Date(a.submissionDate).getTime() : 0;
      const dateB = b.submissionDate ? new Date(b.submissionDate).getTime() : 0;
      return dateA - dateB;
    });

    this.filteredApplications = tempApps;
  }

  setView(status: string, confidence: string) {
    this.statusFilter = status;
    this.confidenceFilter = confidence;
    this.filterApplications();
  }

  delete(appId: number): void {
    if (confirm(`Are you sure you want to delete application ${appId}?`)) {
      this.svc.deleteApplication(String(appId)).subscribe({
        next: () => {
          this.applications = this.applications.filter(app => app.id !== appId);
          this.calculateStats(); // Recalculate cards after delete
          this.filterApplications(); // Re-filter table after delete
          alert('Application deleted successfully.');
        },
        error: (err) => {
          console.error('Failed to delete application', err);
          alert('Failed to delete application.');
        }
      });
    }
  }

  logout(): void {
    this.authService.logout();
  }
}