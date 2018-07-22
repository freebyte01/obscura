package obscura.parts;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Line2D;
import java.io.File;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;
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
	String assObs, assTarg;
	
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
	public void update(){
		vect= pos!=null && targ!=null ? targ.dup().sub(pos) : null; 
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
		if (x!=0 || y!=0) targ= new Point(x, y);
		// file= path==null || path=="" ? null : new File(path);
		update();
	}

	public boolean isSimilarTo(ImgDef def){
		return Database.isSimilar(getKey(), def.getKey()); }
	public boolean isSimilarTo(String key){
		return Database.isSimilar(getKey(), key); }
	
	public ImgDef[] samePosition(){ return pos==null ? Database.NO_IMG_DEFS : near( vect==null? 2 : vect.length()/4, pos.x, pos.y, false, this); }
	public ImgDef[] sameTarget(){ return targ==null ? Database.NO_IMG_DEFS : vect==null? Database.NO_IMG_DEFS : near( vect.length()/10, targ.x, targ.y, true, this); }

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
	

	public void paint(Graphics2D g, float opacity){
		double zoom= g.getTransform().getScaleX();
		if (pos!=null){
			g.setColor(new Color(0,0,0,opacity));
			Utils.doEllipse(g, pos.x-8/zoom, pos.y-8/zoom, 16/zoom, 16/zoom, true);
			g.setColor(new Color(1,1,0,opacity));
			Utils.doEllipse(g, pos.x-6/zoom, pos.y-6/zoom, 12/zoom, 12/zoom, true);
			g.setColor(new Color(0,0,1,opacity));
			Utils.doEllipse(g, pos.x-3/zoom, pos.y-3/zoom, 6/zoom, 6/zoom, true);
		}
		if (targ!=null){
			g.setColor(new Color(0,0,1,opacity));
			BasicStroke viewStroke = new BasicStroke((float) (1.5/zoom));
			g.setStroke(viewStroke); 
			g.draw(new Line2D.Double(pos.x, pos.y, targ.x, targ.y));
			Utils.doEllipse(g, targ.x-5/zoom, targ.y-5/zoom, 10/zoom, 10/zoom, true);
			if (this==Obscura.viewer.selectedDef && vect!=null){
				g.setColor(new Color(1f,1f,0,1f));
				double r= vect.length()/4.0;
				r=r>3/Utils.ratioMetric?3:r;
				Utils.doEllipse(g, pos.x-r, pos.y-r, r*2, r*2, false);
				r= vect.length()/10; r=r>3/Utils.ratioMetric?3:r;
				Utils.doEllipse(g, targ.x-r, targ.y-r, r*2, r*2, false);
			}
			g.setColor(new Color(1,1,0,opacity));
			Utils.doEllipse(g, targ.x-2/zoom, targ.y-2/zoom, 4/zoom, 4/zoom, true);
		}
	}
}