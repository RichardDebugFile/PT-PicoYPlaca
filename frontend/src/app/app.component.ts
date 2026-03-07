import { Component } from '@angular/core';
import { ConsultaFormComponent } from './components/consulta-form/consulta-form.component';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [ConsultaFormComponent],
  templateUrl: './app.component.html',
  styleUrl: './app.component.scss'
})
export class AppComponent {
  title = 'Pico y Placa - Quito';
}
