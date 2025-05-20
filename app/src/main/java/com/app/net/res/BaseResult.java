package com.app.net.res;


import java.io.Serializable;

/**
 * 返回基类
 */
//@JsonIgnoreProperties(ignoreUnknown = true)
public class BaseResult implements Serializable {
    // 状态码
    public String code;
    // 消息
    public String msg;
    //public boolean succ;
    /**
     * 有效期7天
     */
    public String token;

    /**
     * 翻页
     */
    public Paginator paginator;
}
