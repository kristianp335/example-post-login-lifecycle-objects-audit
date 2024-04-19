package com.liferay.kris.post.login.portlet;

import com.liferay.portal.kernel.audit.AuditMessage;
import com.liferay.portal.kernel.audit.AuditRouter;
import com.liferay.portal.kernel.events.ActionException;
import com.liferay.portal.kernel.events.LifecycleAction;
import com.liferay.portal.kernel.events.LifecycleEvent;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.security.auth.AuthTokenUtil;
import com.liferay.portal.kernel.service.UserLocalService;
import com.liferay.portal.kernel.util.PortalUtil;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * @author kpatefield
 */

@Component(
		immediate = true, 
		property = {"key=login.events.post"},
	    service = LifecycleAction.class
)

public class PostLoginExamample implements LifecycleAction {
	
	@Reference
	UserLocalService userlocalService;
	
	@Reference 
	AuditRouter auditRouter;
	
	
	 @Override
	 public void processLifecycleEvent(LifecycleEvent lifecycleEvent) throws ActionException {
		 System.out.println("The login event post action is running");
		 HttpServletRequest request = lifecycleEvent.getRequest();		 
		 String sessionId = request.getSession().getId();
		 System.out.println(sessionId);
		 String csrfToken = AuthTokenUtil.getToken(request);
		 
		 User user;
		 try {
			user = PortalUtil.getUser(request);
			System.out.println("The user who just logged in was " + user.getFullName());
			
			
			
			String cdpEndpoint = "https://webserver-lctaffinity2-prd.lfr.cloud/o/c/cdpdatas/";			
			
			Client client = ClientBuilder.newClient();
		    WebTarget target = client.
		            target(cdpEndpoint);
		 	Invocation.Builder invocationBuilder = 
		    		target.request(MediaType.APPLICATION_JSON).header("x-csrf-token", csrfToken).header("Cookie", "JSESSIONID=" + sessionId);
		    System.out.println(invocationBuilder.toString());
		    
		    JSONObject cdpJSON = JSONFactoryUtil.createJSONObject();
		    cdpJSON.put("firstName", user.getFirstName());
		    cdpJSON.put("surname", user.getLastName());
		    cdpJSON.put("email", user.getEmailAddress());
		    cdpJSON.put("authType", "");	
		    
		    Entity<String> entity = Entity.json(cdpJSON.toJSONString());
		    Response response = invocationBuilder.post(entity);
		    
		    System.out.println(response.getStatus());
		    System.out.println(response.readEntity(String.class));
		    System.out.println(response.getHeaders().toString());
		    
		    String newsletterEndpoint = "https://webserver-lctaffinity2-prd.lfr.cloud/o/c/newsletters/";
		    
		    Client newsletterClient = ClientBuilder.newClient();
		    WebTarget newsletterTarget = newsletterClient.
		            target(newsletterEndpoint);
		    Invocation.Builder newsletterInvocationBuilder = 
		    		newsletterTarget.request(MediaType.APPLICATION_JSON).header("x-csrf-token", csrfToken).header("Cookie", "JSESSIONID=" + sessionId);
		    
		    JSONObject newsletterJSON = JSONFactoryUtil.createJSONObject();
		    newsletterJSON.put("firstname", user.getFirstName());
		    newsletterJSON.put("surname", user.getLastName());
		    newsletterJSON.put("email", user.getEmailAddress());
		    newsletterJSON.put("frequency", "Daily");	
		    newsletterJSON.put("campaign", "");	
		    newsletterJSON.put("primaryInterest", "unknown");	
		    
		    Entity<String> newsletterEntity = Entity.json(newsletterJSON.toJSONString());
		    Response newsletterResponse = newsletterInvocationBuilder.post(newsletterEntity);
		    
		    System.out.println(newsletterResponse.getStatus());
		    System.out.println(newsletterResponse.readEntity(String.class));
		    System.out.println(newsletterResponse.getHeaders().toString());
		    
		    AuditMessage auditMessage = new AuditMessage(
					"Post Login Liferaycycle Event - Sync", user.getCompanyId(), user.getUserId(), user.getFullName(), user.getClass().getName(), 
					String.valueOf(user.getPrimaryKey()), null, newsletterJSON);
			auditRouter.route(auditMessage);
		    
		    
			
		} catch (PortalException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		 
	 }
	 
}