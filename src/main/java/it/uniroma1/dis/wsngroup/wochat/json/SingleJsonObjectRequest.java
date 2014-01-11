package it.uniroma1.dis.wsngroup.wochat.json;

public class SingleJsonObjectRequest {
	private String request;
	private Object data;

	public String getRequest() {
		return request;
	}

	public void setRequest(String request) {
		this.request = request;
	}

	public Object getData() {
		return data;
	}

	public void setData(Object data) {
		this.data = data;
	}
}
