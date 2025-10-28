package br.fau.laser_booking.repository;

import br.fau.laser_booking.model.Reserva;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface ReservaRepository extends JpaRepository<Reserva, Long> {

    // Conflito de horário por EQUIPAMENTO + STATUS (ex.: ATIVA)
    // sobreposição: (inicio < fimNovo) AND (fim > inicioNovo)
    boolean existsByEquipamentoAndStatusInAndInicioBeforeAndFimAfter(
            String equipamento,
            Collection<Reserva.Status> status,
            LocalDateTime fimNovo,
            LocalDateTime inicioNovo
    );

    // Mesmo que o de cima, mas IGNORANDO a própria reserva (para edição)
    boolean existsByEquipamentoAndStatusInAndInicioBeforeAndFimAfterAndIdNot(
            String equipamento,
            Collection<Reserva.Status> status,
            LocalDateTime fimNovo,
            LocalDateTime inicioNovo,
            Long idToIgnore
    );

    // Listagem dos agendamentos do aluno (tela principal)
    List<Reserva> findAllByTitularIdOrderByInicioDesc(Long titularId);
}
