package tw.com.codedata.jersey;

import javax.ws.rs.ApplicationPath;

import org.glassfish.jersey.server.ResourceConfig;

import socket_service.SocketServer;

@ApplicationPath("helloRestful")
public class MyApplication extends ResourceConfig {
	//
	public MyApplication() {
		packages("tw.com.codedata.jersey");

		(new SocketServer()).start();

		Utils.printUserMenu();

		register(CustomLoggingFilter.class);
	}
}