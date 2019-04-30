package com.sibilante.houseseats;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

import com.sibilante.houseseats.resource.IgnoreShowResource;

@ApplicationPath("/rest")
public class RESTApplication extends Application {

	public RESTApplication() {
		super();
	}

	@Override
	public Set<Class<?>> getClasses() {
		final Set<Class<?>> resources = new HashSet<Class<?>>( );
        resources.add(IgnoreShowResource.class);
        return resources;
	}

}
