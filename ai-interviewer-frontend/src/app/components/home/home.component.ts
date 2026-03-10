import { Component, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { InterviewService } from '../../services/interview.service';

@Component({
    selector: 'app-home',
    standalone: true,
    imports: [FormsModule, CommonModule],
    templateUrl: './home.component.html',
    styleUrl: './home.component.css'
})
export class HomeComponent implements OnInit, OnDestroy {
    username: string = '';
    showCommunicationDesc: boolean = false;

    // Score properties
    showScore: boolean = false;
    attemptedQuestions: string = '0';
    correctAnswers: string = '0';
    testDate: string = '--/--/----';
    testCategory: string = '--';

    // Voice recording state
    isRecording: boolean = false;
    waveformBars: number[] = Array(28).fill(0);
    private recognition: any = null;
    private waveInterval: any = null;

    constructor(
        private interviewService: InterviewService,
        private router: Router,
        private cdr: ChangeDetectorRef
    ) { }

    ngOnInit() {
        const attempted = localStorage.getItem('attemptedQuestions');
        if (attempted) {
            this.attemptedQuestions = attempted;
            this.correctAnswers = localStorage.getItem('correctAnswers') || '0';
            this.testDate = localStorage.getItem('testDate') || '--/--/----';
            this.testCategory = localStorage.getItem('testCategory') || 'Communication';
        }
    }

    start() {
        if (!this.username.trim()) return;
        this.interviewService.startInterview(this.username).subscribe({
            next: (session) => {
                localStorage.setItem('sessionId', session.id.toString());
                localStorage.setItem('username', this.username);
                localStorage.setItem('communicationMode', this.showCommunicationDesc ? 'true' : 'false');
                this.router.navigate(['/interview']);
            },
            error: (err) => console.error('Error starting interview', err)
        });
    }

    startVoice() {
        const SpeechRecognition = (window as any).SpeechRecognition || (window as any).webkitSpeechRecognition;
        if (!SpeechRecognition) {
            alert('Voice input is not supported in your browser. Please use Chrome.');
            return;
        }

        this.recognition = new SpeechRecognition();
        this.recognition.lang = 'en-US';
        this.recognition.continuous = false;
        this.recognition.interimResults = true;

        this.recognition.onresult = (event: any) => {
            let transcript = '';
            for (let i = 0; i < event.results.length; i++) {
                transcript += event.results[i][0].transcript;
            }
            this.username = transcript;
            this.cdr.detectChanges();
        };

        this.recognition.onend = () => {
            // Auto-confirm when speech stops naturally
            if (this.isRecording) {
                this.confirmVoice();
            }
        };

        this.recognition.onerror = (event: any) => {
            console.error('Speech error:', event.error);
            this.stopWaveAnimation();
            this.isRecording = false;
            this.cdr.detectChanges();
        };

        this.isRecording = true;
        this.recognition.start();
        this.startWaveAnimation();
        this.cdr.detectChanges();
    }

    cancelVoice() {
        if (this.recognition) {
            this.recognition.onend = null; // Prevent auto-confirm
            this.recognition.abort();
            this.recognition = null;
        }
        this.username = '';
        this.stopWaveAnimation();
        this.isRecording = false;
        this.cdr.detectChanges();
    }

    confirmVoice() {
        if (this.recognition) {
            this.recognition.onend = null;
            this.recognition.stop();
            this.recognition = null;
        }
        this.stopWaveAnimation();
        this.isRecording = false;
        this.cdr.detectChanges();
    }

    private startWaveAnimation() {
        this.waveInterval = setInterval(() => {
            this.waveformBars = this.waveformBars.map(() => Math.random());
            this.cdr.detectChanges();
        }, 80);
    }

    private stopWaveAnimation() {
        if (this.waveInterval) {
            clearInterval(this.waveInterval);
            this.waveInterval = null;
        }
        this.waveformBars = Array(28).fill(0);
    }

    ngOnDestroy() {
        this.cancelVoice();
    }
}
