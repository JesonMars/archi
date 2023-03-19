package org.metalohe.archi.gateway.starter.entity;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author zhangxinxiu
 */
@Data
@Accessors(chain = true)
public class JarEntity {
    private String name;
    private String path;
    private String version;
    private String downloadUrl;
}
