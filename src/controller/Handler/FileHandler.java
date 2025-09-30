package controller.Handler;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.function.Consumer;

import com.google.gson.Gson;

import cloudinary.CloudinaryService;
import model.ChatMessage;
import model.FileInfo;
import model.Packet;

public class FileHandler {
	private static final int BUFFER_SIZE = 4096;

	private CloudinaryService cloudinaryService;
	private InputStream inputStream;
	private Consumer<Packet> packetCallback;
	private Gson gson;

	public FileHandler(CloudinaryService cloudinaryService, Gson gson, InputStream inputStream) {
		this.cloudinaryService = cloudinaryService;
		this.inputStream = inputStream;
		this.gson = gson;

	}

	public void setPacketCallback(Consumer<Packet> handler) {
		this.packetCallback = handler;
	}

	public void receiveFile(FileInfo fileInfo, ChatMessage chatMessage) {

		try {
			// Nhận metadata
			if (fileInfo != null) {
				String fileName = fileInfo.getFileName();
				long fileSize = fileInfo.getFileSize();

				ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
				byte[] buffer = new byte[BUFFER_SIZE];
				long totalReceived = 0;

				// ghi content file vao byteArray
				while (totalReceived < fileSize) {
					int count = inputStream.read(buffer);
					if (count == -1)
						break;
					byteArrayOutputStream.write(buffer, 0, count);
					totalReceived += count;
				}
				byte[] filebytes = byteArrayOutputStream.toByteArray();

				String uploadURL = cloudinaryService.uploadFile(filebytes, fileInfo);

				if (uploadURL != null && !uploadURL.isEmpty()) {
					Packet packet = new Packet();
					packet.setType("FILE_UPLOAD_RESULT");
					System.out.println(uploadURL);

					FileInfo fileInfoReponse = new FileInfo(fileName, 0, uploadURL);
					chatMessage.setContent(gson.toJson(fileInfoReponse));
					packet.setData(chatMessage);

					if (packetCallback != null) {
						packetCallback.accept(packet);
					}
				}
				byteArrayOutputStream.close();
				System.out.println("File received: " + fileName);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
}
