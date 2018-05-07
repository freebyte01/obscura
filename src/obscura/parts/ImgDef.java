package obscura.parts;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.io.File;
import java.util.LinkedList;

import obscura.Database;
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
	public String path;
	public String label;
	public String keywords;
	public String map;
	public File file;
	public Point obs, targ;
	String assObs, assTarg;
	
	public ImgDef(int hash) {
		this.hash= hash;
		if (hash!=0)
			Database.images.put(hash, this);
	}
	public ImgDef(String def) {
		read(def);
		System.out.println("red img def "+ hash+ " : "+ (path!=""?new File(path).getName():""));
		if (hash!=0)
			Database.images.put(hash, this);
	}
	
	public ImgDef cloneTo(int hash, String path) {
		ImgDef clone= Database.images.get(hash);
		if (clone==null)
			clone= new ImgDef(hash);
		clone.path= path;
		clone.map= map;
		clone.keywords= keywords;
		clone.obs= obs.dup();
		clone.targ= targ.dup();
		System.out.println("cloned "+ this.path+ " to "+ path);
		return clone;
	}
	public String store(){
		StringBuilder def= new StringBuilder("img;");
		def.append( "hash:"+ hash+ ";");
		if (xhash!=0) def.append( "xhash:"+ xhash+ ";");
		if (path!=null) def.append( "path:"+ path+ ";");
		if (rot!=0) def.append( "rot:"+ rot+ ";");
		if (compo!=0) def.append( "compo:"+ compo+ ";");
		if (map!=null) def.append( "map:"+ map+ ";");
		if (label!=null) def.append( "label:"+ label+ ";");
		if (assObs!=null) def.append( "assObs:"+ assObs+ ";");
		if (assTarg!=null) def.append( "assTarg:"+ assTarg+ ";");
		if (keywords!=null) def.append( "keys:"+ keywords+ ";");
		if (obs!=null) def.append( "x:"+ Utils.shorter(obs.x)+ ";y:"+ Utils.shorter(obs.y)+ ";");
		if (targ!=null) def.append( "tx:"+ Utils.shorter(targ.x)+ ";ty:"+ Utils.shorter(targ.y)+ ";");
		return def.toString()+"\n";
	}
	
	public void read(String definition){
		if (!definition.startsWith("img;"))
			return;
		path= Database.getValue(definition, "path", null);
		label= Database.getValue(definition, "label", null);
		assObs= Database.getValue(definition, "assObs", null);
		assTarg= Database.getValue(definition, "assTarg", null);
		map= Database.getValue(definition, "map", null);
		keywords= Database.getValue(definition, "keys", null);
		hash= Integer.parseInt(Database.getValue(definition,"hash", "0"));
		xhash= Integer.parseInt(Database.getValue(definition,"xhash", "0"));
		compo= Integer.parseInt(Database.getValue(definition,"compo", "0"));
		rot= Integer.parseInt(Database.getValue(definition,"rot", "0"));
		double x= Double.parseDouble(Database.getValue(definition,"x", "0"));
		double y= Double.parseDouble(Database.getValue(definition,"y", "0"));
		if (x!=0 || y!=0) obs= new Point(x, y);
		x= Double.parseDouble(Database.getValue(definition,"tx", "0"));
		y= Double.parseDouble(Database.getValue(definition,"ty", "0"));
		if (x!=0 || y!=0) targ= new Point(x, y);
		file= path==null || path=="" ? null : new File(path);
	}

	
	public ImgDef[] sameLocation(double radius){ return obs==null ? Database.NO_IMG_DEFS : near(radius, obs.x, obs.y, false, this); }
	public ImgDef[] sameTarget(double radius){ return targ==null ? Database.NO_IMG_DEFS : near(radius, targ.x, targ.y, true, this); }

	static ImgDef[] near(double radius, double x, double y, boolean target, ImgDef exclude){
		LinkedList<ImgDef> res= new LinkedList<ImgDef>();
		ImgDef nearest= null;
		double min= Double.MAX_VALUE;
		for (ImgDef i : Database.images.values())
			if (!target || exclude!=i){
				Point p= target? i.targ: i.obs;
				if ( p!= null ){
					double radius2 = radius*radius;
					double vx= p.x-x, vy= p.y-y;
					double diff= vx*vx+vy*vy;
					if (diff!=0 && diff<min){
						nearest= i;
						min= diff; }
					if ( diff< radius2)
						res.add(i); }}
		ImgDef[] resA= new ImgDef[res.size()];
		res.toArray( resA );
		if (res.size()==0 && nearest!=null)
			res.add(nearest);
		return resA; }
	

	public void paint(Graphics2D g, float opacity){
		double zoom= g.getTransform().getScaleX();
		if (obs!=null){
			g.setColor(new Color(0,0,0,opacity));
			g.fill(new Ellipse2D.Double(obs.x-8/zoom, obs.y-8/zoom, 16/zoom, 16/zoom));
			g.setColor(new Color(1,1,0,opacity));
			g.fill(new Ellipse2D.Double(obs.x-6/zoom, obs.y-6/zoom, 12/zoom, 12/zoom));
			g.setColor(new Color(0,0,1,opacity));
			g.fill(new Ellipse2D.Double(obs.x-3/zoom, obs.y-3/zoom, 6/zoom, 6/zoom));
		}
		if (targ!=null){
			g.setColor(new Color(0,0,1,opacity));
			if (obs!=null){
				BasicStroke viewStroke = new BasicStroke((float) (1.5/zoom));
				g.setStroke(viewStroke); 
				g.draw(new Line2D.Double(obs.x, obs.y, targ.x, targ.y));
			}
			g.fill(new Ellipse2D.Double(targ.x-5/zoom, targ.y-5/zoom, 10/zoom, 10/zoom));
			g.setColor(new Color(1,1,0,opacity));
			g.fill(new Ellipse2D.Double(targ.x-2/zoom, targ.y-2/zoom, 4/zoom, 4/zoom));
		}
	}
}