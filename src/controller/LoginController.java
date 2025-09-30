package controller;

import java.io.IOException;
import java.util.UUID;

import controller.Common.CommonController;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import model.User;

public class LoginController {

	@FXML
	private TextField nameTextField;
	
	private CommonController commonController=new CommonController();

	@FXML
	void clickAttend(MouseEvent event) throws IOException {
		if (nameTextField.getText() != null && !nameTextField.getText().isEmpty()) {
			//generate ramdom id user
			UUID userUuid=UUID.randomUUID();
			
			User user=new User();
			user.setUserId(userUuid.toString());
			user.setUsername(nameTextField.getText());
			ChatController chatController = commonController.loaderToResource(event, "Chat/client").getController();
			chatController.setUser(user);
		}

	}

}
