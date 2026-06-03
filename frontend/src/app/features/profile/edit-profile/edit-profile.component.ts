import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { ToastService } from '../../../core/services/toast.service';

@Component({
  selector: 'app-edit-profile',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './edit-profile.component.html',
  styleUrls: ['./edit-profile.component.css']
})
export class EditProfileComponent implements OnInit {

  profile = {
    firstName: '',
    lastName: '',
    displayName: '',
    contactNumber: ''
  };

  errorMessage = '';

  constructor(private authService: AuthService, private router: Router, private toast: ToastService) { }

  ngOnInit(): void {
    this.authService.getProfile().subscribe({
      next: (data) => {
        this.profile.firstName = data.firstName || '';
        this.profile.lastName = data.lastName || '';
        this.profile.displayName = data.displayName || '';
        this.profile.contactNumber = data.contactNumber || '';
      },
      error: (err) => console.error('Error loading profile', err)
    });
  }

  onSave() {

    this.errorMessage = '';
    this.authService.updateProfile(this.profile).subscribe({
      next: () => {
        this.toast.success('Profile updated successfully!');
        this.router.navigate(['/profile']);
      },
      error: (err) => {
        this.errorMessage = 'Error updating profile';
      }
    });
  }

  cancel() {
    this.router.navigate(['/profile']);
  }

}
