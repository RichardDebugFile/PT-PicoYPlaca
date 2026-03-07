import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { HttpErrorResponse } from '@angular/common/http';

import { PicoPlacaService } from '../../services/pico-placa.service';
import { ConsultaResponse, ErrorResponse } from '../../models/pico-placa.model';
import { ResultadoComponent } from '../resultado/resultado.component';

@Component({
  selector: 'app-consulta-form',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatDatepickerModule,
    MatNativeDateModule,
    MatProgressSpinnerModule,
    MatIconModule,
    MatTooltipModule,
    ResultadoComponent
  ],
  templateUrl: './consulta-form.component.html',
  styleUrl: './consulta-form.component.scss'
})
export class ConsultaFormComponent implements OnInit {

  form!: FormGroup;
  resultado: ConsultaResponse | null = null;
  errorMsg: string | null = null;
  cargando = false;
  // Getter en lugar de propiedad para que el valor sea siempre la hora actual al momento de leer
  get minFechaHora(): string {
    return this.toLocalIsoString(new Date());
  }

  constructor(
    private fb: FormBuilder,
    private picoPlacaService: PicoPlacaService
  ) {}

  ngOnInit(): void {
    const ahora = new Date();
    const fechaLocal = this.toLocalIsoString(ahora);

    this.form = this.fb.group({
      placa: ['', [
        Validators.required,
        Validators.pattern(/^[A-Za-z]{3}-[0-9]{3,4}$/)
      ]],
      fechaHora: [fechaLocal, [Validators.required]]
    });
  }

  consultar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const { placa, fechaHora } = this.form.value;

    // Truncar a minutos: datetime-local envia HH:mm:00 y new Date() tiene milisegundos,
    // lo que causaria que la hora actual siempre parezca "pasada" sin este ajuste.
    const fechaIngresada = new Date(fechaHora);
    const ahoraTruncado = new Date();
    ahoraTruncado.setSeconds(0, 0);
    if (fechaIngresada < ahoraTruncado) {
      this.errorMsg = 'La fecha y hora ingresada no puede ser anterior a la fecha y hora actual.';
      this.resultado = null;
      return;
    }

    this.cargando = true;
    this.resultado = null;
    this.errorMsg = null;

    const fechaFormateada = this.formatearParaApi(fechaHora);

    this.picoPlacaService.verificar({ placa: placa.toUpperCase(), fechaHora: fechaFormateada })
      .subscribe({
        next: (resp) => {
          this.resultado = resp;
          this.cargando = false;
        },
        error: (err: HttpErrorResponse) => {
          const body = err.error as ErrorResponse;
          this.errorMsg = body?.mensaje ?? 'Error al conectar con el servidor. Verifique que el backend este activo.';
          this.cargando = false;
        }
      });
  }

  // Mascara de entrada: fuerza el formato ABC-1234 mientras el usuario escribe
  onPlacaInput(event: Event): void {
    const inputEvent = event as InputEvent;
    const input = event.target as HTMLInputElement;
    // Elimina todo caracter que no sea letra o digito y convierte a mayusculas
    const clean = input.value.toUpperCase().replace(/[^A-Z0-9]/g, '');

    let letters = '';
    let digits = '';

    // Acepta hasta 3 letras al inicio; solo admite digitos despues de completar las letras
    for (const ch of clean) {
      if (/[A-Z]/.test(ch) && letters.length < 3 && digits.length === 0) {
        letters += ch;
      } else if (/[0-9]/.test(ch) && letters.length === 3 && digits.length < 4) {
        digits += ch;
      }
    }

    let formatted: string;
    if (letters.length < 3) {
      formatted = letters;
    } else if (digits.length === 0 && inputEvent.inputType?.startsWith('delete')) {
      // Al borrar el guion se deja sin el para permitir seguir borrando las letras
      formatted = letters;
    } else if (digits.length === 0) {
      formatted = letters + '-'; // Inserta el guion automaticamente al completar 3 letras
    } else {
      formatted = letters + '-' + digits;
    }

    // emitEvent: false evita disparar valueChanges y re-entrar en este handler
    this.form.get('placa')?.setValue(formatted, { emitEvent: false });
    input.value = formatted;
  }

  actualizarHoraActual(): void {
    this.form.patchValue({ fechaHora: this.toLocalIsoString(new Date()) });
    this.errorMsg = null;
  }

  limpiar(): void {
    this.resultado = null;
    this.errorMsg = null;
    this.form.reset({ placa: '', fechaHora: this.toLocalIsoString(new Date()) });
  }

  get placaError(): string {
    const ctrl = this.form.get('placa');
    if (ctrl?.hasError('required')) return 'La placa es obligatoria';
    if (ctrl?.hasError('pattern')) return 'Formato invalido. Ejemplos: ABC-1234 (particular), ABC-123 (moto)';
    return '';
  }

  get fechaError(): string {
    const ctrl = this.form.get('fechaHora');
    if (ctrl?.hasError('required')) return 'La fecha y hora son obligatorias';
    return '';
  }

  private toLocalIsoString(date: Date): string {
    const pad = (n: number) => n.toString().padStart(2, '0');
    return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`;
  }

  // datetime-local devuelve "YYYY-MM-DDTHH:mm" (16 chars); el backend espera "HH:mm:ss"
  private formatearParaApi(fechaHora: string): string {
    return fechaHora.length === 16 ? fechaHora + ':00' : fechaHora;
  }
}
