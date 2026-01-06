package com.currency_data_transformer.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.currency_data_transformer.model.request.UploadFileRequest;
import com.currency_data_transformer.model.response.UploadFileResponse;
import com.currency_data_transformer.service.CurrencyDataTransformerService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Validated
public class CurrencyDataTransformerController {

    private final CurrencyDataTransformerService currencyDataTransformerService;
    
    @PostMapping("/upload")
    public ResponseEntity<UploadFileResponse> uploadFile(@Valid @RequestBody UploadFileRequest request) {
        return ResponseEntity.ok(currencyDataTransformerService.transformCurrencyData(request));
    }
}
