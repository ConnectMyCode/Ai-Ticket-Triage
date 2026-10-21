package com.supporttriage.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import com.supporttriage.entity.Ticket;
import com.supporttriage.entity.TicketPriority;
import com.supporttriage.entity.TicketStatus;
import com.supporttriage.entity.User;

@DataJpaTest
@ActiveProfiles("Test")
class TicketRepositoryTest {

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private UserRepository userRepository;

    // ==========================
    // Helper Methods
    // ==========================

    private User createUser(String email) {

        User user = new User();
        user.setEmail(email);
        user.setPasswordHash("password");

        return userRepository.save(user);
    }

    private Ticket createTicket(
            User user,
            String title,
            TicketStatus status,
            TicketPriority priority) {

        Ticket ticket = new Ticket();

        ticket.setTitle(title);
        ticket.setDescription("Sample Description");
        ticket.setStatus(status);
        ticket.setPriority(priority);
        ticket.setUser(user);

        return ticketRepository.save(ticket);
    }

    // =====================================================
    // Test 1
    // Find by Owner
    // =====================================================

    @Test
    void findByUserShouldReturnOnlyUsersTickets() {

        User user1 = createUser("user1@test.com");
        User user2 = createUser("user2@test.com");

        createTicket(user1, "Ticket-1", TicketStatus.OPEN, TicketPriority.LOW);
        createTicket(user1, "Ticket-2", TicketStatus.IN_PROGRESS, TicketPriority.HIGH);
        createTicket(user2, "Ticket-3", TicketStatus.OPEN, TicketPriority.MEDIUM);

        List<Ticket> tickets = ticketRepository.findByUser(user1);

        assertEquals(2, tickets.size());

        assertTrue(
                tickets.stream()
                        .allMatch(ticket ->
                                ticket.getUser().getId().equals(user1.getId()))
        );
    }

    // =====================================================
    // Test 2
    // Filter by Status
    // =====================================================

    @Test
    void findByUserAndStatusInShouldReturnMatchingTickets() {

        User user = createUser("status@test.com");

        createTicket(user, "OPEN-1", TicketStatus.OPEN, TicketPriority.LOW);

        createTicket(user, "OPEN-2", TicketStatus.OPEN, TicketPriority.HIGH);

        createTicket(user, "PROGRESS",
                TicketStatus.IN_PROGRESS,
                TicketPriority.MEDIUM);

        Page<Ticket> page =
                ticketRepository.findByUserAndStatusIn(
                        user,
                        List.of(TicketStatus.OPEN),
                        PageRequest.of(0, 10));

        assertEquals(2, page.getContent().size());

        assertTrue(
                page.getContent()
                        .stream()
                        .allMatch(ticket ->
                                ticket.getStatus() == TicketStatus.OPEN)
        );
    }

    // =====================================================
    // Test 3
    // Filter by Priority
    // =====================================================

    @Test
    void findByUserAndPriorityInShouldReturnMatchingTickets() {

        User user = createUser("priority@test.com");

        createTicket(user,
                "LOW",
                TicketStatus.OPEN,
                TicketPriority.LOW);

        createTicket(user,
                "HIGH",
                TicketStatus.OPEN,
                TicketPriority.HIGH);

        createTicket(user,
                "MEDIUM",
                TicketStatus.OPEN,
                TicketPriority.MEDIUM);

        Page<Ticket> page =
                ticketRepository.findByUserAndPriorityIn(
                        user,
                        List.of(TicketPriority.HIGH),
                        PageRequest.of(0, 10));

        assertEquals(1, page.getContent().size());

        assertEquals(
                TicketPriority.HIGH,
                page.getContent().get(0).getPriority());
    }

    // =====================================================
    // Test 4
    // Filter by Status + Priority
    // =====================================================

    @Test
    void findByUserAndStatusInAndPriorityInShouldReturnMatchingTickets() {

        User user = createUser("combined@test.com");

        createTicket(user,
                "Ticket-1",
                TicketStatus.OPEN,
                TicketPriority.HIGH);

        createTicket(user,
                "Ticket-2",
                TicketStatus.OPEN,
                TicketPriority.LOW);

        createTicket(user,
                "Ticket-3",
                TicketStatus.IN_PROGRESS,
                TicketPriority.HIGH);

        Page<Ticket> page =
                ticketRepository.findByUserAndStatusInAndPriorityIn(
                        user,
                        List.of(TicketStatus.OPEN),
                        List.of(TicketPriority.HIGH),
                        PageRequest.of(0, 10));

        assertEquals(1, page.getContent().size());

        Ticket ticket = page.getContent().get(0);

        assertEquals(TicketStatus.OPEN, ticket.getStatus());

        assertEquals(TicketPriority.HIGH, ticket.getPriority());
    }

}