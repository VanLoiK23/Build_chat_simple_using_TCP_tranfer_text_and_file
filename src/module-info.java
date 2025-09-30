module HuynhVanLoi_23IT.EB056_4{
	requires javafx.controls;
	requires javafx.fxml;
	requires com.google.gson;
	requires java.persistence;
	requires lombok;
	requires cloudinary.core;
	requires org.apache.httpcomponents.httpclient;
	requires org.apache.httpcomponents.httpcore;
	requires commons.logging;
	requires org.apache.httpcomponents.httpmime;
	requires javafx.graphics;
	requires org.apache.commons.pool2;
	requires org.slf4j;
	requires fontawesomefx;
	requires java.desktop;
	requires gson.javatime.serialisers;

	opens application to javafx.graphics, javafx.fxml;
	opens model to com.google.gson;

	opens controller to javafx.fxml;
	opens controller.render to javafx.fxml; 

	exports model;
	exports controller;
	exports controller.Common;
	exports controller.render;
}
