package com.ceit.admin.service;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ceit.jdbc.SimpleJDBC;

public class UserService {

	public static final ThreadLocal<String> clientUserId = new ThreadLocal<>();
	public static final ThreadLocal<String> clientUserAccount = new ThreadLocal<>();
	public static final ThreadLocal<String> clientUserName = new ThreadLocal<>();
	public static final ThreadLocal<String> clientUserIP = new ThreadLocal<>();
	
	private static final Logger logger=LoggerFactory.getLogger(UserService.class);
	
	public static String getCurrentUserId()
	{
		return clientUserId.get();
	}
	
	public static String getCurrentUserAccount()
	{
		return clientUserAccount.get();
	}
	
	public static String getCurrentUserName()
	{
		return clientUserName.get();
	}
	
	public static String getCurrentUserIp()
	{
		return clientUserIP.get();
	}

}
