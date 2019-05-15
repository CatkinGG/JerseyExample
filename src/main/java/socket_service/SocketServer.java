package socket_service;

import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.LinkedBlockingQueue;

public class SocketServer extends java.lang.Thread {
	private boolean OutServer = false;
	private ServerSocket server;
	private final int ServerPort = 8765;// 要監控的port
	private DataStreamThread dataStream;
	private KeyboardInputThread keyboardInputThread = new KeyboardInputThread();

	public SocketServer() {
		try {
			server = new ServerSocket(ServerPort);
			InetAddress addresses = InetAddress.getLocalHost();
			System.out.println(addresses.getHostAddress() + ":" + ServerPort);
			//
			keyboardInputThread.start();
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Socket啟動有問題:" + e.toString());
		}
	}

	public void run() {
		System.out.println("伺服器已啟動 !");
		while (!OutServer) {
			Socket socket = null;
			try {
				synchronized (server) {
					socket = server.accept();
				}
				System.out.println("取得連線,InetAddress = " + socket.getInetAddress());
				//
				if (dataStream != null) {
					dataStream.interrupt();
					dataStream = null;
				}

				dataStream = new DataStreamThread(socket);
				keyboardInputThread.setKeyboardInput(dataStream);

				dataStream.start();
				
//				Thread.sleep(10000);
//				System.out.println("中斷連線");
//				socket.close();

				// in.close();
				// in = null;
				// out.close();
				// out = null;
				// socket.close();
			} catch (Exception e) {
				System.out.println("Socket連線有問題!");
				e.printStackTrace();
				break;
			}
		}
		System.out.println("伺服器結束 !");
	}

//	public static void main(String args[]) {
//		(new SocketServer()).start();
//	}
}
