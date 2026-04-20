import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { environment } from '../../environments/environment';
import { Observable, catchError, throwError, tap } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class PortalService {

  // Use a base URL for the entire API.
  private apiBase = environment.apiBase;

  constructor(private http: HttpClient) { }

  //helper method to attach JWT token
  private getAuthHeaders(): HttpHeaders {
    const token = localStorage.getItem('auth-token');
    let headers = new HttpHeaders();
    if (token) {
      headers = headers.set('Authorization', `Bearer ${token}`);
    }
    return headers;
  }

  updateApplicationStatus(id: string, status: string): Observable<any> {
    const payload = { status: status };
    return this.http.put(`${this.apiBase}/applications/${id}/status`, payload, { headers: this.getAuthHeaders() });
  }

  deleteApplication(id: string): Observable<any> {
    return this.http.delete(`${this.apiBase}/applications/${id}`, { headers: this.getAuthHeaders() });
  }

  //public endpoint, no auth headers needed
  submitApplication(formData: FormData): Observable<any> {
  return this.http.post(`${this.apiBase}/applications`, formData).pipe(
    tap(res => console.log('FormData submit response:', res)),
    catchError(err => {
      console.error('❌ Service Error:', err);
      return throwError(() => err);
    })
  );
  }

  //Fetches a single application by its reference ID.
  getApplicationByRef(refId: string): Observable<any> {
    return this.http.get(`${this.apiBase}/applications/ref/${refId}`);
  }

  //Fetches a single application by its database ID.
  getApplicationById(id: string): Observable<any> {
    return this.http.get(`${this.apiBase}/applications/${id}`, { headers: this.getAuthHeaders() });
  }

   //Fetches all merchant applications.
  getApplications(): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiBase}/applications`, { headers: this.getAuthHeaders() });
  }

  createAdmin(data: any): Observable<any> {
    return this.http.post(`${this.apiBase}/admins`, data, { headers: this.getAuthHeaders() });
  }

  getAdmins(): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiBase}/admins`, { headers: this.getAuthHeaders() });
  }

  grantAdmin(adminId: number): Observable<any> {
    return this.http.post(`${this.apiBase}/admins/${adminId}/grant`, {}, { headers: this.getAuthHeaders() });
  }

  revokeAdmin(adminId: number): Observable<any> {
    return this.http.post(`${this.apiBase}/admins/${adminId}/revoke`, {}, { headers: this.getAuthHeaders() });
  }

  deleteAdmin(adminId: number): Observable<any> {
    return this.http.delete(`${this.apiBase}/admins/${adminId}`, { headers: this.getAuthHeaders() });
  }

  /**
   * Fetches a document by its UUID from the backend as a binary Blob.
   * responseType: 'blob' tells Angular to treat the response as raw binary data
   * instead of trying to parse it as JSON.
   * The Authorization header is included so the secured /api/documents endpoint
   * accepts the request.
   */
  getDocumentBlob(documentId: string): Observable<Blob> {
    return this.http.get(`${this.apiBase}/documents/${documentId}`, {
      headers: this.getAuthHeaders(),
      responseType: 'blob'
    });
  }
}
