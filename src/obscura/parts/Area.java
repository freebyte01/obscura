package obscura.parts;

import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import obscura.Database;
import obscura.Map;
import obscura.Obscura;

public class Area{
	public Area assoc;
	public ArrayList<Area> subs= new ArrayList<Area>();
	public ArrayList<Place> places= new ArrayList<Place>();
	public HashMap<Integer, Map> maps= new HashMap<Integer, Map>();
	public ArrayList<Poly> polys= new ArrayList<Poly>();
	double relX, relY;
	public int level;
	public String id, pid, label;
	public Area() {}
	
	public Area(String label) {
		this.label= label;
		this.id= System.currentTimeMillis()+"";
		Database.areas.put(this.id, this);
		System.err.println("adding area "+ label+ ":"+ this.id);
		Obscura.data.writeDatabase(); }

	public void read(String definition){
		if (!definition.startsWith("area;"))
			return;
		id= Database.getValue(definition, "id", null);
		pid= Database.getValue(definition, "pid", null);
		label= Database.getValue(definition, "label", null);
		level= Integer.parseInt(Database.getValue(definition, "lev", "0"));
		relX= Double.parseDouble(Database.getValue(definition, "rx", "0"));
		relY= Double.parseDouble(Database.getValue(definition, "ry", "0"));
		Database.areas.put(this.id, this);
		System.err.println("adding area "+ label+ ":"+ this.id); }
	
	synchronized public String store(){
		StringBuilder def= new StringBuilder("area;");
		def.append( "id:"+ id+ ";");
		if (label!=null) def.append( "label:"+ label+ ";");
		if (relX!=0) def.append( "rx:"+ relX+ ";");
		if (relY!=0) def.append( "ry:"+ relY+ ";");
		if (level!=0) def.append( "lev:"+ level+ ";");
		if (assoc!=null) def.append( "ass:"+ assoc.id+ ";");
		def.append("\n");
		for(Place p : places)
			def.append(p.store(id));
		for(Poly p : polys)
			def.append(p.store(id));
		for (Entry<Integer, Map> e: maps.entrySet()){
			Map m= e.getValue();
			def.append("\nmap;ass:"+ this.id+ ";lev:"+ e.getKey()+ ";x:"+ m.x+ ";y:"+ m.y+ ";rot:"+ m.rot+ ";sc:"+ m.scale+";" );
		}

		return def.toString()+"\n"; }
	
	synchronized public Poly addPoly(){
		Poly npl = new Poly(this);
		polys.add( npl );
		return activePoly= npl; }
	
	public Poly getPolyFor(Point[] p){ return p!=null && p.length>0 ? getPolyFor(p[0]) : activePoly; }
	public Poly getPolyFor(Point p){
		if (p==null)
			return null;
		for (Poly pl : polys)
			if (pl.points.contains(p))
				return pl;
		return null; }
	
	synchronized public Poly removePoly(Poly poly){
		if (polys.contains(poly)){
			polys.remove(poly);
			if (activePoly== poly)
				activePoly= null;
			update(true);
			return poly; }
		return null; }
	
	synchronized public Point removePoint(Point p){
		Poly pl= getPolyFor(p);
		if (pl!=null)
			if (pl.points.remove(p)){
				if (pl.points.size()==0)
					polys.remove(pl);
				update(true);
			}
		return p; }
	
	public Poly activePoly;
	synchronized public Point addPoint(Point p){ return addPoint(p, activePoly!=null?activePoly: polys.size()>0 ? polys.get(0) : addPoly() , -1); }
	synchronized public Point addPoint(Point p, Point[] context){
		if (context==null || context.length==0)
			return addPoint(p);
		Poly pl= getPolyFor(context[0]);
		return 	addPoint(p, pl, pl.points.indexOf(context[0])+1); }
	synchronized public Point addPoint(Point p, Poly poly, int pos){
		System.err.println(label+ " adding area point "+ p);
		if (p==null) return null;
		if (poly==null)	polys.add( poly= activePoly!=null?activePoly:new Poly(this) );
		activePoly= poly;
		poly.addPoint(p, pos); 
		update(true);
		Obscura.data.writeDatabase();
		return p; }
	
	
	
	synchronized public Point[] checkMouse(double rX, double rY, double rad2){
		Point[] res= null;
		for (Poly pl : polys)
			if ((res= pl.checkMouse(rX, rY, rad2))!=null )
					return res;
		return null; }
	
	synchronized public void update(boolean store){
		for( Poly pl : polys )
			pl.update();
		if (store)
			Obscura.data.writeDatabase(); }
	
	public void paint(Graphics2D g, double rX, double rY){
		for (Poly pl : polys)
			pl.paint(g, rX, rY); }
}