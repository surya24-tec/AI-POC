import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { environment } from '../../environments/environment';

@Injectable({
    providedIn: 'root'
})
export class AdminService {
    private apiUrl = `${environment.apiUrl}/api/admin`;

    constructor(private http: HttpClient) { }

    getQuestions(): Observable<any[]> {
        return this.http.get<any[]>(`${this.apiUrl}/questions`);
    }

    addQuestion(question: any): Observable<any> {
        return this.http.post(`${this.apiUrl}/questions`, question);
    }

    deleteQuestion(id: number): Observable<any> {
        return this.http.delete(`${this.apiUrl}/questions/${id}`);
    }
}
