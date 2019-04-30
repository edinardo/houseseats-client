package com.sibilante.houseseats.resource;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.HttpsURLConnection;
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
	private HttpsURLConnection httpsURLConnection;
	private static final Logger logger = Logger.getLogger(CheckNewShowsServlet.class.getName());
	private List<String> cookies;
	private static List<Show> currentShows = new ArrayList<>();
	private IgnoreShowInterface ignoreShow = new IgnoreShowService();
	private final String topic = "shows";
	private final Properties properties = new Properties();

	@Override
	public void init() throws ServletException {
		super.init();
		CookieHandler.setDefault(new CookieManager());
		try {
			InputStream appPropertiesStream = getServletContext().getResourceAsStream("/WEB-INF/app.properties");
			properties.load(appPropertiesStream);
			currentShows = findShows(showsURL); 
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
		CookieHandler.setDefault(new CookieManager());
		response.setContentType("text/plain");
		response.setCharacterEncoding("UTF-8");
		List<Show> lastShows;
		try {
			lastShows = findShows(showsURL);
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

	private ArrayList<Show> findShows(String url) throws UnsupportedEncodingException {
		ArrayList<Show> showList = new ArrayList<>();
		while (showList.isEmpty()) {
			Document doc = Jsoup.parse(getPageContent(url));
			Element showArea = doc.getElementById("grid-view-info");
			Elements showElements = showArea.getElementsByClass("panel-heading");

			for (Element showElement : showElements) {
				showList.add(new Show(Long.parseLong(showElement.select("a").first().attr("href").substring(23)),
									  showElement.select("a").first().text()));
			}
		}
		return showList;
	}

	private String getPageContent(String url) {

		StringBuilder response = null;


		try {
			login();

			URL obj = new URL(url);
			httpsURLConnection = (HttpsURLConnection) obj.openConnection();
			httpsURLConnection.setReadTimeout(10000 /* milliseconds */);
			httpsURLConnection.setConnectTimeout(15000 /* milliseconds */);

			// act like a browser
			httpsURLConnection.setRequestProperty("User-Agent", "Mozilla/5.0");
			httpsURLConnection.setRequestProperty("Accept",
					"text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
			httpsURLConnection.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
			if (cookies != null) {
				for (String cookie : this.cookies) {
					httpsURLConnection.addRequestProperty("Cookie", cookie.split(";", 1)[0]);
				}
			}

			BufferedReader in = new BufferedReader(new InputStreamReader(httpsURLConnection.getInputStream()));
			String inputLine;
			response = new StringBuilder();

			while ((inputLine = in.readLine()) != null) {
				response.append(inputLine);
			}
			in.close();
		} catch (IOException exception) {
			logger.log(Level.SEVERE, exception.getMessage(), exception);
		}

		return response != null ? response.toString() : null;
	}

	private int login() {
		int response = -1;
		try {
			URL url = new URL(loginURL);
			httpsURLConnection = (HttpsURLConnection) url.openConnection();
			httpsURLConnection.setReadTimeout(5000);
			httpsURLConnection.setConnectTimeout(10000);
			httpsURLConnection.setRequestMethod("POST");
			httpsURLConnection.setDoOutput(true);
			httpsURLConnection.setDoInput(true);

			DataOutputStream dataOutputStream = new DataOutputStream(httpsURLConnection.getOutputStream());
			String postParams = "submit=login&email="
					+ URLEncoder.encode(properties.getProperty("houseseats.email"), "UTF-8") + "&password="
					+ URLEncoder.encode(properties.getProperty("houseseats.password"), "UTF-8");
			dataOutputStream.writeBytes(postParams);
			dataOutputStream.flush();
			dataOutputStream.close();

			response = httpsURLConnection.getResponseCode();
		} catch (IOException exception) {
			logger.log(Level.SEVERE, exception.getMessage(), exception);
		}
		return response;
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