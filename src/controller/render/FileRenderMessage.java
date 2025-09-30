package controller.render;

import java.awt.Desktop;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcons;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import javafx.stage.DirectoryChooser;
import model.FileInfo;

public class FileRenderMessage {
	@FXML
	private ProgressIndicator progressCircle;
	
	@FXML
	private FontAwesomeIcon checkIcon;

	@FXML
	private FontAwesomeIcon downloadIcon;

	@FXML
	private StackPane downloadStatus;

	@FXML
    private Label nameText;

	private FileInfo fileInfo;

	@FXML
	private AnchorPane fileFormat;

	public void setFileInfo(FileInfo info) {
		this.fileInfo = info;

		if (nameText != null) {
			nameText.setText(info.getFileName());
			nameText.setMaxWidth(90);
			nameText.setEllipsisString("..."); 
			nameText.setTextOverrun(OverrunStyle.ELLIPSIS);

		}

		if (downloadStatus != null) {
			downloadStatus.setOnMouseClicked(e -> {
				DirectoryChooser chooser = new DirectoryChooser();
				chooser.setTitle("Chọn thư mục lưu file");
				File folder = chooser.showDialog(downloadStatus.getScene().getWindow());

				if (folder != null) {
					startDownload(info.getUrlUpload(), info.getFileName(), folder);
				}
			});
		}
	}

	public void setFileInfoLocal(File file) {
		downloadIcon.setIcon(FontAwesomeIcons.FOLDER_OPEN);

		if (nameText != null) {
			nameText.setText(file.getName());
			nameText.setMaxWidth(90);
			nameText.setEllipsisString("..."); 
			nameText.setTextOverrun(OverrunStyle.ELLIPSIS);
		}

		if (downloadStatus != null) {
			downloadStatus.setOnMouseClicked(e -> {
				try {
					Desktop.getDesktop().open(file); // Mở file bằng app mặc định
				} catch (IOException ex) {
					ex.printStackTrace();
				}
			});
		}
	}

	public void startDownload(String url, String filename, File targetFolder) {
		progressCircle.setVisible(true);
		downloadIcon.setVisible(false);
		checkIcon.setVisible(false);

		Task<Void> downloadTask = new Task<>() {
			@Override
			protected Void call() throws Exception {
				URL website = new URL(url);
				URLConnection connection = website.openConnection();
				int fileSize = connection.getContentLength();

				try (InputStream in = website.openStream();
						FileOutputStream fos = new FileOutputStream(new File(targetFolder, filename))) {

					byte[] buffer = new byte[4096];
					int bytesRead;
					int totalRead = 0;

					while ((bytesRead = in.read(buffer)) != -1) {
						fos.write(buffer, 0, bytesRead);
						totalRead += bytesRead;
						updateProgress(totalRead, fileSize);
					}
				}

				return null;
			}
		};

		progressCircle.progressProperty().bind(downloadTask.progressProperty());

		downloadTask.setOnSucceeded(ev -> {
			progressCircle.setVisible(false);
			checkIcon.setVisible(true);
			try {
				Desktop.getDesktop().open(new File(targetFolder, filename));
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		});

		downloadTask.setOnFailed(ev -> {
			progressCircle.setVisible(false);
			downloadIcon.setVisible(true);
		});

		new Thread(downloadTask).start();
	}

}
