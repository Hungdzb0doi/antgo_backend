package com.flashjobweb.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;
@Data
@AllArgsConstructor
public class ResponseLocationWorkerDTO {


        private UUID workerId;
        private String fullName;
        private Double longitude;
        private Double latitude;

}
