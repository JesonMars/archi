package org.metalohe.archi.gateway.starter.service;

import org.metalohe.archi.gateway.starter.dao.mapper.IGateWayThirdJarMapper;
import org.metalohe.archi.gateway.starter.dao.model.TGateWayThirdjarModel;
import org.metalohe.archi.gateway.starter.entity.JarEntity;
import org.metalohe.archi.gateway.starter.util.LoadClassUtil;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ThirdJarService {
    @Resource
    IGateWayThirdJarMapper gateWayThirdJarMapper;

    public void initThirdJar(){
        List<TGateWayThirdjarModel> tGateWayThirdjarModels = gateWayThirdJarMapper.selectAll();
        List<JarEntity> collect = tGateWayThirdjarModels.stream().map(x -> {
            JarEntity jarEntity = new JarEntity();
            jarEntity.setDownloadUrl(x.getDownloadUrl());
            jarEntity.setName(x.getName());
            jarEntity.setVersion(x.getVersion());
            return jarEntity;
        }).collect(Collectors.toList());

        LoadClassUtil.loadThirdJar(collect);
    }
}
