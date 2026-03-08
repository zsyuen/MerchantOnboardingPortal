import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { PortalService } from '../../services/portal.service';

@Component({
  selector: 'app-manage-admins',
  standalone: true,
  imports: [CommonModule, RouterLink, FormsModule],
  templateUrl: './manage-admins.component.html',
  styleUrls: ['./manage-admins.component.css']
})
export class ManageAdminsComponent implements OnInit {
  admins: any[] = [];
  filteredAdmins: any[] = [];
  searchTerm = '';
  isLoading = true;
  errorMsg = '';

  constructor(private svc: PortalService) { }

  ngOnInit(): void {
    this.loadAdmins();
  }

  loadAdmins(): void {
    this.isLoading = true;
    this.svc.getAdmins().subscribe({
      next: (data) => {
        console.log('Admins data:', data);
        // Default status to 'Granted'
        this.admins = data.map((admin: any) => ({
          ...admin,
          status: admin.status || 'Granted'
        }));
        this.filteredAdmins = this.admins;
        this.isLoading = false;
      },
      error: (err) => {
        console.error('Failed to load admins', err);
        this.errorMsg = 'Could not load the list of admins.';
        this.isLoading = false;
      }
    });
  }

  filterAdmins(): void {
    if (!this.searchTerm) {
      this.filteredAdmins = this.admins;
    } else {
      const term = this.searchTerm.toLowerCase();
      this.filteredAdmins = this.admins.filter(admin => 
        (admin.username && admin.username.toLowerCase().includes(term)) || 
        (admin.email && admin.email.toLowerCase().includes(term))
      );
    }
  }

  grantAccess(adminId: number): void {
    this.svc.grantAdmin(adminId).subscribe({
      next: () => {
        alert('Access granted successfully!');
        this.loadAdmins(); // Refresh the list
      },
      error: (err) => {
        console.error('Error granting access', err);
        alert('Failed to grant access.');
      }
    });
  }

  revokeAccess(adminId: number): void {
    if (confirm('Are you sure you want to revoke access for this admin?')) {
      this.svc.revokeAdmin(adminId).subscribe({
        next: () => {
          alert('Access revoked successfully!');
          this.loadAdmins(); // Refresh the list
        },
        error: (err) => {
          console.error('Error revoking access', err);
          alert('Failed to revoke access.');
        }
      });
    }
  }
}