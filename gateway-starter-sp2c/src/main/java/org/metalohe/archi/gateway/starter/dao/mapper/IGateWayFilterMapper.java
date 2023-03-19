package org.metalohe.archi.gateway.starter.dao.mapper;

import org.metalohe.archi.gateway.starter.dao.IBaseMapper;
import org.metalohe.archi.gateway.starter.dao.model.TGateWayFilterModel;
import org.apache.ibatis.annotations.Mapper;
import tk.mybatis.mapper.entity.Example;

import java.util.List;

@Mapper
public interface IGateWayFilterMapper extends IBaseMapper<TGateWayFilterModel> {

    default List<TGateWayFilterModel> selectOrderBy(String routerCode){
        Example orderBy = Example.builder(TGateWayFilterModel.class).orderByAsc("orderBy").build();
        Example.Criteria criteria = orderBy.createCriteria();
        criteria.andEqualTo("routerCode",routerCode);
        return this.selectByExample(orderBy);
    }
}
