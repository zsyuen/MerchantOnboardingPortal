import { Component, OnInit, ViewChild, ElementRef } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { PortalService } from '../../services/portal.service';
import * as faceapi from 'face-api.js';

@Component({
  selector: 'app-merchant-register',
  templateUrl: './register-merchants.component.html',
  styleUrls: ['./register-merchants.component.css'],
  standalone: true,
  imports: [ReactiveFormsModule, CommonModule, RouterLink],
})
export class MerchantRegisterComponent implements OnInit {
  form!: FormGroup;
  submitting = false;
  currentStep = 1;

  @ViewChild('videoElement') videoElement!: ElementRef<HTMLVideoElement>;
  @ViewChild('canvasElement') canvasElement!: ElementRef<HTMLCanvasElement>;

  isCameraActive = false;
  isImageCaptured = false;
  isFaceDetected = false;
  capturedImagePreview: string | null = null;
  capturedImageBlob: Blob | null = null;
  modelsLoaded = false;
  
  // Live feedback message
  feedbackMessage: string = '';
  feedbackClass: string = ''; // 'success', 'warning', 'danger'

  // Scheme & Facility checkbox options
  schemeOptions = ['Visa', 'MasterCard', 'JCB', 'UPI', 'Wechat', 'Alipay', 'Amex'];
  facilityOptions = ['Full Payment', 'Instalment', 'Redemption', 'DCC', 'MCC', 'Recurring', 'e-Commerce', 'MOTO', 'WeChat/ Alipay', 'K+shop'];
  selectedSchemes: string[] = [];
  selectedFacilities: string[] = [];

  toggleScheme(value: string): void {
    const idx = this.selectedSchemes.indexOf(value);
    if (idx > -1) this.selectedSchemes.splice(idx, 1);
    else this.selectedSchemes.push(value);
    this.form.get('schemeRequired')?.setValue(this.selectedSchemes.length ? this.selectedSchemes.join(',') : '');
    this.form.get('schemeRequired')?.markAsTouched();
  }

  toggleFacility(value: string): void {
    const idx = this.selectedFacilities.indexOf(value);
    if (idx > -1) this.selectedFacilities.splice(idx, 1);
    else this.selectedFacilities.push(value);
    this.form.get('facilityRequired')?.setValue(this.selectedFacilities.length ? this.selectedFacilities.join(',') : '');
    this.form.get('facilityRequired')?.markAsTouched();
  }

  constructor(private fb: FormBuilder, private portal: PortalService, private router: Router) { }

  async ngOnInit(): Promise<void> {
    this.form = this.fb.group({
      businessRegNo: ['', Validators.required],
      companyName: ['', Validators.required],
      incorporationDate: ['', Validators.required],
      countryOfCorp: ['', Validators.required],
      merchantNameEn: ['', Validators.required],
      merchantNameLocal: ['', Validators.required],
      taxId: ['', Validators.required],
      entityType: ['', Validators.required],
      address1: ['', Validators.required],
      address2: [''],
      address3: [''],
      address4: [''],
      city: ['', Validators.required],
      state: ['', Validators.required],
      postal: ['', [Validators.required, Validators.pattern('^[0-9]{4,10}$')]],
      country: ['', Validators.required],
      phone1: ['', [Validators.required, Validators.pattern('^[0-9]+$')]],
      phone2: ['', Validators.pattern('^[0-9]+$')],
      ownerFirstName: ['', Validators.required],
      ownerLastName: ['', Validators.required],
      ownerEmail: ['', [Validators.required, Validators.pattern('^[a-zA-Z0-9._%+\-]+@[a-zA-Z0-9.\-]+\.[a-zA-Z]{2,}$')]],
      ownerDob: ['', Validators.required],
      ownerIdNo: ['', Validators.required],
      ownerNationality: ['', Validators.required],
      ownerIdFront: [null, Validators.required],
      ownerIdBack: [null, Validators.required],
      industry: ['', Validators.required],
      businessType: ['', Validators.required],
      numEmployees: ['', [Validators.required, Validators.pattern('^[0-9]+$')]],
      schemeRequired: ['', Validators.required],
      facilityRequired: ['', Validators.required],
      proofOfBusiness: [null, Validators.required],
    });
  }

