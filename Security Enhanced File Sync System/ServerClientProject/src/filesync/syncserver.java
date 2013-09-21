package filesync;

import java.util.HashMap;
import java.util.Map;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.apache.commons.codec.binary.Base64;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import org.json.simple.parser.JSONParser;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Key;
import java.security.KeyPair;


public class syncserver {

	private static final JSONParser parser = new JSONParser();
	static int WorkIndicator = 1;
	static String fileName = "";
	public static final String PRIVATE_KEY_FILE = "private.key";
	public static final String PUBLIC_KEY_FILE = "public.key";
	public static final String File_Enrypt_Password="password12345678";
	public static String password = "123456";
	public static SecretKey SharedSessionKey = null;

	public static void main(String[] args) throws IOException 
	{
		
		ServerSocket serverSocket = null;
		Socket clientSocket = null;
		PrintWriter out = null;
		BufferedReader in = null;
		
		
		 fileName=args[0];
		//fileName = "txtb.txt";

		try 
		{
			serverSocket = new ServerSocket(6666);
			System.out.println("Server has been established.");
		} 
		catch (IOException e) {
			System.err.println("Could not listen on port: 6666.");
			System.exit(1);
		}

		
		
		
		// Check if the pair of keys are present else generate those.
		if (!EncryptionUntil.areKeysPresent()) {
			// Method generates a pair of keys using the RSA algorithm 
			// and stores it in their respective files
			EncryptionUntil.generateKey();
			
			// Encrypt private key file
			EncryptPrivateKey(PRIVATE_KEY_FILE);
		}
		
		
		
		

		
		ObjectInputStream inputStream = null;
		String DecryptedPath="";
		try {
			inputStream = new ObjectInputStream(new FileInputStream(PUBLIC_KEY_FILE));
			final PublicKey publicKey = (PublicKey) inputStream.readObject();
			// Get Private Key
			
			DecryptedPath=DecryptPrivateKey(PRIVATE_KEY_FILE);
			File decryptedPrivateKeyFile=new File(DecryptedPath);
			
			
			if(!decryptedPrivateKeyFile.exists()){
				System.out.println("Private Key File No Found"+DecryptedPath);
				return;
			}
		
			inputStream = new ObjectInputStream(new FileInputStream(DecryptedPath));
		    final PrivateKey privateKey = (PrivateKey) inputStream.readObject();
			// delete decrypted private key file
		    inputStream.close();
			if(!decryptedPrivateKeyFile.delete()){
					System.err.println("privatekey.delete() failed.");
					System.exit(-1);
				}
			
			
			try 
			{
				clientSocket = serverSocket.accept();
			} 
			catch (IOException e) 
			{
				System.err.println("Accept failed.");
				System.exit(1);
			}
		

		
	
			out = new PrintWriter(clientSocket.getOutputStream(), true);
			in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
			try {
				
				String MsgFromClient;
				
				if ((MsgFromClient = in.readLine()) != null) 
				{
					
					if (MsgFromClient.equalsIgnoreCase("Request Publick Key"))
					{
						String StringPublicKey = new String(Base64.encodeBase64(publicKey.getEncoded()));
					    out.println(StringPublicKey);
						 
							if ((MsgFromClient = in.readLine()) != null) 
							{				
								String strSessionKey = EncryptionUntil.decrypt(Base64.decodeBase64(MsgFromClient),privateKey);
								byte[] data = Base64.decodeBase64(strSessionKey); 
								SharedSessionKey = new SecretKeySpec(data, 0,data.length, "AES");
								System.out.println("Server received shared session key.");

								while ((MsgFromClient = in.readLine()) != null) {
									String receivedPassword = EncryptionUntil.decryptBySessionKey(Base64.decodeBase64(MsgFromClient),SharedSessionKey);

									if (!receivedPassword.equals(password)) 
									{
										System.err.println("Invalid Client, please re-enter password!");
										out.println("false");
									} 
									else 
									{
										System.err.println("Valid Client.");
										out.println("true");
										break;
									}

									
								}
								
							

							while (WorkIndicator == 1) {
								/*
								 * First, server initializes the query about the
								 * default source and blocksize
								 */

								String ServerFileModifiedDate = "";
								String ClientFileModifiedDate = "";
								File file = new File(fileName);
								SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
								ServerFileModifiedDate = sdf.format(file.lastModified());

								Map<String, String> initialQuery = new HashMap<String, String>();
								initialQuery.put("BeSource", "Yes");
								initialQuery.put("BlockSize", "1024");
								initialQuery.put("LastModified",ServerFileModifiedDate);

								JSONObject jsonQuery = new JSONObject();
								jsonQuery.putAll(initialQuery);
								String initialQJson = jsonQuery.toString();

								
								out.println(GetEncryptText(initialQJson)); // First message sent
								System.err.println("Server initializes the query asking source/blocksize");

								/*
								 * Second, server received the reply about
								 * source blocksize
								 */
								String intialFromClient;
								JSONObject obj = null;

								String ToBeSource;
								String BlockSize = "1024";
								String LastModified;
								String SourceOrDestinationIndicator = "Destination";

								if ((intialFromClient = in.readLine()) != null) 
								{  
									intialFromClient=GetDecryptText(intialFromClient);
									System.out.println("Server receives the reply about source/blocksize"+ intialFromClient);

									try {
										obj = (JSONObject) parser.parse(intialFromClient);
										ToBeSource = obj.get("BeSource").toString();
										BlockSize = obj.get("BlockSize").toString();
										LastModified = obj.get("LastModified").toString();
										ClientFileModifiedDate = LastModified;

										if (ToBeSource.equalsIgnoreCase("Yes")) 
										{
											SourceOrDestinationIndicator = "Destination";
											out.println(GetEncryptText("OK, you are source"));
											System.out.println("Server sent to client says: OK, you are source");
										} else {
											SourceOrDestinationIndicator = "Source";
											out.println(GetEncryptText("OK, you are destination"));
											System.out.println("Server sent to client says: OK, you are destination");
										}

									} catch (ParseException e) {
										e.printStackTrace();
									}

								}

								SynchronisedFile ServerFile = null;
								try {
									// toFile=new SynchronisedFile(args[1]);
									ServerFile = new SynchronisedFile(fileName,
											Integer.parseInt(BlockSize));
								} catch (IOException e) {
									e.printStackTrace();
									System.exit(-1);
								}

								/*
								 * This section server starts running as
								 * Destination
								 */

								if (SourceOrDestinationIndicator.equalsIgnoreCase("Destination")) {
									if (!ServerFileModifiedDate.equals(ClientFileModifiedDate)) 
									{
										ProcessAsDestinaion(ServerFile, out, in);
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
										ProcessAsSource(ServerFile, out, in);
									} 
									else 
									{
										System.err.println("File was not modified. No need to synchronise.");
									}

								}
							}
						}
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
				System.exit(1);
			}
		} 
		catch (Exception e) 
		{

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
  
  protected static void EncryptPrivateKey(String fileName)
  {
	  try{
			
		  File file = new File(fileName);
			if(!file.exists()){
				System.out.println("No file "+fileName);
				return;
			}

			String tempFileName=fileName+".enc";
			
				EncryptionUntil.copy(Cipher.ENCRYPT_MODE, fileName, tempFileName, File_Enrypt_Password);	
				// delete original file
				if(!file.delete()){
					System.err.println("privatekey.delete() failed.");
					System.exit(-1);
				}
				
				//rename encryted file
				File file1 = new File(tempFileName);
				if(!file1.renameTo(file)){
					System.err.println("RenameTo(file) failed.");
					System.exit(-1);
				}
				
			//System.out.println("Success. HA");
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	  
	  
//String resultFileName=fileName+".dec";
//		File file2 = new File(tempFileName);
//		File file3 = new File(resultFileName);
//		if(file2.exists() || file3.exists()){
//			System.out.println("File for encrypted temp file or for the result decrypted file already exists. Please remove it or use a different file name");
//			return;
//		}
	  
  }
  
  
  
  protected static String DecryptPrivateKey(String fileName) throws Exception
  {
	  String resultFileName=fileName+".dec";
		  File file = new File(fileName);
			if(!file.exists())
			{
				System.out.println("No file "+fileName);
				return null;
			}

	   EncryptionUntil.copy(Cipher.DECRYPT_MODE, fileName, resultFileName, File_Enrypt_Password);	
	  return resultFileName;
  }
  
  
  
	protected static void ProcessAsDestinaion(SynchronisedFile ServerFile,PrintWriter out, BufferedReader in) throws IOException 
	{

		String msg;
		String msgTypeIndicator;
		InstructionFactory instFact = new InstructionFactory();

		while ((msg = in.readLine()) != null) {

			msg=GetDecryptText(msg);
			// System.err.println("Server side working");
			System.out.println("Server receives message: " + msg);

			/*
			 * The Server receives the instruction here.
			 */
			Instruction receivedInst = instFact.FromJSON(msg);
			msgTypeIndicator = receivedInst.Type();

			try {
				// The Server processes the instruction
				ServerFile.ProcessInstruction(receivedInst);

			} catch (IOException e) {
				e.printStackTrace();
				System.exit(-1); // just die at the first sign of trouble
			} catch (BlockUnavailableException e) {
				System.out.println("Server says block not found");
				// The server does not have the bytes referred to by the block
				// hash.
				try {
					/*
					 * At this point the Server needs to send a request back to
					 * the Client to obtain the actual bytes of the block.
					 */
					out.println(GetEncryptText("NewBlock Request"));
					System.out.println("Server sends new block request");
					// network delay

					/*
					 * Client upgrades the CopyBlock to a NewBlock instruction
					 * and sends it.
					 */

					if ((msg = in.readLine()) != null) 
					{   
						msg=GetDecryptText(msg);
						System.out.println("Server receives new block from client"+ msg);
						/*
						 * Server receives the NewBlock instruction.
						 */
						Instruction receivedInst2 = instFact.FromJSON(msg);
						ServerFile.ProcessInstruction(receivedInst2);
					}

				} catch (IOException e1) {
					e1.printStackTrace();
					System.exit(-1);
				} catch (BlockUnavailableException e1) {
					assert (false); // a NewBlockInstruction can never throw
									// this exception
				}
			}

			out.println(GetEncryptText("Process Acknowledged"));
			System.out.println("server sends process acknowledged");

			if (msgTypeIndicator.equalsIgnoreCase("EndUpdate")) {
				try {
					String continueOrNot = in.readLine();
					continueOrNot=GetDecryptText(continueOrNot);
					if (continueOrNot.equalsIgnoreCase("Exit")) {
						WorkIndicator = 0;
						System.exit(0);
					} else {
						WorkIndicator = 1;
					}

				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				break; // finish one sync
			}

			/*
			 * If using a synchronous RequestReply protocol, the server can now
			 * acknowledge that the block was correctly received, and the next
			 * instruction can be sent.
			 */

			// network delay

			/*
			 * Client receives acknowledgement and moves on to process next
			 * instruction.
			 */
		}

	}

	protected static void ProcessAsSource(SynchronisedFile ServerFile,PrintWriter out, BufferedReader in) 
	{

		// start running thread to check file state
		Thread stt = new Thread(new CheckFileStateThread(ServerFile));
		stt.start();

		Instruction inst;
		Instruction upgraded = null;
		String msgFromDestination;
		String InsTypeIndicator;
		// Server reads instructions to send to the Client
		while ((inst = ServerFile.NextInstruction()) != null) {
			String msg = inst.ToJSON();
			InsTypeIndicator = inst.Type();
			System.out.println("Sending from Server: " + msg);
			/*
			 * the Source sends the msg to the Destination.
			 */

			out.println(GetEncryptText(msg));

			try {

				while ((msgFromDestination = in.readLine()) != null) 
				{
					msgFromDestination=GetDecryptText(msgFromDestination);
					System.out.println("Client says: " + msgFromDestination);
					if (msgFromDestination.equalsIgnoreCase("NewBlock Request")) 
					{
						upgraded = new NewBlockInstruction((CopyBlockInstruction) inst);
						String msg2 = upgraded.ToJSON();
						System.err.println("Sending again from Server: " + msg2);
						out.println(GetEncryptText(msg2));

					}

					if (msgFromDestination.equalsIgnoreCase("Process Acknowledged")) 
					{
						msgFromDestination = null;
						break;
					}
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			if (InsTypeIndicator.equalsIgnoreCase("EndUpdate")) {
				try {
					String continueOrNot = in.readLine();
					continueOrNot=GetDecryptText(continueOrNot);
					if (continueOrNot.equalsIgnoreCase("Exit")) {
						WorkIndicator = 0;
						System.exit(0);
					} else {
						WorkIndicator = 1;
					}

				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				break; // finish one sync
			}

			/*
			 * Client receives acknowledgment and moves on to process next
			 * instruction.
			 */
		} // get next instruction loop forever

	}

}