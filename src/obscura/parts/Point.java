package obscura.parts;

public class Point{
	
	public double x, y;
	private double a=Double.MIN_VALUE, l= Double.MIN_VALUE;
	
	private Point u;
	
	public Point() {}
	
	public Point( String def ) {
		
		if ( def!=null && def.contains( "/" )){
			String[] defA= def.split( "/" );
			try{
				x= Double.parseDouble( defA[ 0 ]);
				y= Double.parseDouble( defA[ 1 ]);
				
			} catch ( NumberFormatException e ){ System.err.println( "cant read point "+ def ); }}}
	
	public Point( double x, double y ) { 
		this.x= x; this.y= y; }
	
	public Point set( double x, double y ) { 
		this.x = x; 
		this.y = y; 
		a = l = Double.MIN_VALUE; 
		u = null; 
		return this; }
	
	public String serialize() { 
		return Math.round( x*10000 ) / 10000.00 + "/" + Math.round( y*10000 ) / 10000.00; }
	
	public Point add( double x, double y ) { 
		this.x += x; this.y += y; return this; }
	
	public Point add( Point p ) { 
		this.x += p.x; this.y += p.y; return this; }
	
	public Point sub( double x, double y ) { 
		this.x -= x; this.y-= y; return this; }
	
	public Point sub( Point p ) { 
		this.x -= p.x; this.y-= p.y; return this; }
	
	public Point mul( double m ) { 
		this.x *= m; this.y*= m; return this; }
	
	public Point mul( double mx, double my ) { 
		this.x *= mx; this.y*= my; return this; }
	
	public double length(){ 
		return l == Double.MIN_VALUE ? 
					Math.sqrt( x*x + y*y )
					: l; }
	
	public double angle(){
		
		if ( a != Double.MIN_VALUE )
			return a;
		
		double ac= Math.acos( this.unit().x );
		double as= Math.asin( this.unit().y );
		
		a= x < 0 ? 
				y < 0 ? 
					Math.PI - as 
					: ac 
				: y < 0 ? 
					as : ac;
		return a; }
	
	public Point unit(){ return u!=null? u : ( u = this.dup().mul( 1/this.length())); }
	public Point dup(){ return new Point( x, y ); }
	
	public String toString() { 
		return "[ "+ Math.round( x*10000 )/10000.00+ ":"+ Math.round( y*10000 )/10000.00+ " ]"; }
	
	public static Point avg( Point[] points ){
		Point res= new Point();
		for ( Point p : points )
			res.add( p.x, p.y );
		return res.mul( points.length == 0 ? 
							1
							: 1d / points.length ); }
}