  async loadModels() {
    if (this.modelsLoaded) return;
    
    try {
      this.feedbackMessage = 'Loading face detection models...';
      this.feedbackClass = 'warning';
      
      const MODEL_URL = 'https://cdn.jsdelivr.net/npm/@vladmandic/face-api/model';
      
      await Promise.all([
        faceapi.nets.ssdMobilenetv1.loadFromUri(MODEL_URL),
        faceapi.nets.faceLandmark68Net.loadFromUri(MODEL_URL),
      ]);
      
      this.modelsLoaded = true;
      this.feedbackMessage = 'Models loaded. Starting camera...';
      this.feedbackClass = 'success';
      console.log('✅ Models Loaded from CDN');
    } catch (error) {
      console.error('Failed to load models:', error);
      this.feedbackMessage = 'Failed to load models. Check connection.';
      this.feedbackClass = 'danger';
      alert('Failed to load face detection models. Please check your internet connection.');
    }
  }

  async startCamera() {
    if (!this.modelsLoaded) {
      await this.loadModels();
    }

    this.isImageCaptured = false;
    this.capturedImageBlob = null;
    this.isCameraActive = true;
    this.feedbackMessage = 'Accessing camera...';
    this.feedbackClass = 'warning';

    navigator.mediaDevices.getUserMedia({ video: true })
      .then(stream => {
        this.videoElement.nativeElement.srcObject = stream;
        this.feedbackMessage = 'Position your face in the frame';
        this.feedbackClass = 'warning';
        this.detectFace();
      })
      .catch(err => {
        console.error("Camera error:", err);
        this.feedbackMessage = 'Camera access denied';
        this.feedbackClass = 'danger';
      });
  }

  async detectFace() {
    if (!this.isCameraActive || this.isImageCaptured) return;

    const video = this.videoElement.nativeElement;
    
    if(video.readyState === 4) {
      const detections = await faceapi.detectAllFaces(
        video, 
        new faceapi.SsdMobilenetv1Options({ minConfidence: 0.5 })
      ).withFaceLandmarks();
      
      if (detections.length === 0) {
        this.isFaceDetected = false;
        this.feedbackMessage = 'No face detected. Please look at the camera.';
        this.feedbackClass = 'danger';
      } else if (detections.length > 1) {
        this.isFaceDetected = false;
        this.feedbackMessage = 'Multiple faces detected. Only one person allowed.';
        this.feedbackClass = 'danger';
      } else {
        const detection = detections[0];
        const box = detection.detection.box;
        const videoWidth = video.videoWidth;
        const videoHeight = video.videoHeight;
        
        // Calculate face size relative to frame
        const faceWidth = box.width;
        const faceHeight = box.height;
        const faceArea = faceWidth * faceHeight;
        const frameArea = videoWidth * videoHeight;
        const faceRatio = faceArea / frameArea;
        
        // Check if face is centered
        const faceCenterX = box.x + box.width / 2;
        const faceCenterY = box.y + box.height / 2;
        const frameCenterX = videoWidth / 2;
        const frameCenterY = videoHeight / 2;
        const offsetX = Math.abs(faceCenterX - frameCenterX);
        const offsetY = Math.abs(faceCenterY - frameCenterY);
        
        if (faceRatio < 0.08) {
          this.isFaceDetected = false;
          this.feedbackMessage = 'Move closer to the camera';
          this.feedbackClass = 'warning';
        } else if (faceRatio > 0.4) {
          this.isFaceDetected = false;
          this.feedbackMessage = 'Move further from the camera';
          this.feedbackClass = 'warning';
        } else if (offsetX > videoWidth * 0.15) {
          this.isFaceDetected = false;
          this.feedbackMessage = 'Center your face horizontally';
          this.feedbackClass = 'warning';
        } else if (offsetY > videoHeight * 0.15) {
          this.isFaceDetected = false;
          this.feedbackMessage = 'Center your face vertically';
          this.feedbackClass = 'warning';
        } else {
          this.isFaceDetected = true;
          this.feedbackMessage = '✓ Perfect! Ready to capture';
          this.feedbackClass = 'success';
        }
      }
    } else {
      this.feedbackMessage = 'Initializing camera...';
      this.feedbackClass = 'warning';
    }

    requestAnimationFrame(() => this.detectFace());
  }

