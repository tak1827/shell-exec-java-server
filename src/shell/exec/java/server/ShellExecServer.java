package shell.exec.java.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;

public class ShellExecServer {

	public static final int PORT = 10007;
	public static final String PID = ManagementFactory.getRuntimeMXBean()
			.getName().split("@")[0];

	public static void main(String[] args) {
		ServerSocket serverSocket = null;
		Socket socket = null;

		try {
			// Create Server
			serverSocket = new ServerSocket(PORT);
			System.out.println("CmdServer started (port="
					+ serverSocket.getLocalPort() + ")");

			// Don't stop server, until killed the PID
			while (true) {
				// Accept connection
				socket = serverSocket.accept();
				System.out.println("Accepted connection");
				// Execute command in a thread
				new CmdExecThread(socket).start();
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (serverSocket != null) {
					serverSocket.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}

class CmdExecThread extends Thread {
	private Socket socket;

	public CmdExecThread(Socket socket) {
		this.socket = socket;
	}

	@Override
	public void run() {
		StringBuffer output = new StringBuffer();
		String line = "";

		try {

			// Read http request
			BufferedReader request = new BufferedReader(new InputStreamReader(
					socket.getInputStream()));
			line = request.readLine();
			// Remove useless part
			line = line.replaceAll("GET /", "");
			line = line.replaceAll(" HTTP/1.1", "");
			// Decode url to utf-8
			String cmdLine = URLDecoder.decode(line,"utf-8");
			System.out.println(cmdLine);
			try {
				Process p = Runtime.getRuntime().exec(cmdLine);
				// Wait for finish
				p.waitFor();
				// Read comand output
				BufferedReader stdInput = new BufferedReader(
						new InputStreamReader(p.getInputStream()));
				BufferedReader stdError = new BufferedReader(
						new InputStreamReader(p.getErrorStream()));
				// Write output
				line = "";
				while ((line = stdInput.readLine()) != null) {
					output.append(line + "<br>");
				}
				while ((line = stdError.readLine()) != null) {
					output.append(line + "<br>");
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			// Create http response
			PrintWriter response = new PrintWriter(
					socket.getOutputStream(), true);
			response.write("HTTP/1.1 200 OK\r\n");
			response.write("Content-Type: text/html\r\n");
			response.write("\r\n");
			response.write("<html><body>");
			response.write("PID:" + ShellExecServer.PID + "<br><br>");
			response.write(output.toString());
			response.write("</body</html>");
			response.flush();
			
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (socket != null) {
					socket.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
