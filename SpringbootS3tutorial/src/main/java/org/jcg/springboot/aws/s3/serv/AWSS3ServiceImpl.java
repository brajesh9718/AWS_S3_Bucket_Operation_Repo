package org.jcg.springboot.aws.s3.serv;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.ListObjectsV2Request;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.util.IOUtils;

@Service
public class AWSS3ServiceImpl implements AWSS3Service {

	private static final Logger LOGGER = LoggerFactory.getLogger(AWSS3ServiceImpl.class);

	@Autowired
	private AmazonS3 amazonS3;
	@Value("${aws.s3.bucket}")
	private String bucketName;

	// @Async annotation ensures that the method is executed in a different
	// background thread
	// but not consume the main thread.
	@Async
	public void uploadFile(final MultipartFile multipartFile) {
		LOGGER.info("File upload in progress.");
		try {
			final File file = convertMultiPartFileToFile(multipartFile);
			uploadFileToS3Bucket(bucketName, file);
			LOGGER.info("File upload is completed.");
			file.delete(); // To remove the file locally created in the project folder.
		} catch (final AmazonServiceException ex) {
			LOGGER.info("File upload is failed.");
			LOGGER.error("Error= {} while uploading file.", ex.getMessage());
		}
	}

	private File convertMultiPartFileToFile(final MultipartFile multipartFile) {
		final File file = new File(multipartFile.getOriginalFilename());
		try (final FileOutputStream outputStream = new FileOutputStream(file)) {
			outputStream.write(multipartFile.getBytes());
		} catch (final IOException ex) {
			LOGGER.error("Error converting the multi-part file to file= ", ex.getMessage());
		}
		return file;
	}

	private void uploadFileToS3Bucket(final String bucketName, final File file) {
		final String uniqueFileName = LocalDateTime.now() + "_" + file.getName();
		LOGGER.info("Uploading file with name= " + uniqueFileName);
		final PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, uniqueFileName, file);
		amazonS3.putObject(putObjectRequest);
	}

	// @Async annotation ensures that the method is executed in a different
	// background thread
	// but not consume the main thread.
	@Async
	public byte[] downloadFile() {
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		try {
			ListObjectsV2Request req = new ListObjectsV2Request().withBucketName(bucketName);
			ListObjectsV2Result listObjectsResult = amazonS3.listObjectsV2(req);
			System.out.println("Size :: " + listObjectsResult.getObjectSummaries().size());
			//creating byteArray stream, make it bufforable and passing this buffor to ZipOutputStream	        
	        BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(byteArrayOutputStream);
	        ZipOutputStream zipOutputStream = new ZipOutputStream(bufferedOutputStream);
			for (int i = 0; i < listObjectsResult.getObjectSummaries().size(); i++) {
				String key = listObjectsResult.getObjectSummaries().get(i).getKey();
				System.out.printf(" - %s (size: %d)\n", key, listObjectsResult.getObjectSummaries().get(i).getSize());
				final S3Object s3Object = amazonS3.getObject(bucketName, key);
				// only try to read pdf files
				if (!key.contains(".pdf")) {
					continue;
				}
	            zipOutputStream.putNextEntry(new ZipEntry(key));
				InputStream stream = new BufferedInputStream(s3Object.getObjectContent());
				IOUtils.copy(stream, zipOutputStream);
				LOGGER.info("File downloaded successfully.");
	            zipOutputStream.closeEntry();
				stream.close();
			}
			zipOutputStream.close();			
		} catch (final IOException ex) {
			LOGGER.info("IO Error Message= " + ex.getMessage());
		}
		return byteArrayOutputStream.toByteArray();
	}

	// @Async annotation ensures that the method is executed in a different
	// background thread
	// but not consume the main thread.
	@Async
	public void deleteFile(final String keyName) {
		LOGGER.info("Deleting file with name= " + keyName);
		final DeleteObjectRequest deleteObjectRequest = new DeleteObjectRequest(bucketName, keyName);
		amazonS3.deleteObject(deleteObjectRequest);
		LOGGER.info("File deleted successfully.");
	}
}
