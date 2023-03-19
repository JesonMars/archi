package org.metalohe.archi.gateway.starter.service;

import lombok.extern.apachecommons.CommonsLog;
import org.metalohe.archi.gateway.filter.FilterDefinition;
import org.metalohe.archi.gateway.starter.dao.mapper.IGateWayFilterMapper;
import org.metalohe.archi.gateway.starter.dao.mapper.IGateWayMapper;
import org.metalohe.archi.gateway.starter.dao.model.TGateWayFilterModel;
import org.metalohe.archi.gateway.starter.entity.FilterEnum;
import org.metalohe.archi.gateway.util.GsonUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@CommonsLog
public class FilterService {


    @Resource
    IGateWayFilterMapper gateWayFilterMapper;

    public List<FilterDefinition> getByRouterCode(String code){
//        TGateWayFilterModel model=new TGateWayFilterModel();
        List<FilterDefinition> filterDefinitionList=new ArrayList<>();

//        model.setRouterCode(code);
        List<TGateWayFilterModel> filterModelList = gateWayFilterMapper.selectOrderBy(code);
                //gateWayFilterMapper.select(model);
        if(CollectionUtils.isEmpty(filterModelList)){
            FilterDefinition emptyFilter = new FilterDefinition();
            emptyFilter.setName(FilterEnum.AddRequestHeader.name());
            Map<String, String> filterParams = new HashMap<>(8);
            //该_genkey_前缀是固定的，见org.springframework.cloud.gateway.support.NameUtils类
            filterParams.put("_genkey_0", "requestFrom");
            filterParams.put("_genkey_1", "bxGateWay");
            emptyFilter.setArgs(filterParams);
            filterDefinitionList.add(0,emptyFilter);
            return filterDefinitionList;
        }


        for (TGateWayFilterModel tGateWayFilterModel : filterModelList) {
            FilterDefinition filterDefinition=new FilterDefinition();
            filterDefinition.setName(tGateWayFilterModel.getFilterName());

            Map map = GsonUtils.fromJson2Map(tGateWayFilterModel.getFilterArgs());
            filterDefinition.setArgs(map);
            filterDefinitionList.add(filterDefinition);
        }
        return filterDefinitionList;
    }

}