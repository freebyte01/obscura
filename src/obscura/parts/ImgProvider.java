package obscura.parts;

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import javax.imageio.ImageIO;

import com.drew.imaging.jpeg.JpegMetadataReader;
import com.drew.imaging.jpeg.JpegProcessingException;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory;

import ij.ImageJ;
import ij.ImagePlus;
import ij.io.Opener;
import obscura.Database;
import obscura.Obscura;
import obscura.Utils;

public class ImgProvider {
	
	public static BufferedImage providerImgOrig; 
	public static File providerImgOrigF, providerImgMiniF;
	public static HashMap<File, Integer> orientations= new HashMap<File, Integer>(); 
	public static BufferedImage provideImage( File imgPath ) throws IOException{ return provideImage( imgPath, true, 1, 0 ); }
	public static BufferedImage provideImage( File imgPath, boolean genThumb ) throws IOException{ return provideImage( imgPath, genThumb, 1, 0 ); }
	public static BufferedImage provideImage( File imgPath, int level ) throws IOException{ return provideImage( imgPath, true, level, 0 ); }
	
	
	public static HashMap<String, Integer> REQUESTED_IMAGES = new HashMap<>();
	
	
	public static String getThumbsPath( File imgF ){
		String res= ".thumbs/";
		for ( File o : Obscura.OBSERVE_DIRS )
			if ( imgF.getAbsolutePath().toLowerCase().startsWith( o.getAbsolutePath().toLowerCase()))
				return o.getAbsolutePath()+"/.thumbs/";
		return res; }

	
	public static synchronized BufferedImage tryProvideImage( File imgF ) throws Exception {
		return tryProvideImage(imgF, true, 1, 0 ); }

	public static synchronized BufferedImage tryProvideImage( File imgF, boolean genThumb, int requestLevel, int minLevel ) throws Exception {
		if ( REQUESTED_IMAGES.containsKey(imgF.getPath())
		&& REQUESTED_IMAGES.get(imgF.getPath()) > 0 )
			throw new IllegalStateException( "provider not ready" );
		return provideImage( imgF, genThumb, requestLevel, minLevel ); } 
	
	
	public static boolean useImageJ = false;
	
