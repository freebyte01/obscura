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
	
	interface ComparisonEvaluator {
		void evaluate( Comparison comp );
	}
	
	public static class Comparison {

		public ImgDef def, otherDef;
		public ComparisonEvaluator evaluator;

		public String[] matchingPois;
		public int cardinality;
		public Point[] rels, relsOther;
		public double[] scales, angles, dScales, dAngles;
		public double scalesAvg, anglesAvg;
		
		public int[] scoreScales, scoreAngles; 
		
		public double generalScale=1, generalRotation=0;
		
		public double score=0;
		
		public Comparison( ImgDef def, ImgDef otherDef, String[] matchingPois ) {
			this.def= def;
			this.otherDef= otherDef;
			this.matchingPois= matchingPois;
			this.cardinality= ( matchingPois.length - 1 ) * ( matchingPois.length ) / 2; // suma aritmetickeho radu 1 -> ( matchingPois.length-1 )
			System.err.println("\n *** "+ matchingPois.length+"/"+ otherDef.POIs.size() +" same poi adept: "+ otherDef.fName );
			init(); }
		
		void init(){
			rels= new Point[ cardinality ]; 
			relsOther= new Point[ cardinality ];
			scales= new double[ cardinality ];
			angles= new double[ cardinality ];
			dScales= new double[ cardinality ];
			dAngles= new double[ cardinality ];
			scoreScales= new int[ cardinality ]; 
			scoreAngles= new int[ cardinality ]; 

			
			int poiCombo=0;
			for (int refPoi=0; refPoi<matchingPois.length-1; refPoi++){ // calculate minimum vector relations for each of the compared definitions matching POIs
				//System.err.println("  * "+ pois[i]);
				for (int otherPoi=refPoi+1; otherPoi<matchingPois.length; otherPoi++){
					rels[poiCombo]= this.def.POIs.get( matchingPois[refPoi] ).dup().sub( this.def.POIs.get( matchingPois[otherPoi] ) );
					relsOther[poiCombo]= otherDef.POIs.get( matchingPois[refPoi] ).dup().sub( otherDef.POIs.get( matchingPois[otherPoi] )); 
					//System.err.println(poiCombo+"  ->  "+ rels[poiCombo]+" : "+  relsOther[poiCombo]); 
					poiCombo++; }}
			double scalesSum=0;
			double anglesSum=0;
			for (int cmb=0; cmb<cardinality; cmb++){
				scalesAvg+= scales[cmb]= rels[cmb].length()/relsOther[cmb].length(); // relative scale
				anglesAvg+= angles[cmb]= rels[cmb].angle()-relsOther[cmb].angle(); 
				System.err.println(cmb+"  ->  "+ rels[cmb]+" : "+  relsOther[cmb]+ " a:"+ Math.round(angles[cmb]*1000)/1000d+" s:"+  Math.round(scales[cmb]*1000)/1000d); 
			}
			scalesAvg/= cardinality; // average angle
			anglesAvg/= cardinality; // average scale


			for (int cmb=0; cmb<cardinality; cmb++){ // now calculate scales and angles just differences to the averages for each combo
				dScales[cmb]= scales[cmb]- scalesAvg;
				dAngles[cmb]= angles[cmb]- anglesAvg; }
			
			//System.err.println("  -> "+ pois[t]+" : "+combo + " : "+ relScale+ " / "+ relAngle); 

		}
		
		public boolean contains( String poi ){
			for (String p : this.matchingPois )
				if ( p.equals( poi ) )
					return true;
			return false; }
		
		void evaluate( ComparisonEvaluator evaluator ){
			(this.evaluator= evaluator).evaluate( this ); }
	}
	
	
	public Comparison[] findSimilarPatterns(){
		
		LinkedList<Comparison> result= new LinkedList<Comparison>();
		
		// will be based on finding the same faces in the poi maps (triangles with same names of points, indiferrent to rotation and scale 
		// if very similar, rot&scale will go to similarity evaluation along with factor of how similar and how many the faces are
		int winnerAnglesMatches= 0;
		int winnerScalesMatches= 0;
		
		System.err.println("\n\n*********** matching "+ POIs.size()+ " : "+ file);
		
		if (POIs.size()>1) // min 3 points available in current picture
			
			for (ImgDef otherDef: Database.imgInfos.values()) 
			
				if (otherDef!=this && otherDef.POIs.size()>1){ // search all other images which have matching POIs and min. 2 other pois
										
					ArrayList<String> samePOIs= new ArrayList<String>(); // find all same pois in the other picture
					for (Entry<String, Point> poi : otherDef.POIs.entrySet())
						if (!poi.getKey().startsWith(".") && POIs.containsKey(poi.getKey())) // we have the same couple in other def
							samePOIs.add(poi.getKey()); 						
					
					if (samePOIs.size()>1){ // we have enough same points, let's calc triangles similarities
						
						Comparison comp= new Comparison( this, otherDef, samePOIs.toArray(new String[samePOIs.size()]));
						
						// let's prepare comparable pois combinations in one direction

						comp.evaluate( new FragmentComparisonModel() );
						
						if ( comp.score>0 )
							result.addFirst( comp );
						} else 
							System.err.println(" **** not similar "+ otherDef.fName);						
					}

		Comparison[] res= result.toArray(new Comparison[result.size()]);
		if (res.length>1);
			Arrays.sort(res, new Comparator<Comparison>() {
				public int compare(Comparison c1, Comparison c2) {
					return c1.score - c2.score < 0 ? -1 : 1;  }});
		res= res.length>25 ? Arrays.copyOf(res, 25): res;
	
		return res; }



	class FragmentComparisonModel implements ComparisonEvaluator{
		Comparison comp;
		double score;
		
		public void evaluate( Comparison comp ){
			this.comp= comp;
			System.err.print("\n matches:");
			int winnerAnglesMatches= -1;
			
			for (int cmbRef=0; cmbRef<comp.cardinality-1; cmbRef++) // now calculate make spectrum which combos are fitting others within certain tolerance
				for (int cmbOth= cmbRef+1; cmbOth<comp.cardinality; cmbOth++){
					double dScale= Math.abs( comp.scales[cmbRef]- comp.scales[cmbOth]);
					double dAngle= Math.abs( comp.angles[cmbRef]- comp.angles[cmbOth]);
					if ( dScale < 0.1 ){ comp.scoreScales[cmbRef]++; comp.scoreScales[cmbOth]++; System.err.print(" s:"+cmbRef+":"+cmbOth+", ");}
					if ( dAngle < 0.01 ){ comp.scoreAngles[cmbRef]++; comp.scoreAngles[cmbOth]++; System.err.print(" a:"+cmbRef+":"+cmbOth+", "); }}
					
				// lets find max spikes -> general rotation and general scale
						
				int spikeScale=-1;
				for (int i=1; i< comp.cardinality; i++) // maximum similar scale count on
					if ( spikeScale<0 ? comp.scoreScales[i]>0 : comp.scoreScales[i] > comp.scoreScales[spikeScale])
						spikeScale= i;
				comp.generalScale= comp.scalesAvg+ (spikeScale>-1? comp.scoreScales[spikeScale]:0); 
			
				int spikeAngle=-1;
				for (int i=1; i< comp.cardinality; i++) // maximum similar angle count on
					if ( spikeAngle<0 ? comp.scoreAngles[i]>0 : comp.scoreAngles[i] > comp.scoreAngles[ spikeAngle ])
						spikeAngle= i;
				
				comp.generalRotation= comp.anglesAvg+ ( spikeAngle > -1? comp.scoreAngles[ spikeAngle ]:0); 
			
				// lets base final score based on angles only
				// rules :
				// 		the more matching pois the better
				//		the more poi combos within limits the better for both angle and scale
						
				// matching pois is 1
				// lowered by each non fitting combo
			
//				if (	spikeAngle>=0 
//			//			&& spikeScale>=0 
//						&& comp.scoreAngles[spikeAngle]>= winnerAnglesMatches 
//			//			&& scoreScales[spikeScale]>=winnerScalesMatches 
//				)
					comp.score= 1d- 1d*(comp.cardinality-spikeAngle)/comp.cardinality;
			
			}
	}


	
	private Object[] wrapCurrentWinner(
			double score,
			ImgDef matchingDef, 
			int spikeScale, int spikeAngle,
			int[] spectrumScales, int[] spectrumAngles,
			double generalScale,	double generalAngle, 
			String[] matchPois,	
			Point[] currConns, Point[] matchConns
			){
		

		ArrayList<String> matchedPois= new ArrayList<String>(); 
		
		if (matchingDef!=null 
				&& (spikeScale>-1 && spectrumScales[spikeScale]>0) 
				&& (spikeAngle>-1 && spectrumAngles[spikeAngle]>0)
		){
			
			System.err.println("  *** wrapping as matching "+matchingDef.fName+ " with angle match "+ generalAngle+ " and with scale match "+ generalScale);
			int combo=0, cnt=0, pcnt=0;
			Point shift= new Point();
			double cos= Math.cos( generalAngle );
			double sin= Math.sin( generalAngle );
			
			for (int i=0; i<matchPois.length-1; i++){
				String poii= matchPois[i];
				Point ci= POIs.get(poii);
				Point mi= matchingDef.POIs.get(poii);
				for (int j=i+1; j<matchPois.length; j++){
					String poij= matchPois[j];
					boolean ca= spectrumAngles[ combo ]>= spectrumAngles[ spikeAngle ];
					boolean cs= spectrumScales[ combo ]>= spectrumScales[ spikeScale ];
					if (ca 
//						&& cs
					){
						currConns[combo].add( ci );
						matchConns[combo].add( mi );//.mul(ca&&cs?1:0.5));
						if (!matchedPois.contains( poii )){
							Point rmi= new Point( mi.x*cos- mi.y*sin, mi.y*cos+ mi.x*sin);
							Point v= ci.dup().sub( rmi.mul( generalScale ));
							//System.err.println("    * diff "+poii+":"+ v);
							shift.add( v );//.mul(ca&&cs?1:0.5));
							matchedPois.add( poii );
							pcnt++; }
						if (!matchedPois.contains(poij)){
							Point mj= matchingDef.POIs.get( poij );
							Point rmj= new Point( mj.x*cos- mj.y*sin, mj.y*cos+ mj.x*sin);
							Point v= POIs.get(poij).dup().sub(rmj.mul(generalScale));
							//System.err.println("    * diff "+poij+":"+ v);
							shift.add(v);//.mul(ca&&cs?1:0.5));
							matchedPois.add( poij );
							pcnt++; }
						cnt+= 1; }//ca&&cs?2:1; }
					combo++; }}
			shift.mul(1d/pcnt);
			//System.err.println("    ** shift:"+ shift+ " scale:"+ generalScale+ " rotation:"+ generalAngle);
			return new Object[]{ score, matchingDef, generalAngle, generalScale, shift, matchedPois }; }
		return  new Object[]{ 0d, null, new Double(0), new Double (1), null, null, null }; }
	


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