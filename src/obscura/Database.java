package obscura;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;

import obscura.parts.Area;
import obscura.parts.ImgDef;
import obscura.parts.Place;
import obscura.parts.Poly;

public class Database {

	public static HashMap<Integer, ImgDef> images= new HashMap<Integer, ImgDef>();
	public static HashMap<String, Area> areas= new HashMap<String, Area>();

	public static final ImgDef[] NO_IMG_DEFS = new ImgDef[0]; 
	
	String dataFile= null ;
	public Database() { this("imageDefs.dat"); }
	public Database(String databaseFile) {
		if (databaseFile==null)
			throw new RuntimeException("no database file specified!");
		readDatabase(databaseFile);
	}
	
	ImgDef getImgDef(int hash){ return getImgDef(hash, false); }
	ImgDef getImgDef(int hash, boolean force){
		if (images.containsKey(hash))
			return images.get(hash);
		else 
			return force ? new ImgDef(hash) : null;
	}
	
	void readDatabase(String path){
		images.clear();
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
		    	else if (line.startsWith("area;")) new Area().read(line);
		    	else if (line.startsWith("place;")) new Place(line);
		    	else if (line.startsWith("poly;")) new Poly(line);
		    }
		    ImgDef[] sort= new ImgDef[images.size()];
		    images.values().toArray(sort);
		    Arrays.sort(sort, new Comparator<ImgDef>() {@Override
		    	public int compare(ImgDef o1, ImgDef o2) {
		    		return o1.file == null ? o2.file == null ? 0 : -1 : o2.file==null ? 1 : o1.file.lastModified() >= o2.file.lastModified() ? 1 : -1;
		    }
			});
		    images.clear();
		    for (ImgDef id : sort)
		    	images.put(id.hash, id);
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
		for ( ImgDef id : images.values() )
			sb.append( id.store() );
		for ( Area a : areas.values() )
			sb.append( a.store() );
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