	public static synchronized BufferedImage provideImage( File imgF, boolean genThumb, int requestLevel, int minLevel ) throws IOException{

		try { 
			if ( !REQUESTED_IMAGES.containsKey(imgF.getPath()))
				REQUESTED_IMAGES.put(imgF.getPath(), 1 );
			
			else 
				REQUESTED_IMAGES.put(imgF.getPath(), REQUESTED_IMAGES.get(imgF.getPath()) + 1 );
			
			long wt = System.currentTimeMillis();
			
			if ( imgF == null || !imgF.exists())
				return null;
			
			// System.err.println( "reading img "+ imgF + " req:"+requestLevel+ " min:"+ minLevel );
			
			String thumbsPath= getThumbsPath( imgF );
			
			String thumbPath= thumbsPath + Database.getHashCode( imgF ) + ".jpg";
			
			File thumbFile= new File( thumbPath );
			String miniPath= thumbsPath + "mini/" + Database.getHashCode( imgF )+".jpg";
			
			File miniFile = new File( miniPath );
			BufferedImage orig = null, thumb = null, mini = null;
			
			if ( requestLevel == 1 
			&& thumbFile.exists()) 
				 return thumb= useImageJ ? Opener.openUsingImageIO( thumbFile.getPath()).getBufferedImage() : ImageIO.read( thumbFile ); 
			
			if ( requestLevel == 2
			&& miniFile.exists()) 
				 return mini= useImageJ ? Opener.openUsingImageIO( miniFile.getPath() ).getBufferedImage() : ImageIO.read( miniFile ); 
	
			// read original
			if ( requestLevel == 0 
			|| requestLevel == 1 
				&& !thumbFile.exists() 
			|| requestLevel == 2 
				&& !thumbFile.exists() 
				&& !miniFile.exists()){
		
				System.err.println( requestLevel + " reading orig " + imgF + " : "+ imgF.length() / 1024 + "kB at " + ( System.currentTimeMillis() - wt ) + "ms" );
				
//				BitmapFactory.Options options = new BitmapFactory.Options();
//			    options.inPurgeable = true;
//			    options.inInputShareable = true;
//			    options.inJustDecodeBounds = false;
//			    options.inPreferredConfig = Bitmap.Config.ARGB_8888;
				orig =  providerImgOrig = ( providerImgOrigF == imgF ? providerImgOrig :  useImageJ ? Opener.openUsingImageIO(( providerImgOrigF = imgF ).getPath()).getBufferedImage() : ImageIO.read( providerImgOrigF = imgF ));
				orig = convertTo( orig, BufferedImage.TYPE_INT_RGB ); 
				
				System.err.println( "red orig " 
						+ imgF + " : "+ imgF.length()/1024 + "kB"
						+ " "+ orig.getWidth()+ "x"+ orig.getHeight()
						+ " "+ orig.getWidth() * orig.getHeight() * 4 / 1024 / 1000+ "MB" 
						+ " in " + ( System.currentTimeMillis() - wt ) + "ms" );
				
//				Bitmap bb = BitmapFactory.decodeStream( new FileInputStream(imgF));
				
				}
			
	
			if ( requestLevel == 1 
			|| genThumb 
				&& !thumbFile.exists()){
				
				int maxW = 1024, maxH = 800;
				double iR = orig.getWidth() * 1d / orig.getHeight();
				double z = iR > 1 ? maxW *1d / orig.getWidth() : maxH * 1d / orig.getHeight();
				int w = ( int ) Math.round( orig.getWidth() * z );
				int h = ( int ) Math.round( orig.getHeight() * z );
				
				thumb = new BufferedImage(( int ) w, ( int ) h, orig.getType()); {
					Graphics2D gg = ( Graphics2D ) thumb.createGraphics();
					Utils.aaOn( gg, true );
					gg.drawImage( orig, 0, 0, thumb.getWidth(), thumb.getHeight(), null );
					
					if ( genThumb ) {
						System.err.println( "writing thumb "+ thumbFile + " in " + ( System.currentTimeMillis() - wt ) + "ms" ); 
						thumbFile.getParentFile().mkdirs();
						ImageIO.write( thumb, "jpg", thumbFile );}}
				
				System.err.println( imgF+ "( "+ imgF.length()+ " )"
						+ "( "+ ( orig.getWidth()*orig.getHeight()*4 )/1024+ "kB )"
						+ " creating thumb " + thumbFile+ "( "+ thumbFile.length()+ " )( "+ ( thumb.getWidth()*thumb.getHeight()*4 )/1024+ "kB )"
						+ " in " + ( System.currentTimeMillis() - wt ) + "ms" );
				
				if ( !genThumb && minLevel == 1 )
					return thumb; }
				
			if ( minLevel == 2 
			&& genThumb 
			&& !miniFile.exists()){
			
				if ( thumb == null )
					 return thumb= useImageJ ? Opener.openUsingImageIO( thumbFile.getPath() ).getBufferedImage() : ImageIO.read( thumbFile );
				
				int w= ( int ) Math.round( thumb.getWidth()/4 );
				int h= ( int ) Math.round( thumb.getHeight()/4 );
				double rw= w, rh= h;
				
				int orientation = getOrientation( imgF );
				switch ( orientation ) {
				case 1:
				case 2: // Flip X
				case 3: // PI rotation
				case 4: // Flip Y
					break;
				case 5: // - PI/2 and Flip X
				case 6: // -PI/2 and -width
				case 7: // PI/2 and Flip
				case 8: // PI / 2
					rw= h;
					rh= w;
					break;
				default:
					break; }
				
				mini = new BufferedImage(( int )rw, ( int )rh, thumb.getType());
				providerImgMiniF = imgF;
				
				if ( mini != null ){
					
					Graphics2D gg = ( Graphics2D ) mini.createGraphics();
					Utils.aaOn( gg, true );
					gg.translate( rw/2, rh/2 );
	
					gg.setTransform( ImgProvider.getAffineForOrientation( gg.getTransform(), orientation, ( int ) Math.round( w ), ( int ) Math.round( h )) );
					gg.drawImage( thumb, 0, 0, ( int )w, ( int )h, null );
					
					if ( genThumb ) {
						miniFile.getParentFile().mkdirs();
						ImageIO.write( mini, "jpg", miniFile );
						System.err.println( "writing mini thumb "+ thumbFile + " in " + ( System.currentTimeMillis() - wt ) + "ms" ); }}
				
				if ( minLevel == 2 )
					return mini; }
	
			if ( minLevel == 1 )
				return thumb;
	
			System.err.println( "providing img "+ imgF + " in " + ( System.currentTimeMillis() - wt ) + "ms" );
			
			return orig; 

		} finally {
			if ( REQUESTED_IMAGES.containsKey(imgF.getPath())) {
				REQUESTED_IMAGES.put(imgF.getPath(), REQUESTED_IMAGES.get(imgF.getPath()) - 1 );
				if ( REQUESTED_IMAGES.get(imgF.getPath()) == 0 )
					REQUESTED_IMAGES.remove(imgF.getPath()); }}}
	
	
	
