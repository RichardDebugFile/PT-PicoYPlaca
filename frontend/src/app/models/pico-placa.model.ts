export interface ConsultaRequest {
  placa: string;
  fechaHora: string;
}

export interface ConsultaResponse {
  placa: string;
  fechaHora: string;
  diaSemana: string;
  puedeCircular: boolean;
  mensaje: string;
  digitosRestringidosHoy: string | null;
  franjaHorariaRestriccion: string | null;
}

export interface ErrorResponse {
  status: number;
  error: string;
  mensaje: string;
  errores?: Record<string, string>;
  timestamp: string;
}
