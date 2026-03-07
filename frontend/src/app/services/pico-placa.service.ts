import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { ConsultaRequest, ConsultaResponse } from '../models/pico-placa.model';

@Injectable({
  providedIn: 'root'
})
export class PicoPlacaService {

  private readonly baseUrl = `${environment.apiUrl}/pico-placa`;

  constructor(private http: HttpClient) {}

  verificar(request: ConsultaRequest): Observable<ConsultaResponse> {
    return this.http.post<ConsultaResponse>(`${this.baseUrl}/verificar`, request);
  }
}
