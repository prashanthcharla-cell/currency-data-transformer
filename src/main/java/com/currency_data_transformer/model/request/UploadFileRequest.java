package com.currency_data_transformer.model.request;

import org.springframework.web.multipart.MultipartFile;

public record UploadFileRequest(MultipartFile file) {}
