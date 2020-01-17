import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

//ARTUR MARCINIAK
//MATEUSZ TOMCZYK
//DAWID WYCIS�O

public class Client {
	
	private static DataInputStream in;
	private static InputStream in_sock;
	private static BufferedInputStream in_buf;
	
	private static DataOutputStream out;
	private static OutputStream out_sock;
	private static BufferedOutputStream out_buf;
	

	public static void main(String[] args) {
		Socket socket;
		InetSocketAddress address;
		String server = "localhost";
		int port = 6667;
		int timeout = 10000;	
		try {
			//socket, po��czenie z serverem
			socket = new Socket();
			address = new InetSocketAddress(server, port);
			socket.connect(address, timeout);
			System.out.println("Connected to server!");

			try {
				in_sock = socket.getInputStream();				
				in = new DataInputStream(in_sock);
				out_sock = socket.getOutputStream();
				out = new DataOutputStream(out_sock);
				
				//uruchomienie w�tku obs�uguj�cego wysy�anie wiadomo�ci
				SendHandler snd = new SendHandler(out);
				snd.start();
				
				String v;
				
				//odbieranie i wy�wietlanie wiadomo�ci, dop�ki nie otrzyma komendy wyj�cia
				while(true) {
					v = in.readUTF();

					//je�li klient zostanie wyrzucony lub wpisze komend� /quit,
					//to otrzymuje od serwera wiadomo�� /q inicjuj�c� koniec
					//wykonywania w�tk�w
					if(v.equals("/q")) {
						snd.setQuit();
						snd.join();	//oczekiwanie na zako�czenie w�tku obs�uguj�cego wysy�anie wiadomo�ci
						socket.close();
						break;
					}
					System.out.println(v);
				}
			} catch (IOException e) {
				System.err.println(e.getMessage());
				e.printStackTrace();
			}
			
			
		} catch(Exception e) {
			System.err.println(e.getMessage());
			e.printStackTrace();
		}
		
	}

}
