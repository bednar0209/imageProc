import java.io.File;
import java.io.FileOutputStream;
import java.util.List;

import marvin.image.MarvinImage;
import marvin.io.MarvinImageIO;
import marvin.plugin.MarvinImagePlugin;
import marvin.util.MarvinPluginLoader;

import org.apache.commons.io.IOUtils;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.ClasspathPropertiesFileCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.CreateQueueRequest;
import com.amazonaws.services.sqs.model.DeleteMessageRequest;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;

public class imageProcess {

    public static final String whichPhoto = null;
    private static MarvinImage image, backupImage;
	private static MarvinImagePlugin  imagePlugin; 
	private static String bucketName = "artur-project";
	private static AmazonS3Client     s3Client;
	private static AmazonSQS          sqs;
	
	
	//public static void main(String[] args) throws Exception {

	public void imageProc() throws Exception{
		AWSCredentialsProvider credentialsProvider = new ClasspathPropertiesFileCredentialsProvider();
				
        //AmazonSQS 
        sqs = new AmazonSQSClient(credentialsProvider);
        Region usWest2 = Region.getRegion(Regions.US_WEST_2);
        sqs.setRegion(usWest2);
        
       // AmazonS3 
        s3Client = new AmazonS3Client(credentialsProvider);
        s3Client.setRegion(usWest2);


       //////
        try {
        	
            CreateQueueRequest createQueueRequest = new CreateQueueRequest("bednarczykSQS");
            String myQueueUrl = sqs.createQueue(createQueueRequest).getQueueUrl();

            // Receive messages
            System.out.println("Receiving messages from MyQueue.\n");
            ReceiveMessageRequest receiveMessageRequest = new ReceiveMessageRequest(myQueueUrl);
            List<Message> messages = sqs.receiveMessage(receiveMessageRequest).getMessages();
            for (Message message : messages) {
                System.out.println("    MessageId:     " + message.getMessageId());
                System.out.println("    Body:          " + message.getBody());
            
               String whichPhoto = message.getBody();
               System.out.println("Which photo: "+whichPhoto);

               ///////////////////////////////////////////////////////////////////////////////////
               File dir = new File("C:/Temp");
               if (!dir.exists()) {
            	    System.out.println("creating directory C:/Temp");
            	    boolean result = false;

            	    try{
            	        dir.mkdir();
            	        result = true;
            	     } catch(SecurityException se){
            	        //handle it
            	     }        
            	     if(result) {    
            	       System.out.println("DIR created");  
            	     }
            	  }
                 
               
               File localFile = new File("C:/Temp/"+whichPhoto);
               GetObjectRequest request = new GetObjectRequest(bucketName, whichPhoto);
            		  S3Object object = s3Client.getObject(request);
            		  S3ObjectInputStream objectContent = object.getObjectContent();
            		  FileOutputStream fos = new FileOutputStream(localFile);
            		  IOUtils.copy(objectContent, fos);
               
            		  
            ///////////////////////////////////////////////////////////////////////////////image sepia
               image = MarvinImageIO.loadImage("C:/Temp/"+whichPhoto); 
               backupImage = image.clone(); 
               image = backupImage.clone(); 
               
               imagePlugin = MarvinPluginLoader.loadImagePlugin("org.marvinproject.image.color.sepia.jar"); 
               imagePlugin.setAttribute("hsIntensidade", 50);                 
               imagePlugin.process(image, image); 
           
               image.update(); 
            //   MarvinImageIO.saveImage(image, "C:/Temp/"+"sepia_"+whichPhoto);
               MarvinImageIO.saveImage(image, "C:/Temp/"+whichPhoto);
               System.out.println("Successful processing!");
               System.out.println(whichPhoto);

               localFile.delete();
               
            //   File fileToUpload = new File("C://Temp/"+"sepia_"+whichPhoto);
               File fileToUpload = new File("C://Temp/"+whichPhoto);
           //    PutObjectRequest por = new PutObjectRequest(bucketName, "sepia_"+whichPhoto, fileToUpload);
               PutObjectRequest por = new PutObjectRequest(bucketName, whichPhoto, fileToUpload);
               por.setCannedAcl(CannedAccessControlList.PublicRead);
               s3Client.putObject(por);
               System.out.println("Sepia uploaded to S3.\n");
               fileToUpload.delete();
               
            }
            System.out.println();

            // Delete a message
            System.out.println("Deleting a message.\n");
            String messageRecieptHandle = messages.get(0).getReceiptHandle();
            sqs.deleteMessage(new DeleteMessageRequest(myQueueUrl, messageRecieptHandle));

        }
        catch (AmazonServiceException ase) {
            System.out.println("Caught an AmazonServiceException, which means your request made it " +
                    "to Amazon SQS, but was rejected with an error response for some reason.");
            System.out.println("Error Message:    " + ase.getMessage());
            System.out.println("HTTP Status Code: " + ase.getStatusCode());
            System.out.println("AWS Error Code:   " + ase.getErrorCode());
            System.out.println("Error Type:       " + ase.getErrorType());
            System.out.println("Request ID:       " + ase.getRequestId());
        } catch (AmazonClientException ace) {
            System.out.println("Caught an AmazonClientException, which means the client encountered " +
                    "a serious internal problem while trying to communicate with SQS, such as not " +
                    "being able to access the network.");
            System.out.println("Error Message: " + ace.getMessage());
        }  
    
	 //repeat
	
	}
}
