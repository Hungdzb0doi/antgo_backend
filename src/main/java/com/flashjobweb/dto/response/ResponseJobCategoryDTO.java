package com.flashjobweb.dto.response;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter@Setter
public class ResponseJobCategoryDTO {
   private UUID id;
   private String name;
}
