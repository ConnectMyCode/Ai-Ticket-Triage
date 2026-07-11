package com.supporttriage.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

import com.supporttriage.dto.AiTriageResponse;
import com.supporttriage.entity.AiRun;
import com.supporttriage.entity.Ticket;
import com.supporttriage.entity.TicketPriority;
import com.supporttriage.entity.TicketStatus;
import com.supporttriage.entity.User;
import com.supporttriage.repository.AiRunRepository;
import com.supporttriage.repository.TicketRepository;
import com.supporttriage.repository.UserRepository;

import jakarta.transaction.Transactional;

@SpringBootTest
@ActiveProfiles("Test")
@Transactional
public class AiTriageServiceTest {
	
	 @Autowired
	    private AiTriageService aiTriageService;

	    @Autowired
	    private TicketService ticketService;

	    @Autowired
	    private TicketRepository ticketRepository;

	    @Autowired
	    private UserRepository userRepository;

	    @Autowired
	    private AiRunRepository aiRunRepository;

	    @MockBean
	    private OpenAiService openAiService;

//Unlike TicketControllerTest, there is no MockMvc login here because we're calling the service directly.

	    
	    //**********************************Helper Methods************************************//
	    
	    
	    private void authenticate(User user) {

	        UsernamePasswordAuthenticationToken authentication =
	                new UsernamePasswordAuthenticationToken(
	                        user.getEmail(),
	                        null,
	                        Collections.emptyList());

	        SecurityContextHolder.getContext().setAuthentication(authentication);
	    }
	    
	    
	    
	    private Ticket createTicket(User user) {

	        Ticket ticket = new Ticket();
	        ticket.setTitle("Login Issue");
	        ticket.setDescription("Unable to login");
	        ticket.setPriority(TicketPriority.LOW);
	        ticket.setStatus(TicketStatus.OPEN);
	        ticket.setUser(user);

	        return ticketRepository.save(ticket);
	    }
	    
	    
	    
	    
	    //***************************************************************************************//
	   
	    
	    /*Purpose

Verify that a valid AI triage request:

creates an AiRun
calls AI exactly once
parses AI JSON
validates AI response
stores SUCCESS status
returns DTO*/
	    
	    @Test
	    void runTriageSuccess() {

	        // Arrange
	        User user = new User();
	        user.setEmail("ticketuser@example.com");
	        user.setPasswordHash("password");
	        user = userRepository.save(user);

	        authenticate(user);
	        
	        
	        //Create Ticket
	        Ticket ticket = createTicket(user);

	        String aiResponse = """
	        {
	          "choices": [
	            {
	              "message": {
	                "content": "{\\"category\\":\\"TECHNICAL\\",\\"priority\\":\\"HIGH\\",\\"tags\\":[\\"login\\",\\"authentication\\"],\\"sentiment\\":\\"NEGATIVE\\",\\"summary\\":\\"User cannot login\\",\\"replyDraft\\":\\"Please reset your password.\\"}"
	              }
	            }
	          ]
	        }
	        """;

	        Mockito.when(openAiService.runTriage(Mockito.anyString()))
	                .thenReturn(aiResponse);

	        // Act
	        AiTriageResponse response = aiTriageService.runTriage(ticket.getId());

	        // Assert DTO
	        assertEquals("TECHNICAL", response.getCategory());
	        assertEquals("HIGH", response.getPriority());
	        assertEquals("NEGATIVE", response.getSentiment());
	        assertEquals("User cannot login", response.getSummary());
	        assertEquals("Please reset your password.", response.getReplyDraft());

	        assertEquals(2, response.getTags().size());
	        assertTrue(response.getTags().contains("login"));
	        assertTrue(response.getTags().contains("authentication"));

	        // Verify AI called exactly once
	        Mockito.verify(openAiService, Mockito.times(1))
	                .runTriage(Mockito.anyString());

	        // Verify AiRun persisted
	        List<AiRun> runs = aiRunRepository.findByTicket(ticket);

	        assertEquals(1, runs.size());

	        AiRun run = runs.get(0);

	        assertEquals("SUCCESS", run.getStatus());
	        assertEquals("TRIAGE", run.getPurpose());
	        assertEquals("GROQ", run.getProvider());
	        assertEquals("llama-3.1-8b-instant", run.getModel());

	        assertNotNull(run.getRawRequest());
	        assertNotNull(run.getRawResponse());

	        assertNull(run.getErrorMessage());
	    }
	    
	    
	    /*Test 2 — Invalid JSON
Purpose

Verify that if the AI provider returns malformed JSON:

Parser fails
AiRun status becomes FAILED
Error message is stored
Service throws RuntimeException("AI triage failed")*/
	    
	    
	    @Test
	    void runTriageInvalidJsonShouldFail() {

	        // Arrange
	        User user = new User();
	        user.setEmail("ticketuser@example.com");
	        user.setPasswordHash("password");
	        user = userRepository.save(user);

	        //Authenticate
	        authenticate(user);

	        //Create Ticket
	        Ticket ticket = createTicket(user);

	        // Invalid JSON returned by AI
	        when(openAiService.runTriage(anyString()))
	                .thenReturn("This is not valid JSON");

	        // Act + Assert
	        RuntimeException exception = assertThrows(
	                RuntimeException.class,
	                () -> aiTriageService.runTriage(ticket.getId())
	        );

	        assertEquals("AI triage failed", exception.getMessage());

	        // AI should be called once
	        verify(openAiService, times(1))
	                .runTriage(anyString());

	        // Verify AiRun persisted as FAILED
	        List<AiRun> runs = aiRunRepository.findByTicket(ticket);

	        assertEquals(1, runs.size());

	        AiRun run = runs.get(0);

	        assertEquals("FAILED", run.getStatus());

	        assertNotNull(run.getRawRequest());

	        // Raw response should still be stored
	        assertEquals("This is not valid JSON", run.getRawResponse());

	        // Parser error should be stored
	        assertEquals("Invalid AI JSON response", run.getErrorMessage());
	    }
	    
	    
	    /*This verifies that if the AI provider times out:

OpenAiService throws timeout exception
AiRun becomes FAILED
Error is persisted
Service returns "AI triage failed"*/
	    
	    
	    @Test
	    void runTriageTimeoutShouldFail() {

	        // Arrange
	        User user = new User();
	        user.setEmail("ticketuser@example.com");
	        user.setPasswordHash("password");
	        user = userRepository.save(user);

	        authenticate(user);

	        Ticket ticket = createTicket(user);

	        // Mock timeout
	        when(openAiService.runTriage(anyString()))
	                .thenThrow(new RuntimeException("AI request timeout"));

	        // Act + Assert
	        RuntimeException exception = assertThrows(
	                RuntimeException.class,
	                () -> aiTriageService.runTriage(ticket.getId())
	        );

	        assertEquals("AI triage failed", exception.getMessage());

	        // Verify AI called once
	        verify(openAiService, times(1))
	                .runTriage(anyString());

	        // Verify AiRun
	        List<AiRun> runs = aiRunRepository.findByTicket(ticket);

	        assertEquals(1, runs.size());

	        AiRun run = runs.get(0);

	        assertEquals("FAILED", run.getStatus());

	        assertNotNull(run.getRawRequest());

	        // AI never returned a response
	        assertNull(run.getRawResponse());

	        // Original exception should be stored
	        assertEquals("AI request timeout", run.getErrorMessage());
	    }
	    
	    
	    
