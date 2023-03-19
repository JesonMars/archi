package org.metalohe.archi.gateway.starter.dao.model;

import org.metalohe.archi.gateway.starter.dao.BaseModel;
import lombok.Data;

import javax.persistence.Table;

@Data
@Table(name = "t_gateway")
public class TGateWayModel extends BaseModel {

    /**
     * 路由id
     */
    private String routerCode;

    /**
     * host
     */
    private String path;
    /**
     * url前缀
     */
    private String segment;

    /**
     * 前缀对应的uri
     */
    private String routerUri;
    private String crudCode;

}
