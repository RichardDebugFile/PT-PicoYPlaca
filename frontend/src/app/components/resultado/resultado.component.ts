import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { ConsultaResponse } from '../../models/pico-placa.model';

@Component({
  selector: 'app-resultado',
  standalone: true,
  imports: [CommonModule, MatIconModule, MatChipsModule],
  templateUrl: './resultado.component.html',
  styleUrl: './resultado.component.scss'
})
export class ResultadoComponent {

  @Input() resultado!: ConsultaResponse;

  get iconoEstado(): string {
    return this.resultado.puedeCircular ? 'check_circle' : 'cancel';
  }

  get colorEstado(): string {
    return this.resultado.puedeCircular ? 'libre' : 'restringido';
  }

  get fechaFormateada(): string {
    const d = new Date(this.resultado.fechaHora);
    return d.toLocaleDateString('es-EC', {
      weekday: 'long', year: 'numeric', month: 'long', day: 'numeric'
    });
  }

  get horaFormateada(): string {
    const d = new Date(this.resultado.fechaHora);
    return d.toLocaleTimeString('es-EC', { hour: '2-digit', minute: '2-digit' });
  }
}
