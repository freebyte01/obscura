package obscura.parts;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Line2D;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map.Entry;

import obscura.Database;
import obscura.Obscura;
import obscura.Utils;




public class ImgDef{
	
	static final int NO_ROT=0;
	static final int ROT_CW=1;
	static final int ROT_180=2;
	static final int ROT_CCW=3;
	public int rot;
	
	public int hash; // standard file hash
	public int xhash; // extended calculated from size+content of the image - in future to have just one representative
	int compo; // compo ~ panorama
	public String path, fName;
	public String label;
	public String keywords;
	public String map;
	public Similarity similar;
	public File file;
	public Point pos, targ, vect;
	public HashMap<String, Point> POIs= new HashMap<String, Point>();
	String assObs, assTarg;
	public boolean oldPOI= true;
	
	public String getKey(){
		return fName.toLowerCase(); }
	
	public ImgDef(File file) {
		
		this.xhash= Database.getHashCode( file );
		this.hash= xhash;
		this.file= file;
		
		this.path= this.fName= file.getName().toLowerCase();
		if (hash!=0)
			Database.imgInfos.put( this.fName, this);
	}
	public ImgDef(String def) {
		read(def);
		System.out.println("red img def "+ hash+ " : "+ fName);
		//if (xhash!=0 && !Database.imgInfos.containsKey(xhash))
			//Database.imgInfos.put( this.path, this);
		Database.imgInfos.put( this.fName.toLowerCase(), this);
	}
	
	public Point vect(){
		return vect= pos==null || targ==null ? null : targ.sub(pos); 
	}
	
	public ImgDef cloneTo(File f) {
		ImgDef clone= Database.imgInfos.get(hash);
		if (clone==null)
			clone= new ImgDef(f);
		clone.map= map;
		clone.keywords= keywords;
		clone.pos= pos.dup();
		clone.targ= targ.dup();
		System.out.println("cloned "+ this.path+ " to "+ path);
		return clone;
	}
	public String store(){
		StringBuilder def= new StringBuilder("img;");
		def.append( "hash:"+ hash+ ";");
		def.append( "xhash:"+ Database.getHashCode(file)+ ";");
		if (file!=null) def.append( "fname:"+ file.getName()+ ";");
		if (rot!=0) def.append( "rot:"+ rot+ ";");
		if (compo!=0) def.append( "compo:"+ compo+ ";");
		if (map!=null) def.append( "map:"+ map+ ";");
		if (label!=null) def.append( "label:"+ label+ ";");
		if (assObs!=null) def.append( "assObs:"+ assObs+ ";");
		if (assTarg!=null) def.append( "assTarg:"+ assTarg+ ";");
		if (similar!=null) def.append( "simile:"+ similar.id+ ";");
		if (keywords!=null) def.append( "keys:"+ keywords+ ";");
		if (pos!=null) def.append( "x:"+ Utils.shorter(pos.x)+ ";y:"+ Utils.shorter(pos.y)+ ";");
		if (targ!=null) def.append( "tx:"+ Utils.shorter(targ.x)+ ";ty:"+ Utils.shorter(targ.y)+ ";");
		ArrayList<String> toRemove= new ArrayList<String>();
		if (POIs.size()>0){
			StringBuilder sb= new StringBuilder("pois:");
			for (Entry<String, Point> e : POIs.entrySet())
				if (e.getValue()!=null)
					sb.append( e.getKey()).append("@").append(e.getValue().serialize()+",");
				else {
					System.err.println(e.getKey()+" is null!!!");
					toRemove.add(e.getKey());
				}
			for (String rem : toRemove)
				POIs.remove(rem);
			def.append(sb.toString().substring(0, sb.length()-1)+";");
			if (!oldPOI) def.append( "oldpoi:false;"); }
		return def.toString()+"\n";
	}
	
	public void read(String definition){
		if (!definition.startsWith("img;"))
			return;
		fName= Database.getValue(definition, "fname", null);
		path= Database.getValue(definition, "path", null);
		fName= fName != null ? fName : path;
		label= Database.getValue(definition, "label", null);
		assObs= Database.getValue(definition, "assObs", null);
		assTarg= Database.getValue(definition, "assTarg", null);
		map= Database.getValue(definition, "map", null);
		keywords= Database.getValue(definition, "keys", null);
		hash= Integer.parseInt(Database.getValue(definition,"hash", "0"));
		xhash= Integer.parseInt(Database.getValue(definition,"xhash", "0"));
		compo= Integer.parseInt(Database.getValue(definition,"compo", "0"));
		rot= Integer.parseInt(Database.getValue(definition,"rot", "0"));
		// similar= Database.regToSimilarity( Database.getValue(definition ,"simile", null ), fName.toLowerCase() );
		double x= Double.parseDouble(Database.getValue(definition,"x", "0"));
		double y= Double.parseDouble(Database.getValue(definition,"y", "0"));
		if (x!=0 || y!=0) pos= new Point(x, y);
		x= Double.parseDouble(Database.getValue(definition,"tx", "0"));
		y= Double.parseDouble(Database.getValue(definition,"ty", "0"));
		String pois= Database.getValue(definition,"pois", "");
		if (pois.length()>0){
			for (String poi : pois.split(","))
				if (poi.contains("@")){
					String[] def= poi.split("@");
					POIs.put( def[0], new Point(def[1])); 
					Database.addPOI(def[0]); }}
		oldPOI= Boolean.parseBoolean(Database.getValue(definition,"oldpoi", "true"));
		
		if (x!=0 || y!=0) targ= new Point(x, y);
		// file= path==null || path=="" ? null : new File(path);
	}

