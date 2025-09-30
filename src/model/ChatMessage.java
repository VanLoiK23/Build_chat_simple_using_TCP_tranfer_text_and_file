package model;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class ChatMessage {

	private String id;
	private User sender;
	//chưa cần vì đag broadcast
//	private String receiverId; // hoặc roomId nếu chat nhóm
	private String content;
	private String type; // "text", "image", "file", "sticker", "call"
	private LocalDateTime timestamp;
	private boolean isRead;

}