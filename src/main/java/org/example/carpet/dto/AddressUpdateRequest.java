package org.example.carpet.dto;

import lombok.Data;

@Data
public class AddressUpdateRequest {
    private String line1;
    private String line2;
    private String city;
    private String stateOrProvince;
    private String postalCode;
    private String country;
}
