import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { Title } from '@angular/platform-browser';
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
  pagedApplications: any[] = [];

  // Pagination
  currentPage = 1;
  pageSize = 10;
  totalPages = 1;

  isLoading = true;
  errorMsg = '';
  isReviewer = false;
  
  // Search and Filter
  searchTerm = '';
  pendingSearchTerm = '';
  statusFilter = 'Pending';
  confidenceFilter = 'All';

  // Dynamic Stats
  pendingTotal = 0;
  lowCount = 0;
  mediumCount = 0;
  highCount = 0;

  // Drawer
  showSettings = false;

  constructor(
    private svc: PortalService, 
    private router: Router,
    private authService: AuthService,
    private titleService: Title
  ) { }

  ngOnInit() {
    this.titleService.setTitle('Bank Officer Portal');
    console.log('Stored role:', this.authService.getRole());
    this.isReviewer = this.authService.isReviewer(); 
    console.log('isReviewer:', this.isReviewer);

    this.svc.getApplications().subscribe({
      next: (data: any[]) => {
        this.applications = data;
        this.calculateStats();
        this.filterApplications();
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

    // Sort: newest submission date first
    tempApps.sort((a, b) => {
      const dateA = a.submissionDate ? new Date(a.submissionDate).getTime() : 0;
      const dateB = b.submissionDate ? new Date(b.submissionDate).getTime() : 0;
      return dateB - dateA; // descending: newest first
    });

    this.filteredApplications = tempApps;
    this.currentPage = 1;
    this.updatePage();
  }

  updatePage() {
    this.totalPages = Math.max(1, Math.ceil(this.filteredApplications.length / this.pageSize));
    const start = (this.currentPage - 1) * this.pageSize;
    this.pagedApplications = this.filteredApplications.slice(start, start + this.pageSize);
  }

  goToPage(page: number) {
    if (page < 1 || page > this.totalPages) return;
    this.currentPage = page;
    this.updatePage();
  }

  get pageNumbers(): number[] {
    return Array.from({ length: this.totalPages }, (_, i) => i + 1);
  }

  triggerSearch() {
    this.searchTerm = this.pendingSearchTerm;
    this.filterApplications();
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
          this.calculateStats();
          this.filterApplications();
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