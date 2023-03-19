package org.metalohe.archi.gateway.entity;

import lombok.Data;

@Data
public class FileOutData {
    private byte[] content;
    private String fileName;

}
