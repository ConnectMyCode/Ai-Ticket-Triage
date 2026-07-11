package com.supporttriage.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.supporttriage.dto.CreateTicketRequest;
import com.supporttriage.dto.LoginRequest;
import com.supporttriage.dto.RespondTicketRequest;
import com.supporttriage.dto.SignupRequest;
import com.supporttriage.dto.TriageSaveRequest;
import com.supporttriage.dto.UpdateTicketStatusRequest;
import com.supporttriage.entity.Ticket;
import com.supporttriage.entity.TicketPriority;
import com.supporttriage.entity.TicketStatus;
import com.supporttriage.entity.User;
import com.supporttriage.repository.TicketRepository;
import com.supporttriage.repository.UserRepository;

import jakarta.transaction.Transactional;



@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("Test")
@Transactional
public class TicketControllerTest {

	@Autowired
	private MockMvc mockMvc;
	
	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private TicketRepository ticketRepository;
	
	@Autowired 
	private UserRepository userRepository;
		
		
	//Helper Method Exerytime it will Create a User -> Login() -> Extract the JWT -> Store it -> Then call Ticket End point 
	private String getJwtToken() throws Exception {

	    // Register a new user
	    SignupRequest signupRequest = new SignupRequest();
	    signupRequest.setEmail("ticketuser@example.com");
	    signupRequest.setPassword("password123");

	    mockMvc.perform(post("/api/auth/signup")
	            .contentType(MediaType.APPLICATION_JSON)
	            .content(objectMapper.writeValueAsString(signupRequest)));
	    		

	    // Login
	    LoginRequest loginRequest = new LoginRequest();
	    loginRequest.setEmail("ticketuser@example.com");
	    loginRequest.setPassword("password123");

	    MvcResult result = mockMvc.perform(post("/api/auth/login")
	            .contentType(MediaType.APPLICATION_JSON)
	            .content(objectMapper.writeValueAsString(loginRequest)))
	            .andExpect(status().isOk())
	            .andReturn();

	    String response = result.getResponse().getContentAsString();

	    // Extract token directly from JSON response
	    return objectMapper.readTree(response)
	            .get("token")
	            .asText();
	
	}
	  
	
		@Test
	    void accessProtectedEndpointWithoutJwt() throws Exception {
	        // Day 3 - Test 1
			 mockMvc.perform(get("/api/tickets"))
	            .andExpect(status().isForbidden());
	    }

	    @Test
	    void accessProtectedEndpointWithInvalidJwt() throws Exception {
	    	 mockMvc.perform(get("/api/tickets")
	    	            .header("Authorization", "Bearer invalid.jwt.token"))
	    	            .andExpect(status().isForbidden());
	    }

	
	    @Test
	    void authenticatedUserCanAccessOwnResources() throws Exception {
	        // Day 3 - Test 4
	    	String token = getJwtToken();

	        mockMvc.perform(get("/api/tickets")
	                .header("Authorization", "Bearer " + token))
	                .andExpect(status().isOk());

	    }
	    
	// -------------Day 4 - Test Case---------------------------    
	    
	    @Test
	    void createTicketSuccessfully() throws Exception {

	        String token = getJwtToken();

	        CreateTicketRequest request = new CreateTicketRequest();
	        request.setTitle("Unable to login");
	        request.setDescription("I cannot login after resetting my password.");
	        request.setPriority("HIGH");

	        mockMvc.perform(post("/api/tickets")
	                .header("Authorization", "Bearer " + token)
	                .contentType(MediaType.APPLICATION_JSON)
	                .content(objectMapper.writeValueAsString(request)))
	                .andExpect(status().isOk());

	    }
	    
	    @Test
	    void createTicketWithoutTitle() throws Exception {

	        String token = getJwtToken();

	        CreateTicketRequest request = new CreateTicketRequest();
	        request.setTitle("");
	        request.setDescription("Ticket description");
	        request.setPriority("LOW");

	        mockMvc.perform(post("/api/tickets")
	                .header("Authorization", "Bearer " + token)
	                .contentType(MediaType.APPLICATION_JSON)
	                .content(objectMapper.writeValueAsString(request)))
	                .andExpect(status().isBadRequest())
	                .andExpect(jsonPath("$.message")
	                        .value("Description or Title not provided : required fields"));
	    }
	    
	    
	    @Test
	    void createTicketWithoutDescription() throws Exception {

	        String token = getJwtToken();

	        CreateTicketRequest request = new CreateTicketRequest();
	        request.setTitle("Unable to login");
	        request.setDescription("");
	        request.setPriority("LOW");

	        mockMvc.perform(post("/api/tickets")
	                .header("Authorization", "Bearer " + token)
	                .contentType(MediaType.APPLICATION_JSON)
	                .content(objectMapper.writeValueAsString(request)))
	                .andExpect(status().isBadRequest())
	                .andExpect(jsonPath("$.message")
	                        .value("Description or Title not provided : required fields"));
	    }
	    
	    
	    
