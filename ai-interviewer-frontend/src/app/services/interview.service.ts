import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { environment } from '../../environments/environment';

@Injectable({
    providedIn: 'root'
})
export class InterviewService {
    private apiUrl = `${environment.apiUrl}/api/interview`;

    constructor(private http: HttpClient) { }

    /**
     * Starts a new interview session for the user
     */
    startInterview(username: string): Observable<any> {
        return this.http.post(`${this.apiUrl}/start?username=${encodeURIComponent(username)}`, {});
    }

    /**
     * Fetches the next question from the AI based on the session ID
     */
    getNextQuestion(sessionId: number): Observable<any> {
        return this.http.get<any>(`${this.apiUrl}/next-question/${sessionId}`);
    }

    /**
     * Submits the user's answer to the current question
     */
    submitAnswer(sessionId: number, questionId: number, content: string): Observable<any> {
        return this.http.post(`${this.apiUrl}/submit-answer`, {
            sessionId,
            questionId,
            content
        });
    }

    /**
     * Retrieves all messages (questions and answers) for the current session
     */
    getHistory(sessionId: number): Observable<any[]> {
        return this.http.get<any[]>(`${this.apiUrl}/history/${sessionId}`);
    }

    /**
     * Ends the session and returns final AI feedback/score
     */
    finishInterview(sessionId: number): Observable<string> {
        return this.http.post(`${this.apiUrl}/finish/${sessionId}`, {}, {
            responseType: 'text'
        });
    }

    /**
     * Evaluates a single answer and gets feedback and improved answer
     */
    evaluateAnswer(question: string, answer: string): Observable<string> {
        return this.http.post(`${environment.apiUrl}/ai/evaluate`, {
            question,
            answer
        }, { responseType: 'text' });
    }
}
