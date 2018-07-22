package obscura;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import javax.swing.JOptionPane;
import obscura.parts.Area;
import obscura.parts.ImgDef;
import obscura.parts.Place;
import obscura.parts.Point;
import obscura.parts.Poly;
import obscura.parts.Similarity;

public class Database {

	public static HashMap<String, ImgDef> imgInfos= new HashMap<String, ImgDef>();
	public static HashMap<String, Area> areas= new HashMap<String, Area>();
	public static HashMap<String, Similarity> similarities= new HashMap<String, Similarity>();
	
	public static boolean isSimilar(String defKey1, String defKey2){
		Similarity sim1= getSimilarityContaining(defKey1);
		if (sim1==null) return false;
		return sim1 == getSimilarityContaining(defKey2); }
	
	public static Similarity getSimilarityContaining(String key){
		for (Similarity sim : similarities.values())
			if ( sim.register.contains(key) )
				return sim;
		return null; }
	
	public static Similarity  regToSimilarity(String simKey, String defKey ){
		return regToSimilarity(simKey, defKey, false); }
	static Similarity  regToSimilarity(String simKey, String defKey, boolean createIfNoSimilarityKey ){
		Similarity sim= simKey!=null ? similarities.get(simKey) : getSimilarityContaining(defKey);
		if (sim==null)
			if (createIfNoSimilarityKey)
				sim= new Similarity( simKey==null ? defKey : simKey );
			else 
				return null;
		else
			if (!isCompatible(sim.id, defKey)) // don't include def if too different to others
				return sim;
		return sim.registerKey( defKey ); }

	// at least they should be nearly same location in average and same vector
	public static boolean isCompatible(Similarity sim1, Similarity sim2){
		if (sim1==null || sim2==null)
			return false;
		if (sim1==sim2)
			return true;
		Point[] p1= sim1.getPositioning();
		Point[] p2= sim2.getPositioning();
		Point v1= p1[1].sub(p1[0]); 
		Point v2= p2[1].sub(p2[0]);
		double ratio= v1.length()/v2.length(); 
		if (Math.abs(1 - ratio)>0.1) 
			return false; // ratio needs to be nearly 1
		Point distLoc= p1[0].sub(p2[0]);
		if (Math.abs((v1.length()+v2.length())/2/distLoc.length())<10){ 
			System.err.println("locations incompatible "+ (( v1.length() + v2.length() ) / 2) + " vs " + distLoc.length() );
			return false; }// the locations for similarity groups should differ max 1/10 of distance to target
		Point distTarg= p1[1].sub(p2[1]);
		if ( ( v1.length() + v2.length() ) / 2/ distTarg.length() < 5){
			System.err.println("targets incompatible "+ (( v1.length() + v2.length() ) / 2) + " vs " + distTarg.length() );
			return false; } // the locations for similarity groups should differ max 1/5 of distance to target
		return false; } // so far let's proof it works before allowing big merges
	
	public static boolean isCompatible(String simKey, String defKey){
		Similarity sim1= similarities.get( simKey );
		ImgDef def= imgInfos.get( defKey );
		if (def==null || def.targ==null) // for now only fully defined imgdefs are considered
			return false;
		Point[] p1= sim1.getPositioning();
		Point v1= p1[1].sub(p1[0]); 
		Point v2= def.targ.sub(def.pos);
		double ratio= v1.length()/v2.length(); 
		if (Math.abs(1 - ratio)>0.1) 
			return false; // ratio needs to be nearly 1
		Point distLoc= p1[0].sub(def.pos);
		if (Math.abs((v1.length()+v2.length())/2/distLoc.length())<10){ 
			System.err.println("locations incompatible "+ (( v1.length() + v2.length() ) / 2) + " vs " + distLoc.length() );
			return false; }// the locations for similarity groups should differ max 1/10 of distance to target
		Point distTarg= p1[1].sub(def.targ);
		if ( ( v1.length() + v2.length() ) / 2/ distTarg.length() < 5){
			System.err.println("targets incompatible "+ (( v1.length() + v2.length() ) / 2) + " vs " + distTarg.length() );
			return false; } // the locations for similarity groups should differ max 1/5 of distance to target
		return false; } // so far let's proof it works before allowing big merges
	
	public static Similarity  pairAsSimilar(String defKey1, String defKey2){
		Similarity sim1= getSimilarityContaining( defKey1 );
		Similarity sim2= getSimilarityContaining( defKey2 );
		
		if (sim1==sim2)
			if (sim1!=null)
				return sim1;
			else {
				sim1= regToSimilarity( defKey1, defKey1, true);
				return sim1.registerKey( defKey1); }
		else 
			if ( sim1 == null )
				return sim2.registerKey(defKey1);
			else if ( sim2 == null )
				return sim1.registerKey(defKey2);
			else { // merge similarities
				if (!isCompatible(sim1, sim2)){
					int dialogResult = JOptionPane.showConfirmDialog (null, 
							"Similarity groups incompatible. Do you still want to continue and merge them?","Warning",
							JOptionPane.YES_NO_OPTION);
					if(dialogResult == JOptionPane.NO_OPTION)
						return sim1; }
				for (String key : sim2.register)
					sim1.registerKey(key, false);
				similarities.remove(sim2.id);
				Obscura.data.writeDatabase();
				return sim1; }}

