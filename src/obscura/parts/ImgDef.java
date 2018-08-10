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
	
	
	public Object[] findSimilarPattern(){
		
		// will be based on finding the same faces in the poi maps (triangles with same names of points, indiferrent to rotation and scale 
		// if very similar, rot&scale will go to similarity evaluation along with factor of how similar and how many the faces are
		ImgDef matchingDef=null;
		int[] winnerAnglesSpectrum= null;
		int[] winnerScalesSpectrum= null;
		double winnerAnglesMatch= 0;
		double winnerScalesMatch= 0;
		double winnerAngle=0, winnerScale=0;
		String[] matchPois= {};
		Point[] currConns={};
		Point[] matchConns={};
		int winnerSpikeAngle=0, winnerSpikeScale=0;
		ArrayList<String> matchedPois= new ArrayList<String>(); 
		
		
		if (POIs.size()>1)
			for (Entry<String, Point> poi : POIs.entrySet())
				for (ImgDef def: Database.imgInfos.values())
					if (def!=this && def.POIs.size()>3 && def.POIs.containsKey(poi.getKey())){
						ArrayList<String> samePOIs= new ArrayList<String>();
						for (Entry<String, Point> poi2 : def.POIs.entrySet())
							if (poi2.getKey()!=poi.getKey() && POIs.containsKey(poi2.getKey())) // we have the same couple in other def
								samePOIs.add(poi2.getKey());
						
						if (samePOIs.size()>1){ // we have enough same points, let's calc triangles similarities
							String[] pois= samePOIs.toArray(new String[samePOIs.size()]);
							Point[] conns= new Point[pois.length*pois.length/2+1];
							Point[] conns2= new Point[pois.length*pois.length/2+1];
							double[] scales= new double[pois.length*pois.length/2+1];
							double[] angles= new double[pois.length*pois.length/2+1];
							int combo=0;
							for (int i=0; i<pois.length-1; i++)
								for (int j=i+1; j<pois.length; j++){
									conns[combo]= POIs.get(pois[i]).dup().sub(POIs.get(pois[j]));
									conns2[combo++]= def.POIs.get(pois[i]).dup().sub(def.POIs.get(pois[j])); }
							double[] avgScale= new double[scales.length]; 
							double[] avgAngle= new double[scales.length]; 
							int[] spectrumScale= new int[scales.length]; 
							int[] spectrumAngles= new int[scales.length]; 
							for (int i=0; i<combo; i++){
								scales[i]= conns[i].length()/conns2[i].length();
								angles[i]= conns[i].angle()-conns2[i].angle();
								for (int j=0; j<i; j++){
									if (Math.abs(scales[i]-scales[j])/(scales[i]+scales[j])*2<0.2){
										avgScale[i]+=scales[j];
										avgScale[j]+=scales[i];
										spectrumScale[i]++;
										spectrumScale[j]++; }
									if (Math.abs(angles[i]-angles[j])/(angles[i]+angles[j])*2<0.2){
										avgAngle[i]+=angles[j];
										avgAngle[j]+=angles[i];
										spectrumAngles[i]++;
										spectrumAngles[j]++; }}}
							int spikeScale=0;
							for (int i=1; i< spectrumScale.length; i++) // maximum similar angle count on
								if (spectrumScale[i]>spectrumScale[spikeScale])
									spikeScale= i;
							int spikeAngle=0;
							for (int i=1; i< spectrumAngles.length; i++) // maximum similar angle count on
								if (spectrumAngles[i]>spectrumAngles[spikeAngle])
									spikeAngle= i;
							if (spectrumAngles[spikeAngle]*1d/spectrumAngles.length>winnerAnglesMatch){
								if (spectrumScale[spikeScale]*1d/spectrumScale.length>winnerScalesMatch){
									matchingDef= def;
									winnerAnglesSpectrum= spectrumAngles;
									winnerScalesSpectrum= spectrumScale;
									winnerAngle= avgAngle[spikeAngle]/spectrumScale[spikeScale];
									winnerScale= avgScale[spikeScale]/spectrumAngles[spikeAngle];
									winnerSpikeAngle= spikeAngle;
									winnerSpikeScale= spikeScale;
									winnerAnglesMatch= spectrumAngles[spikeAngle]*1d/spectrumAngles.length;
									winnerScalesMatch= spectrumScale[spikeScale]*1d/spectrumScale.length;
									matchPois= pois;
									currConns= conns;
									matchConns= conns2;
									}}
								
							}}
		if (matchingDef!=null){
			System.err.println("winner matching for "+this.file+" is "+matchingDef.file+ " with angle match "+ winnerAnglesMatch+ " and with scale match "+ winnerScalesMatch);
			if (winnerAnglesMatch<0.5 || winnerScalesMatch<0.5)
				return new Object[]{ null, 0, 1, null, null, null };
			int combo=0, cnt=0, pcnt=0;
			Point shift= new Point();
			double cos= Math.cos(winnerAngle);
			double sin= Math.sin(winnerAngle);
			
			for (int i=0; i<matchPois.length-1; i++){
				String poii= matchPois[i];
				Point ci= POIs.get(poii);
				Point mi= matchingDef.POIs.get(poii);
				for (int j=i+1; j<matchPois.length; j++){
					String poij= matchPois[j];
					boolean ca= winnerAnglesSpectrum[ combo ]>=winnerAnglesSpectrum[ winnerSpikeAngle ];
					boolean cs= winnerScalesSpectrum[ combo ]>=winnerScalesSpectrum[ winnerSpikeScale ];
					if (ca || cs){
						currConns[combo].add( ci );
						matchConns[combo].add( mi );//.mul(ca&&cs?1:0.5));
						if (!matchedPois.contains( poii )){
							Point rmi= new Point( mi.x*cos- mi.y*sin, mi.y*cos+ mi.x*sin);
							Point v= ci.dup().sub( rmi.mul( winnerScale ));
							System.err.println("diff "+poii+":"+ v);
							shift.add( v );//.mul(ca&&cs?1:0.5));
							matchedPois.add( poii );
							pcnt++; }
						if (!matchedPois.contains(poij)){
							Point mj= matchingDef.POIs.get( poij );
							Point rmj= new Point( mj.x*cos- mj.y*sin, mj.y*cos+ mj.x*sin);
							Point v= POIs.get(poij).dup().sub(rmj.mul(winnerScale));
							System.err.println("diff "+poij+":"+ v);
							shift.add(v);//.mul(ca&&cs?1:0.5));
							matchedPois.add( poij );
							pcnt++; }
						cnt+= 1; }//ca&&cs?2:1; }
					combo++; }}
			shift.mul(1d/pcnt);
			System.err.println(shift+":"+5);
			return new Object[]{ matchingDef, winnerAngle, winnerScale, shift, matchedPois }; }
		return  new Object[]{ null, 0, 1, null, null, null }; }

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