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
  currentConfidenceFilter = 'All';

  // Dynamic Stats
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
    this.lowCount = 0;
    this.mediumCount = 0;
    this.highCount = 0;

    this.applications.forEach(app => {
      const confidence = app.confidenceLevel ? app.confidenceLevel.toLowerCase() : '';
      if (confidence === 'low') this.lowCount++;
      else if (confidence === 'medium') this.mediumCount++;
      else if (confidence === 'high') this.highCount++;
    });
  }

  // 2. Filter by both Search Text AND Confidence Filter Buttons
  filterApplications() {
    let tempApps = this.applications;

    // Apply Confidence Filter
    if (this.currentConfidenceFilter !== 'All') {
      tempApps = tempApps.filter(app => 
        app.confidenceLevel && app.confidenceLevel.toLowerCase() === this.currentConfidenceFilter.toLowerCase()
      );
    }

    // Apply Search Term
    if (this.searchTerm) {
      const term = this.searchTerm.toLowerCase();
      tempApps = tempApps.filter(app => 
        app.companyName.toLowerCase().includes(term) || 
        app.referenceId.toLowerCase().includes(term)
      );
    }

    this.filteredApplications = tempApps;
  }

  // Update confidence filter and re-trigger filter logic
  setFilter(level: string) {
    this.currentConfidenceFilter = level;
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