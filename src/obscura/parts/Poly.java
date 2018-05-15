package obscura.parts;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.util.LinkedList;
import obscura.Database;
import obscura.Obscura;
import obscura.Utils;
public class Poly{
	
	Area area;
	String pid;
	
	static final Color defBgClr= new Color(255,255,255,50);
	static final Color defLnClr= new Color(0,0,255,200);
	public boolean closed=true, fill=true;
	public Color clr= defLnClr;
	public Color bg= defBgClr;
	private Path2D path;
	public LinkedList<Point> points= new LinkedList<Point>();
	public Point[] getPoints(){ return points.toArray(new Point[0]); }

	Poly(Area a){
		area= a;
		pid= a.id;
		Obscura.data.writeDatabase();
	}
	
	public Poly(String def) {
		read(def);
	}
	
	void read(String definition){
		if (!definition.startsWith("poly;"))
			return;
		pid= Database.getValue(definition, "pid", null);
		//clr= decodeColor(getValue(definition, "clr"), Color.blue); 
		//bg= decodeColor(getValue(definition, "bg"), Color.white);
		String pp= Database.getValue(definition, "pp","");
		if (Database.areas.containsKey(pid))
			(area=Database.areas.get(pid)).polys.add(this);
		for(String ps : pp.split(","))
			if (ps!=null && ps.length()>0){
				ps= ps.replaceAll("\\[","").replaceAll("\\]","");
				String[] psa= ps.split(":");
				Point p = new Point( Double.parseDouble( psa[0] ), Double.parseDouble( psa[1] ));
				points.add(p);
				System.err.println( area.label+ " added point "+ p ); }}

	
	String store(String parentId){
		StringBuilder def= new StringBuilder("poly;");
		def.append( "pid:"+ parentId+ ";");
		def.append( "cl:"+ closed+ ";");
		def.append( "clr:"+ Utils.encodeColor(clr)+ ";");
		def.append( "bg:"+ Utils.encodeColor(bg)+ ";");
		def.append( "pp:" );
		for(Point p : points)
			def.append( p.toString()+ "," );
		def.append(";\n");
		return def.toString();
	}
	
	synchronized public Point addPoint(Point p, int pos){ return addPoint(p, pos, true); }
	synchronized public Point addPoint(Point p, int pos, boolean update){
		if (p!=null)
			points.add( pos<0 || pos > points.size() ? points.size() : pos, p );
		if (update)
			update();
		return p; }
	
	public void update(){
		path= new Path2D.Double();
		boolean first= true;
		for (Point p : points )
			if (first) { path.moveTo(p.x, p.y); first= false; }
			else path.lineTo(p.x, p.y);
		if (closed && points.size()>0){
			path.closePath(); }}
	
	public Point adjust(Point p, double x, double y){
		if (points.contains(p)){
			p.set(x,y);
			update();
			Obscura.data.writeDatabase(); }
		return p; }

	public void moveBy(double offX, double offY){
		for (Point p : points)
			p.add( offX, offY); }

	
	synchronized public Point[] checkMouse(double rX, double rY, double rad2){
		double min= Double.MAX_VALUE;
		Point[] nearest= null;
		if (points.size()==0) 
			return null;
		Point prev= points.getLast();
		for (Point p : points ){
			double dX= p.x-rX;
			double dY= p.y-rY;
			double d= dX*dX +dY*dY;
			if ( d<rad2 && d<min ){
				min= d;
				nearest= new Point[]{ p }; 
			} else	if (prev!=null){
				dX= (prev.x+p.x)/2-rX;
				dY= (prev.y+p.y)/2-rY;
				d= dX*dX +dY*dY;
				if ( d<rad2 && d<min ){
					min= d;
					nearest= new Point[]{ prev, p }; }}
			prev= p; }
		return nearest; }
	
	
	
