package com.supporttriage.dto;

import com.supporttriage.entity.TicketStatus;

/**
 * Request to update ticket status
 */
public class UpdateTicketStatusRequest {

    private TicketStatus status;

    public TicketStatus getStatus() {
        return status;
    }

    public void setStatus(TicketStatus status) {
        this.status = status;
}
}