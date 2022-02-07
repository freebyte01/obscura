package obscura.parts;

public class Edge{
	
	public double x, y;
	private double a=Double.MIN_VALUE, l= Double.MIN_VALUE;
	
	private Edge u;
	
	public Edge() {}
	
	public Edge( String def ) {
		
		if ( def!=null && def.contains( "/" )){
			String[] defA= def.split( "/" );
			try{
				x= Double.parseDouble( defA[ 0 ]);
				y= Double.parseDouble( defA[ 1 ]);
				
			} catch ( NumberFormatException e ){ System.err.println( "cant read point "+ def ); }}}
	
	public Edge( double x, double y ) { 
		this.x= x; this.y= y; }
	
	public Edge set( double x, double y ) { 
		this.x = x; 
		this.y = y; 
		a = l = Double.MIN_VALUE; 
		u = null; 
		return this; }
	
	public String serialize() { 
		return Math.round( x*10000 ) / 10000.00 + "/" + Math.round( y*10000 ) / 10000.00; }
	
	public Edge add( double x, double y ) { 
		this.x += x; this.y += y; return this; }
	
	public Edge add( Edge p ) { 
		this.x += p.x; this.y += p.y; return this; }
	
	public Edge sub( double x, double y ) { 
		this.x -= x; this.y-= y; return this; }
	
	public Edge sub( Edge p ) { 
		this.x -= p.x; this.y-= p.y; return this; }
	
	public Edge mul( double m ) { 
		this.x *= m; this.y*= m; return this; }
	
	public Edge mul( double mx, double my ) { 
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
	
	public Edge unit(){ return u!=null? u : ( u = this.dup().mul( 1/this.length())); }
	public Edge dup(){ return new Edge( x, y ); }
	
	public String toString() { 
		return "[ "+ Math.round( x*10000 )/10000.00+ ":"+ Math.round( y*10000 )/10000.00+ " ]"; }
	
	public static Edge avg( Edge[] points ){
		Edge res= new Edge();
		for ( Edge p : points )
			res.add( p.x, p.y );
		return res.mul( points.length == 0 ? 
							1
							: 1d / points.length ); }
}