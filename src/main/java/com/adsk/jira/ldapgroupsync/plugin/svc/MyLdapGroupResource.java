/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.adsk.jira.ldapgroupsync.plugin.svc;

import com.atlassian.jira.permission.GlobalPermissionKey;
import com.atlassian.jira.security.GlobalPermissionManager;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.user.ApplicationUser;
import com.adsk.jira.ldapgroupsync.plugin.model.LdapGroupSyncBean;
import com.adsk.jira.ldapgroupsync.plugin.model.MessageBean;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.log4j.Logger;

/**
 *
 * @author prasadve
 */
@Path("/run")
public class MyLdapGroupResource {
    
    private static final Logger LOGGER = Logger.getLogger(MyLdapGroupResource.class);
    private final JiraAuthenticationContext jiraAuthenticationContext;
    private GlobalPermissionManager permissionManager = null;
    
    public MyLdapGroupResource(JiraAuthenticationContext jiraAuthenticationContext, GlobalPermissionManager permissionManager) {
        this.jiraAuthenticationContext = jiraAuthenticationContext;
        this.permissionManager = permissionManager;
    }
    
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/sync")
    public Response runLdapGroupSync(LdapGroupSyncBean syncBean) {
            
        long startTime = System.currentTimeMillis();
        
        //message object
        MessageBean messageBean = new MessageBean();
                
        ApplicationUser loggedInAppUser = jiraAuthenticationContext.getLoggedInUser();
        
        LOGGER.debug("["+loggedInAppUser.getUsername() +"] ["+ syncBean.getLdapGroup() +":"+ syncBean.getJiraGroup() +"] Started.");
        
        //check permissions
        if( permissionManager.hasPermission(GlobalPermissionKey.ADMINISTER, loggedInAppUser) == false ) {
            messageBean.setMessage("[Error] Permission denied. System admin access is required.");
            return Response.status(Response.Status.FORBIDDEN).entity(messageBean).build();
        }
        
        //check required paramaters
        if( syncBean.getLdapGroup() == null || "".equals(syncBean.getLdapGroup()) 
                || syncBean.getJiraGroup() == null || "".equals(syncBean.getJiraGroup()) ) {
            messageBean.setMessage("[Error] Required fields are missing.");
            return Response.status(Response.Status.BAD_REQUEST).entity(messageBean).build();
        }                
        
        MyLdapGroupSyncDAO syncDAO = MyLdapGroupSyncDAO.getInstance();
        MessageBean result = syncDAO.sync(syncBean.getLdapGroup(), syncBean.getJiraGroup());
        
        long totalTime = System.currentTimeMillis() - startTime;
        
        LOGGER.debug("["+loggedInAppUser.getUsername()+"] ["+ syncBean.getLdapGroup() +":"+ 
                syncBean.getJiraGroup() +"] "+ result.getMessage() +". Took "+ totalTime/ 1000d +" Seconds");
        
        result.setMessage(result.getMessage() +". Took "+ totalTime/ 1000d +" Seconds");
        return Response.ok(result).build();
    }
}
