package br.fau.laser_booking.service;

import br.fau.laser_booking.model.Aluno;
import br.fau.laser_booking.model.Reserva;
import br.fau.laser_booking.repository.ReservaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
public class AgendamentoService {

    private final ReservaRepository reservaRepository;

    public AgendamentoService(ReservaRepository reservaRepository) {
        this.reservaRepository = reservaRepository;
    }

    // === UC01: Agendar horário ============================================
    @Transactional
    public Reserva agendarHorario(Aluno aluno,
                                  LocalDateTime inicio,
                                  LocalDateTime fim,
                                  String equipamento) {

        // 0) validações básicas
        if (aluno == null || aluno.getId() == null) {
            throw new IllegalArgumentException("Aluno inválido.");
        }
        if (equipamento == null || equipamento.isBlank()) {
            throw new IllegalArgumentException("Equipamento é obrigatório.");
        }
        if (inicio == null || fim == null) {
            throw new IllegalArgumentException("Início e fim são obrigatórios.");
        }
        if (!fim.isAfter(inicio)) {
            throw new IllegalArgumentException("Fim deve ser depois do início.");
        }
        if (inicio.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Não é possível agendar no passado.");
        }

        // 1) Conflito por equipamento + status ATIVA + sobreposição
        boolean conflito = reservaRepository
                .existsByEquipamentoAndStatusInAndInicioBeforeAndFimAfter(
                        equipamento,
                        List.of(Reserva.Status.ATIVA),
                        fim,
                        inicio
                );

        if (conflito) {
            throw new IllegalStateException("Horário já está reservado para este equipamento.");
        }

        // 2) Regra de permissão (TFG / 3º–8º / <24h)
        if (!podeAgendar(aluno, inicio)) {
            throw new IllegalStateException("Você não tem permissão para agendar esse horário.");
        }

        // 3) Criar e salvar
        Reserva r = new Reserva();
        r.setTitular(aluno);
        r.setEquipamento(equipamento);
        r.setInicio(inicio);
        r.setFim(fim);
        r.setStatus(Reserva.Status.ATIVA);
        r.setCriadoEm(LocalDateTime.now());
        r.setAtualizadoEm(LocalDateTime.now());

        return reservaRepository.save(r);
    }

    // Regras de prioridade
    public boolean podeAgendar(Aluno aluno, LocalDateTime inicioSlot) {
        DayOfWeek dia = inicioSlot.getDayOfWeek();
        boolean ehTFG = aluno.getTipoTrabalho() == Aluno.TipoTrabalho.TFG;
        Integer semestre = aluno.getSemestre();

        boolean diaTFG = (dia == DayOfWeek.MONDAY
                || dia == DayOfWeek.TUESDAY
                || dia == DayOfWeek.THURSDAY);

        boolean diaSemestres = (dia == DayOfWeek.WEDNESDAY
                || dia == DayOfWeek.FRIDAY);

        if (ehTFG && diaTFG) return true;

        if (semestre != null && semestre >= 3 && semestre <= 8 && diaSemestres) return true;

        Duration diff = Duration.between(LocalDateTime.now(), inicioSlot);
        return diff.toHours() < 24;
    }

    // === UC02: Cancelar ============================================
    @Transactional
    public void cancelarReserva(Aluno aluno, Long reservaId) {
        Reserva r = reservaRepository.findById(reservaId)
                .orElseThrow(() -> new IllegalArgumentException("Reserva não encontrada."));

        if (!Objects.equals(r.getTitular().getId(), aluno.getId())) {
            throw new IllegalStateException("Você não pode cancelar essa reserva.");
        }
        if (r.getStatus() != Reserva.Status.ATIVA) {
            throw new IllegalStateException("Apenas reservas ativas podem ser canceladas.");
        }

        Duration diff = Duration.between(LocalDateTime.now(), r.getInicio());
        if (diff.toMinutes() < 60) {
            throw new IllegalStateException("Cancelamento não permitido: falta menos de 1h para o início.");
        }

        r.setStatus(Reserva.Status.CANCELADA);
        r.setAtualizadoEm(LocalDateTime.now());
        reservaRepository.save(r);
    }

