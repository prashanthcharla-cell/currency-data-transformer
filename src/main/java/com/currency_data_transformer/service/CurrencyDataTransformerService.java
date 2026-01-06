package com.currency_data_transformer.service;

import org.springframework.stereotype.Service;

import com.currency_data_transformer.model.request.UploadFileRequest;
import com.currency_data_transformer.model.response.UploadFileResponse;

@Service
public class CurrencyDataTransformerService {
    
    public UploadFileResponse transformCurrencyData(UploadFileRequest request) {
        return new UploadFileResponse("123", "COMPLETED", "File uploaded successfully. Processing started.");
    }
}
