package filesync;

import java.io.*;
import java.net.*;

/*
 * This test thread provides comments to explain how the Client/Server 
 * architecture could be implemented that uses the file synchronisation protocol.
 */

public class CheckFileStateThread  implements Runnable {

	SynchronisedFile fromFile; // this would be on the Client
//	SynchronisedFile toFile; // this would be on the Server
	

	CheckFileStateThread (SynchronisedFile ff)
	{
		fromFile=ff;
	}
	
	@Override
	public void run() {
		
		
	
			try 
			{
			//	System.err.println("SynchTest: Source calling CheckFileState()");
				fromFile.CheckFileState();
			} 
			catch (IOException e) 
			{
				e.printStackTrace();
				System.exit(-1);
			} 
			catch (InterruptedException e) 
			{
				e.printStackTrace();
				System.exit(-1);
			}
	}
}
