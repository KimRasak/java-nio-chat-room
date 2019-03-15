package cn.sysu.ChatRoom;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.Set;

import com.alibaba.fastjson.JSONObject;

import cn.sysu.ChatRoom.Message.Message;
import cn.sysu.ChatRoom.Message.Client.ClientChatMessage;
import cn.sysu.ChatRoom.Message.Client.ClientLoginMessage;
import cn.sysu.ChatRoom.Message.Client.ClientMessage;
import cn.sysu.ChatRoom.Message.Server.ServerChatMessage;
import cn.sysu.ChatRoom.Message.Server.ServerLoginMessage;
import cn.sysu.ChatRoom.Message.Server.ServerLogoutMessage;
import cn.sysu.ChatRoom.Message.Server.ServerMessage;

public class Server {
	private static Charset charset = Charset.forName("utf-8");
	private static int numLogin = 0;
	
	public static void main(String[] args) {
		try {
			Selector selector = Selector.open();
			
			registerServerChannel(selector);
			listenLoop(selector);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	private static void listenLoop(Selector selector) throws IOException {
		while (true) {
			int numSelectedKeys = selector.select();
			if (numSelectedKeys == 0) continue;
			
			Set<SelectionKey> selectedKeys =  selector.selectedKeys();
			Iterator<SelectionKey> iterator = selectedKeys.iterator();
			while (iterator.hasNext()) {
				SelectionKey selectionKey = iterator.next();
				iterator.remove();
				
				if (selectionKey.isAcceptable()) acceptChannel(selectionKey);
				else if (selectionKey.isReadable()) readChannel(selectionKey);
			}
			
		}
		
	}

	/**
	 * 注册新的连接。将{@code ServerSocketChannel}所新接收的{@code SocketChannel}绑定到{@code Selector}上。
	 * @param selectionKey {@code ServerSocketChannel}与{@code Selector}联系的句柄
	 */
	private static void acceptChannel(SelectionKey selectionKey) throws IOException {
		ServerSocketChannel serverChannel = (ServerSocketChannel) selectionKey.channel();
		SocketChannel socketChannel = serverChannel.accept();
		socketChannel.configureBlocking(false); // 如果不设置为非阻塞就没有意义
		socketChannel.register(selectionKey.selector(), SelectionKey.OP_READ);
	}

	/**
	 * 从已"读取准备就绪"的通道，读取信息。
	 * @param selectionKey {@code ServerSocketChannel}与{@code Selector}֮所联系的句柄
	 */
	private static void readChannel(SelectionKey selectionKey) {
		SocketChannel channel = (SocketChannel) selectionKey.channel();
		
		ByteBuffer buffer = ByteBuffer.allocate(Constant.BUFFER_SIZE);
		try {
			// 读取消息至buffer
			while (true) {
				int numRead = channel.read(buffer);
				String temp = new String(buffer.array());
				if (numRead == -1) {
					close(selectionKey);
					System.out.println("ͨ数据读取至尽头，可能客户端通道调用close方法");
				}
				if (numRead == 0) break;
			}
			buffer.flip();
			
			// 解析消息
			ClientMessage message = ClientMessage.parseBuffer(buffer, charset);
			
			// 处理消息
			dealWithMessage(selectionKey, message);

		} catch (IOException e) {
			try {
				close(selectionKey);
				System.out.println("ͨ连接断开，可能客户端强制关闭了连接");
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
	}

	private static void dealWithMessage(SelectionKey selectionKey, ClientMessage message) throws IOException {
		if (message instanceof ClientLoginMessage)
			dealWithLogin(selectionKey, (ClientLoginMessage) message);
		else if (message instanceof ClientChatMessage)
			dealWithChat(selectionKey, (ClientChatMessage) message);
		
	}

	private static void dealWithChat(SelectionKey selectionKey, ClientChatMessage message) {
		SocketChannel inChannel = (SocketChannel) selectionKey.channel();
		
		ServerChatMessage serverMessage = new ServerChatMessage(message.getName(), message.getContent());
		
		// 创建buffer, 放入消息
		ByteBuffer buffer = serverMessage.toBuffer(charset);
		
		System.out.println(String.format("[服务器]收到%s的消息: %s", message.getName(), message.getContent()));
		
		// 广播[聊天信息]给其他人
		broadCast(selectionKey.selector(), inChannel, buffer);
	}

	private static void dealWithLogin(SelectionKey selectionKey, ClientLoginMessage message) throws IOException {
		SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
		Selector selector = selectionKey.selector();
		
		// 将用户名附加到key上
		selectionKey.attach(message.getName());
		
		// 创建buffer, 放入消息
		ServerLoginMessage serverMessage = new ServerLoginMessage(message.getName(), numLogin);
		ByteBuffer buffer = serverMessage.toBuffer(charset);
		
		// 广播[登录信息]给其他人
		broadCast(selector, socketChannel, buffer);
		
		// 聊天人数+1
		numLogin++;
		System.out.println(String.format("[服务器]用户%s登录, 目前聊天室有%d人", message.getName(), numLogin));
	}
	
	/**
	 * 广播给输入通道以外的其他通道
	 * @param selector 选择器
	 * @param inChannel 输入消息的通道
	 * @param buffer 存储输入信息的{@code ByteBuffer}
	 */
	private static void broadCast(Selector selector, SocketChannel inChannel, ByteBuffer buffer) {
		Set<SelectionKey> keys = selector.keys();
		for (SelectionKey key : keys)
			if (key.channel() instanceof SocketChannel && key.channel() != inChannel) {
				SocketChannel socketChannel = (SocketChannel) key.channel();
				writeChannel(key, buffer);
			}
	}
	
	/**
	 * 向通道写入信息
	 * @param key 持有通道引用的句柄
	 * @param buffer 要被写入的buffer
	 */
	private static void writeChannel(SelectionKey key, ByteBuffer buffer) {
		buffer.flip();
		SocketChannel socketChannel = (SocketChannel) key.channel();
		try {
			while (true) {
				int numWrite = socketChannel.write(buffer);
				if (numWrite == 0) break;
				if (numWrite == -1) close(key);
				
			}
		} catch (IOException e) {
			// 连接中断, 关闭连接
			try {
				close(key);
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
	}

	private static void close(SelectionKey selectionKey) throws IOException {
		Selector selector = selectionKey.selector();
		SocketChannel toCloseChannel = (SocketChannel) selectionKey.channel();
		
		// 关闭连接
		selectionKey.cancel();
		toCloseChannel.close();
		
		// 聊天人数-1
		numLogin--;
		System.out.println(String.format("用户断开连接, 目前聊天室有%d人", numLogin));
		
		// 为所有其他用户发送离线信息
		String name = (String)selectionKey.attachment();
		ServerLogoutMessage logoutMessage = new ServerLogoutMessage(name, numLogin);
		broadCast(selector, toCloseChannel, logoutMessage.toBuffer(charset));
	}

	private static void registerServerChannel(Selector selector) throws IOException {
		ServerSocketChannel serverChannel = ServerSocketChannel.open();
		serverChannel.bind(new InetSocketAddress(Constant.port)); // 绑定至本地端口
		serverChannel.configureBlocking(false); // 如果不设置为非阻塞就没有意义了
		serverChannel.register(selector, SelectionKey.OP_ACCEPT); // ServerSocketChannel只能注册accept事件
		
		System.out.println("服务器开启监听...");
	}
}