	void paint(Graphics2D g, double rX, double rY){
		double z= g.getTransform().getScaleX();
		if (path==null && points.size()>0)
			update();
		if (fill && path!=null){
			g.setColor(bg);
			g.fill(path); }
		g.setColor(new Color(0,0,0,area.activePoly==this && Obscura.viewer.mapEditMode?150:40));
		if (Obscura.viewer.mapEditMode && area.activePoly==this)
			g.setStroke(new BasicStroke((float)(5/z)));
		else
			g.setStroke(new BasicStroke((float)(3/z)));
		if (path!=null)
			g.draw(path);
		g.setColor(Color.WHITE);
		double r= (points.size()==1?5:2)/z;
		if (Obscura.viewer.mapEditMode && area.activePoly==this && points.size()>1){
			System.err.println(g.getTransform().getScaleX());
			g.setFont( new Font("Arial", Font.PLAIN, (int) 20));
			AffineTransform t= g.getTransform();
			AffineTransform tt ;
			Utils.aaOn(g);
			double us= 1/t.getScaleX();
			double c4= Math.cos(Math.PI/4), c41= Math.cos(Math.PI/180*44.5)-Math.cos(Math.PI/180*45);
			double c2= Math.cos(Math.PI/2), c21= Math.cos(Math.PI/180*89.5)-Math.cos(Math.PI/180*90);
			Point last= points.getLast();
			Point v=null, lv= points.size()>2?points.getLast().dup().sub(points.get(points.size()-2)):null;
			
			for (Point p : points){
				v= p.dup().sub(last);
				
				if (lv!=null){
					double ca = Math.abs(Math.cos(v.angle()-lv.angle()));
					if (Math.abs(c2-ca)<c21*4/(lv.length()+v.length()) || Math.abs(c4-ca)<c41*4/(lv.length()+v.length())){
						g.setTransform(t);
						//g.draw(new Ellipse2D.Double( last.x- .5, last.y- .5, 1, 1));
						g.draw(new Line2D.Double( last.x, last.y, last.x- lv.x/4, last.y- lv.y/4 ));
						g.draw(new Line2D.Double( last.x, last.y, last.x+ v.x/4, last.y+ v.y/4 ));
					}
					} else g.fill(new Ellipse2D.Double( p.x- 5/z, p.y- 5/z, 10/z, 10/z));

				tt= (AffineTransform) t.clone();
				tt.translate( p.x- v.x/2+ v.unit().y*us*10, p.y- v.y/2- v.unit().x*us*10 );
				tt.rotate(  v.angle() );
				tt.scale( us, us);
				g.setTransform(tt);
				if (Obscura.viewer.mapEditMode)
					g.drawString(""+(Math.round(v.length()*Utils.ratioMetric*100)/100.00), 0, 0);
					
				last= p;
				lv= v;
			}
			if (v!=null && lv!=null){
				double ca = Math.abs(Math.cos(v.angle()-lv.angle()));
				if (Math.abs(c2-ca)<0.1 || Math.abs(c4-ca)<0.005){
					g.setTransform(t);
					g.draw(new Ellipse2D.Double( last.x- 1, last.y- 1, 2, 2));
					g.draw(new Line2D.Double( last.x, last.y, last.x- lv.x/3, last.y- lv.y/3 ));
					g.draw(new Line2D.Double( last.x, last.y, last.x+ v.x/3, last.y+ v.y/3 ));
				}}
			g.setTransform(t);
			Utils.aaOff(g);
		}
		/*if (Viewer.selRangeP1!=null && Viewer.selRangeP2!=null){
			g.setColor(Color.yellow);
			for (Point p : points)
				if ( p.x> Viewer.selRangeP1.x && p.y> Viewer.selRangeP1.y &&
						p.x< Viewer.selRangeP2.x && p.y< Viewer.selRangeP2.y)
					g.fill(new Ellipse2D.Double( p.x- 4/z, p.y- 4/z, 8/z, 8/z));
			
		}*/

		
		if (area.activePoly==this || points.size()<2)
			for ( Point p : points ){
				g.fill(new Ellipse2D.Double(p.x- r, p.y-r, r*2, r*2));
		}
	}
}