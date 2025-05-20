package com.app.net.req;

import com.fasterxml.jackson.annotation.JsonInclude;

/**上传文件
 * Created by Administrator on 2016/9/7.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UploadingBeanReq extends BaseReq {
    public String service = "smarthos.system.file.upload";
    public String module = "APPOINTMENT";
    public String fileType = "IMAGE";

}