	public static BufferedImage convertTo( BufferedImage image, int imgType ){
	    BufferedImage newImage = new BufferedImage(
	        image.getWidth(), image.getHeight(),
	        imgType );
	    Graphics2D g = newImage.createGraphics();
	    g.drawImage( image, 0, 0, null );
	    g.dispose();
	    return newImage; }
	
	
	public static final int getOrientation( File imgF ){
		
		if ( orientations.containsKey( imgF ))
			return orientations.get( imgF );
		
		if ( imgF.getName().toLowerCase().matches( ".*\\.jpeg|.*\\.jpg" ))
			
			try {
				Metadata metadata = JpegMetadataReader.readMetadata( imgF );
				ExifIFD0Directory exifIFD0Directory = metadata.getFirstDirectoryOfType( ExifIFD0Directory.class );
				
				if ( exifIFD0Directory == null )
					return 1;
				
				try {
					
					int orientation= exifIFD0Directory == null ? 
															1 
															: exifIFD0Directory.containsTag( ExifIFD0Directory.TAG_ORIENTATION )? exifIFD0Directory.getInt( ExifIFD0Directory.TAG_ORIENTATION ) : 1;
					Date dateTime= exifIFD0Directory == null ? 
															new Date( imgF.lastModified()) 
															: exifIFD0Directory.containsTag( ExifIFD0Directory.TAG_DATETIME ) ? exifIFD0Directory.getDate( ExifIFD0Directory.TAG_DATETIME ) : new Date( imgF.lastModified());
					
					System.err.println( "!!! *** "+imgF.getName()+ " date "+ dateTime + " mod "+ new Date( imgF.lastModified()));
					
					if ( dateTime.getTime()!=0 && dateTime.getTime()< imgF.lastModified()){
						/*int dialogResult = JOptionPane.showConfirmDialog ( null, 
							"!!! *** "+imgF.getName()+ " found disproportion on image date "+ dateTime + " vs mod "+ new Date( imgF.lastModified())+
							"\nDo you want to correct the modification date?",
							"Warning",
							JOptionPane.YES_NO_OPTION );
						if( dialogResult == JOptionPane.YES_OPTION )
							imgF.setLastModified( dateTime.getTime());*/						
					}
					orientations.put( imgF, orientation );
					return orientation;
					
				} catch ( Exception ex ) {
					ex.printStackTrace(); }
				
			} catch ( JpegProcessingException e ) {
				System.err.println( e );
				
			} catch ( IOException e ) {
				System.err.println( e ); }
		
		return 1; }
	
	
	public static final AffineTransform getAffineForOrientation( AffineTransform current, int orientation, int width, int height ){

		AffineTransform affineTransform = current;
		switch ( orientation ) {
		case 1:
			affineTransform.translate( -width/2, -height/2 );
			break;
		case 2: // Flip X
			affineTransform.scale( -1.0, 1.0 );
			affineTransform.translate( -width, 0 );
			break;
		case 3: // PI rotation
			affineTransform.translate( width, height );
			affineTransform.rotate( Math.PI );
			affineTransform.translate( width/2, height/2 );
			break;
		case 4: // Flip Y
			affineTransform.scale( 1.0, -1.0 );
			affineTransform.translate( -width/2, -height/2 );
			break;
		case 5: // - PI/2 and Flip X
			affineTransform.rotate( -Math.PI / 2 );
			affineTransform.scale( -1.0, 1.0 );
			affineTransform.translate( -width/2, -height/2 );
			break;
		case 6: // -PI/2 and -width
			affineTransform.translate( height, 0 );
			affineTransform.rotate( Math.PI / 2 );
			affineTransform.translate( -width/2, height/2 );
			break;
		case 7: // PI/2 and Flip
			affineTransform.scale( -1.0, 1.0 );
			affineTransform.translate( -height, width );
			affineTransform.rotate( 3 * Math.PI / 2 );
			affineTransform.translate( width/2, height/2 );
			break;
		case 8: // PI / 2
			affineTransform.translate( 0, width );
			affineTransform.rotate( 3 * Math.PI / 2 );
			affineTransform.translate( width/2, -height/2 );
			break;
		default:
			break; }

		return affineTransform; }

}