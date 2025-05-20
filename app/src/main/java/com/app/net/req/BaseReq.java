package com.app.net.req;

import java.io.Serializable;

public class BaseReq implements Serializable {
	public String service = "";
	/**
	 * 服务商编码
	 **/
	public String spid = "1001";

	/**
	 * 终端ip
	 **/
	public String oper = "127.0.0.1";
	/**
	 * 第一位表示客户端类型  1-安卓 2-IOS
	 * 第二位 表示医生端还是患者端   1-患者端  2-医生端
	 */
	public String channel = "12";
	/**
	 * 随机码
	 */
	public String random = "1234";

	public String format = "JSON";

	public String token;
	//版本号
	public String version = "100";
	public String hosId = "1100990002";
	//是否开方
	public Boolean openRecipeFlag;

	public void setToken(String token) {
		this.token = token;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public void setRandom(String random) {
		this.random = random;
	}

	public void setService(String service) {
		this.service = service;
	}

}
