package org.metalohe.archi.gateway.starter;

import org.metalohe.archi.gateway.starter.dao.mapper.IGateWayMapper;
import org.metalohe.archi.gateway.starter.dao.model.TGateWayModel;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest
public class GateWayMapperTest {
    @Autowired
    IGateWayMapper mapper;

    @Test
    public void test(){
        List<TGateWayModel> select = mapper.select(new TGateWayModel());

        System.out.println(select);
        Assert.assertNotNull(select);
    }
}
