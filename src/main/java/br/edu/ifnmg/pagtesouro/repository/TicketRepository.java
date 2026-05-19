package br.edu.ifnmg.pagtesouro.repository;

import br.edu.ifnmg.pagtesouro.domain.ticket.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {
}
