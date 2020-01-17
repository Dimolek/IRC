import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

//ARTUR MARCINIAK
//MATEUSZ TOMCZYK
//DAWID WYCIS£O

public class ConnectionHandler extends java.lang.Thread {
	private Socket socket;
	private int number;
	private Boolean isAdmin;
	private Boolean isAway;
	private static Boolean isQuit;
	
	//Sending data
	private DataOutputStream out;
	private OutputStream out_sock;
	
	//Receiving data
	private DataInputStream in;
	private InputStream in_sock;
	
	public ConnectionHandler(Socket s, int n) {
		socket = s;
		number = n;
		isAway = false;
		isQuit = false;
		isAdmin = false;
		//ustawienie administratora, je¿eli nie ma ¿adnego na serwerze
		if(Server.getAdminNumber() == -1)
			isAdmin = true;
	}
	
	public void run() {
		
		try {
			out_sock = socket.getOutputStream();
			out = new DataOutputStream(out_sock);
			
			in_sock = socket.getInputStream();
			in = new DataInputStream(in_sock);			
		} catch (IOException e) {
			System.err.println(e.getMessage());
			e.printStackTrace();
		}
		
		//pobranie pseudonimu od u¿ytkownika i jego weryfikacja
		sendData("<server> What's your nickname?");
		String msg = null;
		while(true) {
			msg = receiveData();
			msg = msg.substring(2);
			if(msg == null)
				sendData("<server> Nickname cannot be empty");
			else if(msg.length() < 3 || msg.indexOf(' ') != -1)
				sendData("<server> Nickname must be longer than 2 characters and/or cannot have spaces");
			else if(Server.isUser(msg))
				sendData("<server> Nickname is already taken");
			else
				break;			
		}
		
		//jeœli pseudonim jest poprawny, klient zostaje dodany do listy
		Server.addUser(number, msg);
		sendData("<server> Hello " + msg);
		broadcast(msg + " has joined the chat");
		
		//poinformowanie u¿ytkownika jeœli zosta³ administratorem
		if(checkIfAdmin())
			sendData("<server> You have been promoted to Admin");
		
		//dekodowanie komend wys³anych przez u¿ytkownika, oraz rozsy³anie jego wiadomoœci
		//wiadomoœci wysy³ane przez u¿ytkownika rozpoczynaj¹ siê zawsze od "um",
		//innej wiadomoœci u¿ywamy aby zakoñczyæ w¹tek obs³uguj¹cy klienta
		while(!isQuit) {
			msg = receiveData();
			if(!msg.startsWith("um")) {
				System.out.println("Klient z ID " + number + " wyszedl");
				//	closeSocket();
					try {
						socket.close();
						quit();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
			} else {
				msg = msg.substring(2);
				if(msg.startsWith("/")) {
					String command = decodeCommand(msg.substring(1));
					String cmd = command.substring(0, 1);
					
					switch (cmd) {
					case "0":
						sendData("<server> Wrong command");
						break;
						
					case "1":
						String to = command.substring(1, command.indexOf(' '));
						String text = command.substring(to.length() + 2);
						if(!privateMsg(to, text, Server.getUsername(number)))
							sendData("<server> Private message error");			
						sendData("Priv to <" + to + "> :" + text);
						break;
						
					case "2":
						String new_nickname = command.substring(1);
						if(Server.changeNickname(number, new_nickname))
							sendData("<server> Your nickname changed successfully to: " + new_nickname);
						else
							sendData("<server> Nickname already taken");					
						break;
						
					case "3":
						sendData("<server> Bye Bye <" + Server.getUsername(number) + ">");
						broadcast("<" + Server.getUsername(number) + "> has left the chat", number);
						sendData("/q", number);
						Server.eraseThread(number);
						//quit();
						break;
						
					case "4":					
						sendData("<server> List of users: " + Server.listUsers());
						break;
						
					case "5":
						if(checkIfAdmin()) {
							String kick_nickname = command.substring(1);
							if(Server.isUser(kick_nickname)) {
								sendData("<server> You have been kicked", Server.getUserNumber(kick_nickname));
								sendData("/q", Server.getUserNumber(kick_nickname));
								broadcast("<" + kick_nickname + "> has been kicked", Server.getUserNumber(kick_nickname));						
								Server.kickClient(kick_nickname);
							}
						} else {
							sendData("<server> You have to be Admin to use kick command", number);
						}
						
						break;
						
					case "6":
						if(checkIfAdmin()) {
							String kick_nickname_wr = command.substring(1, command.indexOf(' '));
							String reason = command.substring(kick_nickname_wr.length() + 2);
							if(Server.isUser(kick_nickname_wr)) {
								sendData("<server> You have been kicked for: " + reason , Server.getUserNumber(kick_nickname_wr));
								sendData("/q", Server.getUserNumber(kick_nickname_wr));
								broadcast("<" + kick_nickname_wr + "> has been kicked for: " + reason, Server.getUserNumber(kick_nickname_wr));						
								Server.kickClient(kick_nickname_wr);
							}
						} else {
							sendData("<server> You have to be Admin to use kick command", number);
						}
						break;
						
					case "7":
						isAway = false;
						break;
						
					case "8":
						isAway = true; //przy GUI dodac ze ma sie gdzies status wyswietlac
						break;
						
					default:
						sendData("<server> Wrong command");
					}
				}
				else
					broadcast(Server.getUsername(number), msg);
			}
		}
	}

	
	//wysy³anie wiadomoœci do klienta, który obs³ugiwany jest przez w¹tek
	private void sendData(String data) {
		try {
			out.writeUTF(getTime() + data);
			out.flush();
		} catch (IOException e) {
			System.err.println(e.getMessage());
			e.printStackTrace();
		}
	}
	
	//wys³anie wiadomoœci do innego klienta
	private void sendData(String data, int n) {
		ArrayList<ConnectionHandler> thread_list = Server.getThreads();
		String user = Server.getUsername(n);
		
		for(ConnectionHandler x : thread_list)
		{
			OutputStream temp;

			if(x.get_number() == n) {
				try {
					temp = x.socket.getOutputStream();
					DataOutputStream temp2 = new DataOutputStream(temp);
					temp2.writeUTF(data);
					temp2.flush();
					break;
				} catch (IOException e) {
					System.err.println(e.getMessage());
					e.printStackTrace();
				}
			}
		}
	}
	
	//odbieranie danych od servera
	private String receiveData() {
		String v = null;
		try {
			v = in.readUTF();
			System.out.println(v);			
		} catch (IOException e) {
			System.err.println(e.getMessage());
			e.printStackTrace();
		}
		return v;
	}
	
	//pobranie numeru klienta
	public int get_number() {
		return number;
	}
	
	//sprawdzenie, czy u¿ytkownik jest w trybie 'zaraz wracam'
	public Boolean isUserAway() {
		return isAway;
	}
	
	//rozes³anie wiadomoœci do wszystkich klientów oprócz siebie
	public void broadcast(String msg) {
		ArrayList<ConnectionHandler> thread_list = Server.getThreads();
		
		for(ConnectionHandler x : thread_list)
		{
			OutputStream temp;
			if (x.get_number() != number)
			{
				if(!x.isUserAway()) {
					try {
						temp = x.socket.getOutputStream();
						DataOutputStream temp2 = new DataOutputStream(temp);
						temp2.writeUTF(getTime() + "<server> " + msg);
						temp2.flush();
					} catch (IOException e) {
						System.err.println(e.getMessage());
						e.printStackTrace();
					}
				}
			}
		}
	}
	
	//rozes³anie wiadomoœci do wszystkich klientów od danego u¿ytkownika
	public void broadcast(String user, String msg) {
		ArrayList<ConnectionHandler> thread_list = Server.getThreads();

		for(ConnectionHandler x : thread_list)
		{
			OutputStream temp;

			if(!x.isUserAway()) {
				try {
					temp = x.socket.getOutputStream();
					DataOutputStream temp2 = new DataOutputStream(temp);
					temp2.writeUTF(getTime() + "<" + user + "> " + msg);
					temp2.flush();
				} catch (IOException e) {
					System.err.println(e.getMessage());
					e.printStackTrace();
				}
			}
		}
	}
	
	//wysy³anie do wszystkich oprócz konkretnego u¿ytkownika
	public void broadcast(String msg, int n) {
		ArrayList<ConnectionHandler> thread_list = Server.getThreads();
		
		for(ConnectionHandler x : thread_list)
		{
			OutputStream temp;
			if (x.get_number() != n)
			{
				if(!x.isUserAway()) {
					try {
						temp = x.socket.getOutputStream();
						DataOutputStream temp2 = new DataOutputStream(temp);
						temp2.writeUTF(getTime() + "<server> " + msg);
						temp2.flush();
					} catch (IOException e) {
						System.err.println(e.getMessage());
						e.printStackTrace();
					}
				}
			}
		}
	}
	
	//wysy³anie wiadomoœci prywatnej
	public static Boolean privateMsg(String name, String msg, String sender) {
		ArrayList<ConnectionHandler> thread_list = Server.getThreads();
		int user_id = Server.getUserNumber(name);
		if (user_id == -1) //jesli nie ma takiego uzytkownika
			return false;
		
		for(ConnectionHandler x : thread_list)
		{
			OutputStream temp;
			if (user_id == -1)
				return false;
			if (x.get_number() == user_id)
			{
				if(!x.isUserAway()) {
					try {
						temp = x.socket.getOutputStream();
						//BufferedOutputStream temp2 = new BufferedOutputStream(temp);
						DataOutputStream temp2 = new DataOutputStream(temp);
						temp2.writeUTF(getTime() + "Priv: <" + sender + "> " + msg);
						temp2.flush();
						return true;
					} catch (IOException e) {
						System.err.println(e.getMessage());
						e.printStackTrace();
					}
				}
			}
		}		
		return false;
	}
	
	
	//pobranie aktualnego czasu
	public static String getTime() {
		java.util.Date date = new java.util.Date();
		SimpleDateFormat sdf = new SimpleDateFormat("[HH:mm:ss] ");
		return sdf.format(date);
	}
	
	//dekodowanie i walidacja komend klientów
	public static String decodeCommand(String cmd) {
		int len = cmd.length();
		if (len < 3)
			return "0";
		
		String cmd3 = cmd.substring(0, 3);
		if (cmd3.toUpperCase().equals("MSG")) { //pierwsze trzy litery msg
			if (len == 3) //tylko msg
				return "0";
			else if (len == 4) //msg spacja
				return "0";
			else if (len > 4) {
				if (!cmd.substring(3, 4).equals(" ")) //4 znak to musi byc spacja
					return "0";
				if (cmd.substring(4).length() < 5 || cmd.substring(4).indexOf(' ') == -1 ) //po spacji conajmniej 3literowy nick i spacja po nicku
					return "0";
				if (cmd.substring(4, 7).indexOf(' ') != -1) //jezeli w nicku jest spacja
					return "0";
				if (cmd.substring(7).indexOf(' ') == -1) // brak wiadomosci po nicku
					return "0";
				String nick = cmd.substring(4);
				nick = nick.substring(0, nick.indexOf(' '));
				String msg = cmd.substring(nick.length() + 5);
				return "1" + nick + " " + msg;
			}
			else
				return "0";			
		}
		if (len < 4)
			return "0";
		String cmd4 = cmd.substring(0, 4);
		
		if (cmd4.toUpperCase().equals("NICK")) { //pierwsze cztery litery nick
			if (len == 4) //tylko nick
				return "0";
			else if (len == 5) //nick spacja
				return "0";
			else if (len > 5) {
				if (!cmd.substring(4, 5).equals(" ")) //5 znak to musi byc spacja
					return "0";
				if (cmd.substring(5).length() < 3) //po spacji conajmniej 3literowy nick
					return "0";
				if (cmd.substring(5, 8).indexOf(' ') != -1) //jezeli w nicku jest spacja
					return "0";
				if (cmd.substring(8).indexOf(' ') != -1) // brak nowego nicku
					return "0";
				String nick = cmd.substring(5);
				return "2" + nick;
			}
		}
		else if (cmd4.toUpperCase().equals("QUIT")) {
			if (cmd.toUpperCase().equals("QUIT"))
				return "3";
			else
				return "0";			
		}
		else if (cmd4.toUpperCase().equals("LIST")) {
			if (cmd.toUpperCase().equals("LIST"))
				return "4";
			else
				return "0";				
		}
		else if (cmd4.toUpperCase().equals("KICK")) {
			if (len == 4) //tylko kick
				return "0";
			else if (len == 5) //kick spacja
				return "0";
			else if (len > 5) {
				if (!cmd.substring(4, 5).equals(" ")) //5 znak to musi byc spacja
					return "0";
				if (cmd.substring(5).length() < 3) //po spacji conajmniej 3literowy nick
					return "0";
				if (cmd.substring(5, 8).indexOf(' ') != -1) //jezeli w nicku jest spacja
					return "0";
				if (cmd.substring(8).indexOf(' ') == -1) // brak powodu
					return "5" + cmd.substring(5);
				else {
					
					String nick = cmd.substring(5);
					nick = nick.substring(0, nick.indexOf(' '));
					String msg = cmd.substring(nick.length() + 6);
					return "6" + nick + " " + msg;
				}			
			}		
		}
		else if (cmd4.toUpperCase().equals("AWAY")) {
			if (cmd.toUpperCase().equals("AWAY"))
				return "7";
			else if (len == 5) //away spacja
				return "0";
			else if (len > 5) {
				if (!cmd.substring(4, 5).equals(" ")) //5 znak to musi byc spacja
					return "0";
				return "8" + cmd.substring(5);	
			}		
		}
		return "0";
	}
	
	// zakoñczenie w¹tku serwera obs³uguj¹cego klienta
	public void quit() {isQuit = true;}
	
	//sprawdzenie czy u¿ytkownik jest administratorem
	public Boolean checkIfAdmin() {return isAdmin;}	
}
