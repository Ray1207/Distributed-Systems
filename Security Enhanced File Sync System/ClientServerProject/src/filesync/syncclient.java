package filesync;
import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.Map;
import java.io.IOException;
import java.security.Key;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.text.SimpleDateFormat;
import javax.crypto.SecretKey;


import org.apache.commons.codec.binary.Base64;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import org.json.simple.parser.JSONParser;
public class syncclient {
	
	private static final JSONParser parser = new JSONParser();
	static int WorkIndicator=1;
	static String fileName="";
	static String hostName="";
    static SecretKey SharedSessionKey = null;
    public static void main(String[] args) throws IOException 
    {
    	
       
		// create socket and I/O stream
    	Socket Socket = null;
    	
        PrintWriter out = null;
        BufferedReader in = null;
        hostName=args[0];
        fileName=args[1];
//        hostName="localhost";
//        fileName="txta.txt";
        
    	try 
		{
	    	Socket = new Socket(hostName, 6666);
	        out = new PrintWriter(Socket.getOutputStream(), true);
	        in = new BufferedReader(new InputStreamReader(Socket.getInputStream()));
	    } 
		catch (UnknownHostException e) 
		{
	        System.err.println("Don't know about host: "+hostName);
	        System.exit(1);
	    } 
		catch (IOException e) 
		{
	        System.err.println("Couldn't get I/O for the connection to:."+hostName);
	        System.exit(1);
	    }
    	
    	BufferedReader bfReaderUser = new BufferedReader(new InputStreamReader(System.in));
    	
    	String RequestMsg="Request Publick Key";
		String ServerPublickKeyMsg;
		String ServerPublicKey="";
		
		out.println(RequestMsg);
		
		if((ServerPublickKeyMsg=in.readLine())!=null)
		{
			ServerPublicKey=ServerPublickKeyMsg;
			
		}
		
		if(ServerPublicKey!="")
		{
		
					//Generate shared session key
					SharedSessionKey=EncryptionUntil.generateSessionKey();
					
					PublicKey _PublicKey=null;
					_PublicKey=EncryptionUntil.StringToPublicKey(ServerPublicKey);

					//Shared Session Key is encrypted by PublicKey and then sent to server
					String strSharedSessionKey=new String(Base64.encodeBase64(SharedSessionKey.getEncoded()));
					String EncryptedSessionKey = new String(Base64.encodeBase64(EncryptionUntil.encrypt(strSharedSessionKey, _PublicKey)));
					System.out.println("Client sent shared session key.");
					out.println(EncryptedSessionKey);
		            // @add receive server reply about getting seesion key			
					//Get user password input, encrypt it with public key and then send it to server
					try {
						String password="";
						while((password=askForPassword(out,in))!=null)
						{
							String validationResult = Validate(SharedSessionKey,password,out,in);
							if(validationResult.equalsIgnoreCase("true"))
							{
								System.err.println("Client Validation Succeeded.");
								break;
							}
							else
							{
								System.err.println("Client Validation Failed.");
							}
						}
						while (WorkIndicator==1)
						{
							/*
							 * Compare client file with server file 
							 * if server's file date is == Client file's then skip sending messages
							 */

							//First get client's file last modified date time
							String ClientFileModifiedDate ="";
							String ServerFileModifiedDate="";
							File file = new File(fileName);
							SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
							ClientFileModifiedDate = sdf.format(file.lastModified());
							//System.err.println("Test file length"+file.length());
						 /*
						  * First Message from server, Server initialise the process, a json string consisting of  Block Size
						  * Source/Destination and Server File LastModified Date is passed in.
						 */
							     
							    Map<String, String> initialAnswer = new HashMap<String, String>();
							    
								String intialFromServer;
								String ChoiceFromUser;
								JSONObject obj=null;
							
								String ToBeSource;
								String BlockSize="1024";
								String LastModified="";
								
								String SourceOrDestinationIndicator="Source";
								//BufferedReader bfReaderUser = new BufferedReader(new InputStreamReader(System.in));
								
								if ((intialFromServer=in.readLine())!=null)
								{   //System.err.println("See secure thing:"+intialFromServer);
									intialFromServer=GetDecryptText(intialFromServer);
									System.err.println("Client received First Message JSON"+intialFromServer);
										try {
												obj = (JSONObject) parser.parse(intialFromServer);
												ToBeSource=obj.get("BeSource").toString();
												BlockSize=obj.get("BlockSize").toString();
												LastModified=obj.get("LastModified").toString();
												
											     System.out.println("Would you want to be the source? Type (Yes/No):");
												 ChoiceFromUser = bfReaderUser.readLine();
												 
												 if (ChoiceFromUser.equalsIgnoreCase("Yes"))
												 {
													//System.out.println("You typed Yes");
													 ToBeSource="Yes";
													 System.out.println("Please specify block size, by default, it is 1024KB:");
													 while ((ChoiceFromUser = bfReaderUser.readLine())!=null)
													 {
													   try{
														    Integer.parseInt(ChoiceFromUser);
														    BlockSize =ChoiceFromUser;
														    break;
													     } 
												     	 catch (NumberFormatException  e) 
												     	 {
														 System.err.println("Please enter an integer!");
														 System.out.println("Please specify block size:");
													     }
													 }
													 
												 }
												 else if (ChoiceFromUser.equalsIgnoreCase("No"))
												 {
						
														//System.out.println("You typed No");
														 ToBeSource="No";
														 System.out.println("Please Specify block size:");
														 while ((ChoiceFromUser = bfReaderUser.readLine())!=null)
														 {
														   try{
															    Integer.parseInt(ChoiceFromUser);
															    BlockSize =ChoiceFromUser;
															    break;
														     } 
													     	 catch (NumberFormatException  e) 
													     	 {
															 System.err.println("Please enter an integer!");
															 System.out.println("Please specify block size:");
														     }
														 }
														 
													 
												 }
												 ServerFileModifiedDate=LastModified;
												 initialAnswer.put("BeSource", ToBeSource);
											     initialAnswer.put("BlockSize", BlockSize);
											     initialAnswer.put("LastModified", ClientFileModifiedDate);
											     
											     JSONObject jsonAnswer = new JSONObject();
											     jsonAnswer.putAll(initialAnswer);
											     String initialAJson=jsonAnswer.toString();
											     out.println(GetEncryptText(initialAJson));   // first response sent
											     System.out.println("Client replies the first message"+initialAJson);	 
												 
											} 
										catch (ParseException e) 
										   {
												e.printStackTrace();
										   }
								}
								
								
								/*
								   * Second Message from server, server confirmed the client
								 */
								
								String SecondMsg;
								if ((SecondMsg=in.readLine())!=null)
								{
									   SecondMsg=GetDecryptText(SecondMsg);
									  if (SecondMsg.equalsIgnoreCase("OK, you are source"))
										 {
											 SourceOrDestinationIndicator="Source";
											 System.out.println("Client receives confirmation and starting as source");
										 }
									  else if (SecondMsg.equalsIgnoreCase("OK, you are destination"))
										 {
											 SourceOrDestinationIndicator="Destination";
											 System.out.println("Client receives confirmation and starting as destination");
										 }
									  else 
									  {
										  System.err.println("Server is not responding. System stops.");
										  System.exit(-1);
									  }
								}
								
								
								SynchronisedFile ClientFile=null;
						    	/*
								 * Initialise the SynchronisedFiles.
								 */
								try {
									ClientFile=new SynchronisedFile(fileName,Integer.parseInt(BlockSize));
								    } 
								catch (IOException e) 
								{
									e.printStackTrace();
									System.exit(-1);
								}
								
								
								/*
								 * This section client starts running as Source
								 */
								if (SourceOrDestinationIndicator.equalsIgnoreCase("Source"))
							    {
									if (ServerFileModifiedDate.equals(ClientFileModifiedDate))
									{
										System.err.println("File was not modified. No need to synchronise.");
									}
									else
									{
									    ProcessAsSource(ClientFile,out,in);
									}
							    }
								
								/*
								 * This section client starts running as Destination
								 */
								
								if (SourceOrDestinationIndicator.equalsIgnoreCase("Destination"))
								{
									if (ServerFileModifiedDate.equals(ClientFileModifiedDate))
									{
										System.err.println("File was not modified. No need to synchronise.");
									}
									else 
									{
									   ProcessAsDestinaion(ClientFile,out,in);
									}
								}
								
				        }
					} 
		        	 catch (IOException e) 
		        	 {
						e.printStackTrace();
					  }
		}
    }
    