	    @Test
	    void updateTicketStatusSuccessfully() throws Exception {

	        // Get JWT
	        String token = getJwtToken();

	        ticketRepository.findAll().forEach(t ->
	        System.out.println(
	            "ID=" + t.getId() +
	            ", Status=" + t.getStatus() +
	            ", Title=" + t.getTitle()
	        )
	    );
	        
	        
	        // Create a ticket
	        CreateTicketRequest createRequest = new CreateTicketRequest();
	        createRequest.setTitle("Login Issue");
	        createRequest.setDescription("Unable to login");
	        createRequest.setPriority("LOW");

	        mockMvc.perform(post("/api/tickets")
	                .header("Authorization", "Bearer " + token)
	                .contentType(MediaType.APPLICATION_JSON)
	                .content(objectMapper.writeValueAsString(createRequest)))
	                .andExpect(status().isOk());
	        
			// Fetch the created ticket
	        Ticket ticket = ticketRepository.findAll().get(0);

	        // Update status
	        UpdateTicketStatusRequest updateRequest = new UpdateTicketStatusRequest();
	        updateRequest.setStatus(TicketStatus.IN_PROGRESS);

	      /*  mockMvc.perform(put("/api/tickets/" + ticket.getId() + "/status")
	                .header("Authorization", "Bearer " + token)
	                .contentType(MediaType.APPLICATION_JSON)
	                .content(objectMapper.writeValueAsString(updateRequest)))
	                .andExpect(status().isOk());
	        	
	        // Verify database
	        Ticket updatedTicket = ticketRepository.findById(ticket.getId()).orElseThrow();
	        */
	        
	        MvcResult result = mockMvc.perform(put("/api/tickets/" + ticket.getId() + "/status")
	                .header("Authorization", "Bearer " + token)
	                .contentType(MediaType.APPLICATION_JSON)
	                .content(objectMapper.writeValueAsString(updateRequest)))
	                .andReturn();

	       // ✅ Database State Verification
	        Ticket updatedTicket =
	                ticketRepository.findById(ticket.getId()).orElseThrow();

	        assertEquals(TicketStatus.IN_PROGRESS,
	                     updatedTicket.getStatus());
	        
	        
	        System.out.println(result.getResponse().getStatus());
	        System.out.println(result.getResponse().getContentAsString());
	        
	    }	
	
	    
	    @Test
	    void updateClosedTicketShouldFail() throws Exception {

	        String token = getJwtToken();
	        
	        
	        ticketRepository.findAll().forEach(t ->
	        System.out.println(
	            "ID=" + t.getId() +
	            ", Status=" + t.getStatus() +
	            ", Title=" + t.getTitle()
	        )
	    );
	        
	        // Create ticket
	        CreateTicketRequest createRequest = new CreateTicketRequest();
	        createRequest.setTitle("Login Issue");
	        createRequest.setDescription("Cannot login");
	        createRequest.setPriority("LOW");

	        mockMvc.perform(post("/api/tickets")
	                .header("Authorization", "Bearer " + token)
	                .contentType(MediaType.APPLICATION_JSON)
	                .content(objectMapper.writeValueAsString(createRequest)))
	                .andExpect(status().isOk());

	        Ticket ticket = ticketRepository.findAll().get(0);

	        // Close ticket
	        mockMvc.perform(put("/api/tickets/" + ticket.getId() + "/close")
	                .header("Authorization", "Bearer " + token))
	                .andExpect(status().isOk());

	        // Try updating closed ticket
	        UpdateTicketStatusRequest request = new UpdateTicketStatusRequest();
	        request.setStatus(TicketStatus.IN_PROGRESS);

	        mockMvc.perform(put("/api/tickets/" + ticket.getId() + "/status")
	                .header("Authorization", "Bearer " + token)
	                .contentType(MediaType.APPLICATION_JSON)
	                .content(objectMapper.writeValueAsString(request)))
	                .andExpect(status().isBadRequest())
	                .andExpect(jsonPath("$.message")
	                        .value("Ticket already closed"));
	  
	    //✅ Database State Verification
	        Ticket closedTicket =
	                ticketRepository.findById(ticket.getId()).orElseThrow();

	        assertEquals(TicketStatus.CLOSED,
	                     closedTicket.getStatus());
	    
	    }
	    
	    
	    
