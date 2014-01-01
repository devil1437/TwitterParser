
import twitter4j.HashtagEntity;
import twitter4j.Paging;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.User;
import twitter4j.UserMentionEntity;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;

import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
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
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

public class GetDataSet {
	public static void main(String[] args) {

		//getAccessToken("eqAYO7roPzCMhrDwNi8cg", "BTBK6A0v3HU9cSoxwacj5UiPqsuxm6ZGQq6C9RM");


		Twitter twitter = new TwitterFactory().getInstance();
		long ID = 0;
		String user = null;


		try {
			//user = twitter.verifyCredentials().getScreenName();
			//ID = twitter.getId();

			FileInputStream fstream = new FileInputStream("IDListIn.txt");
			DataInputStream in = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			BufferedWriter bw = new BufferedWriter(new FileWriter(new File(
					"IDListOut.txt"), false));
			String strLine;
			while ((strLine = br.readLine()) != null)   {
				// Print the content on the console
				System.out.println (strLine);
				String[] tokens = strLine.split("\t");
				if(tokens.length != 2){
					System.out.println("Unknown line format.");
					continue;
				}
				if(Integer.parseInt(tokens[1]) == 1){
					System.out.println(tokens[0] + " has been collected.");
					bw.write(strLine+"\n");
					bw.flush();
					continue;
				}
				File file = new File(tokens[0]+".outedge.txt");
				if(file.exists()){
					System.out.println(tokens[0] + " has been collected.");
					bw.write(tokens[0]+"\t1\n");
					bw.flush();
					continue;
				}
				
				ID = Long.parseLong(tokens[0]);
				user = twitter.showUser(ID).getScreenName();
				System.out.println("Collecting.." + ID + ", " + user);
				collectData(twitter, ID, user);
				bw.write(tokens[0]+"\t1\n");
				bw.flush();
				
			}
			
			in.close();
			br.close();
			bw.close();
			//user = "devil1437";
			//ID = twitter.showUser(user).getId();
			
			//ID = 0;
			//user = twitter.showUser(ID).getScreenName();


			
			/*
	        for(User u : twitter.getFriendsList(user, -1)) {
	        	collectData(twitter, u.getId(), u.getScreenName());
	        }
			 */


		} catch (TwitterException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}


	public static void collectData(Twitter twitter, long ID, String user) {

		List<Status> statuses = null;

		Map<HashtagEntity, Feature> hashtags = new HashMap<HashtagEntity, Feature>();
		Map<UserMentionEntity, Feature> mentions = new HashMap<UserMentionEntity, Feature>();
		List<User> followers = null;
		List<User> following = null;

		try {
			statuses = twitter.getUserTimeline(user, new Paging(1, 100));
			followers = twitter.getFollowersList(user, -1);
			following = twitter.getFriendsList(user, -1);
		} catch (TwitterException te) {
			te.printStackTrace();
			System.out.println("Failed to get timeline: " + te.getMessage());
			System.exit(-1);
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
	}




	public static void featNames(long ID, Map<HashtagEntity, Feature> hashtags, Map<UserMentionEntity, Feature>mentions) {
		Writer w = null;
		try {
			w = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(String.valueOf(ID) + ".featnames.txt"), "utf-8"));
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
			w = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(String.valueOf(ID) + ".egofeat.txt"), "utf-8"));
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
			w = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(String.valueOf(ID) + ".feat.txt"), "utf-8"));
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

	public static void inEdge(long ID, List<User> followers) {
		Writer w = null;
		try {
			w = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(String.valueOf(ID) + ".inedge.txt"), "utf-8"));
			for(User u : followers) {
				w.write(String.valueOf(u.getId()) + "\n");
			}
		} catch (IOException ex) {
		} finally {
			try {w.close();} catch (Exception ex) {}
		}
	}

	public static void outEdge(long ID, List<User> following) {
		Writer w = null;
		try {
			w = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(String.valueOf(ID) + ".outedge.txt"), "utf-8"));
			for(User u : following) {
				w.write(String.valueOf(u.getId()) + "\n");
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
			System.exit(0);
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
