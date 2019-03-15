package cn.sysu.ChatRoom.Message.Server;

import com.alibaba.fastjson.JSONObject;

public class ServerChatMessage extends ServerMessage {
	protected static String TYPE = "server chat";
	private String content;

	public ServerChatMessage(String name, String content) {
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
