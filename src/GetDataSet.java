
import twitter4j.HashtagEntity;
import twitter4j.IDs;
import twitter4j.Paging;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.UserMentionEntity;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;

import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

public class GetDataSet {
	private static final int TRACE_DEPTH = 2;
	private static final String DATA_FOLDER = "data/";
	private static final int FILE_EGOFEAT = 0;
	private static final int FILE_FEAT = 1;
	private static final int FILE_FEATNAMES = 2;
	private static final int FILE_INEDGE = 3;
	private static final int FILE_OUTEDGE = 4;
	private static final String[] FILE_FILENAMES = {".egofeat.txt", ".feat.txt", ".featnames.txt", ".inedge.txt", ".outedge.txt"};
	private static int PARSE_COUNT = 0;
	
	public static void main(String[] args) throws TwitterException, InterruptedException {

		//getAccessToken("WQK8kHZNlycFb6gDkK2d4g", "CzCIwjJGXwQEP3BSiJ9BRw8js4km2C6jXO82mwJ4WY");
		//Thread.sleep(1000);


		Twitter twitter = new TwitterFactory().getInstance();
		long ID = 0;
		//String user;
		
		ArrayList<Long> allIds = new ArrayList<Long>();
		
		try {
			File f = new File("IDsEdgesOK.txt");
			if(!f.exists()) System.out.println("Need start!");
			BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(f)));
			
			String strLine = br.readLine();
			// Print the content on the console
			System.out.println (strLine);
			
			String[] tokens = strLine.split("\t");
			if(tokens.length != 2){
				System.out.println("Unknown line format.");
			}
			if(Integer.parseInt(tokens[1]) == 1){
				System.out.println(tokens[0] + " has been collected.");
			}
			ID = Long.parseLong(tokens[0]);
			//user = twitter.showUser(ID).getScreenName();
			
			
			collectEdges(twitter, ID, allIds);	//generate files, write to idlist files, add to arraylist
			
			
			for(long id : allIds) {
				try {
					if(isFileExist(id, FILE_OUTEDGE) == null) { //not generated yet
						collectEdges(twitter, id, null);	//generate, write to idlist file (only big)
					}
					else {
						System.out.println("File already exists");
					}
				} catch(TwitterException te) {
					if(te.exceededRateLimitation()) {
						te.printStackTrace();
						Thread.sleep(te.getRateLimitStatus().getSecondsUntilReset()*1000);
					}
				} 
			}
			
			br.close();
			
			//user = "devil1437"; ID = 2260179674;
			//ID = twitter.showUser(user).getId();
			
			//ID = 0;
			//user = twitter.showUser(ID).getScreenName();

		} catch (TwitterException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (NumberFormatException e) {
			e.printStackTrace();
		}
		
		System.out.println("Finish!");
	}
	
	public static File isFileExist(long ID, int type){
		File file = new File(DATA_FOLDER + ID + FILE_FILENAMES[type]);
		if(file.exists())	return file;
		else return null;
	}
	
	
	public static void collectEdges(Twitter twitter, long ID, ArrayList<Long> allIds) throws InterruptedException, TwitterException, IOException{
		System.out.println("Collecting edges.." + ID + ", " + PARSE_COUNT++);
		
		File smallF = new File("EdgesLevel1.txt");
		File bigF = new File("EdgesLevel2.txt");
		BufferedWriter bw1 = new BufferedWriter(new FileWriter(smallF, true));
		BufferedWriter bw2 = new BufferedWriter(new FileWriter(bigF, true));
		
		
		//ArrayList<Long> followers = new ArrayList<Long>();
		ArrayList<Long> following = new ArrayList<Long>();
		//IDs followersIDs = twitter.getFollowersIDs(ID, -1);
		IDs followingIDs = twitter.getFriendsIDs(ID, -1);
		
		Map<UserMentionEntity, Feature> mentions = new HashMap<UserMentionEntity, Feature>();
		for (Status status : twitter.getUserTimeline(ID, new Paging(1, 100))) {
			for(UserMentionEntity me: status.getUserMentionEntities()) {
				if(mentions.containsKey(me)) {
					Feature mf = mentions.get(me);
					mf.inReplyToIds.add(status.getInReplyToUserId());
					mf.inReplyToNames.add(status.getInReplyToScreenName());
				}
				else {
					Feature mf = new Feature(status.getInReplyToScreenName(), status.getInReplyToUserId());
					mentions.put(me, mf);
				}
			}
		}
		
		//for(long id : followersIDs.getIDs()) {
		//	followers.add(id);
		//}
		for(UserMentionEntity m : mentions.keySet()) {
			following.add(m.getId());
		}
		for(long id : followingIDs.getIDs()) {
			following.add(id);
		}
		//inEdge(ID, followers);
		outEdge(ID, following);
		/*
		for(long id : followers) {
			if(allIds != null) {
				allIds.add(id);
				bw1.write(id + "\t0\n");
			}
			bw2.write(id + "\t0\n");
		}
		*/
		for(long id : following) {
			if(allIds != null) {
				allIds.add(id);
				bw1.write(id + "\t0\n");
			}
			bw2.write(id + "\t0\n");
		}
		
		bw1.flush();
		bw1.close();
		bw2.flush();
		bw2.close();
		//Thread.sleep(5000);
	}
	
	
