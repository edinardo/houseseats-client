package com.sibilante.houseseats.resource;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.sibilante.houseseats.model.Show;
import com.sibilante.houseseats.service.IgnoreShowInterface;
import com.sibilante.houseseats.service.IgnoreShowService;

@SuppressWarnings("serial")
@WebServlet(name = "HelloAppEngine", urlPatterns = { "/checkNewShows" })
public class CheckNewShowsServlet extends HttpServlet {

	private final String loginURL = "https://lv.houseseats.com/member/index.bv";
	private final String showsURL = "https://lv.houseseats.com/member/ajax/upcoming-shows.bv?sortField=name";
	private static final Logger logger = Logger.getLogger(CheckNewShowsServlet.class.getName());
	private static List<Show> currentShows = new ArrayList<>();
	private IgnoreShowInterface ignoreShow = new IgnoreShowService();
	private final String topic = "shows";
	private final Properties properties = new Properties();
	private Instant lastFindShows;

	@Override
	public void init() throws ServletException {
		super.init();
		// Handle the cookies for the org.jsoup.Connection or URLConnectons
		CookieHandler.setDefault(new CookieManager());
		try {
			InputStream appPropertiesStream = getServletContext().getResourceAsStream("/WEB-INF/app.properties");
			properties.load(appPropertiesStream);
			currentShows = findShows();
			InputStream serviceAccountKey = getServletContext().getResourceAsStream("/WEB-INF/serviceAccountKey.json");
			FirebaseOptions firebaseOptions = new FirebaseOptions.Builder()
					.setCredentials(GoogleCredentials.fromStream(serviceAccountKey))
					.build();
			FirebaseApp.initializeApp(firebaseOptions);			
		} catch (IOException exception) {
			logger.log(Level.SEVERE, exception.getMessage(), exception);
		}
	}

	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
		response.setContentType("text/plain");
		response.setCharacterEncoding("UTF-8");
		List<Show> lastShows;
		try {
			lastShows = findShows();
			List<Show> newShows = new ArrayList<>(lastShows);
			newShows.removeAll(currentShows);
			newShows.removeAll(ignoreShow.findAll());
			if (!newShows.isEmpty()) {
				for (Show newShow : newShows) {
					sendTelegram(newShow);
					sendFirebaseCloudMessage(newShow);
				}
			}
			currentShows = new ArrayList<>(lastShows);
			response.getWriter().print(newShows);
		} catch (Exception exception) {
			logger.log(Level.SEVERE, exception.getMessage(), exception);
		}
	}
	
	/**
	 * Retrieve the list of the currently available shows.
	 * 
	 * Since the session duration is managed on the server side and seems that end after 20 minutes of inactivity,
	 * it will authentic just at the first call, or if the last call was executed more than 20 minutes ago.
	 * 
	 * @return The list of currently available shows.
	 * @throws IOException
	 */
	private ArrayList<Show> findShows() throws IOException {
		ArrayList<Show> showList = new ArrayList<>();
		if (lastFindShows == null || Duration.between(lastFindShows, Instant.now()).getSeconds() > 1200) {
			Jsoup.connect(loginURL)
				.data("submit", "login")
				.data("email", properties.getProperty("houseseats.email"))
				.data("password", properties.getProperty("houseseats.password"))
				.post();
		}
		Document doc = Jsoup.connect(showsURL)
				.get();
		Element showArea = doc.getElementById("grid-view-info");
		Elements showElements = showArea.getElementsByClass("panel-heading");
		for (Element showElement : showElements) {
			showList.add(new Show(Long.parseLong(showElement.select("a").first().attr("href").substring(23)),
					showElement.select("a").first().text()));

		}
		lastFindShows = Instant.now();
		return showList;
	}
	
	private void sendTelegram(Show show) throws IOException {
		String urlString = "https://api.telegram.org/bot%s/sendMessage?chat_id=%s&text=%s&parse_mode=%s";
		String text = URLEncoder.encode(
				"[" + show.getName() + "](https://lv.houseseats.com/member/tickets/view/?showid=" + show.getId() + ")",
				"UTF-8");
		String parse_mode = "markdown";
		urlString = String.format(urlString,
				properties.getProperty("telegram.api.token"),
				properties.getProperty("telegram.chat.id"), text, parse_mode);

		URL url = new URL(urlString);
		URLConnection conn = url.openConnection();

		StringBuilder sb = new StringBuilder();
		InputStream is = new BufferedInputStream(conn.getInputStream());
		BufferedReader br = new BufferedReader(new InputStreamReader(is));
		String inputLine = "";
		while ((inputLine = br.readLine()) != null) {
			sb.append(inputLine);
		}
	}
	
	private String sendFirebaseCloudMessage(Show show) throws FirebaseMessagingException {
		Message message = Message.builder()
				.setAndroidConfig(AndroidConfig.builder()
						.setPriority(AndroidConfig.Priority.HIGH)
						.build())
				.putData("id", Long.toString(show.getId()))
			    .putData("name", show.getName())
			    .setTopic(topic)
			    .build();
		return FirebaseMessaging.getInstance().send(message);
	}

}