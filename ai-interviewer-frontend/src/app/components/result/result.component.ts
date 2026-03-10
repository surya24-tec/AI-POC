import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';

@Component({
    selector: 'app-result',
    standalone: true,
    imports: [CommonModule],
    templateUrl: './result.component.html',
    styleUrl: './result.component.css'
})
export class ResultComponent implements OnInit {
    feedback: string = '';
    username: string = '';

    constructor(private router: Router) { }

    ngOnInit() {
        this.feedback = localStorage.getItem('feedback') || 'No feedback available.';
        this.username = localStorage.getItem('username') || 'Candidate';
    }

    goHome() {
        localStorage.clear();
        this.router.navigate(['/']);
    }
}
