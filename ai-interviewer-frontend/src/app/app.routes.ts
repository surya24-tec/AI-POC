import { Routes } from '@angular/router';
import { HomeComponent } from './components/home/home.component';
import { InterviewComponent } from './components/interview/interview.component';
import { ResultComponent } from './components/result/result.component';
import { AdminComponent } from './components/admin/admin.component';

export const routes: Routes = [
    { path: '', component: HomeComponent },
    { path: 'interview', component: InterviewComponent },
    { path: 'result', component: ResultComponent },
    { path: 'admin', component: AdminComponent },
    { path: '**', redirectTo: '' }
];
