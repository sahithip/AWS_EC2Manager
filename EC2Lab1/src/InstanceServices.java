
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.internal.ListWithAutoConstructFlag;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.CreateImageRequest;
import com.amazonaws.services.ec2.model.CreateImageResult;
import com.amazonaws.services.ec2.model.CreateKeyPairRequest;
import com.amazonaws.services.ec2.model.CreateKeyPairResult;
import com.amazonaws.services.ec2.model.DescribeInstanceStatusRequest;
import com.amazonaws.services.ec2.model.DescribeInstanceStatusResult;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.InstanceNetworkInterface;
import com.amazonaws.services.ec2.model.InstancePrivateIpAddress;
import com.amazonaws.services.ec2.model.InstanceStatus;
import com.amazonaws.services.ec2.model.RebootInstancesRequest;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.ec2.model.RunInstancesResult;
import com.amazonaws.services.ec2.model.StartInstancesRequest;
import com.amazonaws.services.ec2.model.StopInstancesRequest;
import com.amazonaws.services.ec2.model.Tag;
import com.amazonaws.services.ec2.model.TerminateInstancesRequest;

public class InstanceServices {

	public void startInstance(AmazonEC2 ec2,String instanceId) 
			throws AmazonServiceException, AmazonClientException{

		StartInstancesRequest startInstancesRequest = new StartInstancesRequest();
		List<String> test = new ArrayList<String>();
		test.add(instanceId);		 
		startInstancesRequest.setInstanceIds(test);
		ec2.startInstances(startInstancesRequest);
	}

	public void stopInstance(AmazonEC2 ec2,String instanceId) 
			throws AmazonServiceException, AmazonClientException{	

		StopInstancesRequest stopinstancerequest = new StopInstancesRequest();
		List<String> test = new ArrayList<String>();
		test.add(instanceId);	
		stopinstancerequest.setInstanceIds(test);
		ec2.stopInstances(stopinstancerequest);
	}

	public void restartInstance(AmazonEC2 ec2,String instanceId) 
			throws AmazonServiceException, AmazonClientException, InterruptedException{

		System.out.println("Stopping Instance...");
		stopInstance(ec2,instanceId);

		System.out.println("...Restarting the Instance...");
		while(true){
			try{
				startInstance(ec2,instanceId);
				DescribeInstanceStatusRequest describeInstanceRequest = new DescribeInstanceStatusRequest().withInstanceIds(instanceId);
				DescribeInstanceStatusResult describeInstanceResult = ec2.describeInstanceStatus(describeInstanceRequest);
				List<InstanceStatus> state = describeInstanceResult.getInstanceStatuses();
				while (state.size() < 1) { 
					Thread.sleep(1000);
					startInstance(ec2,instanceId);
					describeInstanceResult = ec2.describeInstanceStatus(describeInstanceRequest);
					state = describeInstanceResult.getInstanceStatuses();
				}
				System.out.println("Instance "+instanceId+" has been restarted");
				break;
			}catch(AmazonServiceException aws){
				if(!aws.getErrorCode().contains("IncorrectInstanceState")){
					throw aws;
				}				
			}
		}
	}

	public void rebootInstance(AmazonEC2 ec2,String instanceId)
			throws AmazonServiceException,AmazonClientException, InterruptedException{
		RebootInstancesRequest rebootInstancesRequest = new RebootInstancesRequest();
		List<String> test = new ArrayList<String>();
		test.add(instanceId);
		rebootInstancesRequest.setInstanceIds(test);
		System.out.println("Rebooting the instance...");
		ec2.rebootInstances(rebootInstancesRequest);
		Thread.sleep(1000);
	}

