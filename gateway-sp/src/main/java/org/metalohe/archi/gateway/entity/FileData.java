package org.metalohe.archi.gateway.entity;

import lombok.Data;

@Data
public class FileData {
    private byte[] content;
    private String header;
    private String json;

}
