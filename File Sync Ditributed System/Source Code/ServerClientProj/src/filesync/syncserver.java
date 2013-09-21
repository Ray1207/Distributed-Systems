package filesync;
import java.net.*;
import java.util.HashMap;
import java.util.Map;
import java.io.*;
import java.text.SimpleDateFormat;
//import java.util.Date;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import org.json.simple.parser.JSONParser;

public class syncserver {
	
	private static final JSONParser parser = new JSONParser();
	static int WorkIndicator=1;
	static String fileName=""; 
    public static void main(String[] args) throws IOException {

     ServerSocket serverSocket = null;
  	 Socket clientSocket = null;
  	 PrintWriter out = null;
  	 BufferedReader in = null;
  	 fileName=args[0];
  	 
  	 //syncProtocal syncPtlServer=new syncProtocal();   
  	   try {
           serverSocket = new ServerSocket(6666);
           System.out.println("Server has been established.");
        } 
     catch (IOException e) 
     {
         System.err.println("Could not listen on port: 6666.");
         System.exit(1);
     }

   
     try {
         clientSocket = serverSocket.accept();
     } catch (IOException e) {
         System.err.println("Accept failed.");
         System.exit(1);
     }
     
     
     out = new PrintWriter(clientSocket.getOutputStream(), true);
     in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
     
     while (WorkIndicator==1)
     {
		     /*
			  * First, server initializes the query about the default source and blocksize
			 */
		    
	    	 String ServerFileModifiedDate = "";
	    	 String ClientFileModifiedDate ="";
	    	 File file = new File(fileName);
			 SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
			 ServerFileModifiedDate = sdf.format(file.lastModified());
			 
		     Map<String, String> initialQuery = new HashMap<String, String>();
		     initialQuery.put("BeSource", "Yes");
		     initialQuery.put("BlockSize", "1024");
		     initialQuery.put("LastModified", ServerFileModifiedDate);
		     
		     JSONObject jsonQuery = new JSONObject();
		     jsonQuery.putAll(initialQuery);
		     String initialQJson=jsonQuery.toString();
		 
		     out.println(initialQJson);  // First message sent
		     System.err.println("Server initializes the query asking source/blocksize");
		     
		     /*
			   * Second, server received the reply about source blocksize
			 */
		     String intialFromClient;
		     JSONObject obj=null;
		    
		     String ToBeSource;
			 String BlockSize="1024";
			 String LastModified;
			 String  SourceOrDestinationIndicator="Destination";
		    
		    if((intialFromClient = in.readLine())!=null)
		    {
		    	System.out.println("Server receives the reply about source/blocksize"+intialFromClient);
		    	
		    	try {
		    	  obj = (JSONObject) parser.parse(intialFromClient);
		    	  ToBeSource=obj.get("BeSource").toString();
		    	  BlockSize=obj.get("BlockSize").toString();
				  LastModified=obj.get("LastModified").toString();
				  ClientFileModifiedDate=LastModified;
				  
				  if (ToBeSource.equalsIgnoreCase("Yes"))
				  {
					  SourceOrDestinationIndicator="Destination";
					  out.println("OK, you are source");
					  System.out.println("Server sent to client says: OK, you are source");
				  }
				  else 
				  {
					  SourceOrDestinationIndicator="Source";
					  out.println("OK, you are destination");
					  System.out.println("Server sent to client says: OK, you are destination");
				  }
				    
				  
		    	  }
		    	catch (ParseException e)
		    	{
		    		e.printStackTrace();
		    	}
		    	
		    }
		    
		    
		    SynchronisedFile ServerFile=null;
		   	try {
		  		//toFile=new SynchronisedFile(args[1]);
		   		ServerFile=new SynchronisedFile(fileName,Integer.parseInt(BlockSize));
		  	    } 
		   	catch (IOException e) 
		   	    {
		  		e.printStackTrace();
		  		System.exit(-1);
		  	    }
		    
		    /*
			 * This section server starts running as Destination
			 */
		     
		     if (SourceOrDestinationIndicator.equalsIgnoreCase("Destination"))
		     {
		    	 if (!ServerFileModifiedDate.equals(ClientFileModifiedDate))
					{
						ProcessAsDestinaion(ServerFile,out,in);
					}
		    	 else
		    	 {
		    		 System.err.println("File was not modified. No need to synchronise.");
		    	 }
		    	
		     }
		     
		     
		     
		     /*
		 	 * This section server starts running as Source
		 	 */
		
		     if (SourceOrDestinationIndicator.equalsIgnoreCase("Source"))
		     {
		    	 if (!ServerFileModifiedDate.equals(ClientFileModifiedDate))
		    	 {
		    		 ProcessAsSource(ServerFile,out,in);
		    	 }
		    	 else
		    	 {
		    		 System.err.println("File was not modified. No need to synchronise.");
		    	 }
		    	 
		     }
     } 
//        out.close();
//        in.close();
//        clientSocket.close();
//        serverSocket.close();
    }
    
    
    protected static void ProcessAsDestinaion(SynchronisedFile ServerFile,PrintWriter out,BufferedReader in) throws IOException
    {

        String msg;
        String msgTypeIndicator;
		InstructionFactory instFact=new InstructionFactory();
       
			while((msg = in.readLine())!=null){
				
				//System.err.println("Server side working");
				System.out.println("Server receives message: " + msg);
				
				/*
				 * The Server receives the instruction here.
				 */
				Instruction receivedInst = instFact.FromJSON(msg);
				msgTypeIndicator=receivedInst.Type();
				
				try {
					// The Server processes the instruction
					ServerFile.ProcessInstruction(receivedInst);
					 
				} 
				catch (IOException e) 
				{
					e.printStackTrace();
					System.exit(-1); // just die at the first sign of trouble
				} 
				catch (BlockUnavailableException e) {
					System.out.println("Server says block not found");
					// The server does not have the bytes referred to by the block hash.
					try {
						/*
						 * At this point the Server needs to send a request back to the Client
						 * to obtain the actual bytes of the block.
						 */
						 out.println("NewBlock Request");
						 System.out.println("Server sends new block request");
						// network delay
						
						/*
						 * Client upgrades the CopyBlock to a NewBlock instruction and sends it.
						 */
						
						 
						 if ((msg = in.readLine())!=null)
				        {
							 System.out.println("Server receives new block from client"+msg);
							 /*
								 * Server receives the NewBlock instruction.
								 */
								Instruction receivedInst2 = instFact.FromJSON(msg);
								ServerFile.ProcessInstruction(receivedInst2);
				        }
						 
					    } 
					catch (IOException e1) 
					{
						e1.printStackTrace();
						System.exit(-1);
					} 
					catch (BlockUnavailableException e1) 
					{
						assert(false); // a NewBlockInstruction can never throw this exception
					}
				}
				
				
				out.println("Process Acknowledged");
				System.out.println("server sends process acknowledged");
				
				if (msgTypeIndicator.equalsIgnoreCase("EndUpdate"))
					{
					try {
						String continueOrNot = in.readLine();
						if (continueOrNot.equalsIgnoreCase("Exit"))
						{
							WorkIndicator=0;
							System.exit(0);
						}
						else 
						{
							WorkIndicator=1;
						}
						
					} catch (IOException e) {
						// TODO Auto-generated catch block
						 e.printStackTrace();
					     }
		        	break;   // finish one sync
					}
				
				/*
				 * If using a synchronous RequestReply protocol, the server can now acknowledge 
				 * that the block was correctly received, and the next instruction can be sent.
				 */
				
				// network delay
				
				/*
				 * Client receives acknowledgement and moves on to process next instruction.
				 */
			}
			
    }
    
    
    protected static void ProcessAsSource(SynchronisedFile ServerFile,PrintWriter out,BufferedReader in)
    {

		// start running thread to check file state
		Thread stt = new Thread(new CheckFileStateThread(ServerFile));
		stt.start();
        

	   	Instruction inst;
    	Instruction upgraded = null;
    	String msgFromDestination;
    	String InsTypeIndicator;
		// Server reads instructions to send to the Client
		while((inst=ServerFile.NextInstruction())!=null){
			String msg=inst.ToJSON();
			InsTypeIndicator= inst.Type();
			System.out.println("Sending from Server: "+msg);
			/*
			 * the Source sends the msg to the Destination.
			 */
			
			 out.println(msg);
			 
			 
	        try {
	        	
				while ((msgFromDestination = in.readLine()) != null) 
				{
				    System.out.println("Client says: " + msgFromDestination);
				    if (msgFromDestination.equalsIgnoreCase("NewBlock Request"))
				    {
				      upgraded=new NewBlockInstruction((CopyBlockInstruction)inst);
					   String msg2 = upgraded.ToJSON();
					   System.err.println("Sending again from Server: "+msg2);
					   out.println(msg2);

				    }
				    
				    if (msgFromDestination.equalsIgnoreCase("Process Acknowledged"))
				    {
				    	msgFromDestination=null;
				     	break;
				    }
				}
			   } 
	           catch (IOException e) {
				// TODO Auto-generated catch block
			 	e.printStackTrace();
			    }
	        
	        
	        if (InsTypeIndicator.equalsIgnoreCase("EndUpdate"))
	        {
	        	try {
					String continueOrNot = in.readLine();
					if (continueOrNot.equalsIgnoreCase("Exit"))
					{
						WorkIndicator=0;
						System.exit(0);
					}
					else 
					{
						WorkIndicator=1;
					}
					
				} catch (IOException e) {
					// TODO Auto-generated catch block
					 e.printStackTrace();
				     }
	        	break;   // finish one sync
	        }
			 
			  /*
			   * Client receives acknowledgment and moves on to process next instruction.
			  */
		   } // get next instruction loop forever
		
 
    }
    
    
}