package com.supporttriage.service;


	import com.supporttriage.dto.AiTriageResponse;
import com.supporttriage.entity.AiRun;
	import com.supporttriage.entity.Ticket;
import com.supporttriage.entity.TicketStatus;
import com.supporttriage.entity.User;
	import com.supporttriage.repository.AiRunRepository;
	import com.supporttriage.repository.TicketRepository;
	import com.supporttriage.repository.UserRepository;
	import com.supporttriage.security.SecurityUtil;
	import org.springframework.stereotype.Service;

	/**
	 * Handles AI triage execution flow
	 * Saves all AI runs for tracking
	 */
	@Service
	public class AiTriageService {

	    private final TicketRepository ticketRepository;
	    private final UserRepository userRepository;
	    private final AiRunRepository aiRunRepository;
	    private final AiPromptBuilder aiPromptBuilder;
	    private final OpenAiService openAiService;
	    private final AiTriageParser aiTriageParser; 
	    private final AiTriageValidator aiTriageValidator;   
	    
	    
	    
	    public AiTriageService(
	            TicketRepository ticketRepository,
	            UserRepository userRepository,
	            AiRunRepository aiRunRepository,
	            AiPromptBuilder aiPromptBuilder,
	            OpenAiService openAiService,
	            AiTriageParser aiTriageParser,
	            AiTriageValidator aiTriageValidator
	    ) {
	        this.ticketRepository = ticketRepository;
	        this.userRepository = userRepository;
	        this.aiRunRepository = aiRunRepository;
	        this.aiPromptBuilder = aiPromptBuilder;
	        this.openAiService = openAiService;
	        this.aiTriageParser  = aiTriageParser;
	        this.aiTriageValidator = aiTriageValidator;
	    }

	    /**
	     * Runs AI triage for a ticket
	     */
	    public AiTriageResponse runTriage(Long ticketId) {

	        // 1. Current user
	        String email = SecurityUtil.getCurrentUserEmail();

	        User user = userRepository.findByEmail(email)
	                .orElseThrow(() -> new RuntimeException("User not found"));

	        // 2. Find ticket
	        Ticket ticket = ticketRepository.findById(ticketId)
	                .orElseThrow(() -> new RuntimeException("Ticket not found"));

	        // 3. Ownership check
	        if (!ticket.getUser().getId().equals(user.getId())) {
	            throw new RuntimeException("Unauthorized");
	        }

	        
	        if(ticket.getStatus() == TicketStatus.CLOSED) { 
	        	throw new RuntimeException("Cannot run AI on closed ticket");
	        }
	        
	        // 4. Build prompt
	        String prompt = aiPromptBuilder.buildTriagePrompt(ticket);

	        // 5. Create AI run
	        AiRun aiRun = new AiRun();
	        aiRun.setTicket(ticket);
	        aiRun.setUser(user);
	        aiRun.setPurpose("TRIAGE");
	        aiRun.setProvider("GROQ");
	        aiRun.setModel("openai/gpt-oss-120b");
	        aiRun.setPromptVersion(aiPromptBuilder.getPromptVersion());
	        aiRun.setStatus("PENDING");
	        aiRun.setRawRequest(prompt);

	        aiRunRepository.save(aiRun);

	        try {
	            // 6. Run AI
	            String rawResponse = openAiService.runTriage(prompt);

	            aiRun.setRawResponse(rawResponse);
	            
	            //Parse AI Response
	            		AiTriageResponse response = aiTriageParser.parse(rawResponse);
	            
	           //Validate AI Response
	            		aiTriageValidator.validate(response); 
	            		
	            
	           //SUCCESS only after validation
	            		aiRun.setStatus("SUCCESS"); 
	            		
	            aiRunRepository.save(aiRun);

	            return response;

	        } catch (Exception e) {
	        	
	        	System.out.println("========== AI TRIAGE ERROR ==========");
	            System.out.println("Exception Type: " + e.getClass().getName());
	            System.out.println("Exception Message: " + e.getMessage());
	            e.printStackTrace();
	            System.out.println("=====================================");
	            
	        	
	            // 8. Save failure
	            aiRun.setStatus("FAILED");
	            aiRun.setErrorMessage(e.getMessage());

	            aiRunRepository.save(aiRun);

	            throw new RuntimeException("AI triage failed");
	        }
	    }
	}	
