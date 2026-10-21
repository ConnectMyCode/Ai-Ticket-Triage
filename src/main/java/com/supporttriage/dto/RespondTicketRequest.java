package com.supporttriage.dto;

public class RespondTicketRequest {


    private String finalResponse;
    private String resolutionNote;

    public String getFinalResponse() {
        return finalResponse;
    }

    public void setFinalResponse(String finalResponse) {
        this.finalResponse = finalResponse;
    }

    public String getResolutionNote() {
        return resolutionNote;
    }
    
    public void setResolutionNote(String resolutionNote) {
        this.resolutionNote = resolutionNote;
    }
    
	
	
}
