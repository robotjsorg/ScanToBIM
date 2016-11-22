import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * The class extends the Thread class so we can receive and send messages at the same time
 */
public class TCPServer extends Thread {

	public static final int SERVERPORT = 4444;
	private static TCPServer mServer;

	public TCPServer(){}

	public static void main(String[] args) {
		mServer = new TCPServer();
		mServer.start();
	}
	

	@Override
	public void run() {
		super.run();
		try {
			System.out.println("S: Connecting...");

			//create a server socket. A server socket waits for requests to come in over the network.
			ServerSocket serverSocket = new ServerSocket(SERVERPORT);

			//create client socket... the method accept() listens for a connection to be made to this socket and accepts it.
			Socket client = serverSocket.accept();
			System.out.println("S: Connected.");

			try {
				InputStream inputStream = client.getInputStream();
				BufferedInputStream bis = new BufferedInputStream(inputStream);
				DataInputStream dis = new DataInputStream(bis);
				int filesCount = dis.readInt();
				File[] files = new File[filesCount];

				System.out.println("S: Receiving...");
				for(int i = 0; i < filesCount; i++)
				{
				    long fileLength = dis.readLong();
				    String fileName = dis.readUTF();

				    files[i] = new File("/home/suriya/Scans/" + fileName);

				    FileOutputStream fos = new FileOutputStream(files[i]);
				    BufferedOutputStream bos = new BufferedOutputStream(fos);

				    for(int j = 0; j < fileLength; j++) bos.write(bis.read());

				    bos.close();
				}

				dis.close();
		        inputStream.close();
		        client.close();
		        serverSocket.close();

		        System.out.println("S: Received the file(s)");
			} catch (Exception e) {
				System.out.println("S: Error");
				e.printStackTrace();
			} finally {
				System.out.println("S: Done.");
			}
		} catch (Exception e) {
			System.out.println("S: Error");
			e.printStackTrace();
		}
	}
}
