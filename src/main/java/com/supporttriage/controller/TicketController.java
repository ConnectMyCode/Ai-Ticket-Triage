package com.supporttriage.controller;

import com.supporttriage.dto.AiTriageResponse; 
import com.supporttriage.dto.CreateTicketRequest;
import com.supporttriage.dto.RespondTicketRequest;
import com.supporttriage.dto.TicketResponse;
import com.supporttriage.dto.TriageSaveRequest;
import com.supporttriage.dto.UpdateTicketStatusRequest;
import com.supporttriage.entity.Ticket;
import com.supporttriage.service.AiTriageService;
import com.supporttriage.service.TicketService;
import org.springframework.web.bind.annotation.*;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.stream.Collectors;

import java.util.List;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    private final TicketService ticketService;
    private final AiTriageService aiTriageService;
    public TicketController(TicketService ticketService , AiTriageService aiTriageService) {
        this.ticketService = ticketService;
        this.aiTriageService = aiTriageService;
    }

    /**
     * Create ticket
     */
    @PostMapping
    public void createTicket(@RequestBody CreateTicketRequest request) {
        ticketService.createTicket(request);
    }

    /**
     * Get my tickets
     */
    /*@GetMapping
    public List<TicketResponse> getMyTickets() {
        return ticketService.getMyTickets();
    }*/
    
    
    /*Get My ticket in pageable format*/
    @GetMapping
    public Page<TicketResponse> getMyTickets(
    		Pageable pageable,
    		@RequestParam(required = false) List<String> status, 
    		@RequestParam(required = false) List<String> priority
    		) 
    {
    	return ticketService.getMyTickets(pageable, status,priority);
    }

    
    /*
     * Update my Tickets
     * */
    @PutMapping("/{id}/status")
    public void updateStatus(@PathVariable Long id,
                             @RequestBody UpdateTicketStatusRequest request) {
        ticketService.updateTicketStatus(id, request.getStatus());
    }
     
    
    
    /*Close the Ticket   
     * 
     * */
    @PutMapping("/{id}/close")
    public void closeTicket(@PathVariable Long id) 
    {
    	ticketService.closeTicket(id);
    }  
    
    
    /* Save mY tICKET  */
    @PostMapping("/{id}/triage/save")
    public void triageSave(@PathVariable Long id, 
    					   @RequestBody TriageSaveRequest request) 
    {
    	ticketService.triageSave(id , request);
    }
    
    
    /*Respond to ticket*/
    @PostMapping("/{id}/respond")
    public void respondToTicket(@PathVariable Long id,
    							@RequestBody RespondTicketRequest request) 
    {
    	ticketService.respondToTicket(id,request);
    }
    
    
    
    /*
     Ai  Triage Run on Ticket 
     * */
    @PostMapping("/{id}/triage/run")
    public AiTriageResponse runTriage(@PathVariable Long id) {
    		return aiTriageService.runTriage(id);
	   
    }
}