	public String createInstance(AmazonEC2 ec2, String imageId, String type, String name) 
			throws AmazonServiceException,AmazonClientException{
		RunInstancesResult runInstancesResult = new RunInstancesResult();
		String instanceId ="";
		try{
			RunInstancesRequest runInstancesRequest = new RunInstancesRequest();

			runInstancesRequest.withImageId(imageId)
			.withInstanceType(type)
			.withMinCount(1)
			.withMaxCount(1)
			.withKeyName(name);

			runInstancesResult = ec2.runInstances(runInstancesRequest);
			instanceId = runInstancesResult.getReservation().getInstances().get(0).getInstanceId();
		}catch(AmazonServiceException aws){
			if(aws.getErrorCode().contains("InvalidKeyPair")){
				CreateKeyPairRequest createKeyPairRequest = 
						new CreateKeyPairRequest();
				createKeyPairRequest=createKeyPairRequest.withKeyName(name);
				CreateKeyPairResult createKeyPairResult = ec2.createKeyPair(createKeyPairRequest);
				System.out.println("New key has been Created with Key Name: "+createKeyPairResult.getKeyPair().getKeyName());
				System.out.println("FingerPrint for Key Created: "+createKeyPairResult.getKeyPair().getKeyFingerprint());
				System.out.println("UnEnCrypted PEM file");
				System.out.println(createKeyPairResult.getKeyPair().getKeyMaterial());
				try {
					String fileName = createKeyPairResult.getKeyPair().getKeyName().concat(".pem");
					File file = new File(fileName);
					System.out.println(fileName+" file has been stored in path: "+file.getAbsolutePath());
					FileWriter write = new FileWriter(file);
					write.write(createKeyPairResult.getKeyPair().getKeyMaterial());
					write.close();
					instanceId = createInstance(ec2, imageId,type,name);
				} catch (IOException ex) {
					System.err.println("Input Output problem has occurred. Please try again.");
				} 
			}else{
				throw aws;
			}
		}
		return instanceId;
	}

	public void terminateInstance(AmazonEC2 ec2,String instanceId)
			throws AmazonServiceException, AmazonClientException{
		TerminateInstancesRequest terminateInstancesRequest = new TerminateInstancesRequest();
		List<String> test = new ArrayList<String>();
		test.add(instanceId);
		terminateInstancesRequest.setInstanceIds(test);
		ec2.terminateInstances(terminateInstancesRequest);			
	}

