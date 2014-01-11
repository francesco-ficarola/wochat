package it.uniroma1.dis.wsngroup.wochat.json;

public class JsonResponse<T> {
	private String response;
	private T data;

	public String getResponse() {
		return response;
	}

	public void setResponse(String response) {
		this.response = response;
	}

	public T getData() {
		return data;
	}

	public void setData(T data) {
		this.data = data;
	}
}
