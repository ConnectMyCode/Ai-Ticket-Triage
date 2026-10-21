package com.supporttriage.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import com.supporttriage.entity.AiRun;
import com.supporttriage.entity.Ticket;
import com.supporttriage.entity.TicketPriority;
import com.supporttriage.entity.TicketStatus;
import com.supporttriage.entity.User;

@DataJpaTest
@ActiveProfiles("Test")
class AiRunRepositoryTest {

    @Autowired
    private AiRunRepository aiRunRepository;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private UserRepository userRepository;

    // =====================================================
    // Helper Methods
    // =====================================================

    private User createUser(String email) {

        User user = new User();
        user.setEmail(email);
        user.setPasswordHash("password");

        return userRepository.save(user);
    }

    private Ticket createTicket(User user, String title) {

        Ticket ticket = new Ticket();
        ticket.setTitle(title);
        ticket.setDescription("Sample Description");
        ticket.setStatus(TicketStatus.OPEN);
        ticket.setPriority(TicketPriority.LOW);
        ticket.setUser(user);

        return ticketRepository.save(ticket);
    }

    private AiRun createAiRun(User user,
                              Ticket ticket,
                              String status) {

        AiRun run = new AiRun();

        run.setUser(user);
        run.setTicket(ticket);

        run.setPurpose("TRIAGE");
        run.setProvider("GROQ");
        run.setModel("llama-3.1-8b-instant");
        run.setPromptVersion("v1");

        run.setStatus(status);

        run.setRawRequest("Prompt");
        run.setRawResponse("Response");

        return aiRunRepository.save(run);
    }

    // =====================================================
    // Test 1
    // Save
    // =====================================================

    @Test
    void saveAiRunSuccessfully() {

        User user = createUser("save@test.com");

        Ticket ticket = createTicket(user, "Login Issue");

        AiRun run = createAiRun(user, ticket, "SUCCESS");

        assertTrue(run.getId() != null);

        assertEquals("SUCCESS", run.getStatus());

        assertEquals(ticket.getId(), run.getTicket().getId());

        assertEquals(user.getId(), run.getUser().getId());
    }

    // =====================================================
    // Test 2
    // Find By Ticket
    // =====================================================

    @Test
    void findByTicketShouldReturnRunsForTicket() {

        User user = createUser("ticket@test.com");

        Ticket ticket1 = createTicket(user, "Ticket-1");

        Ticket ticket2 = createTicket(user, "Ticket-2");

        createAiRun(user, ticket1, "SUCCESS");

        createAiRun(user, ticket1, "FAILED");

        createAiRun(user, ticket2, "SUCCESS");

        List<AiRun> runs =
                aiRunRepository.findByTicket(ticket1);

        assertEquals(2, runs.size());

        assertTrue(
                runs.stream()
                        .allMatch(run ->
                                run.getTicket().getId()
                                        .equals(ticket1.getId()))
        );
    }

    // =====================================================
    // Test 3
    // Find By User
    // =====================================================

    @Test
    void findByUserShouldReturnRunsForUser() {

        User user1 = createUser("user1@test.com");

        User user2 = createUser("user2@test.com");

        Ticket ticket1 = createTicket(user1, "Ticket-1");

        Ticket ticket2 = createTicket(user2, "Ticket-2");

        createAiRun(user1, ticket1, "SUCCESS");

        createAiRun(user1, ticket1, "FAILED");

        createAiRun(user2, ticket2, "SUCCESS");

        List<AiRun> runs =
                aiRunRepository.findByUser(user1);

        assertEquals(2, runs.size());

        assertTrue(
                runs.stream()
                        .allMatch(run ->
                                run.getUser().getId()
                                        .equals(user1.getId()))
        );
    }

}