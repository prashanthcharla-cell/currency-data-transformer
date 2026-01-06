package com.currency_data_transformer.model.request;

import org.springframework.web.multipart.MultipartFile;

import com.currency_data_transformer.validation.ValidFile;

import jakarta.validation.constraints.NotNull;

public record UploadFileRequest(
    @NotNull(message = "File is required")
    @ValidFile
    MultipartFile file
) {}
