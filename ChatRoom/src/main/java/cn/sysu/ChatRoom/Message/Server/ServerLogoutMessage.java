package cn.sysu.ChatRoom.Message.Server;

import com.alibaba.fastjson.JSONObject;

public class ServerLogoutMessage extends ServerMessage {
	protected static String TYPE = "server logout";
	private int numLogin;
	
	public ServerLogoutMessage(String name, int numLogin) {
		super(name);
		this.numLogin = numLogin;
	}

	
	@Override
	public String string() {
		JSONObject jsonObject = new JSONObject();
		jsonObject.put("name", name);
		jsonObject.put("type", TYPE);
		jsonObject.put("numLogin", numLogin);
		return jsonObject.toJSONString();
	}
	
	public int getNumLogin() {
		return numLogin;
	}

}
