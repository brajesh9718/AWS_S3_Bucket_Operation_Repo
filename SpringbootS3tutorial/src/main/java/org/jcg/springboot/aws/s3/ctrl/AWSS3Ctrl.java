package org.jcg.springboot.aws.s3.ctrl;

import javax.servlet.http.HttpServletResponse;

import org.jcg.springboot.aws.s3.serv.AWSS3Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

@Controller
@RequestMapping(value= "/s3")
public class AWSS3Ctrl {

	@Autowired
	private AWSS3Service service;
	
	@GetMapping(value = "/sayHello")
	public String sayHello() {
		return "welcome";
	}

	@PostMapping(value= "/upload")
	@ResponseBody
	public ResponseEntity<String> uploadFile(@RequestPart(value= "file") final MultipartFile multipartFile) {
		service.uploadFile(multipartFile);
		final String response = "[" + multipartFile.getOriginalFilename() + "] uploaded successfully.";
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	@GetMapping(value= "/download")
	@ResponseBody
	public byte[] downloadFile(HttpServletResponse response,String testName) {
		//setting headers
        response.setContentType("application/zip");
        response.setStatus(HttpServletResponse.SC_OK);
        response.addHeader("Content-Disposition", "attachment; filename=\"ZipFile.zip\"");
		System.out.println("Inside downloadFile ....");
		//final ByteArrayResource resource = new ByteArrayResource(data);
		return service.downloadFile();
		 
	}
	
	@DeleteMapping(value= "/delete")
	@ResponseBody
	public ResponseEntity<String> deleteFile(@RequestParam(value= "fileName") final String keyName) {
		service.deleteFile(keyName);
		final String response = "[" + keyName + "] deleted successfully.";
		return new ResponseEntity<>(response, HttpStatus.OK);
	}
}
