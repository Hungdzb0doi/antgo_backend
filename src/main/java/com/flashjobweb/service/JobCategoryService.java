package com.flashjobweb.service;

import com.flashjobweb.dto.response.ResponseJobCategoryDTO;
import com.flashjobweb.entity.JobCategoryEntity;

import java.util.List;

public interface JobCategoryService {
    List<ResponseJobCategoryDTO> getAllCategories();
}
