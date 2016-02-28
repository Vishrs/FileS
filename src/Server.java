import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Date;

public class Server {
	private static final int listener_port = 8765;
	private static final int n_blocks = 128;
	private static final int blocksize = 4096;
	private static final String dirname = ".storage";
	private static final String pathname = ".storage/";
	private static DiskSpace memory;
	public Server() {
		System.out.println( "MultiThreadServer started at " + new Date() );
		memory = new DiskSpace(n_blocks, blocksize, n_blocks);
		System.out.println("Block size is " + blocksize);
		System.out.println("Number of blocks is " + n_blocks);
		System.out.println("Listening on port " + listener_port);
		try {
			ServerSocket serverSocket = new ServerSocket(listener_port);
			
			while (true) {
				Socket sock = serverSocket.accept();
				/*--At this point a client has connected--*/
				//Find clients' host name and IP address
				InetAddress clientIPaddress = sock.getInetAddress();
				System.out.println("Received incoming connection from : " + clientIPaddress.getHostAddress());
				
				/*--Create a new server thread for each client--*/
				HandleClient client = new HandleClient(sock);
				client.start();
			}
		} catch(IOException ex) {
			System.out.println("Exception encountered on accept()");
			ex.printStackTrace();
		}
	}
	
	class HandleClient extends Thread {
		private Socket socket;
		
		public HandleClient(Socket socket) {
			this.socket = socket;
		}
		
		public void run() {
			try {
				DataInputStream inputFromClient = 
						new DataInputStream(socket.getInputStream());
				DataOutputStream outputtoClient = 
						new DataOutputStream(socket.getOutputStream());
				File dir = new File(dirname);
				if (!dir.exists()) {
					if (!dir.mkdir()) {
						System.out.println("failed to create directory");
					}
				} else {
					for(File f : dir.listFiles())
						f.delete();
				}
				while (true) {
					byte[] buffer = new byte[n_blocks * blocksize];
					int bytes = inputFromClient.read(buffer);
					//inputFromClient.read();
					String decoded = new String(buffer, "utf-8");
					String input = decoded.substring(0, bytes - 1);
					//System.out.println(input);
					String[] inputtokens;
					if (!input.equals(""))
						inputtokens = input.split(" ");
					else continue;
					
					printThreadID();
					System.out.println("Rcvd: " + input);
					String command = inputtokens[0];
					switch (command) {
						case "STORE":
							if (inputtokens.length != 3) {
								outputtoClient.writeBytes("[thread " + Thread.currentThread().getId() + "] "
										+ " ERROR: INVALID ARGUMENTS\n");
								outputtoClient.writeBytes("usage: STORE <filename> <bytes\n<file-contents>\n");
							} else {
								String filename = inputtokens[1];
								int bytesz = Integer.parseInt(inputtokens[2]);
								byte[] contents = new byte[bytesz];
								inputFromClient.read(contents);
								
								store(outputtoClient, filename, contents, bytesz);
							}
							continue;
						case "READ":
							if (inputtokens.length != 4) {
								outputtoClient.writeBytes("[thread " + Thread.currentThread().getId() + "] "
										+ " ERROR: INVALID ARGUMENTS\n");
								outputtoClient.writeBytes("usage: READ <filename> <byte-offset <length>\n");
							} else {
								String filename = inputtokens[1];
								int offset = Integer.parseInt(inputtokens[2]);
								int len = Integer.parseInt(inputtokens[3]);
								read(outputtoClient, filename, offset, len);
							}
							continue;
						case "DELETE": 
							if (inputtokens.length != 2) {
								outputtoClient.writeBytes("[thread " + Thread.currentThread().getId() + "] "
										+ " ERROR: INVALID ARGUMENTS\n");
								outputtoClient.writeBytes("usage: DELETE <filename>\n");
							} else {
								String filename = inputtokens[1]; 
								delete(outputtoClient, filename);
							}
							continue;
						case "DIR": 
							if (inputtokens.length != 1) {
								outputtoClient.writeBytes("[thread " + Thread.currentThread().getId() + "] "
										+ " ERROR: INVALID ARGUMENTS\n");
								outputtoClient.writeBytes("usage: DIR\n");
							} else {
								dir(outputtoClient);
							}
							continue;
						default:
							outputtoClient.writeBytes("[thread " + Thread.currentThread().getId() + "] "
									+ " ERROR: INVALID ARGUMENTS\n");
							continue;
					}
				}
				
			} catch(IOException ex) {
				System.out.println("Error encountered");
				ex.printStackTrace();
			}
		}
		
