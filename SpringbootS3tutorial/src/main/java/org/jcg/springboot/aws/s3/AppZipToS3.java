package org.jcg.springboot.aws.s3;

import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.iterable.S3Objects;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.util.IOUtils;

public class AppZipToS3 {
	
	private static AWSCredentials credentials = null;
	private static AmazonS3 client = null;
	// S3 Bucket
	private static String bucketName = "myawsbucketfor-pdf";
	
	static {
		credentials = new BasicAWSCredentials("AKIA2JGGQ6IRD5CDPZ45", "dYnK8Crq06vhJSukpeBismdQCPfIBPyIoegq16hx");
		client = new AmazonS3Client(credentials);
	}
	
	public static void main(String[] args) throws Exception {

		final PipedOutputStream pipedOutputStream = new PipedOutputStream();
		final PipedInputStream pipedInputStream = new PipedInputStream(pipedOutputStream);
		final Thread s3In = new Thread(() -> {
			try (final ZipOutputStream zipOutputStream = new ZipOutputStream(pipedOutputStream)) {
				S3Objects.inBucket(client, bucketName).forEach((S3ObjectSummary objectSummary) -> {
							try {
								if (objectSummary.getKey().endsWith(".pdf")) {
									System.out.println("Processing " + objectSummary.getKey());
									final ZipEntry entry = new ZipEntry(objectSummary.getKey());
									zipOutputStream.putNextEntry(entry);
									IOUtils.copy(client.getObject(
													objectSummary.getBucketName(),
													objectSummary.getKey()
											).getObjectContent(),
											zipOutputStream
									);
									zipOutputStream.closeEntry();
								}
							} catch (final Exception all) {
								all.printStackTrace();
							}
						});
			} catch (final Exception all) {
				all.printStackTrace();
			}
		});
		final Thread s3Out = new Thread(() -> {
			try {
				client.putObject("myawsbucketfor-pdf","previews.zip",pipedInputStream,new ObjectMetadata());
				pipedInputStream.close();
			} catch (final Exception all) {
				all.printStackTrace();
			}
		});
		s3In.start();
		s3Out.start();
		s3In.join();
		s3Out.join();
		
		
		
		
		
		
	}
	
	
	
	
	
	
}