  capturePhoto() {
    if (!this.isFaceDetected) {
      alert("No face detected! Please position yourself clearly.");
      return;
    }

    const video = this.videoElement.nativeElement;
    const canvas = document.createElement('canvas');
    canvas.width = video.videoWidth;
    canvas.height = video.videoHeight;
    canvas.getContext('2d')?.drawImage(video, 0, 0);

    canvas.toBlob((blob) => {
      this.capturedImageBlob = blob;
      this.capturedImagePreview = canvas.toDataURL('image/jpeg');
      this.isImageCaptured = true;
      this.isCameraActive = false;
      this.feedbackMessage = 'Photo captured successfully!';
      this.feedbackClass = 'success';

      const stream = video.srcObject as MediaStream;
      stream.getTracks().forEach(track => track.stop());
    }, 'image/jpeg');
  }

  goToVerification(): void {
    this.form.markAllAsTouched();
    if (this.form.invalid) {
      // Scroll to the first invalid field
      const firstInvalid = document.querySelector('.is-invalid');
      if (firstInvalid) {
        firstInvalid.scrollIntoView({ behavior: 'smooth', block: 'center' });
      }
      return;
    }
    this.currentStep = 2;
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  goBack(): void {
    this.currentStep = 1;
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  submit(): void {
    if (this.form.invalid || !this.capturedImageBlob) {
      this.form.markAllAsTouched();
      if (!this.capturedImageBlob) alert("Please take a selfie verification photo.");
      console.error('Form is invalid or photo missing.');
      return;
    }

    this.submitting = true;
    const formData = new FormData();
    const formValues = this.form.getRawValue();

    Object.keys(formValues).forEach(key => {
      formData.append(key, formValues[key]);
    });

    formData.append('liveSelfie', this.capturedImageBlob, 'selfie.jpg');

    this.portal.submitApplication(formData).subscribe({
      next: (res) => {
        this.submitting = false;
        console.log('Submission successful!', res);
        alert(`Application submitted successfully!\nYour Reference ID is: ${res.referenceId}`);
        this.form.reset();
        this.isImageCaptured = false;
        this.capturedImageBlob = null;
        this.capturedImagePreview = null;
        this.currentStep = 1;
        this.router.navigate(['/merchant/register']);
      },
      error: (err) => {
        this.submitting = false;
        console.error('Submission failed:', err);
        alert('Submission failed. Please try again.');
      },
    });
  }

  showError(controlName: string): boolean {
    const control = this.form.get(controlName);
    if (!control) return false;
    return control.invalid && (control.dirty || control.touched);
  }

  showEmailError(): boolean {
    const control = this.form.get('ownerEmail');
    if (!control) return false;
    return control.invalid && control.touched;
  }

  onFileChange(event: Event, controlName: string): void {
    const control = this.form.get(controlName);
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length) {
      const file = input.files[0];
      control?.setValue(file);
      control?.markAsDirty();
      control?.updateValueAndValidity();
    }
  }

  blockNonNumericChars(event: KeyboardEvent): void {
    if (
      ['0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'Backspace', 'Tab', 'Enter', 'ArrowLeft', 'ArrowRight', 'Delete', 'Home', 'End'].includes(event.key) ||
      ((event.ctrlKey || event.metaKey) && ['a', 'c', 'v', 'x'].includes(event.key.toLowerCase()))
    ) {
      return;
    } else {
      event.preventDefault();
    }
  }
}