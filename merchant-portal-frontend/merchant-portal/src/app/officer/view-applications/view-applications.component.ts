import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
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
  confirmPending: string | null = null;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private svc: PortalService
  ) { }

  ngOnInit(): void {
    const appId = this.route.snapshot.paramMap.get('id');
    if (appId) {
      this.svc.getApplicationById(appId).subscribe({
        next: (data) => {
          this.application = data;
          console.log('Application data:', data);
          console.log('Confidence Level:', data.confidenceLevel);
          console.log('Face Match Score:', data.faceMatchScore);
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

  promptConfirm(newStatus: string): void {
    this.confirmPending = newStatus;
  }

  cancelConfirm(): void {
    this.confirmPending = null;
  }

  confirmAction(): void {
    if (!this.confirmPending) return;
    this.updateStatus(this.confirmPending);
    this.confirmPending = null;
  }

  updateStatus(newStatus: string): void {
    if (!this.application) return;

    this.svc.updateApplicationStatus(this.application.id, newStatus).subscribe({
      next: (updatedApplication) => {
        this.application.status = updatedApplication.status;
        alert(`Application has been ${newStatus.toLowerCase()}!`);
        this.router.navigate(['/officer/dashboard']);
      },
      error: (err) => {
        console.error('Failed to update status:', err);
        alert('Failed to update status. Please try again.');
      }
    });
  }

  /**
   * Fetches a document from the backend with the JWT token attached,
   * converts it to a temporary blob URL, and opens it in a new browser tab.
   * This is needed because /api/documents/** is a secured endpoint —
   * a plain <a href> cannot attach the Authorization header.
   *
   * @param documentId the UUID string stored in the application field (e.g. idUploadFront)
   */
  openDocument(documentId: string): void {
    if (!documentId) return;
    this.svc.getDocumentBlob(documentId).subscribe({
      next: (blob) => {
        // Create a temporary in-memory URL pointing to the blob data
        const blobUrl = URL.createObjectURL(blob);
        // Open the file in a new tab — browser renders images and PDFs inline
        window.open(blobUrl, '_blank');
        // Revoke the temporary URL after a short delay to free memory
        setTimeout(() => URL.revokeObjectURL(blobUrl), 10000);
      },
      error: (err) => {
        console.error('Failed to load document:', err);
        alert('Could not load document. It may have been deleted or expired.');
      }
    });
  }
}