/*
	public static void collectData(Twitter twitter, long ID, String user) throws InterruptedException{
		System.out.println("Collecting.." + ID + ", " + user + ", " + PARSE_COUNT++);

		List<Status> statuses = null;

		Map<HashtagEntity, Feature> hashtags = new HashMap<HashtagEntity, Feature>();
		Map<UserMentionEntity, Feature> mentions = new HashMap<UserMentionEntity, Feature>();
		IDs followers = null;
		IDs following = null;

		try {
			statuses = twitter.getUserTimeline(user, new Paging(1, 100));
			followers = twitter.getFollowersIDs(user, -1);
			following = twitter.getFriendsIDs(user, -1);
		} catch (TwitterException te) {
			te.printStackTrace();
			System.out.println("Failed to get timeline: " + te.getMessage());
			return;
		}


		System.out.println("Showing @" + user + "'s user timeline.");
		for (Status status : statuses) {
			System.out.println("@" + status.getUser().getScreenName() + " - " + status.getText());
			System.out.print("In whose timeline: " + (status.getInReplyToScreenName()==null?"mine":status.getInReplyToScreenName()));
			System.out.print("\n#: ");
			for(HashtagEntity he : status.getHashtagEntities()) {
				System.out.print(he.getText() + "; ");
				if(hashtags.containsKey(he)) {
					Feature hf = hashtags.get(he);
					hf.inReplyToIds.add(status.getInReplyToUserId());
					hf.inReplyToNames.add(status.getInReplyToScreenName());
				}
				else {
					Feature hf = new Feature(status.getInReplyToScreenName(), status.getInReplyToUserId());
					hashtags.put(he, hf);
				}

			}
			System.out.print("\n@: ");
			for(UserMentionEntity me: status.getUserMentionEntities()) {
				System.out.print(me.getText() + "; ");
				if(mentions.containsKey(me)) {
					Feature mf = mentions.get(me);
					mf.inReplyToIds.add(status.getInReplyToUserId());
					mf.inReplyToNames.add(status.getInReplyToScreenName());
				}
				else {
					Feature mf = new Feature(status.getInReplyToScreenName(), status.getInReplyToUserId());
					mentions.put(me, mf);
				}
			}
			System.out.println("\n");
		}


		//Generate files

		featNames(ID, hashtags, mentions);

		egoFeat(ID, hashtags, mentions);
		feat(ID, hashtags, mentions);

		inEdge(ID, followers);
		outEdge(ID, following);
		
		Thread.sleep(5000);
	}
	
	public static void parseEdge(Twitter twitter, long ID, String user, int depth) throws TwitterException, NumberFormatException, IOException, InterruptedException{
		System.out.println("Searching.." + ID + ", " + user + ", " + depth);
		
		if(depth < TRACE_DEPTH){
			File file;
			if((file = isFileExist(ID, FILE_OUTEDGE)) != null){
				FileInputStream fstream = new FileInputStream(file);
				DataInputStream in = new DataInputStream(fstream);
				BufferedReader br = new BufferedReader(new InputStreamReader(in));
				String strLine;
				while ((strLine = br.readLine()) != null)   {
					long tempID = Long.parseLong(strLine);
					String tempUser = twitter.showUser(tempID).getScreenName();
					parseEdge(twitter, tempID, tempUser, depth+1);
				}
				br.close();
			}
			else{
				collectData(twitter, ID, user);
				List<User> following = twitter.getFriendsList(user, -1);
				
				for(User u : following) {
					parseEdge(twitter, u.getId(), u.getScreenName(), depth+1);
				}
			}
			
			if((file = isFileExist(ID, FILE_INEDGE)) != null){
				FileInputStream fstream = new FileInputStream(file);
				DataInputStream in = new DataInputStream(fstream);
				BufferedReader br = new BufferedReader(new InputStreamReader(in));
				String strLine;
				while ((strLine = br.readLine()) != null)   {
					long tempID = Long.parseLong(strLine);
					String tempUser = twitter.showUser(tempID).getScreenName();
					parseEdge(twitter, tempID, tempUser, depth+1);
				}
				br.close();
			}
			else{
				collectData(twitter, ID, user);
				List<User> followers = twitter.getFollowersList(user, -1);
				
				for(User u : followers) {
					parseEdge(twitter, u.getId(), u.getScreenName(), depth+1);
				}
			}
		}
		else if(depth == TRACE_DEPTH){
			File file;
			if((file = isFileExist(ID, FILE_OUTEDGE)) == null){
				collectData(twitter, ID, user);
			}
			
			if((file = isFileExist(ID, FILE_INEDGE)) == null){
				collectData(twitter, ID, user);
			}		
		}
		else{
			return;
		}	
//		Thread.sleep(5000);
	}
*/
	public static void featNames(long ID, Map<HashtagEntity, Feature> hashtags, Map<UserMentionEntity, Feature>mentions) {
		Writer w = null;
		try {
			w = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(DATA_FOLDER + String.valueOf(ID) + FILE_FILENAMES[FILE_FEATNAMES]), "utf-8"));
			for(HashtagEntity h : hashtags.keySet()) {
				w.write("#" + h.getText() + "\n");
			}
			for(UserMentionEntity m: mentions.keySet()) {
				w.write("@" + m.getText() + "\n");
			}
		} catch (IOException ex) {
		} finally {
			try {w.close();} catch (Exception ex) {}
		}
	}

	public static void egoFeat(long ID, Map<HashtagEntity, Feature> hashtags, Map<UserMentionEntity, Feature>mentions) {
		Writer w = null;
		try {
			w = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(DATA_FOLDER + String.valueOf(ID) + FILE_FILENAMES[FILE_EGOFEAT]), "utf-8"));
			boolean have;
			for(HashtagEntity k : hashtags.keySet()) {
				have = false;
				for(String s : hashtags.get(k).inReplyToNames) {
					if(s == null) {
						w.write("1 "); 
						have = true; break;
					}
				}
				if(have == false) w.write("0 ");
			}
			for(UserMentionEntity k : mentions.keySet()) {
				have = false;
				for(String s : mentions.get(k).inReplyToNames) {
					if(s == null) {
						w.write("1 "); 
						have = true; break;
					}
				}
				if(have == false) w.write("0 ");
			}

		} catch (IOException ex) {
		} finally {
			try {w.close();} catch (Exception ex) {}
		}
	}

	public static void feat(long ID, Map<HashtagEntity, Feature> hashtags, Map<UserMentionEntity, Feature>mentions) {
		Writer w = null;
		try {
			w = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(DATA_FOLDER + String.valueOf(ID) + FILE_FILENAMES[FILE_FEAT]), "utf-8"));
			Set<Long> toIds = new HashSet<Long>();

			for(HashtagEntity k : hashtags.keySet()) {
				for(Long id : hashtags.get(k).inReplyToIds) {
					if(id >= 0) toIds.add(id);
				}
			}
			for(UserMentionEntity k : mentions.keySet()) {
				for(Long id : mentions.get(k).inReplyToIds) {
					if(id >= 0) toIds.add(id);
				}
			}
			//System.out.println(hashtags.size());
			//System.out.println(mentions.size());

			for(Long curId : toIds) {
				w.write(String.valueOf(curId) + " ");
				boolean have;
				for(HashtagEntity k : hashtags.keySet()) {
					have = false;
					for(Long id : hashtags.get(k).inReplyToIds) {
						if(id.equals(curId)) {
							w.write("1 "); 
							have = true; break;
						}
					}
					if(have == false) w.write("0 ");
				}

				for(UserMentionEntity k : mentions.keySet()) {
					have = false;
					for(Long id : mentions.get(k).inReplyToIds) {
						if(id.equals(curId)) {
							w.write("1 "); 
							have = true; break;
						}
					}
					if(have == false) w.write("0 ");
				}
				w.write("\n");
			}
		} catch (IOException ex) {
		} finally {
			try {w.close();} catch (Exception ex) {}
		}
	}

	public static void inEdge(long ID, ArrayList<Long> followers) {
		Writer w = null;
		try {
			w = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(DATA_FOLDER + String.valueOf(ID) + FILE_FILENAMES[FILE_INEDGE]), "utf-8"));
			for(long id : followers) {
				//for(long id : array)
					w.write(id + "\n");
			}
		} catch (IOException ex) {
		} finally {
			try {w.close();} catch (Exception ex) {}
		}
	}

	public static void outEdge(long ID, ArrayList<Long> following) {
		Writer w = null;
		try {
			w = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(DATA_FOLDER + String.valueOf(ID) + FILE_FILENAMES[FILE_OUTEDGE]), "utf-8"));
			for(long id : following) {
				//for(long id : array)
					w.write(id + "\n");
			}
		} catch (IOException ex) {
		} finally {
			try {w.close();} catch (Exception ex) {}
		}
	}



	public static void getAccessToken(String consumerKey, String consumerSecret) {
		File file = new File("twitter4j.properties");
		Properties prop = new Properties();
		InputStream is = null;
		OutputStream os = null;
		try {
			if (file.exists()) {
				is = new FileInputStream(file);
				prop.load(is);
			}
			prop.setProperty("oauth.consumerKey", consumerKey);
			prop.setProperty("oauth.consumerSecret", consumerSecret);
			os = new FileOutputStream("twitter4j.properties");
			prop.store(os, "twitter4j.properties");
		} catch (IOException ioe) {
			ioe.printStackTrace();
			System.exit(-1);
		} finally {
			if (is != null) {
				try {
					is.close();
				} catch (IOException ignore) {
				}
			}
			if (os != null) {
				try {
					os.close();
				} catch (IOException ignore) {}
			}
		}
		try {
			Twitter twitter = new TwitterFactory().getInstance();
			RequestToken requestToken = twitter.getOAuthRequestToken();
			System.out.println("Got request token.");
			System.out.println("Request token: " + requestToken.getToken());
			System.out.println("Request token secret: " + requestToken.getTokenSecret());
			AccessToken accessToken = null;

			BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
			while (null == accessToken) {
				System.out.println("Open the following URL and grant access to your account:");
				System.out.println(requestToken.getAuthorizationURL());
				try {
					Desktop.getDesktop().browse(new URI(requestToken.getAuthorizationURL()));
				} catch (UnsupportedOperationException ignore) {
				} catch (IOException ignore) {
				} catch (URISyntaxException e) {
					throw new AssertionError(e);
				}
				System.out.print("Enter the PIN(if available) and hit enter after you granted access.[PIN]:");
				String pin = br.readLine();
				try {
					if (pin.length() > 0) {
						accessToken = twitter.getOAuthAccessToken(requestToken, pin);
					} else {
						accessToken = twitter.getOAuthAccessToken(requestToken);
					}
				} catch (TwitterException te) {
					if (401 == te.getStatusCode()) {
						System.out.println("Unable to get the access token.");
					} else {
						te.printStackTrace();
					}
				}
			}
			System.out.println("Got access token.");
			System.out.println("Access token: " + accessToken.getToken());
			System.out.println("Access token secret: " + accessToken.getTokenSecret());

			try {
				prop.setProperty("oauth.accessToken", accessToken.getToken());
				prop.setProperty("oauth.accessTokenSecret", accessToken.getTokenSecret());
				os = new FileOutputStream(file);
				prop.store(os, "twitter4j.properties");
				os.close();
			} catch (IOException ioe) {
				ioe.printStackTrace();
				System.exit(-1);
			} finally {
				if (os != null) {
					try {
						os.close();
					} catch (IOException ignore) {
					}
				}
			}
			System.out.println("Successfully stored access token to " + file.getAbsolutePath() + ".");
			return;
		} catch (TwitterException te) {
			te.printStackTrace();
			System.out.println("Failed to get accessToken: " + te.getMessage());
			System.exit(-1);
		} catch (IOException ioe) {
			ioe.printStackTrace();
			System.out.println("Failed to read the system input.");
			System.exit(-1);
		}
	}

}

class Feature {
	//HashtagEntity entity;
	ArrayList<String> inReplyToNames = new ArrayList<String>();
	ArrayList<Long> inReplyToIds = new ArrayList<Long>();
	Feature(String n, long id) {
		//entity = e;
		inReplyToNames.add(n);
		inReplyToIds.add(id);
	}
}
