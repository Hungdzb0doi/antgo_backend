package com.flashjobweb.entity;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class WorkerSkillIdEntity implements Serializable {
    private UUID workerId;
    private UUID categoryId;
}