	public static Similarity  rejectFromSimilar(String defKey){
		Similarity sim= getSimilarityContaining( defKey );
		if (sim!=null){
			sim.register.remove(defKey);
			Obscura.data.writeDatabase();
			return sim; }
		return null; }

	
	public static final ImgDef[] NO_IMG_DEFS = new ImgDef[0]; 
	
	String dataFile= null ;
	public Database() { this("imageDefs.dat"); }
	public Database(String databaseFile) {
		if (databaseFile==null)
			throw new RuntimeException("no database file specified!");
		readDatabase(databaseFile);
	}
	
	// ImgDef getImgDef(int hash){ return getImgDef(hash, false); }
	/*ImgDef getImgDef(int hash, boolean force){
		if (imgInfos.containsKey(hash))
			return imgInfos.get(hash);
		else 
			return force ? new ImgDef(hash) : null; } */
	ImgDef getImgDef(File f){ return getImgDef(f, false); }
	ImgDef getImgDef(File f, boolean force){
		String key= f.getName().toLowerCase();
		if (imgInfos.containsKey(key))
			return imgInfos.get(key);
		else 
			return force ? new ImgDef(f) : null; }
	
	
	public static int getHashCode(File f){ 
		return f==null?0:(f.length()+"_"+f.getName().toLowerCase()).hashCode(); }
	
	void readDatabase(String path){
		imgInfos.clear();
		File file= new File(path);
		if (file.isDirectory())
			throw new RuntimeException("database filename "+ path +" is ocuppied!");
		if (!file.exists()){
			System.err.println("no images data file "+ path + " found!");
			return;
		}
		dataFile = path; 
		BufferedReader br= null;
		try {
			br = new BufferedReader(new FileReader(file)); 
		    String line;
		    while ((line = br.readLine()) != null){ 
		    	if (line.startsWith("img;")) new ImgDef(line);
		    	else if (line.startsWith("simil;")) new Similarity().read(line);
		    	else if (line.startsWith("area;")) new Area().read(line);
		    	else if (line.startsWith("place;")) new Place(line);
		    	else if (line.startsWith("poly;")) new Poly(line);
		    	else if (line.startsWith("map;")) {
		    		String arId= getValue(line, "ass", "0");
		    		Area a= areas.get(arId);
		    		if (a==null) 
		    			continue;
		    		int lev= Integer.parseInt(getValue(line, "lev", "0"));
		    		Map m= a.maps.get(lev)==null? new Map() : a.maps.get(lev);
		    		m.x= Double.parseDouble(getValue(line, "x", "0"));
		    		m.y= Double.parseDouble(getValue(line, "y", "0"));
		    		m.rot= Double.parseDouble(getValue(line, "rot", "0"));
		    		m.scale= Double.parseDouble(getValue(line, "sc", "1"));
		    		a.maps.put(lev, m); }}
		    ImgDef[] sortedImgInfos= new ImgDef[imgInfos.size()];
		    imgInfos.values().toArray(sortedImgInfos);
		    Arrays.sort(sortedImgInfos, new Comparator<ImgDef>() {@Override
		    	public int compare(ImgDef o1, ImgDef o2) {
		    		return o1.file == null ? o2.file == null ? 0 : -1 : o2.file==null ? 1 : o1.file.lastModified() >= o2.file.lastModified() ? 1 : -1;
		    }
			});
		    imgInfos.clear();
		    for (ImgDef imgInfo : sortedImgInfos)
		    	imgInfos.put(imgInfo.fName.toLowerCase(), imgInfo);
		    
		    Watcher.updateImgDefsByFiles();
		    
		}catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (br!=null) try{ br.close(); } catch (Exception e2) {}
		}
	}
	public void writeDatabase(){ writeDatabase(dataFile); }
	public void writeDatabase(String path){
		File file= new File(path);
		while (file.exists() && file.isDirectory()){
			path+="_";
			file= new File(path);
		}
		dataFile= path;
		StringBuilder sb= new StringBuilder();
		for ( ImgDef id : imgInfos.values() )
			if (id.file!=null)
				sb.append( id.store() );
		for ( Area a : areas.values() )
			sb.append( a.store() );
		for ( Similarity s : similarities.values() )
			sb.append( s.store() );
		BufferedWriter bw= null;
		try {
			bw= new BufferedWriter(new FileWriter(file));
			bw.write(sb.toString());
			System.out.println(new Date()+ " - img database written to "+ file.getAbsolutePath());
		} catch (Exception e) {
			e.printStackTrace();
		} finally{
			if (bw!=null) try{ bw.close(); } catch (Exception e2) {}
		}
	}
	
	public static String getValue(String definition, String key){ return getValue(definition, key, null); }
	public static String getValue(String definition, String key, String def){
		key= ";"+ key+":";
		int ix;
		String val= def;
		if ((ix= definition.indexOf(key))>-1){ 
			ix+=key.length(); 
			val= definition.substring(ix, definition.indexOf(";", ix));
		}
		val= "null".equals(val)?def:val;
		return val;
	}
	

}
