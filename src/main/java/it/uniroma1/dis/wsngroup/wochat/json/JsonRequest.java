package it.uniroma1.dis.wsngroup.wochat.json;

public class JsonRequest<T> {
	private String request;
	private T data;

	public String getRequest() {
		return request;
	}

	public void setRequest(String request) {
		this.request = request;
	}

	public T getData() {
		return data;
	}

	public void setData(T data) {
		this.data = data;
	}
}