	    /*Purpose

Verify that if the AI provider rejects the request because of an invalid API key:

OpenAiService throws an exception
AiRun is marked as FAILED
Error message is stored
RuntimeException("AI triage failed") is returned*/
	    
	    
	    
	    @Test
	    void runTriageInvalidApiKeyShouldFail() {

	        // Arrange
	        User user = new User();
	        user.setEmail("ticketuser@example.com");
	        user.setPasswordHash("password");
	        user = userRepository.save(user);

	        authenticate(user);

	        Ticket ticket = createTicket(user);

	        // Mock Invalid API Key
	        when(openAiService.runTriage(anyString()))
	                .thenThrow(new RuntimeException("Invalid AI API key"));

	        // Act + Assert
	        RuntimeException exception = assertThrows(
	                RuntimeException.class,
	                () -> aiTriageService.runTriage(ticket.getId())
	        );

	        assertEquals("AI triage failed", exception.getMessage());

	        // Verify AI called exactly once
	        verify(openAiService, times(1))
	                .runTriage(anyString());

	        // Verify AiRun
	        List<AiRun> runs = aiRunRepository.findByTicket(ticket);

	        assertEquals(1, runs.size());

	        AiRun run = runs.get(0);

	        assertEquals("FAILED", run.getStatus());

	        assertNotNull(run.getRawRequest());

	        // Since AI call failed immediately
	        assertNull(run.getRawResponse());

	        // Original error preserved
	        assertEquals("Invalid AI API key", run.getErrorMessage());
	    }
	    	
	    
	    
	    
	   /*Empty Response: AI returns an empty string (provider returned nothing).*/ 
	    
	    @Test
	    void runTriageEmptyResponseShouldFail() {

	        // Arrange
	        User user = new User();
	        user.setEmail("ticketuser@example.com");
	        user.setPasswordHash("password");
	        user = userRepository.save(user);

	        authenticate(user);

	        Ticket ticket = createTicket(user);

	        // Mock empty AI response
	        when(openAiService.runTriage(anyString()))
	                .thenReturn("");

	        // Act + Assert
	        RuntimeException exception = assertThrows(
	                RuntimeException.class,
	                () -> aiTriageService.runTriage(ticket.getId())
	        );

	        assertEquals("AI triage failed", exception.getMessage());

	        // Verify AI called once
	        verify(openAiService, times(1))
	                .runTriage(anyString());

	        // Verify AiRun
	        List<AiRun> runs = aiRunRepository.findByTicket(ticket);

	        assertEquals(1, runs.size());

	        AiRun run = runs.get(0);

	        assertEquals("FAILED", run.getStatus());

	        assertNotNull(run.getRawRequest());

	        // Empty response should be stored
	        assertEquals("", run.getRawResponse());

	        // Parser should report invalid JSON
	        assertEquals("Invalid AI JSON response", run.getErrorMessage());
	    }
	    
	    
	    
}
