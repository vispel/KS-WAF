package com.ks.pojo.interfaces;

import com.ks.exceptions.ClientIpDeterminationException;

import javax.servlet.http.HttpServletRequest;

public  interface ClientIpDeterminator
		extends Configurable {
	String determineClientIp(HttpServletRequest paramHttpServletRequest)
			throws ClientIpDeterminationException;
}

