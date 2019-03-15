package cn.sysu.ChatRoom;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.Scanner;
import java.util.Set;

import cn.sysu.ChatRoom.Message.Message;
import cn.sysu.ChatRoom.Message.Client.ClientChatMessage;
import cn.sysu.ChatRoom.Message.Client.ClientLoginMessage;
import cn.sysu.ChatRoom.Message.Client.ClientMessage;
import cn.sysu.ChatRoom.Message.Server.ServerChatMessage;
import cn.sysu.ChatRoom.Message.Server.ServerLoginMessage;
import cn.sysu.ChatRoom.Message.Server.ServerLogoutMessage;
import cn.sysu.ChatRoom.Message.Server.ServerMessage;

public class Client {
	private static Charset charset = Charset.forName("utf-8");
	private static long time = System.currentTimeMillis();
	private static String name = String.valueOf(time);
	
	public static void main(String[] args) throws IOException {
		Selector selector = Selector.open();
		InetSocketAddress address = new InetSocketAddress(Constant.port);
		
		// 绑定与服务器的连接
		SocketChannel socketChannel = SocketChannel.open(address);
		socketChannel.configureBlocking(false); // 必须设置为非阻塞， 否则会报错"illegalBlockingModeException"
		SelectionKey selectionKey = socketChannel.register(selector, SelectionKey.OP_READ);
		
		// 登录服务器
		ClientLoginMessage loginMessage = new ClientLoginMessage(name);
		writeChannel(selectionKey, loginMessage.toBuffer(charset));
		
		// 持续监听来自服务器的消息
		new Thread(new ListenServer(selectionKey)).start();
		
		// 持续监听用户输入的聊天信息
		while(true) {
		    Scanner sc = new Scanner(System.in); 
	        String content = sc.nextLine();
	        
	        ClientChatMessage chatMessage = new ClientChatMessage(name, content);
	        writeChannel(selectionKey, chatMessage.toBuffer(charset));
		}
	}
	
	/**
	 * 向通道写入信息
	 * @param key 持有通道引用的句柄
	 * @param buffer 要被写入的buffer
	 */
	private static void writeChannel(SelectionKey key, ByteBuffer buffer) {
		SocketChannel socketChannel = (SocketChannel) key.channel();
		buffer.flip();
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
		SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
		
		// 关闭连接
		selectionKey.cancel();
		socketChannel.close();
	}
	
	private static class ListenServer implements Runnable {
		private SelectionKey selectionKey;
		
		public ListenServer(SelectionKey selectionKey) {
			this.selectionKey = selectionKey; 
		}

		private Selector selector() { return this.selectionKey.selector(); }
		
		private SocketChannel channel() { return (SocketChannel) this.selectionKey.channel(); }
		

		public void run() {
			while (true) {
				try {
					int numKey = selector().select();
					if (numKey == 0) continue;
					
					Set<SelectionKey> keys = selector().selectedKeys();
					Iterator<SelectionKey> iterator =  keys.iterator();
					while (iterator.hasNext()) {
						SelectionKey selectionKey = iterator.next();
						iterator.remove();
						
						if (selectionKey.isReadable()) readChannel(selectionKey);
					}
					
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			
		}

		private void readChannel(SelectionKey selectionKey) {
			SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
			
			ByteBuffer buffer = ByteBuffer.allocate(Constant.BUFFER_SIZE);
			try {
				// 读取消息至buffer
				while (true) {
					int numRead = socketChannel.read(buffer);
					if (numRead == -1) {
						close(selectionKey);
						System.out.println("ͨ数据读取至尽头，可能服务端通道调用close方法");
					} 
					if (numRead == 0) break; // 
				}
				buffer.flip();
				
				// System.out.println(new String(buffer.array())); debug
				
				// 解析消息
				ServerMessage message = ServerMessage.parseBuffer(buffer, charset);
				
				// 处理消息
				dealWithMessage(message);
			} catch (Exception e) {
				try {
					close(selectionKey);
					System.out.println("ͨ连接断开，可能服务端强制关闭了连接");
				} catch (IOException e1) {
					e1.printStackTrace();
				}
				
			}
			
		}
		
		private void dealWithMessage(ServerMessage message) {
			if (message instanceof ServerLoginMessage) {
				String name = message.getName();
				int numLogin = ((ServerLoginMessage) message).getNumLogin();
				System.out.println(String.format("欢迎[%s]进入聊天室, 现在共有%d人.", name, numLogin));
			} else if (message instanceof ServerChatMessage) {
				String name = message.getName();
				String content = ((ServerChatMessage) message).getContent();
				System.out.println(String.format("[%s]: %s", name, content));
			} else if (message instanceof ServerLogoutMessage) {
				String nameString = message.getName();
				int numLogin = ((ServerLogoutMessage) message).getNumLogin();
				System.out.println(String.format("[%s]离开了聊天室, 现在共有%d人", name, numLogin));
			}
		}

		private static void close(SelectionKey selectionKey) throws IOException {
			SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
		
			// 关闭连接
			selectionKey.cancel();
			socketChannel.close();
		}
		
	}

}