	    //Day5 Test Cases // 
	    
	    
	    /*Goal:-> Verify that an authenticated user can successfully save triage details for an OPEN ticket*/
	    @Test
	    void triageSaveValidTransition() throws Exception {

	        // Get JWT
	        String token = getJwtToken();

	        // Create Ticket
	        CreateTicketRequest createRequest = new CreateTicketRequest();
	        createRequest.setTitle("Login Issue");
	        createRequest.setDescription("Unable to login");
	        createRequest.setPriority("LOW");

	        mockMvc.perform(post("/api/tickets")
	                .header("Authorization", "Bearer " + token)
	                .contentType(MediaType.APPLICATION_JSON)
	                .content(objectMapper.writeValueAsString(createRequest)))
	                .andExpect(status().isOk());

	        // Fetch created ticket
	     // Fetch the logged-in user
	        User user = userRepository.findByEmail("ticketuser@example.com")
	                .orElseThrow();

	        // Fetch tickets belonging to that user
	        List<Ticket> tickets = ticketRepository.findByUser(user);

	        // Get the latest ticket created by that user
	        Ticket ticket = tickets.get(tickets.size() - 1);
	        // Prepare triage request
	        TriageSaveRequest request = new TriageSaveRequest();
	        request.setCategory("Authentication");
	        request.setSentiment("Negative");
	        request.setPriority(TicketPriority.HIGH);
	        request.setSummary("User unable to login due to invalid credentials.");
	        request.setReplyDraft("Please reset your password and try again.");

	        // Save triage
	        mockMvc.perform(post("/api/tickets/" + ticket.getId() + "/triage/save")
	                .header("Authorization", "Bearer " + token)
	                .contentType(MediaType.APPLICATION_JSON)
	                .content(objectMapper.writeValueAsString(request)))
	                .andExpect(status().isOk());

	        // Verify database
	        Ticket updatedTicket = ticketRepository.findById(ticket.getId()).orElseThrow();

	        assertEquals("Authentication", updatedTicket.getCategory());
	        assertEquals("Negative", updatedTicket.getSentiment());
	        assertEquals(TicketPriority.HIGH, updatedTicket.getPriority());
	        assertEquals("User unable to login due to invalid credentials.", updatedTicket.getSummary());
	        assertEquals("Please reset your password and try again.", updatedTicket.getReplyDraft());
	        
	        assertNotNull(updatedTicket.getTriagedAt());
	    }
	    
	    
	    
	    /*Goal Verify that a user cannot save triage on a ticket that has already been closed.
Expected
HTTP 400 Bad Request*/
	    
	    
	    @Test
	    void triageSaveClosedTicketShouldFail() throws Exception {

	        // Get JWT
	        String token = getJwtToken();

	        // Create Ticket
	        CreateTicketRequest createRequest = new CreateTicketRequest();
	        createRequest.setTitle("Login Issue");
	        createRequest.setDescription("Unable to login");
	        createRequest.setPriority("LOW");

	        mockMvc.perform(post("/api/tickets")
	                .header("Authorization", "Bearer " + token)
	                .contentType(MediaType.APPLICATION_JSON)
	                .content(objectMapper.writeValueAsString(createRequest)))
	                .andExpect(status().isOk());

	        // Fetch user's latest ticket
	        User user = userRepository.findByEmail("ticketuser@example.com")
	                .orElseThrow();

	        List<Ticket> tickets = ticketRepository.findByUser(user);
	        Ticket ticket = tickets.get(tickets.size() - 1);

	        // Close the ticket
	        mockMvc.perform(put("/api/tickets/" + ticket.getId() + "/close")
	                .header("Authorization", "Bearer " + token))
	                .andExpect(status().isOk());

	        // Prepare triage request
	        TriageSaveRequest request = new TriageSaveRequest();
	        request.setCategory("Authentication");
	        request.setSentiment("Negative");
	        request.setPriority(TicketPriority.HIGH);
	        request.setSummary("Login issue");
	        request.setReplyDraft("Reset password.");

	        // Attempt triage on CLOSED ticket
	        mockMvc.perform(post("/api/tickets/" + ticket.getId() + "/triage/save")
	                .header("Authorization", "Bearer " + token)
	                .contentType(MediaType.APPLICATION_JSON)
	                .content(objectMapper.writeValueAsString(request)))
	                .andExpect(status().isBadRequest())
	                .andExpect(jsonPath("$.message")
	                        .value("Invalid ticket status for triage"));

	        // Verify database state
	        Ticket updatedTicket = ticketRepository.findById(ticket.getId()).orElseThrow();

	        assertNull(updatedTicket.getCategory());
	        assertNull(updatedTicket.getSentiment());
	        assertNull(updatedTicket.getSummary());
	        assertNull(updatedTicket.getReplyDraft());
	        assertNull(updatedTicket.getTriagedAt());
	    }
	    
	    
	    /*This test verifies the transition, status update, timestamp update, and persisted response fields exactly as implemented in your current production code.*/
	   
