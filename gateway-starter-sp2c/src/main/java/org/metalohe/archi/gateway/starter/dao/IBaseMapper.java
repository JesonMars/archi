package org.metalohe.archi.gateway.starter.dao;

import tk.mybatis.mapper.common.BaseMapper;
import tk.mybatis.mapper.common.base.select.SelectMapper;
import tk.mybatis.mapper.common.example.SelectByExampleMapper;

public interface IBaseMapper<T extends BaseModel> extends BaseMapper<T>, SelectMapper<T>, SelectByExampleMapper<T> {
}
