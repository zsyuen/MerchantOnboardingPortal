import { Component, OnInit, ViewChild, ElementRef, HostListener, ChangeDetectorRef } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators, AbstractControl, ValidationErrors } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { PortalService } from '../../services/portal.service';
import { FaceLandmarker, FilesetResolver } from '@mediapipe/tasks-vision';

function minAgeValidator(minAge: number) {
  return (control: AbstractControl): ValidationErrors | null => {
    if (!control.value) return null;
    const dob = new Date(control.value);
    const today = new Date();
    const age = today.getFullYear() - dob.getFullYear() -
      (today < new Date(today.getFullYear(), dob.getMonth(), dob.getDate()) ? 1 : 0);
    return age >= minAge ? null : { minAge: { required: minAge, actual: age } };
  };
}

function notFutureDate(control: AbstractControl): ValidationErrors | null {
  if (!control.value) return null;
  // Parse the date string as local date (split to avoid UTC offset issues)
  const [year, month, day] = (control.value as string).split('-').map(Number);
  const selected = new Date(year, month - 1, day);
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  return selected <= today ? null : { futureDate: true };
}

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

  countdownTimer: any = null;
  countdownValue: number = 0;
  faceLandmarker: FaceLandmarker | null = null;
  staticFaceLandmarker: FaceLandmarker | null = null;

  // Liveness state
  livenessStep: 'detecting' | 'blink_wait_close' | 'blink_wait_open' | 'passed' = 'detecting';

  // Live feedback message
  feedbackMessage: string = '';
  feedbackClass: string = ''; // 'success', 'warning', 'danger'

  // Tooltip / modal state
  proofTooltipOpen = false;
  passportModalOpen = false;

  // File upload limits (must match backend: 10MB per file, 30MB total)
  readonly MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB in bytes
  readonly MAX_TOTAL_SIZE = 30 * 1024 * 1024; // 30MB in bytes
  fileUploadErrors: { [key: string]: string } = {};

  // Today's date string for date input max attributes (YYYY-MM-DD)
  todayStr = new Date().toISOString().split('T')[0];

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

  constructor(private fb: FormBuilder, private portal: PortalService, private router: Router, private cdr: ChangeDetectorRef, private titleService: Title) { }

  async ngOnInit(): Promise<void> {
    this.titleService.setTitle('Merchant Onboarding Portal');
    this.form = this.fb.group({
      businessRegNo: ['', Validators.required],
      companyName: ['', Validators.required],
      incorporationDate: ['', [Validators.required, notFutureDate]],
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
      ownerDob: ['', [Validators.required, minAgeValidator(18)]],
      ownerIdNo: ['', Validators.required],
      ownerNationality: ['', Validators.required],
      ownerIdFront: [null, Validators.required],
      ownerIdBack: [null, Validators.required],
      passportPhoto: [null, Validators.required],
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

      const vision = await FilesetResolver.forVisionTasks(
        "https://cdn.jsdelivr.net/npm/@mediapipe/tasks-vision@latest/wasm"
      );
      this.faceLandmarker = await FaceLandmarker.createFromOptions(vision, {
        baseOptions: {
          modelAssetPath: "https://storage.googleapis.com/mediapipe-models/face_landmarker/face_landmarker/float16/1/face_landmarker.task",
          delegate: "GPU"
        },
        outputFaceBlendshapes: true,
        runningMode: "VIDEO",
        numFaces: 1
      });

      this.staticFaceLandmarker = await FaceLandmarker.createFromOptions(vision, {
        baseOptions: {
          modelAssetPath: "https://storage.googleapis.com/mediapipe-models/face_landmarker/face_landmarker/float16/1/face_landmarker.task",
          delegate: "GPU"
        },
        runningMode: "IMAGE",
        numFaces: 1
      });

      this.modelsLoaded = true;
      this.feedbackMessage = 'Models loaded. Starting camera...';
      this.feedbackClass = 'success';
      console.log('MediaPipe Models Loaded');
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
    this.countdownValue = 0;
    this.livenessStep = 'detecting';
    this.cancelCountdown();

    this.isCameraActive = true;
    this.feedbackMessage = 'Accessing camera...';
    this.feedbackClass = 'warning';

    navigator.mediaDevices.getUserMedia({ video: true })
      .then(stream => {
        this.videoElement.nativeElement.srcObject = stream;
        this.videoElement.nativeElement.play();
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
    if (!this.isCameraActive || this.isImageCaptured || !this.faceLandmarker) return;

    const video = this.videoElement.nativeElement;

    if(video.readyState >= 2) {
      const startTimeMs = performance.now();
      const results = this.faceLandmarker.detectForVideo(video, startTimeMs);

      // IMPORTANT: the async face detection takes a few milliseconds.
      // If a photo was captured *during* this wait time, stop processing immediately
      // to prevent overwriting the success message or starting a new countdown!
      if (!this.isCameraActive || this.isImageCaptured) return;

      if (!results || !results.faceLandmarks || results.faceLandmarks.length === 0) {
        this.cancelCountdown();
        this.isFaceDetected = false;
        this.feedbackMessage = 'No face detected. Please look at the camera.';
        this.feedbackClass = 'danger';
        this.livenessStep = 'detecting';
      } else if (results.faceLandmarks.length > 1) {
        this.cancelCountdown();
        this.isFaceDetected = false;
        this.feedbackMessage = 'Multiple faces detected. Only one person allowed.';
        this.feedbackClass = 'danger';
        this.livenessStep = 'detecting';
      } else {
        const landmarks = results.faceLandmarks[0];
        
        // Calculate Bounding Box from Landmarks manually
        let minX = 1, minY = 1, maxX = 0, maxY = 0;
        for (const pt of landmarks) {
          if (pt.x < minX) minX = pt.x;
          if (pt.y < minY) minY = pt.y;
          if (pt.x > maxX) maxX = pt.x;
          if (pt.y > maxY) maxY = pt.y;
        }

        const videoWidth = video.videoWidth;
        const videoHeight = video.videoHeight;
        const box = {
          originX: minX * videoWidth,
          originY: minY * videoHeight,
          width: (maxX - minX) * videoWidth,
          height: (maxY - minY) * videoHeight
        };

        if (box.width === 0) {
          this.cancelCountdown();
          this.isFaceDetected = false;
          this.feedbackMessage = 'Calculating face position...';
          this.feedbackClass = 'warning';
        } else {
          // Calculate face size relative to frame
          const faceWidth = box.width;
          const faceHeight = box.height;
          const faceArea = faceWidth * faceHeight;
          const frameArea = videoWidth * videoHeight;
          const faceRatio = faceArea / frameArea;

          // Check if face is centered
          const faceCenterX = box.originX + box.width / 2;
          const faceCenterY = box.originY + box.height / 2;
          const frameCenterX = videoWidth / 2;
          const frameCenterY = videoHeight / 2;
          const offsetX = Math.abs(faceCenterX - frameCenterX);
          const offsetY = Math.abs(faceCenterY - frameCenterY);

          if (faceRatio < 0.04) {
            this.cancelCountdown();
            this.isFaceDetected = false;
            this.livenessStep = 'detecting';
            this.feedbackMessage = 'Move closer to the camera';
            this.feedbackClass = 'warning';
          } else if (faceRatio > 0.6) {
            this.cancelCountdown();
            this.isFaceDetected = false;
            this.livenessStep = 'detecting';
            this.feedbackMessage = 'Move further from the camera';
            this.feedbackClass = 'warning';
          } else if (offsetX > videoWidth * 0.25) {
            this.cancelCountdown();
            this.isFaceDetected = false;
            this.livenessStep = 'detecting';
            this.feedbackMessage = 'Center your face horizontally';
            this.feedbackClass = 'warning';
          } else if (offsetY > videoHeight * 0.25) {
            this.cancelCountdown();
            this.isFaceDetected = false;
            this.livenessStep = 'detecting';
            this.feedbackMessage = 'Center your face vertically';
            this.feedbackClass = 'warning';
          } else {
            this.isFaceDetected = true;
            this.feedbackClass = 'success';

            // Start Liveness Check if face is correctly positioned
            if (this.livenessStep === 'detecting') {
              this.livenessStep = 'blink_wait_close';
            }

            if (this.livenessStep === 'blink_wait_close' || this.livenessStep === 'blink_wait_open') {
              // Extract Blendshapes if available
              if (results.faceBlendshapes && results.faceBlendshapes.length > 0) {
                const blendshapes = results.faceBlendshapes[0].categories;
                const leftBlink = blendshapes.find(b => b.categoryName === 'eyeBlinkLeft')?.score || 0;
                const rightBlink = blendshapes.find(b => b.categoryName === 'eyeBlinkRight')?.score || 0;

                if (this.livenessStep === 'blink_wait_close') {
                  this.feedbackMessage = 'Active Liveness: Please blink your eyes...';
                  // Check if both eyes are closed (score > 0.4 typically implies closed eye in blendshapes)
                  if (leftBlink > 0.4 && rightBlink > 0.4) {
                    this.livenessStep = 'blink_wait_open';
                    this.feedbackMessage = 'Active Liveness: Now open your eyes...';
                  }
                } else if (this.livenessStep === 'blink_wait_open') {
                  this.feedbackMessage = 'Active Liveness: Great, now open your eyes...';
                  // Check if eyes are reopened (score < 0.2 means wide open)
                  if (leftBlink < 0.2 && rightBlink < 0.2) {
                    this.livenessStep = 'passed';
                  }
                }
              } else {
                this.feedbackMessage = 'Liveness error: blendshapes unsupported';
              }
            }
            
            if (this.livenessStep === 'passed' && !this.countdownTimer && !this.isImageCaptured) {
              this.countdownValue = 3;
              this.feedbackMessage = `✓ Liveness passed! Capturing in ${this.countdownValue}...`;
              
              this.countdownTimer = setInterval(() => {
                this.countdownValue--;
                if (this.countdownValue > 0) {
                  this.feedbackMessage = `✓ Liveness passed! Capturing in ${this.countdownValue}...`;
                } else {
                  this.cancelCountdown();
                  this.capturePhoto();
                }
              }, 1000);
            }
          }
        }
      }
    } else {
      this.cancelCountdown();
      this.feedbackMessage = 'Initializing camera...';
      this.feedbackClass = 'warning';
    }

    if (!this.isImageCaptured) {
      requestAnimationFrame(() => this.detectFace());
    }
  }

  cancelCountdown() {
    if (this.countdownTimer) {
      clearInterval(this.countdownTimer);
      this.countdownTimer = null;
    }
  }

  capturePhoto() {
    if (this.isImageCaptured) return; // Prevent double capture

    if (!this.isFaceDetected || !this.faceLandmarker) {
      alert("No face detected! Please position yourself clearly.");
      return;
    }

    this.cancelCountdown(); // Force stop countdown immediately
    this.isCameraActive = false;
    this.isImageCaptured = true;

    const video = this.videoElement.nativeElement;

    // 1. Detect face one last time precisely at the moment of capture
    const results = this.faceLandmarker.detectForVideo(video, performance.now());
    let cropBox = null;
    
    if (results && results.faceLandmarks && results.faceLandmarks.length > 0) {
        const landmarks = results.faceLandmarks[0];
        let minX = 1, minY = 1, maxX = 0, maxY = 0;
        for (const pt of landmarks) {
          if (pt.x < minX) minX = pt.x;
          if (pt.y < minY) minY = pt.y;
          if (pt.x > maxX) maxX = pt.x;
          if (pt.y > maxY) maxY = pt.y;
        }

        const videoWidth = video.videoWidth;
        const videoHeight = video.videoHeight;
        cropBox = {
          originX: minX * videoWidth,
          originY: minY * videoHeight,
          width: (maxX - minX) * videoWidth,
          height: (maxY - minY) * videoHeight
        };
    }

    const canvas = document.createElement('canvas');
    const ctx = canvas.getContext('2d');

    // 2. Crop the image down to just the face area if found
    if (cropBox) {
        // Add 20% padding
        const padX = cropBox.width * 0.2;
        const padY = cropBox.height * 0.2;
        const cropX = Math.max(0, cropBox.originX - padX);
        const cropY = Math.max(0, cropBox.originY - padY);
        const cropW = Math.min(video.videoWidth - cropX, cropBox.width + padX * 2);
        const cropH = Math.min(video.videoHeight - cropY, cropBox.height + padY * 2);

        canvas.width = cropW;
        canvas.height = cropH;
        ctx?.drawImage(video, cropX, cropY, cropW, cropH, 0, 0, cropW, cropH);
    } else {
        // Fallback to full frame if detection fails exactly on the capture frame
        canvas.width = video.videoWidth || 640;
        canvas.height = video.videoHeight || 480;
        ctx?.drawImage(video, 0, 0, canvas.width, canvas.height);
    }

    this.capturedImagePreview = canvas.toDataURL('image/jpeg');
    this.feedbackMessage = 'Photo cropped and captured successfully!';
    this.feedbackClass = 'success';

    canvas.toBlob((blob) => {
      this.capturedImageBlob = blob;

      const stream = video.srcObject as MediaStream;
      if (stream) {
        stream.getTracks().forEach(track => track.stop());
      }
      this.cdr.detectChanges();
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
      // File fields are handled separately below — skip them here
      if (!['ownerIdFront', 'ownerIdBack', 'passportPhoto', 'proofOfBusiness'].includes(key)) {
        formData.append(key, formValues[key]);
      }
    });

    // Append file fields explicitly with their backend-expected names
    formData.append('ownerIdFront',   this.form.get('ownerIdFront')!.value);
    formData.append('ownerIdBack',    this.form.get('ownerIdBack')!.value);
    formData.append('passportPhoto',  this.form.get('passportPhoto')!.value);
    formData.append('proofOfBusiness', this.form.get('proofOfBusiness')!.value);
    formData.append('liveSelfie', this.capturedImageBlob, 'selfie.jpg');

    this.portal.submitApplication(formData).subscribe({
      next: (res) => {
        this.submitting = false;
        console.log('Submission successful!', res);
        alert(`Application submitted successfully!\n\nA confirmation email has been sent to ${formValues.ownerEmail}`);
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
      
      // Validate individual file size (10MB limit)
      if (file.size > this.MAX_FILE_SIZE) {
        this.fileUploadErrors[controlName] = `File "${file.name}" exceeds the maximum size of 10MB. Please select a smaller file.`;
        control?.setValue(null);
        input.value = ''; // Clear the input
        return;
      }

      // Validate total upload size (30MB limit)
      const totalSize = this.calculateTotalFileSize(controlName, file);
      if (totalSize > this.MAX_TOTAL_SIZE) {
        this.fileUploadErrors[controlName] = `Total upload size exceeds the maximum limit of 30MB. Please reduce the file sizes.`;
        control?.setValue(null);
        input.value = ''; // Clear the input
        return;
      }

      // Clear any previous error
      this.fileUploadErrors[controlName] = '';
      
      if (controlName === 'passportPhoto') {
        const img = new Image();
        img.onload = async () => {
           if (!this.staticFaceLandmarker) {
              await this.loadModels();
           }
  
           const results = this.staticFaceLandmarker?.detect(img);
           
           if (results && results.faceLandmarks && results.faceLandmarks.length > 0) {
              const landmarks = results.faceLandmarks[0];
              let minX = 1, minY = 1, maxX = 0, maxY = 0;
              for (const pt of landmarks) {
                if (pt.x < minX) minX = pt.x;
                if (pt.y < minY) minY = pt.y;
                if (pt.x > maxX) maxX = pt.x;
                if (pt.y > maxY) maxY = pt.y;
              }
              const box = {
                 originX: minX * img.width,
                 originY: minY * img.height,
                 width: (maxX - minX) * img.width,
                 height: (maxY - minY) * img.height
              };
              const canvas = document.createElement('canvas');
  
              // Add padding so we don't cut off chin/hair
              const padX = box.width * 0.2;
              const padY = box.height * 0.2;
  
              const cropX = Math.max(0, box.originX - padX);
              const cropY = Math.max(0, box.originY - padY);
              const cropW = Math.min(img.width - cropX, box.width + padX * 2);
              const cropH = Math.min(img.height - cropY, box.height + padY * 2);
  
              canvas.width = cropW;
              canvas.height = cropH;
              canvas.getContext('2d')?.drawImage(img, cropX, cropY, cropW, cropH, 0, 0, cropW, cropH);
  
              canvas.toBlob((blob) => {
                if (blob) {
                  const croppedFile = new File([blob], file.name, { type: "image/jpeg" });
                  control?.setValue(croppedFile);
                  control?.markAsDirty();
                  control?.updateValueAndValidity();
                  this.cdr.detectChanges();
                }
              }, 'image/jpeg');
           } else {
              control?.setValue(file); // fallback to original
              control?.markAsDirty();
              control?.updateValueAndValidity();
              this.cdr.detectChanges();
           }
        };
        img.src = URL.createObjectURL(file);
      } else {
        control?.setValue(file);
        control?.markAsDirty();
        control?.updateValueAndValidity();
      }
    }
  }

  calculateTotalFileSize(currentControlName: string, newFile: File): number {
    const fileControls = ['ownerIdFront', 'ownerIdBack', 'passportPhoto', 'proofOfBusiness'];
    let totalSize = 0;

    fileControls.forEach(controlName => {
      if (controlName === currentControlName) {
        // Use the new file size for the control being updated
        totalSize += newFile.size;
      } else {
        // Add existing file sizes
        const existingFile = this.form.get(controlName)?.value;
        if (existingFile instanceof File) {
          totalSize += existingFile.size;
        }
      }
    });

    return totalSize;
  }

  toggleProofTooltip(event: Event): void {
    event.stopPropagation();
    this.proofTooltipOpen = !this.proofTooltipOpen;
    this.passportModalOpen = false;
  }

  openPassportModal(event: Event): void {
    event.stopPropagation();
    this.passportModalOpen = true;
    this.proofTooltipOpen = false;
  }

  closePassportModal(): void {
    this.passportModalOpen = false;
  }

  @HostListener('document:click')
  onDocumentClick(): void {
    this.proofTooltipOpen = false;
  }

  @HostListener('document:keydown.escape')
  onEscapeKey(): void {
    this.proofTooltipOpen = false;
    this.passportModalOpen = false;
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