	public boolean isSimilarTo(ImgDef def){
		return Database.isSimilar(getKey(), def.getKey()); }
	public boolean isSimilarTo(String key){
		return Database.isSimilar(getKey(), key); }
	
	public ImgDef[] samePosition(){ return pos==null ? Database.NO_IMG_DEFS : near( vect==null? 2 : vect.length()/10, pos.x, pos.y, false, this); }
	public ImgDef[] sameTarget(){ return targ==null ? Database.NO_IMG_DEFS : vect==null? Database.NO_IMG_DEFS : near( vect.length()/15, targ.x, targ.y, true, this); }
	
	double allowedTargetCos= Math.cos(Math.PI/180*30);
	public ImgDef[] sameDirection(){ 
		if ( vect()==null ) return Database.NO_IMG_DEFS;
		ArrayList<ImgDef> res= new ArrayList<ImgDef>();
		for (ImgDef def : Database.imgInfos.values())
			if (def!=this && def.vect()!=null && (vect.x*def.vect.x + vect.y*def.vect.y) >= allowedTargetCos )
				res.add(def);
		return res.toArray(new ImgDef[res.size()]); }

	static ImgDef[] near(double radius, double x, double y, boolean target, ImgDef exclude){
		radius= radius>3*Utils.ratioMetric?3:radius;
		LinkedList<ImgDef> res= new LinkedList<ImgDef>();
		ImgDef nearest= null;
		double min= Double.MAX_VALUE;
		double radius2 = radius*radius;
		for (ImgDef i : Database.imgInfos.values())
			if ( exclude!=i ){
				Point p= target? i.targ: i.pos;
				if ( p!= null ){
					double vx= p.x-x, vy= p.y-y;
					double diff= vx*vx+vy*vy;
					if (diff!=0 && diff<min && diff<radius2){
						nearest= i;
						min= diff; }
					if ( diff< radius2 )
						res.add(i); }}
		if (res.size()==0 && nearest!=null)
			res.add(nearest);
		ImgDef[] resA= new ImgDef[res.size()];
		res.toArray( resA );
		  Arrays.sort(resA, new Comparator<ImgDef>() {@Override
		    	public int compare(ImgDef o1, ImgDef o2) {
		    		return o1.file == null ? o2.file == null ? 0 : -1 : o2.file==null ? 1 : o1.file.lastModified() >= o2.file.lastModified() ? 1 : -1;
		    }
			});
		return resA; }
	
	
	public Object[][] findSimilarPatterns(){
		
		LinkedList<Object[]> result= new LinkedList<Object[]>();
		
		// will be based on finding the same faces in the poi maps (triangles with same names of points, indiferrent to rotation and scale 
		// if very similar, rot&scale will go to similarity evaluation along with factor of how similar and how many the faces are
		int winnerAnglesMatches= 0;
		int winnerScalesMatches= 0;
		
		System.err.println("\n\n*********** matching "+ POIs.size()+ " : "+ file);
		
		if (POIs.size()>2) // min 3 points available in current picture
				for (ImgDef def: Database.imgInfos.values()) // search all other images which have matching POIs and min. 2 other pois
					if (def!=this && def.POIs.size()>2){
						
						// find all same pois in the other picture
						ArrayList<String> samePOIs= new ArrayList<String>();
						for (Entry<String, Point> poi : def.POIs.entrySet())
							if (!poi.getKey().startsWith(".") && POIs.containsKey(poi.getKey())){ // we have the same couple in other def
								samePOIs.add(poi.getKey()); }						
						
						if (samePOIs.size()>2){ // we have enough same points, let's calc triangles similarities
							
							String[] pois= samePOIs.toArray(new String[samePOIs.size()]);
							System.err.println("\n *** "+ pois.length+"/"+ def.POIs.size() +" same poi adept: "+ def.fName );
							
							// let's prepare comparable pois combinations in one direction
							Point[] conns= new Point[pois.length*pois.length/2-1]; 
							Point[] conns2= new Point[pois.length*pois.length/2-1];
							double[] scales= new double[pois.length*pois.length/2-1];
							double[] angles= new double[pois.length*pois.length/2-1];
							int combo=0;
							for (int i=0; i<pois.length-1; i++){
								//System.err.println("  * "+ pois[i]);
								for (int j=i+1; j<pois.length; j++){
									conns[combo]= POIs.get(pois[i]).dup().sub(POIs.get(pois[j]));
									conns2[combo]= def.POIs.get(pois[i]).dup().sub(def.POIs.get(pois[j])); 
									// System.err.println("  -> "+ pois[j]+" : "+combo + " : "+ conns[combo]+ " / "+ conns2[combo]); 
									combo++; }}
							System.err.println("  combo "+ combo );
							
							// lets calculate relevant connections relative scales and angles
							double[] avgScale= new double[scales.length]; 
							double[] avgAngle= new double[scales.length]; 
							int[] spectrumScales= new int[scales.length]; 
							int[] spectrumAngles= new int[scales.length]; 
							int cmb=0;
							for (int f=0; f<pois.length-1; f++){
								System.err.println(" ** "+ pois[f]);
								for (int t=f+1; t<pois.length; t++){
									System.err.println("  * "+ pois[t]+" : "+cmb);
									scales[cmb]= conns[cmb].length()/conns2[cmb].length();
									angles[cmb]= conns[cmb].angle()-conns2[cmb].angle();
									//System.err.println("  -> "+ pois[t]+" : "+combo + " : "+ relScale+ " / "+ relAngle); }
									for (int j=0; j<cmb; j++){
										// relative == abs.difference / average
										double relScale= Math.abs(scales[cmb]-scales[j])*2 / (scales[cmb]+scales[j]);
										double relAngle= Math.abs(angles[cmb]-angles[j])/5;
										if (relScale<0.05){ // max 5% difference of scales
											avgScale[j]+=(scales[j]+scales[cmb])/2;
											avgScale[cmb]+=(scales[j]+scales[cmb])/2;
											spectrumScales[cmb]++;
											spectrumScales[j]++; }
										if (relAngle<5d/180*Math.PI){ // max 5 degrees difference
											avgAngle[j]+=(angles[j]+angles[cmb])/2;
											avgAngle[cmb]+=(angles[j]+angles[cmb])/2;
											spectrumAngles[cmb]++;
											spectrumAngles[j]++; }}
									cmb++; }}
							int spikeScale=-1;
							for (int i=1; i< spectrumScales.length; i++) // maximum similar scale count on
								if ( spikeScale<0 ? spectrumScales[i]>0 : spectrumScales[i]>spectrumScales[spikeScale])
									spikeScale= i;
							int spikeAngle=-1;
							for (int i=1; i< spectrumAngles.length; i++) // maximum similar angle count on
								if ( spikeAngle<0 ? spectrumAngles[i]>0 : spectrumAngles[i]>spectrumAngles[spikeAngle])
									spikeAngle= i;
							/*System.err.println("  * scales:"+ Arrays.toString(scales));
							System.err.println("     scavgs:"+ Arrays.toString(avgScale));
							System.err.println("     scspec:"+ Arrays.toString(spectrumScales));
							System.err.println("     scspik:"+ spikeScale );
							System.err.println("  * angles:"+ Arrays.toString(angles));
							System.err.println("     anavgs:"+ Arrays.toString(avgAngle));
							System.err.println("     anspec:"+ Arrays.toString(spectrumAngles));
							System.err.println("     anspik:"+ spikeAngle );*/
							if (	spikeAngle>=0 
									&& spikeScale>=0 
									&& spectrumAngles[spikeAngle]>=winnerAnglesMatches 
									&& spectrumScales[spikeScale]>=winnerScalesMatches ){
										result.addFirst(
											wrapCurrentWinner(
													def, 
													spikeScale, spikeAngle,
													spectrumScales, spectrumAngles,
													avgScale[spikeScale]/spectrumScales[spikeScale],
													avgAngle[spikeAngle]/spectrumAngles[spikeAngle],
													samePOIs.toArray(new String[samePOIs.size()]), 
													conns, conns2
													));
							} else System.err.println(" **** not similar "+ def.fName);}}
		for (Iterator<Object[]> io= result.iterator(); io.hasNext();)
			if (((ArrayList<String>)io.next()[4]).size()==0)
				io.remove();
		Object[][] res= result.toArray(new Object[result.size()][]);
		Object[][] sorted= res.length>5 ? Arrays.copyOf(res, 5): res;
		if (sorted.length>1);
			Arrays.sort(sorted, new Comparator<Object[]>() {
				public int compare(Object[] o1, Object[] o2) {
					int diff= ((((ArrayList<String>)o2[4]).size() - ((ArrayList<String>)o1[4]).size()) % 2); 
					return (int) (diff==0? Math.round(sorted.length*100*(((Point)o2[3]).length() / ((Point)o1[3]).length())) : diff); }});
		
		return sorted; }
	
