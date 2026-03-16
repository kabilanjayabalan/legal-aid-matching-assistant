package com.legalaid.backend.external.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class NgoExternalDTO {
    private String name;
    private String registrationNo;
    private String city;
    private String state;
}