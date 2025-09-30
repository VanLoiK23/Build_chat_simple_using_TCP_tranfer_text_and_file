package controller.ServerAndClientSocket;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;

import com.fatboyindustrial.gsonjavatime.Converters;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import model.ChatMessage;
import model.FileInfo;
import model.Packet;
import model.User;

public class SocketClient {
	private static SocketClient instance;
	private Socket socket;
	private PrintWriter out;
	private BufferedReader in;
	public Gson gson;
	private OutputStream outputStream;
	private static final String SERVER = "192.168.1.159";// IP server
	private static final int PORT = 12345;
	private Consumer<ChatMessage> messageHandler;
	private Consumer<ChatMessage> fileMessageHandler;
	public BlockingQueue<Packet> responseQueue;

	private SocketClient() {
		try {
			socket = new Socket(SERVER, PORT);
			outputStream = socket.getOutputStream();
			out = new PrintWriter(outputStream, true);
			in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			gson = Converters.registerAll(new GsonBuilder()).setDateFormat("EEE MMM dd HH:mm:ss z yyyy").create();
			responseQueue = new LinkedBlockingQueue<>();

			listenForMessages();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// create singleton
	public static SocketClient getInstance() {
		if (instance == null) {
			instance = new SocketClient();
		}
		return instance;
	}

	public void sendPacket(Packet packet) {
		String json = gson.toJson(packet);
		out.println(json);
		out.flush();
	}

	public void setMessageHandler(Consumer<ChatMessage> handler) {
		this.messageHandler = handler;
	}

	public void setFileMessageHandler(Consumer<ChatMessage> handler) {
		this.fileMessageHandler = handler;
	}

	// receive difference message from other client
	private void listenForMessages() {
		new Thread(() -> {
			try {
				String msg;
				while ((msg = in.readLine()) != null) {
					Packet packet = gson.fromJson(msg, Packet.class);
					if (packet.getType().equals("MESSAGE")) {

						ChatMessage chatMessage = gson.fromJson(gson.toJson(packet.getData()), ChatMessage.class);

						if (messageHandler != null) {
							messageHandler.accept(chatMessage);
						}
					} else if (packet.getType().equals("FILE_UPLOAD_RESULT")) {
						ChatMessage chatMessage = gson.fromJson(gson.toJson(packet.getData()), ChatMessage.class);

						if (fileMessageHandler != null) {
							fileMessageHandler.accept(chatMessage);
						}
					}
//					else {
//						responseQueue.offer(packet);
//					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}).start();
	}

	// chia ra từng phân khúc 4KB và gửi
	private static final int BUFFER_SIZE = 4096;

	// Gửi file từ client
	public void sendFile(File file, User sender, String receiverId) {
		new Thread(() -> {
			try {
				FileInputStream fis = new FileInputStream(file);

				ChatMessage chatMessage = new ChatMessage();
				chatMessage.setSender(sender);
				chatMessage.setType("file");
				chatMessage.setRead(false);
				chatMessage.setTimestamp(LocalDateTime.now());

				Packet packetRequest = new Packet();
				packetRequest.setType("FILE_META");
				packetRequest.setData(chatMessage);

				// Gửi metadata
				chatMessage.setContent(gson.toJson(new FileInfo(file.getName(), file.length(), null)));

				String json = gson.toJson(packetRequest);
				out.println(json);
				out.flush();

				// Gửi nội dung file
				// đọc từng byte của file và ghi
				byte[] buffer = new byte[BUFFER_SIZE];
				int count;
				// ghi byte vào outPutStream và send
				while ((count = fis.read(buffer)) > 0) {
					outputStream.write(buffer, 0, count);
				}
				outputStream.flush();
				fis.close();
				System.out.println("File sent: " + file.getName());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}).start();
	}

	public void close() {
		try {
			in.close();
			out.close();
			socket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}