	private Object[] wrapCurrentWinner(
			ImgDef matchingDef, 
			int spikeScale, int spikeAngle,
			int[] spectrumScales, int[] spectrumAngles,
			double majorScale,	double majorAngle, 
			String[] matchPois,	
			Point[] currConns, Point[] matchConns
			){
		

		ArrayList<String> matchedPois= new ArrayList<String>(); 
		
		if (matchingDef!=null && spectrumScales[spikeScale]>0 && spectrumAngles[spikeAngle]>0){
			
			System.err.println("  *** wrapping as matching "+matchingDef.fName+ " with angle match "+ majorAngle+ " and with scale match "+ majorScale);
			int combo=0, cnt=0, pcnt=0;
			Point shift= new Point();
			double cos= Math.cos(majorAngle);
			double sin= Math.sin(majorAngle);
			
			for (int i=0; i<matchPois.length-1; i++){
				String poii= matchPois[i];
				Point ci= POIs.get(poii);
				Point mi= matchingDef.POIs.get(poii);
				for (int j=i+1; j<matchPois.length; j++){
					String poij= matchPois[j];
					boolean ca= spectrumAngles[ combo ]>= spectrumAngles[ spikeAngle ];
					boolean cs= spectrumScales[ combo ]>= spectrumScales[ spikeScale ];
					if (ca && cs){
						currConns[combo].add( ci );
						matchConns[combo].add( mi );//.mul(ca&&cs?1:0.5));
						if (!matchedPois.contains( poii )){
							Point rmi= new Point( mi.x*cos- mi.y*sin, mi.y*cos+ mi.x*sin);
							Point v= ci.dup().sub( rmi.mul( majorScale ));
							System.err.println("    * diff "+poii+":"+ v);
							shift.add( v );//.mul(ca&&cs?1:0.5));
							matchedPois.add( poii );
							pcnt++; }
						if (!matchedPois.contains(poij)){
							Point mj= matchingDef.POIs.get( poij );
							Point rmj= new Point( mj.x*cos- mj.y*sin, mj.y*cos+ mj.x*sin);
							Point v= POIs.get(poij).dup().sub(rmj.mul(majorScale));
							System.err.println("    * diff "+poij+":"+ v);
							shift.add(v);//.mul(ca&&cs?1:0.5));
							matchedPois.add( poij );
							pcnt++; }
						cnt+= 1; }//ca&&cs?2:1; }
					combo++; }}
			shift.mul(1d/pcnt);
			System.err.println("    ** shift:"+ shift+ " scale:"+ majorScale+ " rotation:"+ majorAngle);
			return new Object[]{ matchingDef, majorAngle, majorScale, shift, matchedPois }; }
		return  new Object[]{ null, new Double(0), new Double (1), null, null, null }; }

