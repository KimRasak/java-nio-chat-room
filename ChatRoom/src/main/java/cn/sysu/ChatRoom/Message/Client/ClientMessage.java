package cn.sysu.ChatRoom.Message.Client;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import com.alibaba.fastjson.JSONObject;

import cn.sysu.ChatRoom.Message.Message;

public abstract class ClientMessage extends Message {

	public ClientMessage(String name) {
		super(name);
	}
	

	public static ClientMessage parseString(String string) {
		JSONObject jsonObject = (JSONObject) JSONObject.parse(string);
		String type = jsonObject.getString("type");
		String name = jsonObject.getString("name");
		
		if (type.equals(ClientLoginMessage.TYPE))
			return new ClientLoginMessage(name);
		else if (type.equals(ClientChatMessage.TYPE)) {
			String content = jsonObject.getString("content");
			return new ClientChatMessage(name, content);
		}
		
		return null;
	}
	
	public static ClientMessage parseBuffer(ByteBuffer buffer, Charset charset) {
		String messageString = new String(charset.decode(buffer).array());
		return ClientMessage.parseString(messageString);
	}

}
