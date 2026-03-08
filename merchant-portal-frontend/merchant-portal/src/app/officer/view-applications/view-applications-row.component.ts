import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-application-row',
  standalone: true,
  imports: [CommonModule],
  template: `
    <tr>
      <td>{{ index }}</td>
      <td>{{ application.referenceId }}</td>
      <td>{{ application.companyName }}</td>
      <td>
        <span [ngClass]="{
          'badge bg-warning': application.status === 'Pending',
          'badge bg-success': application.status === 'Approved',
          'badge bg-danger': application.status === 'Rejected'
        }">{{ application.status }}</span>
      </td>
      <td>
        <button class="btn btn-sm btn-primary" (click)="viewDetails.emit()">Open</button>
      </td>
    </tr>
  `
})
export class ApplicationRowComponent {
  @Input() application: any;
  @Input() index!: number;
  @Output() viewDetails = new EventEmitter<void>();
}
