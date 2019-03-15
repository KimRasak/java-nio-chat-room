package cn.sysu.ChatRoom.Message.Server;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import com.alibaba.fastjson.JSONObject;

import cn.sysu.ChatRoom.Message.Message;

public abstract class ServerMessage extends Message{

	public ServerMessage(String name) {
		super(name);
	}


	public static ServerMessage parseString(String string) {
		JSONObject jsonObject = (JSONObject) JSONObject.parse(string);
		String type = jsonObject.getString("type");
		String name = jsonObject.getString("name");
		
		if (type.equals(ServerLoginMessage.TYPE)) {
			int numLogin = jsonObject.getIntValue("numLogin");
			return new ServerLoginMessage(name, numLogin);
		}
		else if (type.equals(ServerChatMessage.TYPE)) {
			String content = jsonObject.getString("content");
			return new ServerChatMessage(name, content);
		} else if (type.equals(ServerLogoutMessage.TYPE)) {
			int numLogin = jsonObject.getIntValue("numLogin");
			return new ServerLogoutMessage(name, numLogin);
		}
		
		return null;
	}
	
	public static ServerMessage parseBuffer(ByteBuffer buffer, Charset charset) {
		String messageString = new String(charset.decode(buffer).array());
		return ServerMessage.parseString(messageString);
	}
}
