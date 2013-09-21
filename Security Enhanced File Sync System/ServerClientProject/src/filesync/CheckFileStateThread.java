package filesync;

import java.io.*;
import java.net.*;

/*
 * This test thread provides comments to explain how the Client/Server 
 * architecture could be implemented that uses the file synchronisation protocol.
 */

public class CheckFileStateThread implements Runnable {

	SynchronisedFile fromFile; // this would be on the Client
	
	CheckFileStateThread(SynchronisedFile ff)
	{
		fromFile=ff;		
	}
	
	@Override
	public void run() {

			try 
			{
				// TODO: skip if the file is not modified
				//System.err.println("SynchTest: Server calling CheckFileState()");
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
