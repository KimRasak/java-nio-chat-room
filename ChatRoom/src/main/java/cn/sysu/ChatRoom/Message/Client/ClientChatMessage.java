package cn.sysu.ChatRoom.Message.Client;

import com.alibaba.fastjson.JSONObject;

public class ClientChatMessage extends ClientMessage {
	protected static String TYPE = "client chat";
	private String content;
	
	public ClientChatMessage(String name, String content) {
		super(name);
		this.content = content;
	}
	
	public String getContent() {
		return content;
	}

	@Override
	public String string() {
		JSONObject jsonObject = new JSONObject();
		jsonObject.put("name", name);
		jsonObject.put("type", TYPE);
		jsonObject.put("content", content);
		return jsonObject.toJSONString();
	}

}
