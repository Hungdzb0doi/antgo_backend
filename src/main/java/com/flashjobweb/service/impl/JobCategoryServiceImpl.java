package com.flashjobweb.service.impl;

import com.flashjobweb.dto.response.ResponseJobCategoryDTO;
import com.flashjobweb.entity.JobCategoryEntity;
import com.flashjobweb.repository.JobCategoryRepository;
import com.flashjobweb.service.JobCategoryService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.List;
@Service
@RequiredArgsConstructor
public class JobCategoryServiceImpl implements JobCategoryService {
    private final JobCategoryRepository jobCategoryRepository;
    private final ModelMapper modelMapper;
    @Override
    public List<ResponseJobCategoryDTO> getAllCategories() {
        List<JobCategoryEntity> categories = jobCategoryRepository.findAll();
        return categories.stream().map(category -> modelMapper.map(category, ResponseJobCategoryDTO.class)).toList();

    }
}