    protected static String GetEncryptText(String text)
    {
  	  return new String (Base64.encodeBase64(EncryptionUntil.encryptBySessionKey(text, SharedSessionKey)));
    }
    
    protected static String GetDecryptText(String text)
    {
  	  return EncryptionUntil.decryptBySessionKey(Base64.decodeBase64(text.getBytes()),SharedSessionKey);
    }
    
    protected static String askForPassword(PrintWriter out,BufferedReader in)
    {
    	System.out.println("Please enter password for server connection:");
    	String password="";
		BufferedReader bfRD = new BufferedReader(new InputStreamReader(System.in));
		try {
			password=bfRD.readLine();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return password;
    }
    protected static String Validate(SecretKey SharedSessionKey,String password,PrintWriter out,BufferedReader in)
    {
    	String EncryptedPassword = new String( Base64.encodeBase64(EncryptionUntil.encryptBySessionKey(password, SharedSessionKey)));
		 System.out.println("Client sent password.");
		 out.println(EncryptedPassword);
		 String validationResult="false";
    	try
    	{
		 String ServerMsg;
		 if ((ServerMsg=in.readLine())!=null)
		 {
			 validationResult = ServerMsg;
		 }
    	}
    	catch (IOException e) 
   	    {
			e.printStackTrace();
		}
    	return validationResult;
    }
   
    
    protected static void ProcessAsSource(SynchronisedFile ClientFile,PrintWriter out,BufferedReader in)
    {
    	// start running thread to check file state
    				Thread stt = new Thread(new CheckFileStateThread(ClientFile));
    				stt.start();
    				
    			   	Instruction inst;
    		    	Instruction upgraded = null;
    		    	String msgFromDestination;
    		    	String InsTypeIndicator;
    				// The Source reads instructions to send to the Destination
    				while((inst=ClientFile.NextInstruction())!=null){
    					
    					String msg=inst.ToJSON();
    					InsTypeIndicator= inst.Type();
    					System.out.println("Sending from Client: "+msg);
    					
    					/*
    					 * the Source (Client) sends the msg to the Destination.
    					 */
    					out.println(GetEncryptText(msg));
    					 
    						
    			        try {
    			        	
    						while ((msgFromDestination = in.readLine()) != null) 
    						{
    							msgFromDestination=GetDecryptText(msgFromDestination);
    						    System.out.println("Server says: " + msgFromDestination);
    						    if (msgFromDestination.equalsIgnoreCase("NewBlock Request"))
    						    {
    						      upgraded=new NewBlockInstruction((CopyBlockInstruction)inst);
    							   String msg2 = upgraded.ToJSON();
    							   System.err.println("Sending again from Client: "+msg2);
    							   out.println(GetEncryptText(msg2));
    		
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
    			        	String goOnOrNot="";
    			        	System.out.println("---------  Synchronization is done, please type Exit to quit or any key to go on: ---------");
    			        	BufferedReader bfRD = new BufferedReader(new InputStreamReader(System.in));
    			        	try {
								goOnOrNot=bfRD.readLine();
								if (goOnOrNot.equalsIgnoreCase("Exit"))
								{
									out.println(GetEncryptText("Exit"));
									WorkIndicator=0;
									System.out.println("System is terminated. Thanks for trying it out.");
									System.exit(0);
								}
								else 
								{
									out.println(GetEncryptText("Go on"));
									WorkIndicator=1;
								}
								
							    } 
    			        	 catch (IOException e) 
    			        	 {
								e.printStackTrace();
							   }
    			        	//stt.interrupt();
    			        	break;   // finish one sync
    			        }
    					  /*
    					   * Client receives acknowledgment and moves on to process next instruction.
    					  */
    				   } // get next instruction loop forever
    }
    
    
    protected static void ProcessAsDestinaion(SynchronisedFile ClientFile,PrintWriter out,BufferedReader in)
    {


        String msg;
        String msgTypeIndicator;
		InstructionFactory instFact=new InstructionFactory();
       
		try {
			while((msg = in.readLine())!=null){
				msg=GetDecryptText(msg);
				//System.err.println("Client side working");
				System.out.println("Client receives message: " + msg);
				/*
				 * The Client receives the instruction here.
				 */
				Instruction receivedInst = instFact.FromJSON(msg);
				msgTypeIndicator=receivedInst.Type();
				
				try {
					// The Client processes the instruction
					ClientFile.ProcessInstruction(receivedInst);
					 
				} 
				catch (IOException e) 
				{
					e.printStackTrace();
					System.exit(-1); // just die at the first sign of trouble
				} 
				catch (BlockUnavailableException e) {
					System.out.println("Client says block not found");
					// The client does not have the bytes referred to by the block hash.
					try {
						/*
						 * At this point the Client needs to send a request back to the Server
						 * to obtain the actual bytes of the block.
						 */
						 out.println(GetEncryptText("NewBlock Request"));
						 System.out.println("Client sends new block request");
						// network delay
						
						/*
						 * Server upgrades the CopyBlock to a NewBlock instruction and sends it.
						 */
						
						 
						 if ((msg = in.readLine())!=null)
				        {
							 msg=GetDecryptText(msg);
							 System.out.println("Client receives new block from Server"+msg);
							 /*
								 * Client receives the NewBlock instruction.
								 */
								Instruction receivedInst2 = instFact.FromJSON(msg);
								ClientFile.ProcessInstruction(receivedInst2);
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
				
				
				out.println(GetEncryptText("Process Acknowledged"));
				System.out.println("Client sent process acknowledged");
				if (msgTypeIndicator.equalsIgnoreCase("EndUpdate"))
				{
					
					String goOnOrNot="";
		        	System.out.println("--------- Synchronization is done, please type Exit to quit or any key to go on: ---------");
		        	BufferedReader bfRD = new BufferedReader(new InputStreamReader(System.in));
		        	try {
						goOnOrNot=bfRD.readLine();
						if (goOnOrNot.equalsIgnoreCase("Exit"))
						{
							out.println(GetEncryptText("Exit"));
							WorkIndicator=0;
							System.out.println("System is terminated. Thanks for trying it out.");
							System.exit(0);
						}
						else 
						{
							out.println(GetEncryptText("Go on"));
							WorkIndicator=1;
						}
						
					    } 
		        	 catch (IOException e) 
		        	 {
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
		catch (IOException e) 
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		} // get next instruction loop forever

    }
    
}
