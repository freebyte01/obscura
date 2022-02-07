package obscura;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.channels.FileLock;
import java.util.ArrayList;

public class Obscura {
	static boolean running= true;
	static Watcher watcher;
	public static Viewer viewer;
	public static Database data;
	static FileOutputStream lockStream;
	static FileLock lock;
	
	public static final ArrayList<File> OBSERVE_DIRS= new ArrayList<File>();
	public static final ArrayList<File> CURR_IMAGES= new ArrayList<File>();
	
	public static void main( String[] args ) {
		
		System.err.println( "starting Obscura" );
		System.getProperties().setProperty( "sun.java2d.opengl", "true" ); 
		
		try{
			File lockFile= new File( ".lock" );
			if ( lockFile.exists() 
			&& !lockFile.renameTo( lockFile ))
				throw new RuntimeException( "lock file locked.." );
			
			lockStream = new FileOutputStream( lockFile );
		    lock = lockStream.getChannel().lock();
		    
		} catch ( Exception e ){
			e.printStackTrace();
			System.err.println( "Obscura is running already!" );
			System.exit( 1 ); }
		
		for ( String s : args ){
			System.out.println( "testing watch dir "+ s );
			File f= new File( s );
			if ( f.isDirectory() && f.exists())
				OBSERVE_DIRS.add( f ); }
		
		Thread t = new Thread() {
            public void run() {
            	//data.writeDatabase( "imageDefs.dat" );
            	try{ lock.release(); } catch ( Exception e ){ e.printStackTrace(); }
            	try{ lockStream.close(); } catch( Exception e ){ e.printStackTrace(); }
            	System.out.println( "closing Obscura" );
                Obscura.running= false; }
        };
        
        Runtime.getRuntime().addShutdownHook( t );

        data= new Database( "imageDefs.dat" );
		viewer= new Viewer();

		watcher = new Watcher( OBSERVE_DIRS );
        watcher.start(); }
}