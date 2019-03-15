package cn.sysu.ChatRoom.Message.Client;

import com.alibaba.fastjson.JSONObject;

public class ClientLoginMessage extends ClientMessage {
	protected static String TYPE = "client login";

	public ClientLoginMessage(String name) {
		super(name);
	}

	@Override
	public String string() {
		JSONObject jsonObject = new JSONObject();
		jsonObject.put("type", TYPE);
		jsonObject.put("name", name);
		return jsonObject.toJSONString();
	}
	
}
