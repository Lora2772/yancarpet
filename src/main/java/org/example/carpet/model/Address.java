package org.example.carpet.model;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Address {
    private String line1;
    private String line2;
    private String city;
    private String stateOrProvince;
    private String postalCode;
    private String country;
}
