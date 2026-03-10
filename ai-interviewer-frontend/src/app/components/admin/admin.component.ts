import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AdminService } from '../../services/admin.service';

@Component({
    selector: 'app-admin',
    standalone: true,
    imports: [CommonModule, FormsModule],
    templateUrl: './admin.component.html',
    styleUrl: './admin.component.css'
})
export class AdminComponent implements OnInit {
    questions: any[] = [];
    newQuestion = { content: '', category: 'Communication', isAiGenerated: false };

    constructor(private adminService: AdminService) { }

    ngOnInit() {
        this.loadQuestions();
    }

    loadQuestions() {
        this.adminService.getQuestions().subscribe(qs => this.questions = qs);
    }

    saveQuestion() {
        if (!this.newQuestion.content) return;
        this.adminService.addQuestion(this.newQuestion).subscribe(() => {
            this.newQuestion.content = '';
            this.loadQuestions();
        });
    }

    deleteQuestion(id: number) {
        this.adminService.deleteQuestion(id).subscribe(() => this.loadQuestions());
    }
}