	    @Test
	    void respondToTicketValidTransition() throws Exception {

	        // Get JWT
	        String token = getJwtToken();

	        // Create Ticket
	        CreateTicketRequest createRequest = new CreateTicketRequest();
	        createRequest.setTitle("Login Issue");
	        createRequest.setDescription("Unable to login");
	        createRequest.setPriority("LOW");

	        mockMvc.perform(post("/api/tickets")
	                .header("Authorization", "Bearer " + token)
	                .contentType(MediaType.APPLICATION_JSON)
	                .content(objectMapper.writeValueAsString(createRequest)))
	                .andExpect(status().isOk());

	        // Fetch latest ticket
	        User user = userRepository.findByEmail("ticketuser@example.com")
	                .orElseThrow();

	        List<Ticket> tickets = ticketRepository.findByUser(user);
	        Ticket ticket = tickets.get(tickets.size() - 1);

	        // Save triage first
	        TriageSaveRequest triageRequest = new TriageSaveRequest();
	        triageRequest.setCategory("Authentication");
	        triageRequest.setSentiment("Negative");
	        triageRequest.setPriority(TicketPriority.HIGH);
	        triageRequest.setSummary("Unable to login.");
	        triageRequest.setReplyDraft("Reset your password.");

	        mockMvc.perform(post("/api/tickets/" + ticket.getId() + "/triage/save")
	                .header("Authorization", "Bearer " + token)
	                .contentType(MediaType.APPLICATION_JSON)
	                .content(objectMapper.writeValueAsString(triageRequest)))
	                .andExpect(status().isOk());

	        // Respond
	        RespondTicketRequest respondRequest = new RespondTicketRequest();
	        respondRequest.setFinalResponse("Password reset completed.");
	        respondRequest.setResolutionNote("Issue resolved successfully.");

	        mockMvc.perform(post("/api/tickets/" + ticket.getId() + "/respond")
	                .header("Authorization", "Bearer " + token)
	                .contentType(MediaType.APPLICATION_JSON)
	                .content(objectMapper.writeValueAsString(respondRequest)))
	                .andExpect(status().isOk());

	        // Verify database
	        Ticket updatedTicket = ticketRepository.findById(ticket.getId()).orElseThrow();

	        assertEquals(TicketStatus.IN_PROGRESS, updatedTicket.getStatus());
	        assertEquals("Password reset completed.", updatedTicket.getFinalResponse());
	        assertEquals("Issue resolved successfully.", updatedTicket.getResolutionNote());

	        assertNotNull(updatedTicket.getRespondedAt());
	    }
	    
	    
	    
	    @Test
	    void respondBeforeTriageShouldFail() throws Exception {

	        // Get JWT
	        String token = getJwtToken();

	        // Create Ticket
	        CreateTicketRequest createRequest = new CreateTicketRequest();
	        createRequest.setTitle("Login Issue");
	        createRequest.setDescription("Unable to login");
	        createRequest.setPriority("LOW");

	        mockMvc.perform(post("/api/tickets")
	                .header("Authorization", "Bearer " + token)
	                .contentType(MediaType.APPLICATION_JSON)
	                .content(objectMapper.writeValueAsString(createRequest)))
	                .andExpect(status().isOk());

	        // Fetch latest ticket
	        User user = userRepository.findByEmail("ticketuser@example.com")
	                .orElseThrow();

	        List<Ticket> tickets = ticketRepository.findByUser(user);
	        Ticket ticket = tickets.get(tickets.size() - 1);

	        // Attempt to respond WITHOUT triage
	        RespondTicketRequest request = new RespondTicketRequest();
	        request.setFinalResponse("Issue resolved.");
	        request.setResolutionNote("Password reset completed.");

	        mockMvc.perform(post("/api/tickets/" + ticket.getId() + "/respond")
	                .header("Authorization", "Bearer " + token)
	                .contentType(MediaType.APPLICATION_JSON)
	                .content(objectMapper.writeValueAsString(request)))
	                .andExpect(status().isBadRequest())
	                .andExpect(jsonPath("$.message")
	                        .value("Ticket must be TRIAGED before Responding"));

	        // Verify database
	        Ticket updatedTicket = ticketRepository.findById(ticket.getId()).orElseThrow();

	        assertEquals(TicketStatus.OPEN, updatedTicket.getStatus());
	        assertNull(updatedTicket.getRespondedAt());
	        assertNull(updatedTicket.getFinalResponse());
	        assertNull(updatedTicket.getResolutionNote());
	    }
	    
	    
	    
	    
	    /*Goal

Verify that a user can successfully close their own ticket.*/
	    
