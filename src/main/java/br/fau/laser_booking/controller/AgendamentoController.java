package br.fau.laser_booking.controller;

import br.fau.laser_booking.dto.AgendamentoRequest;
import br.fau.laser_booking.model.Aluno;
import br.fau.laser_booking.model.Reserva;
import br.fau.laser_booking.repository.AlunoRepository;
import br.fau.laser_booking.service.AgendamentoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/agendamentos")
public class AgendamentoController {

    private final AgendamentoService agendamentoService;
    private final AlunoRepository alunoRepository;

    public AgendamentoController(AgendamentoService agendamentoService,
                                 AlunoRepository alunoRepository) {
        this.agendamentoService = agendamentoService;
        this.alunoRepository = alunoRepository;
    }

    // === UC01: Agendar horário ==============================================
    @PostMapping
    public ResponseEntity<?> criarAgendamento(@RequestBody AgendamentoRequest req) {
        try {
            Aluno aluno = alunoRepository.findById(req.getAlunoId())
                    .orElseThrow(() -> new RuntimeException("Aluno não encontrado"));

            Reserva reserva = agendamentoService.agendarHorario(
                    aluno, req.getInicio(), req.getFim(), req.getEquipamento()
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(reserva);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erro ao agendar: " + e.getMessage());
        }
    }

    // === UC04: Visualizar horário marcado ===================================
    @GetMapping("/meus/{alunoId}")
    public ResponseEntity<?> listarReservasDoAluno(@PathVariable Long alunoId) {
        try {
            Aluno aluno = alunoRepository.findById(alunoId)
                    .orElseThrow(() -> new RuntimeException("Aluno não encontrado"));
            List<Reserva> reservas = agendamentoService.listarReservasDoAluno(aluno);
            return ResponseEntity.ok(reservas);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erro ao listar reservas: " + e.getMessage());
        }
    }

    // === UC02: Cancelar Horário ============================================
    @PostMapping("/{reservaId}/cancelar")
    public ResponseEntity<?> cancelarReserva(@PathVariable Long reservaId,
                                             @RequestParam Long alunoId) {
        try {
            Aluno aluno = alunoRepository.findById(alunoId)
                    .orElseThrow(() -> new RuntimeException("Aluno não encontrado"));
            agendamentoService.cancelarReserva(aluno, reservaId);
            return ResponseEntity.ok("Reserva cancelada com sucesso.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erro ao cancelar reserva: " + e.getMessage());
        }
    }

    // === UC03: Incluir suplente ============================================
    @PostMapping("/{reservaId}/suplente")
    public ResponseEntity<?> incluirSuplente(@PathVariable Long reservaId,
                                             @RequestParam Long titularId,
                                             @RequestParam Long suplenteId) {
        try {
            Aluno titular = alunoRepository.findById(titularId)
                    .orElseThrow(() -> new RuntimeException("Titular não encontrado"));
            Aluno suplente = alunoRepository.findById(suplenteId)
                    .orElseThrow(() -> new RuntimeException("Suplente não encontrado"));

            agendamentoService.incluirSuplente(titular, reservaId, suplente);
            return ResponseEntity.ok("Suplente incluído com sucesso.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erro ao incluir suplente: " + e.getMessage());
        }
    }

    // === UC05: Editar horário marcado ==============================================
    // Espera params no formato ISO do input datetime-local: yyyy-MM-ddTHH:mm
    @PostMapping("/{reservaId}/editar")
    public ResponseEntity<?> editarReserva(@PathVariable Long reservaId,
                                           @RequestParam Long alunoId,
                                           @RequestParam String inicio,
                                           @RequestParam String fim,
                                           @RequestParam String equipamento) {
        try {
            Aluno aluno = alunoRepository.findById(alunoId)
                    .orElseThrow(() -> new RuntimeException("Aluno não encontrado"));

            LocalDateTime ni = LocalDateTime.parse(inicio);
            LocalDateTime nf = LocalDateTime.parse(fim);

            Reserva editada = agendamentoService.editarHorario(aluno, reservaId, ni, nf, equipamento);
            return ResponseEntity.ok(editada);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erro ao editar reserva: " + e.getMessage());
        }
    }
}
