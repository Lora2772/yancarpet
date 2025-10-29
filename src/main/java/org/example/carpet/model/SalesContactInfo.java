package org.example.carpet.model;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalesContactInfo {
    private String name;
    private String email;
    private String phone;
    private String wechatId;
    private String note;
}
