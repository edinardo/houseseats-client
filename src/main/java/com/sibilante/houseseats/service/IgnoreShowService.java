package com.sibilante.houseseats.service;

import static com.googlecode.objectify.ObjectifyService.ofy;

import java.util.List;

import com.sibilante.houseseats.model.Show;

public class IgnoreShowService implements IgnoreShowInterface {

	@Override
	public List<Show> findAll() {
		return ofy().load().type(Show.class).list();
	}

	@Override
	public void create(Show show) {
		ofy().save().entities(show);
	}

	@Override
	public void update(long id, String name) {
		// TODO Auto-generated method stub

	}

	@Override
	public void delete(long id) {
		ofy().delete().type(Show.class).id(id);
	}

}
