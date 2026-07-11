package com.supporttriage.service;

import com.supporttriage.repository.TicketRepository;

import com.supporttriage.dto.CreateTicketRequest;
import com.supporttriage.dto.RespondTicketRequest;
import com.supporttriage.dto.TicketResponse;
import com.supporttriage.dto.TriageSaveRequest;
import com.supporttriage.entity.Ticket;
import com.supporttriage.entity.TicketPriority;
import com.supporttriage.entity.TicketStatus;
import com.supporttriage.entity.User;
import com.supporttriage.exception.ResourceNotFoundException;
import com.supporttriage.repository.TicketRepository;
import com.supporttriage.repository.UserRepository;
import com.supporttriage.security.SecurityUtil;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


import java.util.stream.Collectors;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class TicketService {

    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;

    public TicketService(TicketRepository ticketRepository,
                         UserRepository userRepository) {
        this.ticketRepository = ticketRepository;
        this.userRepository = userRepository;
    }
       
    /**
     * Create ticket for logged-in user
     */
    public void createTicket(CreateTicketRequest request) { 
    	
        String email = SecurityUtil.getCurrentUserEmail();
        	
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Ticket ticket = new Ticket();
        ticket.setTitle(request.getTitle());
        ticket.setDescription(request.getDescription());
        ticket.setUser(user);
        
        TicketPriority priority = TicketPriority.LOW;
        
        if(request.getPriority() != null && !request.getPriority().isEmpty()) 
        {
        	try 
        	{
        		priority = TicketPriority.valueOf(request.getPriority().trim().toUpperCase());        		
        	}
        	catch(IllegalArgumentException e) 
        	{
        		throw new RuntimeException("Invalid priority value");
        	}
        }
        
        
        
        if(request.getTitle() == null || request.getTitle().trim().isEmpty() || request.getDescription() == null  || request.getDescription().trim().isEmpty()) 
        {
        	throw new RuntimeException("Description or Title not provided : required fields");    
        }
        
        ticket.setPriority(priority); 
        ticketRepository.save(ticket);
    }

    /**
     * 
     * Get tickets for logged-in user	
     * 
     */
    public Page<TicketResponse> getMyTickets(
            Pageable pageable,
            List<String> statusParams,
            List<String> priorityParams
    ) {

        String email = SecurityUtil.getCurrentUserEmail();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // ✅ Convert status
        List<TicketStatus> statuses = null;

        if (statusParams != null && !statusParams.isEmpty()) {
            statuses = statusParams.stream()
                    .map(s -> TicketStatus.valueOf(s.trim().toUpperCase()))
                    .collect(Collectors.toList());
        }

        // ✅ Convert priority (YOUR STEP 4)
        List<TicketPriority> priorities = null;

        if (priorityParams != null && !priorityParams.isEmpty()) {
            priorities = priorityParams.stream()
                    .map(p -> {
                        try {
                            return TicketPriority.valueOf(p.trim().toUpperCase());
                        } catch (IllegalArgumentException e) {
                            throw new RuntimeException("Invalid priority value");
                        }
                    })
                    .collect(Collectors.toList());
        }

        Page<Ticket> ticketPage;

        // ✅ Case 1: No filters
        if (statuses == null && priorities == null) {
            ticketPage = ticketRepository.findByUser(user, pageable);
        }

        // ✅ Case 2: Only status
        else if (statuses != null && priorities == null) {
            ticketPage = ticketRepository.findByUserAndStatusIn(user, statuses, pageable);
        }

        // ✅ Case 3: Only priority
        else if (statuses == null) {
            ticketPage = ticketRepository.findByUserAndPriorityIn(user, priorities, pageable);
        }

        // ✅ Case 4: Both filters
        else {
            ticketPage = ticketRepository.findByUserAndStatusInAndPriorityIn(
                    user, statuses, priorities, pageable);
        }

        return ticketPage.map(ticket -> mapToResponse(ticket));
        
    }
    
    private TicketResponse mapToResponse(Ticket ticket) {
        TicketResponse response = new TicketResponse();
        response.setId(ticket.getId());
        response.setTitle(ticket.getTitle());
        response.setDescription(ticket.getDescription());
        response.setStatus(ticket.getStatus());
        response.setCreatedAt(ticket.getCreatedAt());
        response.setPriority(ticket.getPriority());
        return response;
    }
    
    
    
    /*
     * 
     * Get tickets for admin only
     * 
     * */
    
    public Page<TicketResponse> getAllTicketsForAdmin(
            Pageable pageable,
            List<String> status,
            List<String> priority
    ) {
        String email = SecurityUtil.getCurrentUserEmail();

        if (!email.equals("admin@example.com")) {
            throw new RuntimeException("Unauthorized");
        }

        List<TicketStatus> statusList = null;
        List<TicketPriority> priorityList = null;

        try {
            if (status != null && !status.isEmpty()) {
                statusList = status.stream()
                        .map(s -> TicketStatus.valueOf(s.trim().toUpperCase()))
                        .collect(Collectors.toList());
            }

            if (priority != null && !priority.isEmpty()) {
                priorityList = priority.stream()
                        .map(p -> TicketPriority.valueOf(p.trim().toUpperCase()))
                        .collect(Collectors.toList());
            }
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid status or priority value");
        }

        Page<Ticket> ticketPage;

        if (statusList != null && priorityList != null) {
            ticketPage = ticketRepository.findByStatusInAndPriorityIn(statusList, priorityList, pageable);
        } else if (statusList != null) {
            ticketPage = ticketRepository.findByStatusIn(statusList, pageable);
        } else if (priorityList != null) {
            ticketPage = ticketRepository.findByPriorityIn(priorityList, pageable);
        } else {
            ticketPage = ticketRepository.findAll(pageable); // from JpaRepository
        }

        return ticketPage.map(this::mapToResponse);
    }
    
    
    
    
    /*Update ticket Status */
    public void updateTicketStatus(Long ticketId, TicketStatus status) {

        String email = SecurityUtil.getCurrentUserEmail();   

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket not found"));

        // Ensure user owns the ticket
        if (!ticket.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized");
        }
        
        
        if (ticket.getStatus() == TicketStatus.CLOSED) {
    	    throw new RuntimeException("Ticket already closed");
    	}
        

        ticket.setStatus(status);
        ticketRepository.save(ticket);

    }
    
    
    
    public void closeTicket(Long id)  
    {
    	//1.Get current user
    	String email = SecurityUtil.getCurrentUserEmail();
    	
    	User user =  userRepository.findByEmail(email)
    			.orElseThrow(() -> new ResourceNotFoundException("User not found")); 
    	
    	//2. Find Ticket 
    	Ticket ticket = ticketRepository.findById(id)
    			.orElseThrow(() -> new ResourceNotFoundException("Ticket not Found"));
    					
    	//3. Ownership check (CRITICAL)
    	if(!ticket.getUser().getId().equals(user.getId()))          
    	{
    		throw new RuntimeException("Unauthorized");
    	}
    	// 4. Checking the current state  if CLOSED>>No Update required And Gives error "Already closed" if not closed then will close 🔥 ADD THIS CHECK
    	if (ticket.getStatus() == TicketStatus.CLOSED) {
    	    throw new RuntimeException("Ticket already closed");
    	}
 
    	
    	ticket.setClosedAt(LocalDateTime.now());
    	
    	//Set status 
    	ticket.setStatus(TicketStatus.CLOSED); 
    	
    	
    	//Save
    	ticketRepository.save(ticket);
    	
    }
    
    
    
    /*
     * Responding Ticket
     * */
    
    public void respondToTicket(Long id ,RespondTicketRequest request) 
    {
    	
    	//Get logged-in user	
    	String email = SecurityUtil.getCurrentUserEmail();   
    	
    	User user = userRepository.findByEmail(email)
    			.orElseThrow(() -> new RuntimeException("User Not Found"));   

    	
    	
    	//2.Find ticket
    	Ticket ticket = ticketRepository.findById(id)
    			.orElseThrow(() -> new RuntimeException("Ticket not found") );
    	
    	
    	//3.OwnerShip check
    	if(!ticket.getUser().getId().equals(user.getId())) 
    	{
    		throw new RuntimeException("Unauthorized");
    	}
    	
    	
    	   //4.Transition rule -> only TRIAGED tickets can be responded to
        if(ticket.getStatus() == TicketStatus.CLOSED) 
        {
        		throw new RuntimeException("Ticket already closed");        		
        }
        
      //to check whether the ticket has been triaged before allowing a response.
     // Workflow validation
        if (ticket.getTriagedAt() == null) {
            throw new RuntimeException("Ticket must be TRIAGED before Responding");
        }

        // Request validation
        if (request.getResolutionNote() == null
                || request.getResolutionNote().trim().isEmpty()
                || request.getFinalResponse() == null
                || request.getFinalResponse().trim().isEmpty()) {
            throw new RuntimeException("Final response and resolution note are required");
        }
        
        //5. Save Response details
        ticket.setFinalResponse(request.getFinalResponse());
        ticket.setResolutionNote(request.getResolutionNote());
        
        
        //6. Update workflow
    	ticket.setRespondedAt(LocalDateTime.now());
    	ticket.setStatus(TicketStatus.IN_PROGRESS);
    	
    	
    	//7.Save
    	ticketRepository.save(ticket); 
    }
    
    
    public void triageSave(Long id , TriageSaveRequest request ) 
    {
    	
    	
    	//Get Logged-in user	
        String email = SecurityUtil.getCurrentUserEmail();
        
        
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        
        //2.Find ticket
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ticket not found"));
        
        //3.OwnerShip check
        if (!ticket.getUser().getId().equals(user.getId())) {
        	throw new RuntimeException("Unauthorized");
        }
        
         
        if(ticket.getStatus() == TicketStatus.CLOSED)
        {
        	throw new RuntimeException("Invalid ticket status for triage");
        }	
        
        
        if(request.getCategory() == null || request.getCategory().trim().isEmpty() || request.getSentiment() == null || request.getSentiment().trim().isEmpty() ) 
        {
        	throw new RuntimeException("Categor and Sentiment field required.");
        }
        												
        												
        ticket.setCategory(request.getCategory());
        ticket.setSentiment(request.getSentiment());
        ticket.setTriagedAt(LocalDateTime.now());
        ticket.setPriority(request.getPriority()); 
        ticket.setSummary(request.getSummary());
        ticket.setReplyDraft(request.getReplyDraft()); 
        ticketRepository.save(ticket);
        
    }  	
        	
    	
    
}