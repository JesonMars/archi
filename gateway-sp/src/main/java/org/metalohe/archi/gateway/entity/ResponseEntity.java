package org.metalohe.archi.gateway.entity;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class ResponseEntity {
    private String rCode;
    private String rMsg;
    private Object data;
    public static ResponseEntity buildSuccessRes(String rmsg,Object data){
        ResponseEntity responseBo=new ResponseEntity()
                                    .setRCode("0")
                                    .setRMsg(rmsg)
                                    .setData(data);
        return responseBo;
    }
    public static ResponseEntity buildErrorRes(String rmsg,Object data){
        ResponseEntity responseBo=new ResponseEntity()
                .setRCode("999")
                .setRMsg(rmsg)
                .setData(data);
        return responseBo;
    }

    public static ResponseEntity buildRes(String rCode,String rmsg,Object data){
        ResponseEntity responseBo=new ResponseEntity()
                .setRCode(rCode)
                .setRMsg(rmsg)
                .setData(data);
        return responseBo;
    }
}
