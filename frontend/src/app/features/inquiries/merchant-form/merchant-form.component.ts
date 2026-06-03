import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { MerchantService } from '../../../core/services/merchant.service';
import { ToastService } from '../../../core/services/toast.service';

@Component({
  selector: 'app-merchant-form',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './merchant-form.component.html',
  styleUrls: ['./merchant-form.component.css']
})
export class MerchantFormComponent implements OnInit {
  isEditMode = false;
  merchantId: number | null = null;
  message = '';
  errorMessage = '';

  formData: any = {
    merchantName: '',
    contact: '',
    addressLine1: '',
    addressLine2: '',
    postcode: '',
    city: '',
    country: 'Malaysia',
    status: 'ACTIVE'
  };

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private merchantService: MerchantService,
    private toast: ToastService
  ) {}

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.isEditMode = true;
      this.merchantId = +idParam;
      this.merchantService.getMerchantById(this.merchantId).subscribe({
        next: (merchant) => {
          if (merchant) {
            this.formData = {
              merchantName: merchant.merchantName || '',
              contact: merchant.contact || '',
              addressLine1: merchant.addressLine1 || '',
              addressLine2: merchant.addressLine2 || '',
              postcode: merchant.postcode || '',
              city: merchant.city || '',
              country: merchant.country || 'Malaysia',
              status: merchant.status || 'ACTIVE'
            };
          }
        },
        error: () => this.errorMessage = 'Error loading merchant details'
      });
    }
  }

  onSubmit() {
    if (this.isEditMode && this.merchantId) {
      this.merchantService.updateMerchant(this.merchantId, this.formData).subscribe({
        next: () => {
          this.toast.success('Merchant updated successfully');
          setTimeout(() => this.router.navigate(['/merchants', this.merchantId]), 1000);
        },
        error: (err) => this.toast.error(err.error?.message || 'Error updating merchant')
      });
    } else {
      this.merchantService.createMerchant(this.formData).subscribe({
        next: () => {
          this.toast.success('Merchant created successfully');
          setTimeout(() => this.router.navigate(['/merchants']), 1000);
        },
        error: (err) => this.toast.error(err.error?.message || 'Error creating merchant')
      });
    }
  }

  cancel() {
    if (this.isEditMode && this.merchantId) {
      this.router.navigate(['/merchants', this.merchantId]);
    } else {
      this.router.navigate(['/merchants']);
    }
  }
}