	public void listAndDescribeInstances(AmazonEC2 ec2){
		DescribeInstancesResult yup = ec2.describeInstances(); 
		List<Reservation> reserv = yup.getReservations();
		String name = "";

		for(int i=0;i<reserv.size();i++){
			String instanceId="Instance ID: ";
			String instanceState ="Instance State: ";
			String publicDNSName= "Public DNS: ";
			String elasticIP="Elastic IP: ";
			String instanceType="Instance Type: ";
			String privateDNS="Private DNS: ";
			String availabityZone="Availability zone: ";
			String privateIP="Private IPs: ";
			String securityGrps="Security Groups: ";
			String VpcID="VPC ID: ";
			String AmiID="AMI ID: ";
			String subnetID="Subnet ID: ";
			String platform="Platform: ";
			String networkInterfaces ="Network Interfaces: ";
			String IamRole="IAM Role: ";
			String SourceDestChck= "Source/dest. check: ";
			String keyPairName="Key Pair Name: ";
			String Owner = "Owner: ";
			String ebsOptyimised ="EBS-optimised: ";
			String launchTime="Launch Time: ";
			String rootDeviceType="Root Device Type: ";
			String rootDevice="Root Device: ";
			String lifeCycle="Life Cycle: ";
			String blockDevice = "Block Devices: ";
			String monitoring="Monitoring: ";
			String kernelId="Kernel ID: ";
			String ramDiskID="RAM Disk ID: ";
			String placementGrp="Placement Group: ";
			String virtualization="Virtualization: ";
			String reservation="Reservation: ";
			String amiLaunchIndex="AMI Launch Index: ";
			String tenancy ="Tenancy: ";
			String stateTransitionReason="State Transition Reason: ";

			Reservation iops = (Reservation)reserv.get(i);		
			ListWithAutoConstructFlag<Instance> instanceTemp =(ListWithAutoConstructFlag<Instance>)iops.getInstances();
			Instance instance = (Instance) instanceTemp.get(0);
			List<InstanceNetworkInterface> networkInterface = instance.getNetworkInterfaces();
			List<String> secondaryPrivateIPs = new ArrayList<String>();
			List<String> networkInterfacesStrings = new ArrayList<String>();
			for(int j=0; j<networkInterface.size(); j++){
				networkInterfacesStrings.add("eth" + j);
				List<InstancePrivateIpAddress> privateIPs = networkInterface.get(j).getPrivateIpAddresses();
				for(int k=0; k<privateIPs.size(); k++){
					secondaryPrivateIPs.add(privateIPs.get(k).getPrivateIpAddress());
				}
			}
			instanceId=instanceId.concat(instance.getInstanceId());
			publicDNSName= publicDNSName.concat(instance.getPublicDnsName());
			if(!instance.getTags().isEmpty()){
				for(Tag t : instance.getTags()){
					if(t.getKey().equalsIgnoreCase("Name")){
						name = "("+t.getValue()+")";
						break;
					}
				}		
			}
			instanceState = instanceState.concat(instance.getState().getName());
			if(instance.getPublicIpAddress()!=null){
				elasticIP = elasticIP.concat(instance.getPublicIpAddress());
			}
			else{
				elasticIP = elasticIP.concat("-");
			}
			instanceType=instanceType.concat(instance.getInstanceType());
			privateDNS=privateDNS.concat(instance.getPrivateDnsName());
			availabityZone = availabityZone.concat(instance.getPlacement().getAvailabilityZone());
			if(instance.getPrivateIpAddress()!=null){
				privateIP=privateIP.concat(instance.getPrivateIpAddress());
			}else{
				privateIP=privateIP.concat("-");
			}
			if(!instance.getSecurityGroups().isEmpty()){
				securityGrps=securityGrps.concat(instance.getSecurityGroups().get(0).getGroupName());
			}else{
				securityGrps=securityGrps.concat("-");
			}
			if(instance.getVpcId()!=null){
				VpcID = VpcID.concat(instance.getVpcId());
			}else{
				VpcID = VpcID.concat("-");
			}
			AmiID=AmiID.concat(instance.getImageId());
			if(instance.getSubnetId()!=null){
				subnetID=subnetID.concat(instance.getSubnetId());
			}else{
				subnetID=subnetID.concat("-");
			}
			if(instance.getPlatform()!=null){
				platform=platform.concat(instance.getPlatform());
			}else{
				platform=platform.concat("-");
			}
			if(!networkInterfacesStrings.isEmpty()){
				networkInterfaces=networkInterfaces.concat(networkInterfacesStrings.toString());
			}else{
				networkInterfaces=networkInterfaces.concat("-");
			}
			if(instance.getIamInstanceProfile()!=null){
				IamRole=IamRole.concat(instance.getIamInstanceProfile().getId());
			}else{
				IamRole=IamRole.concat("-");
			}
			if(instance.getSourceDestCheck()!=null){
				SourceDestChck=SourceDestChck.concat(instance.getSourceDestCheck().toString());
			}else{
				SourceDestChck=SourceDestChck.concat("-");
			}
			keyPairName=keyPairName.concat(instance.getKeyName());
			Owner=Owner.concat(iops.getOwnerId());
			ebsOptyimised=ebsOptyimised.concat(instance.getEbsOptimized().toString());
			launchTime=launchTime.concat(instance.getLaunchTime().toString());
			rootDeviceType=rootDeviceType.concat(instance.getRootDeviceType());
			rootDevice=rootDevice.concat(instance.getRootDeviceName());
			if(instance.getInstanceLifecycle()!=null){
				lifeCycle = lifeCycle.concat(instance.getInstanceLifecycle());
			}
			else{
				lifeCycle = lifeCycle.concat("normal");
			}
			if(!instance.getBlockDeviceMappings().isEmpty()){
				blockDevice=blockDevice.concat(instance.getBlockDeviceMappings().get(0).getDeviceName());
			}else{
				blockDevice=blockDevice.concat("-");
			}
			if(instance.getMonitoring()!=null&&instance.getMonitoring().getState().equalsIgnoreCase("enabled")){
				monitoring=monitoring.concat("advanced");
			}else{
				monitoring=monitoring.concat("basic");
			}
			if(instance.getKernelId()!=null){
				kernelId=kernelId.concat(instance.getKernelId());
			}else{
				kernelId=kernelId.concat("-");	
			}
			if(instance.getRamdiskId()!=null){
				ramDiskID=ramDiskID.concat(instance.getRamdiskId());
			}else{
				ramDiskID=ramDiskID.concat("-");
			}
			placementGrp=placementGrp.concat(instance.getPlacement().getGroupName());
			virtualization=virtualization.concat(instance.getVirtualizationType());
			reservation=reservation.concat(iops.getReservationId());
			amiLaunchIndex=amiLaunchIndex.concat(instance.getAmiLaunchIndex().toString());
			tenancy=tenancy.concat(instance.getPlacement().getTenancy());
			if(instance.getStateReason()!=null){
				stateTransitionReason=stateTransitionReason.concat(instance.getStateReason().getMessage());
			}else{
				stateTransitionReason=stateTransitionReason.concat("-");
			}
			System.out.println("Instance:  "+instance.getInstanceId()+name);
			System.out.println("");
			System.out.println("DESCRIPTION");
			System.out.printf("%-60.60s %-70.70s%n",instanceId,publicDNSName);
			System.out.printf("%-60.60s %-60.60s%n",instanceState,elasticIP);
			System.out.printf("%-60.60s %-60.60s%n",instanceType,privateDNS);
			System.out.printf("%-60.60s %-60.60s%n",availabityZone,privateIP);
			System.out.printf("%-60.60s%n",securityGrps);
			System.out.printf("%-60.60s%n",VpcID);
			System.out.printf("%-60.60s %-60.60s%n",AmiID,subnetID);
			System.out.printf("%-60.60s %-60.60s%n",platform , networkInterfaces);
			System.out.printf("%-60.60s %-60.60s%n",IamRole,SourceDestChck);
			System.out.printf("%-60.60s%n",keyPairName);
			System.out.printf("%-60.60s %-60.60s%n",Owner,ebsOptyimised);
			System.out.printf("%-60.60s %-60.60s%n",launchTime ,rootDeviceType);
			System.out.printf("%-60.60s%n",rootDevice);
			System.out.printf("%-60.60s %-60.60s%n",lifeCycle,blockDevice);
			System.out.printf("%-60.60s%n",monitoring);
			System.out.printf("%-60.60s%n",kernelId);
			System.out.printf("%-60.60s%n",ramDiskID);
			System.out.printf("%-60.60s%n",placementGrp);
			System.out.printf("%-60.60s%n",virtualization);
			System.out.printf("%-60.60s%n",reservation);
			System.out.printf("%-60.60s%n",amiLaunchIndex);
			System.out.printf("%-60.60s%n",tenancy);
			System.out.println(stateTransitionReason);
			System.out.println("");
		}
	}

	public String createAMIfromInstance(AmazonEC2 ec2,String instanceId, String imageName, String imageDescription)
			throws AmazonServiceException, AmazonClientException{
		CreateImageRequest createImageRequest = new CreateImageRequest();
		createImageRequest.withInstanceId(instanceId)
		.withDescription(imageDescription)
		.withName(imageName);
		CreateImageResult result = ec2.createImage(createImageRequest);
		return result.getImageId();
	}

	public AmazonEC2 EC2Initialization() throws IOException {
		InputStream credentialsAsStream = InstanceServices.class.getResourceAsStream("/AwsCredentials.properties");		
		AWSCredentials credentials = new PropertiesCredentials(credentialsAsStream);
		AmazonEC2 ec2 = new AmazonEC2Client(credentials);
		ec2.setEndpoint("https://us-west-2.ec2.amazonaws.com");
		return ec2;
	}
}