    // === UC03: Incluir Suplente ===================================
    @Transactional
    public void incluirSuplente(Aluno titular, Long reservaId, Aluno suplente) {
        if (suplente == null || suplente.getId() == null) {
            throw new IllegalArgumentException("Suplente inválido.");
        }

        Reserva r = reservaRepository.findById(reservaId)
                .orElseThrow(() -> new IllegalArgumentException("Reserva não encontrada."));

        if (!Objects.equals(r.getTitular().getId(), titular.getId())) {
            throw new IllegalStateException("Você não é o titular dessa reserva.");
        }
        if (Objects.equals(r.getTitular().getId(), suplente.getId())) {
            throw new IllegalStateException("Titular não pode ser suplente da própria reserva.");
        }

        if (LocalDateTime.now().isAfter(r.getFim())) {
            throw new IllegalStateException("Não é possível adicionar suplente após o término.");
        }

        r.setSuplente(suplente);
        r.setAtualizadoEm(LocalDateTime.now());
        reservaRepository.save(r);
    }

    // === UC05: Editar Horário marcado =============================
    @Transactional
    public Reserva editarHorario(Aluno aluno,
                                 Long reservaId,
                                 LocalDateTime novoInicio,
                                 LocalDateTime novoFim,
                                 String equipamento) {
        // 0) validações
        if (aluno == null || aluno.getId() == null)
            throw new IllegalArgumentException("Aluno inválido.");
        if (equipamento == null || equipamento.isBlank())
            throw new IllegalArgumentException("Equipamento é obrigatório.");
        if (novoInicio == null || novoFim == null)
            throw new IllegalArgumentException("Início e fim são obrigatórios.");
        if (!novoFim.isAfter(novoInicio))
            throw new IllegalArgumentException("Fim deve ser depois do início.");
        if (novoInicio.isBefore(LocalDateTime.now()))
            throw new IllegalArgumentException("Não é possível mover para o passado.");

        Reserva r = reservaRepository.findById(reservaId)
                .orElseThrow(() -> new IllegalArgumentException("Reserva não encontrada."));

        if (!Objects.equals(r.getTitular().getId(), aluno.getId()))
            throw new IllegalStateException("Você não é o titular.");

        if (r.getStatus() != Reserva.Status.ATIVA)
            throw new IllegalStateException("Apenas reservas ativas podem ser editadas.");

        // se não mudou nada, retorna
        if (Objects.equals(equipamento, r.getEquipamento()) &&
            Objects.equals(novoInicio, r.getInicio()) &&
            Objects.equals(novoFim, r.getFim())) {
            return r;
        }

        // conflito ignorando a própria reserva
        boolean conflito = reservaRepository
                .existsByEquipamentoAndStatusInAndInicioBeforeAndFimAfterAndIdNot(
                        equipamento,
                        List.of(Reserva.Status.ATIVA),
                        novoFim,
                        novoInicio,
                        r.getId()
                );
        if (conflito) {
            throw new IllegalStateException("Conflito de horário para o equipamento selecionado.");
        }

        // regra de prioridade novamente
        if (!podeAgendar(aluno, novoInicio)) {
            throw new IllegalStateException("Sem permissão neste novo horário.");
        }

        // persistir alterações
        r.setEquipamento(equipamento);
        r.setInicio(novoInicio);
        r.setFim(novoFim);
        r.setAtualizadoEm(LocalDateTime.now());
        return reservaRepository.save(r);
    }

    // Visualizar minhas reservas (UC "Visualizar Horário marcado")
    public List<Reserva> listarReservasDoAluno(Aluno aluno) {
        return reservaRepository.findAllByTitularIdOrderByInicioDesc(aluno.getId());
    }
}
