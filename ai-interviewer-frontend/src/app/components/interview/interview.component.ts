import { Component, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { InterviewService } from '../../services/interview.service';

interface Message {
    text: string;
    sender: 'AI' | 'USER';
    timestamp: Date;
}

// ── Communication mode: fixed question list asked in order ────────────────────
const COMMUNICATION_QUESTIONS: string[] = [

    // 1️⃣ Basic Introduction
    "What is your name?",
    "Where are you from?",
    "Where do you live now?",
    "How old are you?",
    "What did you study?",
    "What are your hobbies?",
    "What do you like to do in your free time?",
    "What is your favorite color?",
    "What is your favorite food?",
    "What is your favorite place?",
    "Do you have siblings?",
    "What is your favorite hobby?",
    "What language do you speak at home?",

    // 2️⃣ Daily Life
    "What did you do today?",
    "What time do you wake up?",
    "What do you do in the morning?",
    "Do you drink coffee or tea?",
    "What do you usually eat for breakfast?",
    "How do you start your day?",
    "What is your daily routine?",
    "What do you do after work or study?",
    "What time do you go to sleep?",
    "Do you exercise daily?",
    "What is your evening routine?",
    "What do you usually do before sleeping?",
    "How do you spend your weekend?",

    // 3️⃣ Study Questions
    "What did you study in college?",
    "What is your favorite subject?",
    "Which subject is difficult for you?",
    "Who is your favorite teacher?",
    "Why did you choose your course?",
    "Do you like studying?",
    "How do you prepare for exams?",
    "Do you prefer online learning or classroom learning?",
    "What skills are you learning now?",
    "What are you currently learning?",
    "Why did you choose your field?",
    "What is your future study plan?",

    // 4️⃣ Work / Career Questions
    "What is your dream job?",
    "Why do you want this job?",
    "What skills do you have?",
    "What skills are you improving?",
    "What project are you working on?",
    "Do you like working in a team?",
    "What motivates you to work hard?",
    "What are your strengths?",
    "What are your weaknesses?",
    "What is your career goal?",
    "Where do you see yourself in 5 years?",

    // 5️⃣ Technology Questions
    "Do you like technology?",
    "What programming language do you like?",
    "Why do you like coding?",
    "How many hours do you practice coding?",
    "What project are you building now?",
    "Do you use AI tools?",
    "What is your favorite app?",
    "Do you like learning new technologies?",
    "Which technology is interesting to you?",
    "What is your biggest tech goal?",

    // 6️⃣ Friends & Family Questions
    "Who is your best friend?",
    "How did you meet your best friend?",
    "What do you do with your friends?",
    "How often do you meet your friends?",
    "Do you like spending time with family?",
    "What do you usually do with your family?",
    "Who inspires you the most?",
    "Do you have a big family?",
    "What is your favorite family memory?",
    "How do you celebrate birthdays?",

    // 7️⃣ Food Questions
    "Do you like cooking?",
    "What food do you cook best?",
    "Do you like spicy food?",
    "What do you eat for lunch?",
    "What is your favorite restaurant?",
    "Do you like street food?",
    "What food do you dislike?",
    "Do you prefer home food or restaurant food?",
    "What is your favorite dessert?",

    // 8️⃣ Travel Questions
    "Do you like traveling?",
    "What is your favorite travel destination?",
    "Have you traveled to another city?",
    "Do you like mountains or beaches?",
    "What place do you want to visit?",
    "Do you like long trips?",
    "Who do you travel with?",
    "What is your dream country to visit?",
    "What do you pack for travel?",
    "What is your best travel memory?",

    // 9️⃣ Entertainment Questions
    "What is your favorite movie?",
    "Who is your favorite actor?",
    "Do you like watching TV shows?",
    "What music do you like?",
    "Do you watch YouTube?",
    "What is your favorite YouTube channel?",
    "Do you like playing games?",
    "What is your favorite game?",
    "How often do you watch movies?",
    "What movie do you recommend?",

    // 🔟 Opinion Questions
    "What makes you happy?",
    "What makes you stressed?",
    "What do you do to relax?",
    "What motivates you in life?",
    "What is success for you?",
    "What is your biggest goal?",
    "Do you like learning new things?",
    "What motivates you to learn new things?",
    "Do you prefer working alone or in a team?",
    "What advice would you give to others?",
    "What is your biggest dream?",
    "What do you want to achieve in life?"
];

@Component({
    selector: 'app-interview',
    standalone: true,
    imports: [CommonModule, FormsModule],
    templateUrl: './interview.component.html',
    styleUrl: './interview.component.css'
})
export class InterviewComponent implements OnInit, OnDestroy {
    messages: Message[] = [];
    currentAnswer: string = '';
    sessionId: number = 0;
    currentQuestion: any = null;
    aiThinking: boolean = false;
    isFinished: boolean = false;
    inputError: string = '';

    // Communication mode
    isCommunicationMode: boolean = false;
    private currentQuestionIndex: number = 0;

    // Voice recording state
    isRecording: boolean = false;
    waveformBars: number[] = Array(32).fill(0);
    private recognition: any = null;
    private waveInterval: any = null;

    constructor(
        private interviewService: InterviewService,
        private router: Router,
        private cdr: ChangeDetectorRef
    ) { }

    ngOnInit() {
        const storedId = localStorage.getItem('sessionId');
        if (!storedId) {
            this.router.navigate(['/']);
            return;
        }
        this.sessionId = Number(storedId);

        // Read communication mode flag
        this.isCommunicationMode = localStorage.getItem('communicationMode') === 'true';
        this.currentQuestionIndex = 0;

        setTimeout(() => this.loadNextQuestion(), 500);
    }

    // ── Question Loading ────────────────────────────────────────────────────

    loadNextQuestion() {
        if (this.aiThinking) return;

        if (this.isCommunicationMode) {
            this.loadCommunicationQuestion();
        } else {
            this.loadAIQuestion();
        }
    }

    private loadCommunicationQuestion() {
        this.aiThinking = true;
        this.inputError = '';
        this.cdr.detectChanges();

        // All questions done — finish
        if (this.currentQuestionIndex >= COMMUNICATION_QUESTIONS.length) {
            this.aiThinking = false;
            this.finishInterview();
            return;
        }

        const questionText = COMMUNICATION_QUESTIONS[this.currentQuestionIndex];
        const questionId = this.currentQuestionIndex + 1000;
        this.currentQuestionIndex++;

        // Short delay to feel natural
        setTimeout(() => {
            this.currentQuestion = { id: questionId, content: questionText };
            this.addMessage(questionText, 'AI');
            this.aiThinking = false;
            this.cdr.detectChanges();
        }, 800);
    }

    private loadAIQuestion() {
        this.aiThinking = true;
        this.inputError = '';
        this.cdr.detectChanges();

        this.interviewService.getNextQuestion(this.sessionId).subscribe({
            next: (question) => {
                console.log("AI Question Received:", question);
                this.aiThinking = false;

                if (question && question.content) {
                    this.currentQuestion = question;
                    this.addMessage(question.content, 'AI');
                } else {
                    this.inputError = "AI returned an empty response. Click Exit to finish.";
                }

                this.cdr.detectChanges();
            },
            error: (err) => {
                console.error("Interview API Error:", err);
                this.aiThinking = false;
                this.inputError = "Connection lost. Please ensure Backend is running on port 8081.";
                this.cdr.detectChanges();
            }
        });
    }

    // ── Answer Submission ───────────────────────────────────────────────────

    submitAnswer() {
        const answerText = this.currentAnswer.trim();
        const questionText = this.currentQuestion ? this.currentQuestion.content : '';
        if (!answerText || this.aiThinking) return;

        if (answerText.includes('?')) {
            this.inputError = "No questions allowed. Please provide an answer.";
            return;
        }

        this.inputError = '';
        this.addMessage(answerText, 'USER');
        this.currentAnswer = '';

        const questionId = this.currentQuestion ? this.currentQuestion.id : 0;
        this.aiThinking = true;
        this.cdr.detectChanges();

        if (this.isCommunicationMode) {
            // Save answer to backend, then evaluate, then next question
            this.interviewService.submitAnswer(this.sessionId, questionId < 1000 ? questionId : 0, answerText).subscribe({
                next: () => this.evaluateAndContinue(questionText, answerText),
                error: () => this.evaluateAndContinue(questionText, answerText) // still evaluate even if save fails
            });
        } else {
            this.interviewService.submitAnswer(this.sessionId, questionId, answerText).subscribe({
                next: () => {
                    this.interviewService.evaluateAnswer(questionText, answerText).subscribe({
                        next: (feedback) => {
                            this.addMessage(feedback, 'AI');
                            const userMsgCount = this.messages.filter(m => m.sender === 'USER').length;
                            if (userMsgCount >= 5) {
                                this.aiThinking = false;
                                this.finishInterview();
                            } else {
                                setTimeout(() => {
                                    this.aiThinking = false;
                                    this.loadNextQuestion();
                                }, 5000);
                            }
                        },
                        error: () => {
                            const userMsgCount = this.messages.filter(m => m.sender === 'USER').length;
                            if (userMsgCount >= 5) {
                                this.aiThinking = false;
                                this.finishInterview();
                            } else {
                                this.aiThinking = false;
                                this.loadNextQuestion();
                            }
                        }
                    });
                },
                error: () => {
                    this.aiThinking = false;
                    this.inputError = "Failed to submit. Check backend connection.";
                    this.cdr.detectChanges();
                }
            });
        }
    }

    private evaluateAndContinue(questionText: string, answerText: string) {
        this.interviewService.evaluateAnswer(questionText, answerText).subscribe({
            next: (feedback) => {
                this.addMessage(feedback, 'AI');
                const userMsgCount = this.messages.filter(m => m.sender === 'USER').length;
                if (userMsgCount >= 5) {
                    this.aiThinking = false;
                    this.finishInterview();
                } else {
                    setTimeout(() => {
                        this.aiThinking = false;
                        this.loadNextQuestion();
                    }, 4000);
                }
                this.cdr.detectChanges();
            },
            error: () => {
                const userMsgCount = this.messages.filter(m => m.sender === 'USER').length;
                if (userMsgCount >= 5) {
                    this.aiThinking = false;
                    this.finishInterview();
                } else {
                    this.aiThinking = false;
                    this.loadNextQuestion();
                }
                this.cdr.detectChanges();
            }
        });
    }

    // ── Shared Helpers ──────────────────────────────────────────────────────

    addMessage(text: string, sender: 'AI' | 'USER') {
        this.messages.push({
            text,
            sender,
            timestamp: new Date()
        });

        setTimeout(() => {
            const chatList = document.querySelector('.messages-list');
            if (chatList) chatList.scrollTop = chatList.scrollHeight;
        }, 100);
    }

    finishInterview() {
        this.aiThinking = true;
        this.cdr.detectChanges();

        const attempted = this.messages.filter(m => m.sender === 'USER').length;
        const correct = attempted > 0 ? Math.max(0, attempted - Math.floor(Math.random() * 2)) : 0;
        localStorage.setItem('attemptedQuestions', attempted.toString());
        localStorage.setItem('correctAnswers', correct.toString());
        localStorage.setItem('testDate', new Date().toLocaleString());
        localStorage.setItem('testCategory', this.isCommunicationMode ? 'Communication' : 'General');

        this.interviewService.finishInterview(this.sessionId).subscribe({
            next: (feedback) => {
                this.aiThinking = false;
                localStorage.setItem('feedback', feedback);
                this.router.navigate(['/home']);
            },
            error: () => {
                this.aiThinking = false;
                this.router.navigate(['/home']);
            }
        });
    }

    // ─── Voice Recording ────────────────────────────────────────────────────

    startVoice() {
        const SpeechRecognition = (window as any).SpeechRecognition || (window as any).webkitSpeechRecognition;
        if (!SpeechRecognition) {
            alert('Voice input is not supported in your browser. Please use Chrome.');
            return;
        }

        this.recognition = new SpeechRecognition();
        this.recognition.lang = 'en-US';
        this.recognition.continuous = true;
        this.recognition.interimResults = true;

        this.recognition.onresult = (event: any) => {
            let transcript = '';
            for (let i = 0; i < event.results.length; i++) {
                transcript += event.results[i][0].transcript;
            }
            this.currentAnswer = transcript;
            this.cdr.detectChanges();
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
            this.recognition.onend = null;
            this.recognition.abort();
            this.recognition = null;
        }
        this.currentAnswer = '';
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
        this.waveformBars = Array(32).fill(0);
    }

    ngOnDestroy() {
        this.cancelVoice();
    }
}
