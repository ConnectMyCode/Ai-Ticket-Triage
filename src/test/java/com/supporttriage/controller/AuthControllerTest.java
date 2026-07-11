package com.supporttriage.controller;
import org.junit.jupiter.api.Test; 
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import org.springframework.http.MediaType;
import com.supporttriage.dto.LoginRequest;



import com.fasterxml.jackson.databind.ObjectMapper;
import com.supporttriage.dto.SignupRequest;


@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class AuthControllerTest {

	@Autowired
	private MockMvc mockMvc;
	
	@Autowired
	private ObjectMapper objectMapper;
		
	@Test
	void successfullSignup() throws Exception
	{
		SignupRequest request = new SignupRequest();
		
		request.setEmail("test@example.com");	
		request.setPassword("password123");
		
		mockMvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON)
	            .content(objectMapper.writeValueAsString(request)))
	            .andExpect(status().isOk());
	
	}
	
	@Test
	void duplicateEmailSignup() throws Exception
	{
		SignupRequest request = new SignupRequest();
		
		request.setEmail("duplicate@example.com");
		request.setPassword("password123");  
		
	    // First signup should succeed

		 mockMvc.perform(post("/api/auth/signup")
		            .contentType(MediaType.APPLICATION_JSON)
		            .content(objectMapper.writeValueAsString(request)))
		            .andExpect(status().isOk());
	
		 
		// Second signup with the same email should fail
		    mockMvc.perform(post("/api/auth/signup")
		            .contentType(MediaType.APPLICATION_JSON)
		            .content(objectMapper.writeValueAsString(request)))
		            .andExpect(status().isBadRequest())
		    		.andExpect(jsonPath("$.message").value("Email already exists"));
	}
	
	
	@Test 
	void successfullLogin() throws Exception {
	
		    // Step 1: Register a user

		    SignupRequest signupRequest = new SignupRequest();
		    signupRequest.setEmail("login@example.com");
		    signupRequest.setPassword("password123");

		    mockMvc.perform(post("/api/auth/signup")
		            .contentType(MediaType.APPLICATION_JSON)
		            .content(objectMapper.writeValueAsString(signupRequest)))
		            .andExpect(status().isOk());

		    // Step 2: Login with same credentials

		    LoginRequest loginRequest = new LoginRequest();
		    loginRequest.setEmail("login@example.com");
		    loginRequest.setPassword("password123");

		    mockMvc.perform(post("/api/auth/login")
		            .contentType(MediaType.APPLICATION_JSON)
		            .content(objectMapper.writeValueAsString(loginRequest)))
		            .andExpect(status().isOk())
		            .andExpect(jsonPath("$.token").exists())
		            .andExpect(jsonPath("$.token").isNotEmpty());
		}
	
	
		@Test
		void wrongPasswordLogin() throws Exception
		{
			
			//Step 1: Register a user
			 SignupRequest signupRequest = new SignupRequest();
			    signupRequest.setEmail("wrongpassword@example.com");
			    signupRequest.setPassword("password123");
			  //This is automated Testing so I am setting up the Email and password  
			//The Login Credentials created here are saved into H2 Database and Not the Real Db this is ensured By the MockMvc and Mockito libraries
			//All the beans created here are fake beans/object
			    // And the server is also an dummy server.. 
			    
			mockMvc.perform(post("/api/auth/signup")
			            .contentType(MediaType.APPLICATION_JSON)
			            .content(objectMapper.writeValueAsString(signupRequest)))
			            .andExpect(status().isOk());

			
			  // Step 2: Attempt login with wrong password
		    LoginRequest loginRequest = new LoginRequest();
		    loginRequest.setEmail("wrongpassword@example.com");
		    loginRequest.setPassword("wrongPassword");

		    mockMvc.perform(post("/api/auth/login")
		            .contentType(MediaType.APPLICATION_JSON)
		            .content(objectMapper.writeValueAsString(loginRequest)))
		            .andExpect(status().isBadRequest())
		            .andExpect(jsonPath("$.message").value("Invalid credentials"));			
		}
	
	
		//Short password test case is handled by AuthRequest.java by using Annotation like @Value(min = 8)  so framework Is handling the Verification  of the length of password...
	
		
		
		@Test
		void invalidEmailSignup() throws Exception {

		    SignupRequest request = new SignupRequest();
		    request.setEmail("invalid-email");
		    request.setPassword("password123");

		    mockMvc.perform(post("/api/auth/signup")
		            .contentType(MediaType.APPLICATION_JSON)
		            .content(objectMapper.writeValueAsString(request)))
		            .andExpect(status().isBadRequest());
		}
		
		
	
	@Test
	void contextLoads() 
	{
		
	}
	
	
}





