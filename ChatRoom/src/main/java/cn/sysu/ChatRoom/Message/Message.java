package cn.sysu.ChatRoom.Message;

import java.io.Console;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import com.alibaba.fastjson.JSONObject;

import cn.sysu.ChatRoom.Constant;

public abstract class Message {
	protected String name; // 消息发出者的名字
	
	public Message(String name) {
		super();
		this.name = name;
	}
	
	public String getName() {
		return name;
	}
	
	public abstract String string();
	
	public ByteBuffer toBuffer(Charset charset) {
		ByteBuffer buffer = ByteBuffer.allocate(Constant.BUFFER_SIZE);
		buffer.put(charset.encode(this.string()));
		return buffer;
	}
	
}
