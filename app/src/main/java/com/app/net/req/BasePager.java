package com.app.net.req;


public class BasePager extends BaseReq {
	/**
	 *
	 */
	private static final long serialVersionUID = 1L;
	/** 页码，默认1 */
	private int page;
	/** 每页几条，默认10 */
	private String limit = "30";

	public int getPage() {
		return page;
	}

	public void setPage(int page) {
		this.page = page;
	}

	public String getLimit() {
		return limit;
	}

	public void setLimit(String limit) {
		this.limit = limit;
	}
}
