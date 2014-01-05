
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
import java.util.List;
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
		String[] inputFilenames = new String[2];
		String[] outputFilenames = new String[2];
		
		if(args.length == 2){
			inputFilenames[0] = new String(args[0]);
			inputFilenames[1] = new String(args[1]);
		}
		else{
			inputFilenames[0] = new String("EdgesLevel1.txt");
			inputFilenames[1] = new String("EdgesLevel2.txt");
		}
		outputFilenames[0] = "EdgesLevel1_out.txt";
		outputFilenames[1] = "EdgesLevel2_out.txt";
		

		Twitter twitter = new TwitterFactory().getInstance();
		
		// First circle
		collectDataFromFile(twitter, inputFilenames[0], outputFilenames[0]);
		
		// Second circle
		collectDataFromFile(twitter, inputFilenames[1], outputFilenames[1]);
		
		System.out.println("Finish!");
	}
	
	public static void collectDataFromFile(Twitter twitter, String inputFilename, String outputFilename){
		File inputFile = new File(inputFilename);

		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(inputFile)));
			Writer w = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputFilename), "utf-8"));
			String strLine;
			
			while((strLine = br.readLine()) != null){
				String[] tokens = strLine.split("\t");
				if(tokens.length != 2){
					System.out.println("Unknown file format.");
					continue;
				}
				if(Integer.parseInt(tokens[1]) == 1){
					System.out.println("Have parsed: " + tokens[0]);
					w.write(strLine + "\n");
					continue;
				}
				
				if(isFileExist(Long.parseLong(tokens[0]), FILE_EGOFEAT) == null
						|| isFileExist(Long.parseLong(tokens[0]), FILE_FEAT) == null
						|| isFileExist(Long.parseLong(tokens[0]), FILE_FEATNAMES) == null){
					boolean successParse = false;
					while(!successParse){
						try {
							collectFeatures(twitter, Long.parseLong(tokens[0]));
							successParse = true;
						} catch(TwitterException te) {
							if(te.exceededRateLimitation()) {
								te.printStackTrace();
								Thread.sleep(te.getRateLimitStatus().getSecondsUntilReset()*1000 + 1000);
								continue;
							}
							else	successParse = true;
						}
					}
				}
				else{
					System.out.println("Feature files exist: " + tokens[0]);
				}
				
				if(isFileExist(Long.parseLong(tokens[0]), FILE_OUTEDGE) == null){
					boolean successParse = false;
					while(!successParse){
						try {
							collectEdges(twitter, Long.parseLong(tokens[0]));
							successParse = true;
						} catch (TwitterException te) {
							if(te.exceededRateLimitation()) {
								te.printStackTrace();
								Thread.sleep(te.getRateLimitStatus().getSecondsUntilReset()*1000 + 1000);
								continue;
							}
							else	successParse = true;
						}
					}
				}
				else{
					System.out.println("Edge files exist: " + tokens[0]);
				}
				
				w.write(tokens[0] + "\t1\n");
				w.flush();
			}
			
			w.close();
			br.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NumberFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static File isFileExist(long ID, int type){
		File file = new File(DATA_FOLDER + ID + FILE_FILENAMES[type]);
		if(file.exists())	return file;
		else return null;
	}
	
	
	public static void collectEdges(Twitter twitter, long ID) throws InterruptedException, TwitterException, IOException{
		System.out.println("Collecting(Edges).." + ID + ", " + PARSE_COUNT++);
		
		ArrayList<Long> following = new ArrayList<Long>();
		IDs followingIDs = twitter.getFriendsIDs(ID, -1);
		
		Map<UserMentionEntity, Feature> mentions = new HashMap<UserMentionEntity, Feature>();
		for (Status status : twitter.getUserTimeline(ID, new Paging(1, 100))) {
			for(UserMentionEntity me: status.getUserMentionEntities()) {
				if(mentions.containsKey(me)) {
					Feature mf = mentions.get(me);
					mf.inReplyToIds.add(status.getInReplyToUserId());
				}
				else {
					Feature mf = new Feature(status.getInReplyToScreenName(), status.getInReplyToUserId());
					mentions.put(me, mf);
				}
			}
		}
		
		for(UserMentionEntity m : mentions.keySet()) {
			following.add(m.getId());
		}
		for(long id : followingIDs.getIDs()) {
			following.add(id);
		}
		
		outEdge(ID, following); //generate .outedge.txt file
	}
	
	public static void collectFeatures(Twitter twitter, long ID) throws InterruptedException, TwitterException{
		System.out.println("Collecting(Features).." + ID + ", " + PARSE_COUNT++);

		List<Status> statuses = null;

		Map<HashtagEntity, Feature> hashtags = new HashMap<HashtagEntity, Feature>();
		Map<UserMentionEntity, Feature> mentions = new HashMap<UserMentionEntity, Feature>();
		
		statuses = twitter.getUserTimeline(ID, new Paging(1, 100));
	

		for (Status status : statuses) {
			for(HashtagEntity he : status.getHashtagEntities()) {
				Feature hf = null;
				if(hashtags.containsKey(he)) {
					hf = hashtags.get(he);
					hf.inReplyToIds.add(status.getInReplyToUserId());
				}
				else {
					hf = new Feature(status.getInReplyToScreenName(), status.getInReplyToUserId());
					hashtags.put(he, hf);
				}
				for(UserMentionEntity me: status.getUserMentionEntities()) {
					hf.inReplyToIds.add(me.getId());
				}

			}
			
			for(UserMentionEntity me: status.getUserMentionEntities()) {
				if(mentions.containsKey(me)) {
					Feature mf = mentions.get(me);
					mf.inReplyToIds.add(status.getInReplyToUserId());
				}
				else {
					Feature mf = new Feature(status.getInReplyToScreenName(), status.getInReplyToUserId());
					mentions.put(me, mf);
				}
			}
		}


		//Generate feautre files

		featNames(ID, hashtags, mentions);
		egoFeat(ID, hashtags, mentions);
		feat(ID, hashtags, mentions);
	}
	
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
				for(long id : hashtags.get(k).inReplyToIds) {
					if(id < 0) {	//
						w.write("1 "); 
						have = true; break;
					}
				}
				if(have == false) w.write("0 ");
			}
			for(UserMentionEntity k : mentions.keySet()) {
				have = false;
				for(long id : mentions.get(k).inReplyToIds) {
					if(id < 0) {
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
					if(id >= 0) toIds.add(id);	//not myself
				}
			}
			for(UserMentionEntity k : mentions.keySet()) {
				for(Long id : mentions.get(k).inReplyToIds) {
					if(id >= 0) toIds.add(id);	//not myself
				}
			}
			
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

	public static void outEdge(long ID, ArrayList<Long> following) {
		Writer w = null;
		try {
			w = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(DATA_FOLDER + String.valueOf(ID) + FILE_FILENAMES[FILE_OUTEDGE]), "utf-8"));
			for(long id : following) {
				w.write(id + "\n");
			}
		} catch (IOException ex) {
		} finally {
			try {w.close();} catch (Exception ex) {}
		}
	}
}

class Feature {
	ArrayList<Long> inReplyToIds = new ArrayList<Long>();
	Feature(String n, long id) {
		inReplyToIds.add(id);
	}
}
