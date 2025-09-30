package controller;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

import com.google.gson.Gson;

import controller.Common.CommonController;
import controller.ServerAndClientSocket.SocketClient;
import controller.render.FileRenderMessage;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.FileChooser;
import model.ChatMessage;
import model.FileInfo;
import model.Packet;
import model.User;

public class ChatController implements Initializable {

	private User user;
	private SocketClient socketClient;
	private CommonController commonController;
	private Gson gson;

	private String lastSenderId = null;

	@FXML
	private VBox container;

	@FXML
	private Text name;

	@FXML
	private ScrollPane scrollPane;

	@FXML
	private TextField messageText;

	@FXML
	void clickEnter(KeyEvent event) throws IOException {
		if (event.getCode().toString().equals("ENTER")) {
			onSend(messageText.getText(), "all");
		}
	}

	public void setUser(User user) {
		this.user = user;

		name.setText(user.getUsername());
	}

	private void onSend(String messenger, String receiverId) throws IOException {
		if (messageText.getText() != null && !messageText.getText().isEmpty()) {

			ChatMessage chatMessage = new ChatMessage();
			chatMessage.setSender(user);
			chatMessage.setContent(messenger);
			chatMessage.setType("text");
			chatMessage.setRead(false);
			chatMessage.setTimestamp(LocalDateTime.now());

			Packet packetRequest = new Packet();
			packetRequest.setType("MESSAGE");
			packetRequest.setData(chatMessage);

			socketClient.sendPacket(packetRequest);

			onSendAndReceiveMessenge(chatMessage, true);

			messageText.clear();
		}
	}

	class styleDifferenceClass {
		private Pos position;
		private String styleCss;

		styleDifferenceClass(Pos position, String styleCss) {
			this.position = position;
			this.styleCss = styleCss;
		}
	}

	// tách ra MessageRender
	public void onSendAndReceiveMessenge(ChatMessage chatMessage, Boolean isSend) throws MalformedURLException {

		String messageContent = chatMessage.getContent();

		Map<Boolean, styleDifferenceClass> mapStyleMessenger = new HashMap<>();
		mapStyleMessenger.put(true, new styleDifferenceClass(Pos.CENTER_RIGHT, "-fx-text-fill: rgb(239,242,255);"
				+ "-fx-background-color: rgb(15,125,242);" + "-fx-background-radius: 20px;"));
		mapStyleMessenger.put(false, new styleDifferenceClass(Pos.CENTER_LEFT,
				"-fx-text-fill: black;" + "-fx-background-color: rgb(233,233,235);" + "-fx-background-radius: 20px;"));

		User userSender = gson.fromJson(gson.toJson(chatMessage.getSender()), User.class);

		Text text = new Text(messageContent);

		if (isSend) {
			text.setFill(Color.color(0.934, 0.945, 0.996));
		}
		TextFlow textFlow = new TextFlow(text);
		textFlow.setStyle(mapStyleMessenger.get(isSend).styleCss);
		textFlow.setPadding(new Insets(5, 10, 5, 10));
		textFlow.setMaxWidth(300);

		// HBox chứa avatar + tin nhắn
		HBox hBox = new HBox(10);
		hBox.setAlignment(mapStyleMessenger.get(isSend).position);
		hBox.setPadding(new Insets(5, 5, 5, 10));
		hBox.setStyle("-fx-cursor: text;");

		hBox.getChildren().addAll(textFlow);

		// VBox chứa tên + HBox
		VBox messageBox = new VBox(2);

		if (isSend) {
			messageBox.getChildren().addAll(hBox);

			container.getChildren().add(messageBox);
		} else {
			// Tên người gửi
			// nếu trùng người gửi thì ko hiển thị tên
			System.out.println("lastSender: " + lastSenderId);
			if (!userSender.getUserId().equals(lastSenderId)) {
				Label nameLabel = new Label(userSender.getUsername());
				nameLabel.setStyle("-fx-font-weight: bold; -fx-padding: 0 0 3 5;");
				messageBox.getChildren().addAll(nameLabel, hBox);
			} else {
				hBox.setStyle("-fx-translate-x:0px");
				hBox.setSpacing(1.0);
				messageBox.getChildren().add(hBox); // không có tên
			}

			Platform.runLater(() -> container.getChildren().add(messageBox));
		}

		lastSenderId = userSender.getUserId();
	}

