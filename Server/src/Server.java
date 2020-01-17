import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

//ARTUR MARCINIAK
//MATEUSZ TOMCZYK
//DAWID WYCIS£O

public class Server {

	private static ArrayList<ConnectionHandler> Threads = new ArrayList();
	private static Map<Integer, String> Users = new HashMap();

	public static void main(String[] args) {
		ServerSocket server_socket;
		InetSocketAddress address;
		Socket socket;
		int port = 6667;
		int backlog = 1;
		int max_clients = 4;
		int client_number = 0;
		
		try {
			server_socket = new ServerSocket();
			address = new InetSocketAddress(port);
			server_socket.bind(address);
			
			//pêtla zapewniaj¹ca limit ilosci u¿ytkowników
			while(true) {
				if((Threads.size() < max_clients) && !server_socket.isClosed())	{
					//jesli jest miejsce, to powo³ywany jest nowy w¹tek klienta
					socket = server_socket.accept();
					++client_number;
					ConnectionHandler client = new ConnectionHandler(socket, client_number);
					Threads.add(client);
					client.start();
					System.out.println("klient " + client_number +  " sie zglosil");
				}
				else if(!server_socket.isClosed()){
					//jeœli nie ma miejsca, to serversocket jest zamykany
					server_socket.close();
				}
				else if(server_socket.isClosed() && !(Threads.size() < max_clients)) {
					//jeœli miejsce znów siê pojawi³o, to serversocket jest otwierany
					server_socket = new ServerSocket();
					server_socket.bind(address);
				}
			}
		} catch (Exception e ) {
			System.err.println(e.getMessage());
			e.printStackTrace();
		}
	}

	//usuwanie w¹tku klienta
	public static void eraseThread(int n) {
		for(ConnectionHandler x : Threads)
		{
			if (x.get_number() == n)
			{
				Threads.remove(x); //usuwanie z tablicy w¹tków
				Users.remove(n); //usuwanie z mapy u¿ytkowników
				break;
			}
		}
	}
	
	//lista u¿ytkowników
	public static String listUsers() {
		String list = new String();
		for(Map.Entry<Integer, String> pair : Users.entrySet())
			list += "<" + pair.getValue() + ">, ";
		return list.substring(0, list.length() - 2);
	}
	
	//sprawdzenie, czy u¿ytkownik o danym pseudonimie istnieje
	public static Boolean isUser(String name) {
		for (Map.Entry<Integer, String> pair : Users.entrySet()) {
		    if (pair.getValue().equals(name))
		    	return true;
		}
		return false;
	}
	
	//dodanie u¿ytkownika
	public static void addUser(Integer n, String name) {
		Users.put(n, name);
	}
	
	//pobranie nazwy u¿ytkownika z uzyciem jego numeru
	public static String getUsername(Integer n) {
		for (Map.Entry<Integer, String> pair : Users.entrySet())
		    if (pair.getKey().equals(n))
		    	return pair.getValue().toString();		    
		return null;
	}
	
	//pobranie numeru u¿ytkownika z u¿yciem jego nazwy
	public static int getUserNumber(String name) {
		for (Map.Entry<Integer, String> pair : Users.entrySet())
		    if (pair.getValue().equals(name))
		    	return pair.getKey();
		return -1;
	}
	
	//pobranie numeru administratora
	public static int getAdminNumber() {
		for(ConnectionHandler x : Threads)
		{
			if (x.checkIfAdmin())
			{
				return x.get_number();
			}
		}
		return -1;
	}
	
	//zmiana pseudonimu
	public static Boolean changeNickname(Integer n, String new_name) {
		if(!isUser(new_name)) {
			Users.put(n, new_name);
			return true;
		}else
			return false;
	}
	
	//pobranie listy w¹tków
	public static ArrayList getThreads() {
		return Threads;
	}
	
	//wyrzucenie klienta
	public static Boolean kickClient(String name) {
		for (Map.Entry<Integer, String> pair : Users.entrySet())
		    if (pair.getValue().equals(name)) {
		    	eraseThread(pair.getKey());
		    	return true;
		    }
		return false;
	}
}