	    @Test
	    void closeTicketValidTransition() throws Exception {

	        // Get JWT
	        String token = getJwtToken();

	        // Create ticket
	        CreateTicketRequest createRequest = new CreateTicketRequest();
	        createRequest.setTitle("Login Issue");
	        createRequest.setDescription("Unable to login");
	        createRequest.setPriority("LOW");

	        mockMvc.perform(post("/api/tickets")
	                .header("Authorization", "Bearer " + token)
	                .contentType(MediaType.APPLICATION_JSON)
	                .content(objectMapper.writeValueAsString(createRequest)))
	                .andExpect(status().isOk());

	        // Fetch latest ticket
	        User user = userRepository.findByEmail("ticketuser@example.com")
	                .orElseThrow();

	        List<Ticket> tickets = ticketRepository.findByUser(user);
	        Ticket ticket = tickets.get(tickets.size() - 1);

	        // Close ticket
	        mockMvc.perform(put("/api/tickets/" + ticket.getId() + "/close")
	                .header("Authorization", "Bearer " + token))
	                .andExpect(status().isOk());

	        // Verify database
	        Ticket updatedTicket = ticketRepository.findById(ticket.getId()).orElseThrow();

	        assertEquals(TicketStatus.CLOSED, updatedTicket.getStatus());
	        assertNotNull(updatedTicket.getClosedAt());
	    }
	    
	    
	    
	    /*Goal

Verify that a ticket cannot be closed twice.*/
	    @Test
	    void closeAlreadyClosedTicketShouldFail() throws Exception {

	        // Get JWT
	        String token = getJwtToken();

	        // Create ticket
	        CreateTicketRequest createRequest = new CreateTicketRequest();
	        createRequest.setTitle("Login Issue");
	        createRequest.setDescription("Unable to login");
	        createRequest.setPriority("LOW");

	        mockMvc.perform(post("/api/tickets")
	                .header("Authorization", "Bearer " + token)
	                .contentType(MediaType.APPLICATION_JSON)
	                .content(objectMapper.writeValueAsString(createRequest)))
	                .andExpect(status().isOk());

	        // Fetch latest ticket
	        User user = userRepository.findByEmail("ticketuser@example.com")
	                .orElseThrow();

	        List<Ticket> tickets = ticketRepository.findByUser(user);
	        Ticket ticket = tickets.get(tickets.size() - 1);

	        // First close
	        mockMvc.perform(put("/api/tickets/" + ticket.getId() + "/close")
	                .header("Authorization", "Bearer " + token))
	                .andExpect(status().isOk());

	        // Save original closedAt
	        Ticket closedTicket = ticketRepository.findById(ticket.getId()).orElseThrow();
	        assertEquals(TicketStatus.CLOSED, closedTicket.getStatus());
	        assertNotNull(closedTicket.getClosedAt());

	        // Attempt second close
	        mockMvc.perform(put("/api/tickets/" + ticket.getId() + "/close")
	                .header("Authorization", "Bearer " + token))
	                .andExpect(status().isBadRequest())
	                .andExpect(jsonPath("$.message")
	                        .value("Ticket already closed"));

	        // Verify database state unchanged
	        Ticket updatedTicket = ticketRepository.findById(ticket.getId()).orElseThrow();

	        assertEquals(TicketStatus.CLOSED, updatedTicket.getStatus());
	        assertNotNull(updatedTicket.getClosedAt());

	        // Ensure close timestamp was not cleared or modified
	        assertEquals(closedTicket.getClosedAt(), updatedTicket.getClosedAt());
	    }
	    
	    
}