	public void paint(Graphics2D g, float opacity){
		boolean special= opacity==2;
		double zoom= g.getTransform().getScaleX() * (special?2:1);
		opacity= opacity>1?opacity-1:opacity;
		BasicStroke viewStroke = new BasicStroke((float)((special?5:1.5)/zoom));
		if (pos!=null){
			g.setColor(new Color(1,1,0,opacity));
			Utils.doEllipse(g, pos.x-6/zoom, pos.y-6/zoom, 12/zoom, 12/zoom, true);
			g.setColor(new Color(0,0,1,opacity));
			Utils.doEllipse(g, pos.x-3/zoom, pos.y-3/zoom, 6/zoom, 6/zoom, true);
			g.setColor( new Color(0,0,0,opacity) );
			Utils.doEllipse(g, pos.x-8/zoom, pos.y-8/zoom, 16/zoom, 16/zoom, false);
		}
		if (targ!=null){
			g.setColor(new Color(0,0,1,opacity));
			g.setStroke(viewStroke); 
			g.draw(new Line2D.Double(pos.x, pos.y, targ.x, targ.y));
			Utils.doEllipse(g, targ.x-5/zoom, targ.y-5/zoom, 10/zoom, 10/zoom, true);
			if (this==Obscura.viewer.selectedDef && vect!=null){
				g.setColor(new Color(1f,1f,0,1f));
				double r= vect.length();
				r=r>20/Utils.ratioMetric?20/Utils.ratioMetric:r;
				Utils.doEllipse(g, pos.x-r/10, pos.y-r/10, r/5, r/5, false);
				Utils.doEllipse(g, targ.x-r/15, targ.y-r/15, r/7.5, r/7.5, false);
			}
			g.setColor(new Color(1,1,0,opacity));
			Utils.doEllipse(g, targ.x-2/zoom, targ.y-2/zoom, 4/zoom, 4/zoom, true);
		}
	}
}