
import java.io.IOException;
import java.util.Scanner;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.ec2.AmazonEC2;

public class Startup {

	public void startMenu(){
		Integer option =0;
		Scanner scan = new Scanner(System.in);
		String instanceId ="";
		InstanceServices services = new InstanceServices();
		AmazonEC2 ec2 =null;
		do{
			try{
				System.out.println("Enter the Service No. required :\n");
				System.out.println("1. Create New Instance");
				System.out.println("2. Terminate A Instance");
				System.out.println("3. Start An Instance");
				System.out.println("4. Stop An Instance");
				System.out.println("5. Restart A Running Instance");
				System.out.println("6. Reboot A Running Instance");
				System.out.println("7. List and Describe Characteristics of Existing Instances");
				System.out.println("8. Create AMI for Existing Instance");
				System.out.println("9. Quit the Service\n");
				System.out.print("Please Enter here :");
				try{	
					option = scan.nextInt();
					System.out.println("");
				}
				catch(Exception e){
					System.out.println("Please enter valid Integer");	
				} 

				ec2 = services.EC2Initialization();

				if(option==1){

					System.out.print("Please Enter the Image ID for the Instance to be Created :");
					String imageId= scan.next();
					System.out.print("Enter the Instance Type to be created :");
					String type = scan.next();
					System.out.print("Please Enter the Key Name for the Instance :");
					String name = scan.next();
					instanceId = services.createInstance(ec2,imageId,type,name);
					Thread.sleep(2000);
					System.out.println("Instance has been created with Instance Id :"+instanceId);
				}
				else if(option==2){
					System.out.print("Please Enter the Instance ID to be Terminated :");
					instanceId = scan.next();
					services.terminateInstance(ec2, instanceId);
					Thread.sleep(2000);
					System.out.println("Instance "+instanceId+" has been terminated");
				}
				else if(option==3){

					System.out.print("Please Enter the Instance ID to be Started :");
					instanceId = scan.next();
					services.startInstance(ec2, instanceId);
					Thread.sleep(2000);
					System.out.println("Instance "+instanceId+" has been started");

				}
				else if(option==4){
					try{
						System.out.print("Please Enter the Instance ID to be Stopped :");
						instanceId = scan.next();
						services.stopInstance(ec2, instanceId);
						Thread.sleep(2000);
						System.out.println("Instance "+instanceId+" has been stopped");
					}
					catch(AmazonServiceException aws){
						if(aws.getErrorCode().equalsIgnoreCase("IncorrectState")){
							System.err.println("The instance you are trying to stop is already stopped.");
						}else{
							throw aws;
						}
					}

				}
				else if(option==5){
					try{
						System.out.print("Please Enter the Instance ID to be Restarted :");
						instanceId = scan.next();
						services.restartInstance(ec2, instanceId);

					}
					catch(AmazonServiceException aws){
						if(aws.getErrorCode().equalsIgnoreCase("IncorrectState")){
							System.err.println("The instance you are trying to restart is stopped. Please restart a running Instance.");
						}else{
							throw aws;
						}
					}
				}
				else if(option==6){
					try{
						System.out.print("Please Enter the Instance ID to be Rebooted :");
						instanceId = scan.next();
						services.rebootInstance(ec2, instanceId);
						Thread.sleep(2000);
						System.out.println("Instance "+instanceId+" has been rebooted");
					}
					catch(AmazonServiceException aws){
						if(aws.getErrorCode().equalsIgnoreCase("IncorrectState")){
							System.err.println("The instance you are trying to reboot is stopped. Please reboot a running Instance.");
						}else{
							throw aws;
						}
					}
				}
				else if(option==7){
					services.listAndDescribeInstances(ec2);
				}
				else if(option==8){
					System.out.print("Please Enter the Instance ID to create Image :");
					instanceId = scan.next();
					System.out.print("Please Enter the Name for the Image to be created:");
					String imageName = scan.next();
					System.out.print("Please Enter the Description for the Image to be created :");
					String imageDescription = scan.next();
					String ami = services.createAMIfromInstance(ec2, instanceId, imageName, imageDescription);
					Thread.sleep(2000);
					System.out.println("Image Id "+ami+" has been created for the instance "+instanceId);
				}
				else if(option>9||option<1){
					System.out.println("Please enter a valid service number between 1 to 8");
				}

				if(option!=9){
					System.out.println("Press any key to Continue...");
					new Scanner(System.in).nextLine();
				}

			}
			catch(AmazonServiceException aws){
				if(aws.getErrorCode().contains("InvalidInstanceID")){
					System.out.println("");
					System.err.println("Please enter a valid Instance Id to avail the service");
					System.out.println("Press any key to Continue...");
					new Scanner(System.in).nextLine();
				}else{
					System.err.println("Please enter a valid input to avail the service");
					aws.printStackTrace();
					System.out.println("");
					System.out.println("Press any key to Continue...");
					new Scanner(System.in).nextLine();	
				}
			}
			catch(IOException e){
				System.err.println("Error occured in Input Output Services. Please try again");
				System.out.println("");
				System.out.println("Press any key to Continue...");
				new Scanner(System.in).nextLine();
			}
			catch(Exception e){
				System.err.println("Error occured. Please try again.");
				System.out.println("");
				System.out.println("Press any key to Continue...");
				new Scanner(System.in).nextLine();
			}

		}while(option!=9);
		if(option==9){
			System.out.println("Successfully Terminated. Thank You.");
		}

		scan.close();

	}


	public static void main(String[] args) {
		// TODO Auto-generated method stub
		new Startup().startMenu();
	}	

}
