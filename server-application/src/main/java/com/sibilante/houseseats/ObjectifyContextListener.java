package com.sibilante.houseseats;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import com.google.cloud.datastore.DatastoreOptions;
import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.ObjectifyService;
import com.sibilante.houseseats.model.Show;

@WebListener
public class ObjectifyContextListener implements ServletContextListener {

	@Override
	public void contextInitialized(ServletContextEvent sce) {
		ObjectifyService.init(new ObjectifyFactory(
				DatastoreOptions
				.newBuilder()
				.setProjectId("houseseats")
				.build()
				.getService()));
		ObjectifyService.register(Show.class);
	}

	@Override
	public void contextDestroyed(ServletContextEvent sce) {
	}

}