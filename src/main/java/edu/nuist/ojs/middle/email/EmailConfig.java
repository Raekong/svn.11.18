package edu.nuist.ojs.middle.email;

import lombok.Data;

@Data
public class EmailConfig {
    private String host;
    private int port;
    private String account;
    private String password;
    private String senderName;
}
