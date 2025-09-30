package controller.ServerAndClientSocket;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.Vector;

import com.fatboyindustrial.gsonjavatime.Converters;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import cloudinary.CloudinaryService;
import controller.Handler.FileHandler;

import model.ChatMessage;
import model.FileInfo;
import model.Packet;


//Client handle
public class SocketServer implements Runnable {
	private Socket socket;
	private BufferedReader in;
	private BufferedWriter out;
	private InputStream inputStream;
	public Gson gson;
	private CloudinaryService cloudinaryService;


	// Danh sách tất cả client đang kết nối (dùng Vector để quản lý)
	private static Vector<SocketServer> clients = new Vector<>();

	public SocketServer(Socket socket) {
		try {
			this.socket = socket;
			this.inputStream = socket.getInputStream();
			this.in = new BufferedReader(new InputStreamReader(this.inputStream));
			this.out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
			gson = Converters.registerAll(new GsonBuilder()).setDateFormat("EEE MMM dd HH:mm:ss z yyyy").create();
			this.cloudinaryService = new CloudinaryService();

			clients.add(this); 

			System.out.println("Client connected: " + socket.getInetAddress());

		} catch (IOException e) {
			close();
		}
	}


	@Override
	public void run() {
		try {
			String message;
			while ((message = in.readLine()) != null) {
				

				System.out.println("Received: " + message);

				Packet packet = gson.fromJson(message, Packet.class);

				System.out.println(packet);
				switch (packet.getType()) {
				case "FILE_META": {
					ChatMessage chatMessage = gson.fromJson(gson.toJson(packet.getData()), ChatMessage.class);

					FileInfo meta = gson.fromJson(chatMessage.getContent(), FileInfo.class);


//					new Thread(() -> {
						FileHandler fileHandler = new FileHandler(cloudinaryService, gson, inputStream);
						fileHandler.setPacketCallback(packetReponse -> {
							broadcast(gson.toJson(packetReponse));
							
							System.out.println("Reponse: "+packetReponse);
						});
						
						fileHandler.receiveFile(meta, chatMessage);
//					    fileHandler.receiveFile(); // nhận file bằng inputStream.read(...)
//					}).start();

					break;
				}
				default:
					broadcast(message); // gửi cho tất cả client khác
				}
			}
		} catch (IOException e) {
			close();
		}
	}

	private void sendSelfClient(Packet packet) throws IOException {
		this.out.write(gson.toJson(packet));
		this.out.newLine();
		this.out.flush();
	}

	// Gửi tin nhắn cho tất cả client trong Vector
	private void broadcast(String message) {
		for (SocketServer client : clients) {
			if (client != this) {
				try {
					if (client.socket.isClosed())
						continue;

					client.out.write(message);
					client.out.newLine();
					client.out.flush();
				} catch (IOException e) {
					client.close();
				}
			}
		}
	}

	// Đóng kết nối
	private void close() {
		try {
			if (in != null)
				in.close();
			if (out != null)
				out.close();
			if (socket != null)
				socket.close();

			clients.remove(this);

			System.out.println("Client disconnected.");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
