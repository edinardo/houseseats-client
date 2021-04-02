package com.sibilante.houseseats.service;

import java.util.List;

import com.sibilante.houseseats.model.Show;

public interface IgnoreShowInterface {

	public List<Show> findAll();

	public void create(Show show);

	public void update(long id, String name);

	public void delete(long id);

}
