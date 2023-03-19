package org.metalohe.archi.gateway.starter.dao.model;

import org.metalohe.archi.gateway.starter.dao.BaseModel;
import lombok.Data;

import javax.persistence.Table;

@Data
@Table(name = "t_gateway_filter")
public class TGateWayFilterModel extends BaseModel {

    /**
     * 路由id
     */
    private String routerCode;
    private String filterName;
    private String filterArgs;
    private Integer orderBy;

}
