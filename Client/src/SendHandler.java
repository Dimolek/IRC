import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Scanner;

//ARTUR MARCINIAK
//MATEUSZ TOMCZYK
//DAWID WYCIS£O

public class SendHandler extends java.lang.Thread {

	private DataOutputStream out;
	private static Boolean isQuit;
	
	SendHandler(DataOutputStream out){
		this.out = out;
		isQuit = false;
	}
	
	public void run() {
		
		
		Scanner s = new Scanner(System.in);
		String ss;
		//wysy³anie wiadomoœci do servera
		while(!isQuit) {
			try {
				ss = "um" + s.nextLine();
				if(!isQuit)
					out.writeUTF(ss);
				else	//jeœli w¹tek otrzyma wiadomoœæ o skoñczeniu pracy, informuje o tym server
					out.writeUTF("quit");
			}catch (IOException e) {
				System.err.println(e.getMessage());
				e.printStackTrace();
			}
		}
			
	}
	public void setQuit() { isQuit = true; }
}
