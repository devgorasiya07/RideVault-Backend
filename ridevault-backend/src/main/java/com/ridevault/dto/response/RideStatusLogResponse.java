package com.ridevault.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RideStatusLogResponse {

    private Long logId;
    private String fromStatus;
    private String toStatus;
    private String changedBy;
    private String changedByRole;
    private LocalDateTime changedAt;
    private String reason;
}