		public void store(DataOutputStream client, String filename, byte[] data, int bytesize) throws IOException {
			int n_clusters = 0;
			int b = (int)((double)(bytesize + blocksize - 1)/blocksize);
			if (memory.containsFile(filename)) {
				printThreadID();
				try {
					client.writeChars("[thread " + Thread.currentThread().getId() + "] " + "ERROR: FILE EXISTS\n");
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				System.out.println("Sent: ERROR: FILE EXISTS");
			} else if ((n_clusters = memory.allocateFile(filename, bytesize, b)) == -1) {
				printThreadID();
				try {
					client.writeChars("[thread " + Thread.currentThread().getId() + "] " + "ERROR: INSUFFICIENT DISK"
							+ " SPACE\n");
					return;
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				System.out.println("Sent: ERROR: INSUFFICIENT DISK SPACE");
			}
			try {
				FileOutputStream fileToStore = new FileOutputStream(pathname + filename);
				fileToStore.write(data);
				fileToStore.close();
				printThreadID();
				System.out.print("Stored file '" + memory.fileToChar(filename) + 
						"' (" + bytesize + " bytes; " + b);
				if (b == 1)
					System.out.print(" block; ");
				else System.out.print(" blocks; ");
				if (n_clusters == 1)
					System.out.println(n_clusters + " cluster)");
				else System.out.println(n_clusters + " clusters)");
				
				memory.printDiskSpaceMemory();
				client.writeChars("[thread " + Thread.currentThread().getId() + "] " + "ACK\n");
				printThreadID();
				System.out.println("Sent: ACK");
				
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				client.writeChars("[thread " + Thread.currentThread().getId() + "] ERROR: FAILED TO CREATE FILE\n");
				printThreadID();
				System.out.println("Sent: ERROR: FAILED TO CREATE FILE");
				e.printStackTrace();
			}
				
			
			
		}
		
		public void read(DataOutputStream client, String filename, int offset, int length) throws IOException {
			if (!memory.containsFile(filename)) {
				client.writeChars("[thread " + Thread.currentThread().getId() + "] " + "ERROR: NO SUCH FILE\n");
				printThreadID();
				System.out.println("sent: ERROR: NO SUCH FILE");
				return;
			}
			if (offset + length > memory.getSizeofFile(filename)) {
				client.writeChars("[thread " + Thread.currentThread().getId() + "] " + 
						"ERROR: INVALID BYTE RANGE\n");
				printThreadID();
				System.out.println("Sent: ERROR: INVALID BYTE RANGE");
				return;
			}
			try {
				FileInputStream file = new FileInputStream(pathname + filename);
				int byt = (int)memory.getSizeofFile(filename);
				byte[] buffer = new byte[byt];
				int filecontents = file.read(buffer, offset, length);
				client.writeChars("[thread " + Thread.currentThread().getId() + "] " + "ACK " + 
						+ filecontents + "\n"/* + buffer + "\n"*/);
				client.write(buffer);
				client.writeChars("\n");
				printThreadID();
				System.out.println("Sent: ACK " + filecontents);
				int bb = 0;
				if (length % 1024 == 0) {
					bb = length/1024;
				} else bb = length/1024 + 1;
				printThreadID();
				System.out.print("Sent " + length + " bytes (from " + bb + " '" + memory.fileToChar(filename)
				+ "' ");
				if (bb == 1)
					System.out.println("block) from offset " + offset);
				else System.out.println("blocks) from offset " + offset);
				file.close();
				
			} catch (FileNotFoundException e) {
				client.writeChars("[thread " + Thread.currentThread().getId() + "] " + "ERROR: NO SUCH FILE\n");
				printThreadID();
				System.out.println("sent: ERROR: NO SUCH FILE");
				e.printStackTrace();
			}

		}
		
		public void delete(DataOutputStream client, String filename) throws IOException {
			printThreadID();
			System.out.println("Rcvd: DELETE " + filename);
			if (!memory.containsFile(filename)) {
				client.writeChars("[thread " + Thread.currentThread().getId() + "] " + "ERROR: NO SUCH FILE\n");
				printThreadID();
				System.out.println("Sent: ERROR: NO SUCH FILE");
				return;
			}
			int dealloc_b = 0;
			char c = memory.fileToChar(filename);
			if((dealloc_b = memory.deallocateFile(filename)) != -1) {
				File f = new File(pathname + filename);
				f.delete();
				//Files.delete(Paths.get(pathname + filename));
				printThreadID();
				System.out.println("Deleted " + filename + " file '" + c + 
						"' (deallocated " + dealloc_b + " blocks");
				memory.printDiskSpaceMemory();
				client.writeChars("[thread " + Thread.currentThread().getId() + "] " + "ACK\n");
				printThreadID();
				System.out.println("Sent: ACK");
			}
			
		}
		
		public void dir(DataOutputStream client) throws IOException {
			printThreadID();
			System.out.println("Rcvd: DIR");
			memory.printDir(client);
			printThreadID();
			System.out.println("Sent: directory");
		}
		
		public void printThreadID() {
			System.out.print("[thread " + Thread.currentThread().getId() + "] ");
		}
	}
	public static void main(String[] args) {
		new Server();

	}

}
