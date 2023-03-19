package org.metalohe.archi.gateway.starter.dao.model;

import org.metalohe.archi.gateway.starter.dao.BaseModel;
import lombok.Data;

import javax.persistence.Table;

@Data
@Table(name = "t_gateway_thirdjar")
public class TGateWayThirdjarModel extends BaseModel {

    /**
     * .jar结尾的jar包全名称
     */
    private String name;

    private String artifactId;

    private String groupId;

    private String version;

    /**
     * jar在服务器上的路径，或者jar包下载后的路径，默认值为服务所执行jar包同路径的thirdjar路径下
     */
//    private String path;
    /**
     * 要下载的jar包url
     */
    private String downloadUrl;


}
