package org.metalohe.archi.gateway.starter.dao.mapper;

import org.metalohe.archi.gateway.starter.dao.IBaseMapper;
import org.metalohe.archi.gateway.starter.dao.model.TGateWayModel;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.springframework.util.CollectionUtils;
import tk.mybatis.mapper.entity.Example;

import java.util.List;

@Mapper
public interface IGateWayMapper extends IBaseMapper<TGateWayModel> {

    default TGateWayModel selectByPathAndSeg(String path,String segment){
        TGateWayModel tGateWayModel = new TGateWayModel();
        tGateWayModel.setPath(path);
        tGateWayModel.setSegment(segment);
        List<TGateWayModel> entityList = this.select(tGateWayModel);
        if(!CollectionUtils.isEmpty(entityList)){
            tGateWayModel = entityList.get(0);
            return tGateWayModel;
        }
        return tGateWayModel;
    }

    @Select("SELECT router_code as routerCode,path,segment,router_uri as routerUri FROM t_gateway GROUP BY router_code")
    List<TGateWayModel> selectAllRouter();

//    @Select("SELECT router_code as routerCode,path,segment,router_uri as routerUri FROM t_gateway where crud_code<>null")
    default List<TGateWayModel> selectAllCrudCode(String routerCode){
        Example example=Example.builder(TGateWayModel.class).build();
        example.createCriteria()
                .andEqualTo("routerCode",routerCode)
                .andEqualTo("isDelete",0)
                .andIsNotNull("crudCode");
        return this.selectByExample(example);
    }

    @Select("SELECT router_code as routerCode,path,segment,router_uri as routerUri FROM t_gateway where is_delete=0 and router_code not in ('clue','clue_upload') GROUP BY router_code")
    List<TGateWayModel> selectCusRouter();
}