	public void renderFileMessage(ChatMessage chatMessage, boolean isSend) throws IOException {
		FileInfo fileData = socketClient.gson.fromJson(chatMessage.getContent(), FileInfo.class);
		User userSender = gson.fromJson(gson.toJson(chatMessage.getSender()), User.class);

		FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/component/FileBubble.fxml"));
		Node fileBubble = loader.load();

		Map<Boolean, styleDifferenceClass> mapStyleMessenger = new HashMap<>();
		mapStyleMessenger.put(true, new styleDifferenceClass(Pos.CENTER_RIGHT,
				"-fx-background-color: rgb(15,125,242); -fx-background-radius: 20px;"));
		mapStyleMessenger.put(false, new styleDifferenceClass(Pos.CENTER_LEFT,
				"-fx-background-color: rgb(233,233,235); -fx-background-radius: 20px;"));

		FileRenderMessage controller = loader.getController();
		controller.setFileInfo(fileData); // truyền dữ liệu vào controller

		HBox hBox = new HBox(10);
		hBox.setAlignment(mapStyleMessenger.get(isSend).position);
		hBox.setPadding(new Insets(5, 5, 5, 10));

		hBox.getChildren().add(fileBubble);

		VBox messageBox = new VBox(2);
		if (!isSend && !userSender.getUserId().equals(lastSenderId)) {
			Label nameLabel = new Label(userSender.getUsername());
			nameLabel.setStyle("-fx-font-weight: bold; -fx-padding: 0 0 3 5;");
			messageBox.getChildren().addAll(nameLabel, hBox);
		} else {
			messageBox.getChildren().add(hBox);
		}

		Platform.runLater(() -> container.getChildren().add(messageBox));
		lastSenderId = userSender.getUserId();

	}

	@FXML
	void sendMessage(KeyEvent event) throws IOException {
		if (event.getCode().toString().equals("ENTER")) {
			onSend(messageText.getText(), "all");
		}
	}

	public void renderLocalFileMessage(File file) throws IOException {

		FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/component/FileBubble.fxml"));
		Node fileBubble = loader.load();

		FileRenderMessage controller = loader.getController();
		controller.setFileInfoLocal(file); // truyền dữ liệu vào controller

		HBox hBox = new HBox(fileBubble);
		hBox.setAlignment(Pos.CENTER_RIGHT);
		hBox.setPadding(new Insets(5, 5, 5, 10));

		VBox messageBox = new VBox(hBox);
		container.getChildren().add(messageBox);
	}

	@FXML
	void openFileChoosen(MouseEvent event) throws IOException {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Chọn file để gửi");

		// Tùy chọn định dạng file
		fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Tất cả file", "*.*"),
				new FileChooser.ExtensionFilter("Ảnh", "*.png", "*.jpg", "*.jpeg"),
				new FileChooser.ExtensionFilter("Tài liệu", "*.pdf", "*.docx", "*.txt"));

		// Mở hộp thoại
		File selectedFile = fileChooser.showOpenDialog(((Node) event.getSource()).getScene().getWindow());

		if (selectedFile != null) {
			long maxSize = 10 * 1024 * 1024; // 10MB

			if (selectedFile.length() > maxSize) {
				commonController.alertInfo(AlertType.WARNING, "File quá lớn!!!!", "Vui lòng chọn file nhỏ hơn 10MB.");
				return;
			}

			System.out.println("File hợp lệ: " + selectedFile.getAbsolutePath());

			renderLocalFileMessage(selectedFile);

			socketClient.sendFile(selectedFile, user, "all");

		} else {
			commonController.alertInfo(AlertType.INFORMATION, "Không có file nào được chọn!!!!", "Vui lòng chọn file.");
			System.out.println("Không có file nào được chọn.");
		}
	}

	@Override
	public void initialize(URL arg0, ResourceBundle arg1) {

		// always scroll to end
		container.heightProperty().addListener((obs, oldVal, newVal) -> {
			scrollPane.setVvalue(1.0);
		});
		scrollPane.setFitToWidth(true);

		commonController = new CommonController();

		socketClient = SocketClient.getInstance();
		gson = socketClient.gson;
		socketClient.setMessageHandler(chatMessage -> {
			try {
				onSendAndReceiveMessenge(chatMessage, false);
			} catch (MalformedURLException e) {
				e.printStackTrace();
				System.out.println("Can't receive message");
			}
		});

		socketClient.setFileMessageHandler(chatMessage -> {
			try {
				renderFileMessage(chatMessage, false);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			System.out.println("File :" + chatMessage);
		});

	}

}
