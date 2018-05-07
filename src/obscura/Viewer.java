package obscura;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Composite;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;
import javax.swing.DefaultListModel;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.ListCellRenderer;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.drew.imaging.jpeg.JpegMetadataReader;
import com.drew.imaging.jpeg.JpegProcessingException;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory;

import obscura.parts.Area;
import obscura.parts.ImgDef;
import obscura.parts.ImgProvider;
import obscura.parts.Point;
import obscura.parts.Poly;

public class Viewer extends JFrame implements KeyListener{
	boolean mapEditMode;

	public Viewer() {
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		//setUndecorated(true);
		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		GraphicsDevice[] gs = ge.getScreenDevices();
		for(GraphicsDevice curGs : gs){
			GraphicsConfiguration[] gc = curGs.getConfigurations();
			for(GraphicsConfiguration curGc : gc){
				Rectangle bounds = curGc.getBounds();

				System.out.println(bounds.getX() + "," + bounds.getY() + " " + bounds.getWidth() + "x" + bounds.getHeight());
				setSize((int)Math.round(bounds.getWidth()/2), (int)Math.round(bounds.getHeight()/2)); }}
		setVisible(true);
		init();
	}
	private JSplitPane split;
	private JSplitPane splitViewer;
	public JList<File> list;
	public ListModel<File> listModel= new DefaultListModel<File>();
	private JPanel view, map;
	private Container editor;
	static SimpleDateFormat sdf = new SimpleDateFormat("YY.MM.dd HH:mm ");

	static String shortName(String name){
		for (File d : Obscura.observeDirs)
			if (name.startsWith(d.getPath()))
				return name.substring(d.getPath().length()+1);
		return name;
	}	

	boolean isMapMode(){
		return ctrl || mapEditMode;
	}

	double zoom=1;
	double sx=0, sy=0;
	int splitPos= 200;
	Color definedC= new Color(230, 230,230);
	Color selectedC= new Color(230, 230,240);
	Color definedCT= new Color(225, 225,230);
	Font listFont= new Font("Courier New", Font.PLAIN, 12);
	Font listFontB= new Font("Courier New", Font.BOLD, 12);
	void init(){

		getContentPane().add( split= new JSplitPane(JSplitPane.HORIZONTAL_SPLIT));
		split.setLeftComponent( new JScrollPane( list= new JList<File>()){
			
		});
		list.setModel(listModel);
		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		list.setCellRenderer(new ListCellRenderer<File>() {
			public Component getListCellRendererComponent( JList<? extends File> list, File value, int index, boolean isSelected, boolean cellHasFocus) {
				int hash= value.hashCode();
				boolean hasThumb= new File(".thumbs/"+hash+".jpg").exists();
				JLabel c= new JLabel(sdf.format(new Date(value.lastModified()))+ (hasThumb?"[] ":"   ")+ shortName(value.getPath()));
				c.setFont(isSelected?listFontB:listFont);
				c.setForeground(isSelected?Color.black:Color.gray);

				if (Database.images.containsKey(hash)){
					c.setOpaque(true);
					c.setBackground(hasThumb?definedCT:definedC);
				}
				return c;
			}
		});
		split.setRightComponent( splitViewer= new JSplitPane(JSplitPane.VERTICAL_SPLIT));
		split.setDividerLocation(300);
		split.setContinuousLayout(true);

		
		splitViewer.addPropertyChangeListener(JSplitPane.DIVIDER_LOCATION_PROPERTY, 
			    new PropertyChangeListener() {
					public void propertyChange(PropertyChangeEvent evt) {
						if (splitViewer!=null && view!=null && splitViewer.getHeight()-view.getMinimumSize().height>splitPos)
							splitPos= getHeight()- splitViewer.getDividerLocation();
					}
				});
		
		splitViewer.addComponentListener(new ComponentListener() {
			
			public void componentShown(ComponentEvent e) {}
			
			public void componentResized(ComponentEvent e) {
				if (getHeight()-view.getMinimumSize().height> splitPos){
					splitViewer.setDividerLocation(getHeight()-splitPos);
				}
			//splitPos= getHeight()- splitViewer.getDividerLocation();
				
			}
			
			public void componentMoved(ComponentEvent e) {}
			
			public void componentHidden(ComponentEvent e) {}
		});
		splitViewer.setTopComponent( view= new JPanel(){
			public void paint(Graphics g) {
				super.paint(g);
				paintView((Graphics2D) g); 
			}});
		view.setMinimumSize(new Dimension(200,200));
		view.setBackground(Color.black);
		view.addMouseWheelListener(new MouseWheelListener() {
			public void mouseWheelMoved(MouseWheelEvent e) {
				if (isMapMode()){
					System.err.println(zoom);
					zoom-= e.getWheelRotation()*zoom/10d;
					sx=(mX- view.getWidth()/2)/zoom-rX;
					sy=(mY- view.getHeight()/2)/zoom-rY;
					repaint();
				} else {
					if (e.getX() > view.getWidth()-200){
//						nearestSel+= e.getWheelRotation();
//						//nearestSel= nearestSel<0?0:nearestSel;
//						nearPic= shift ? 
//								nearTargs.length > 0 ? Watcher.images.get( nearTargs[Math.abs(nearestSel%nearTargs.length)].hash ) : null 
//										: nearLocs.length>0? Watcher.images.get( nearLocs[Math.abs(nearestSel%nearLocs.length)].hash ) : null;
//						selectNearPic();
//										
					} else {
						nearImgF= null;
						int newIndex= list.getSelectedIndex()+ e.getWheelRotation();
						list.setSelectedIndex( newIndex< 0 ? 0 : newIndex > list.getModel().getSize()- 1 ? list.getModel().getSize()-1 : newIndex);
						list.ensureIndexIsVisible(list.getSelectedIndex());
					}
				}
			}
		});
		view.addMouseMotionListener(new MouseMotionListener() {
			public void mouseMoved(MouseEvent e) {
				onMouseMove(e);
			}

			public void mouseDragged(MouseEvent e) {
				
				drag= true; //!mapEditMode && shift;
				//if (!ctrl)
				//	return;
				mouseMoved(e);
				

				if (!isMapMode()){
					imgOff.add(mX-mXo, mY- mYo);
				} else
					if (selectedArea!=null && pickedPoint!=null && pickedPoint.length>0){
						if (shift)
								selectedArea.getPolyFor(pickedPoint).moveBy( rX-rXo,rY-rYo);
						else 
							if (pickedPoint.length==1)
								pickedPoint[0].set(rX,rY);
							else
								for (Point p : pickedPoint)
									p.add(rX-rXo,rY-rYo);
						selectedArea.update(false);
					} else {
						sx+= (rX-rXo);
						sy+= (rY-rYo);
					}
					
				mouseMoved(e);
				//System.err.println("dragging");
				repaint();
			}
		});

		splitViewer.setBottomComponent( editor= new Container() );
		splitViewer.setContinuousLayout(true);
		splitViewer.setDividerLocation(0.8);

		list.addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent e) {
				selectImage(list.getSelectedValue());
			}
		});
		
		new Thread(){ // slideshow timer
			 public void run() { 
				 while(true){
					try{ sleep(5); }catch (InterruptedException e){}
					if (slideShow && System.currentTimeMillis()>(lastSlideTime+25))
						goNextPic(); }}
		}.start();

		new Thread(){ // lazy img reader
			 public void run() { 
				 while(true){
					try{ sleep(200); }catch (InterruptedException e){}
					if ( viewedImgF !=null && viewedOrigF != viewedImgF && System.currentTimeMillis()>( selectedImgTime + 300)){ // if still on the same pic, show it in it's original resolution (refine) if not already orig
						try { 
							System.err.println("lazy loading orig "+ selectedImgF);
							if (viewedImgF==selectedImgF) selectedImg = ImgProvider.provideImage( viewedOrigF = selectedImgF, false, 0, 0);
							else if (viewedImgF==nearImgF) nearImg= ImgProvider.provideImage( viewedOrigF = nearImgF, false, 0, 0);								
							view.repaint(); } 
						catch (IOException e){ e.printStackTrace(); }}}}
		}.start();

		addKeyListener(this);
		list.addKeyListener(this);
		view.addKeyListener(this);
		//map.addKeyListener(this);
		view.addMouseListener(new MouseListener() {

			public void mouseReleased(MouseEvent e) { 
				if ( pickedArea!=null && pickedPoint!=null && drag )
					pickedArea.update(true);
				if (mapEditMode && shift){
					selRangeP2= new Point(rX, rY);
				}
				drag= false;
				pickedPoint= null;
				view.repaint();
				}

			public void mousePressed(MouseEvent e) {
				
				selRangeP1= selRangeP2= null;
				
				if (mapEditMode){
					pickedPoint= overPoint;
					if (selectedArea!=null)
						selectedArea.activePoly= selectedArea.getPolyFor(pickedPoint);
				}
			}

			public void mouseExited(MouseEvent e) { 
				System.err.println("mouse exited "+ e.getX()+ view.getWidth());
				onMouseMove(e); 
				view.repaint(); }

			public void mouseEntered(MouseEvent e) { }

			public void mouseClicked(MouseEvent e) {
				System.err.println("clicked "+e.getClickCount());	
				if ( !mapEditMode ){
					if (ctrl){
						if (e.getClickCount()>1){
							clickSelect= null;
							if (selectedImgF!=null){
								currDef= Obscura.data.getImgDef(selectedImgF.hashCode(), true);
								if (next){
									if (currDef.targ!=null) currDef.targ.set( rX, rY);
									else currDef.targ= new Point( rX, rY);
								} else {
									if (currDef.obs!=null) currDef.obs.set( rX, rY);
									else currDef.obs= new Point( rX, rY);
								}
								next= !next;
								currDef.path= selectedImgF.getAbsolutePath();
								Obscura.data.writeDatabase();
							}
						} else {
							clickSelect= nearest; 
							new Thread() {
								public void run() {
									try{ 
										sleep(500);
										if (clickSelect!=null){
											list.setSelectedValue(Watcher.images.get(clickSelect.hash), true);
											list.ensureIndexIsVisible(list.getSelectedIndex());
										}
									} catch (InterruptedException e){}							
								}
							}.start();
						}
					} else 
						if (nearImgF!=null){
							list.setSelectedValue(Watcher.images.get(nearImgF.hashCode()), true);
							list.ensureIndexIsVisible(list.getSelectedIndex());
						}

				} else { // MAP EDIT

					if (e.getClickCount()>1){
						if (overPoint!=null)
							if (overPoint.length==1)
								selectedArea.removePoint(overPoint[0]);
							else
								pickedPoint= new Point[]{ selectedArea.addPoint( new Point(rX, rY) , overPoint) };
						else 
							if (shift){
								Poly pl= selectedArea.addPoly();
								pickedPoint= new Point[] { selectedArea.addPoint(new Point(rX, rY), pl, 0) };
							} else
								pickedPoint= new Point[] { selectedArea.addPoint(new Point(rX, rY)) };
						view.repaint();
					}
				}
			}
		});		
		readMaps();
	}
	
	static Point selRangeP1, selRangeP2;
	
	void onMouseMove(MouseEvent e){
		ctrl= e.isControlDown();
		shift= e.isShiftDown();
		alt= e.isAltDown();
		mXo= mX;
		mYo= mY;
		mX= e.getX();
		mY= e.getY();
		rXo= rX;
		rYo= rY;
		rX=(mX- view.getWidth()/2)/zoom -sx;
		rY=(mY- view.getHeight()/2)/zoom -sy;

		int b1 = MouseEvent.BUTTON1_DOWN_MASK;
		int b2 = MouseEvent.BUTTON2_DOWN_MASK;
		if ( (e.getModifiersEx() & (b1 | b2)) == 0) {
			drag= false;
		} else {
			if (mapEditMode && shift){
				selRangeP2= new Point(rX, rY);
			}
		}

	
		if (!drag){
			overTime= System.currentTimeMillis();
			overPoint= null;
			if (mapEditMode){
				for(Area a : Database.areas.values())
					if ((overPoint= a.checkMouse(rX, rY, 100/zoom/zoom))!=null){
						overArea= a;
						break; }
			}
			repaint(); 
		} else
			if (mapEditMode){
				
			} else if (selectedImgF!=null){
				ImgDef def= Database.images.get(selectedImgF.hashCode());
				if (def!=null && def.obs!=null){
					double vx= rXo- def.obs.x, vy= rYo- def.obs.y;
					double radius= 10/zoom;
					double radius2 = radius*radius;
					if ( vx*vx+vy*vy< radius2 ){
						def.obs.x+= rX- rXo;
						def.obs.y+= rY- rYo;
					} else if (def.targ!=null){
						vx= rXo- def.targ.x; vy= rYo- def.targ.y;
						if ( vx*vx+vy*vy< radius2 ){
							def.targ.x+= rX- rXo;
							def.targ.y+= rY- rYo; }}}}
		
		}	

	long selectedImgTime, lastSlideTime;
	synchronized void goNextPic(){
		nearImgF= null;
		list.setSelectedIndex( ((list.getSelectedIndex()+ 1) % list.getModel().getSize()) );
	}
	
	public Point[] pickedPoint, overPoint;
	boolean isPicked(Object o){
		if (o instanceof Point){
			if (pickedPoint!=null)
				for (Point p : pickedPoint)
					if (p == o)
						return true;
		}
		return false;
	}
	
	Area pickedArea;

	int mX, mY, mXo, mYo;
	double rX, rY, rXo, rYo;
	long overTime;
	ImgDef[] nearLocs, nearTargs;
	ImgDef clickSelect, nearest; 
	int nearestSel;
	double imgZoom=1;
	Point imgOff= new Point(0,0);

	void selectImage(File imgF){
		if (selectedImgF == imgF)
			return;
		imgOff.set(0,0);
		imgZoom= 1;
		nearestSel=0;
		selectedImgF= imgF;
		selectedImg=null;
		lastDef= currDef;
		if (selectedImgF!=null && selectedImgF.exists()){
			currDef= Obscura.data.getImgDef(selectedImgF.hashCode());
		}
		next= false;		
		nearLocs= currDef==null ? null : currDef.sameLocation(2); 
		nearTargs= currDef==null ? null : currDef.sameTarget(2); 
		System.out.println("pic changed to "+ selectedImgF.getName()+ " : "+ selectedImgF.hashCode());
		lastSlideTime= System.currentTimeMillis();
		selectedImgTime= lastSlideTime;
		lastThumbsHeight= 0;
		thumbs.clear();
		view.repaint();
		
		}
	

	void selectNearPic(){
		if (nearImgF!=null){
			if ( nearImgF!= lastNear ){ 
				try{
					System.err.println("reading near pic "+ nearImgF);
					nearImg= ImgProvider.provideImage( nearImgF );
					selectedImgTime= System.currentTimeMillis();
					lastNear= nearImgF;
					setTitle(shortName(selectedImgF.getPath())+ " -> "+ shortName(nearImgF.getPath()));
				} catch (Exception e) {	e.printStackTrace(); }
				view.repaint();	
			}
		} else
			if (nearImg!=null){
				nearImg= null;
				view.repaint();	
			}
		
	}
	
	boolean ctrl, shift, alt, drag, next, slideShow;
	public void keyPressed(KeyEvent e) {
		ctrl=e.isControlDown();
		shift= e.isShiftDown();
		alt= e.isAltDown();
		view.repaint();
		if (e.getKeyCode()=='='){
			imgZoom*=1.2;
			view.repaint();
		} else if (e.getKeyCode()=='-'){
			imgZoom*=.8;
			view.repaint();
		} else if (e.getKeyCode()=='m' || e.getKeyCode()=='M'){
			mapEditMode= !mapEditMode;
			view.repaint();
		} else if (e.getKeyCode()=='s' || e.getKeyCode()=='S'){
			lastSlideTime= System.currentTimeMillis();
			slideShow= !slideShow;
			System.err.println("slide show mode "+ slideShow);
		} else if (e.getKeyCode()==' '){
			System.err.println("space pressed");
			if (lastDef!=null)
				currDef= lastDef.cloneTo(selectedImgF.hashCode(), selectedImgF.getAbsolutePath());
			view.repaint();
			list.repaint();
		}
	}

	public void keyReleased(KeyEvent e) {
		System.out.println("key released");
		ctrl=e.isControlDown();
		shift= e.isShiftDown();
		alt= e.isAltDown();
		next= false;
		view.repaint(); }

	public void keyTyped(KeyEvent arg0) {}


	HashMap<String, BufferedImage> maps= new HashMap<String, BufferedImage>();
	static final Pattern allowedFiles= Pattern.compile(".*\\.jpg|.*\\.png");
	void readMaps(){
		File mapsDir= new File("maps");
		if (!mapsDir.exists() || !mapsDir.isDirectory()){
			System.err.println("no maps found!");
			return;
		}
		for (File m : mapsDir.listFiles()){
			if (m.isDirectory() || m.getName().startsWith(".") || !allowedFiles.matcher(m.getName().toLowerCase()).matches() || m.getName().indexOf("_map")<0)
				continue;

			try{
				BufferedImage img= ImageIO.read(m);
				maps.put(m.getName(), img);
				System.out.println("found map file "+ m.getName());
			} catch (Exception e) {	e.printStackTrace(); }
		}
	}

	File viewedOrigF, selectedImgF, viewedImgF, nearImgF, lastNear;
	ImgDef currDef, lastDef;
	BufferedImage selectedImg, viewedImg, nearImg ;


	synchronized void paintView(Graphics2D g){

		if ( nearImgF==null && selectedImgF != null && selectedImg==null )
			if ( viewedImgF != selectedImgF )
				try{
					selectedImg= ImgProvider.provideImage( selectedImgF );
					viewedOrigF= ImgProvider.providerImgOrigF == viewedImgF ? selectedImgF : null; // no need to refine image later - already in original size
					if (selectedImg!=null) setTitle(shortName(selectedImgF.getPath()));
					view.repaint();
				} catch (Exception e) {	e.printStackTrace(); }

		viewedImg= isMapMode() ? maps.size()>0 ? maps.values().iterator().next() : null : nearImgF!=null ? nearImg : selectedImg;
		viewedImgF= isMapMode() ? maps.size()>0 ? null : null : nearImgF!=null ? nearImgF : selectedImgF;
		
		if (viewedImg==null) 
			return;

		double vw= view.getWidth();
		double vh= view.getHeight();
		double iw = viewedImg.getWidth(null);
		double ih = viewedImg.getHeight(null);


		AffineTransform old= g.getTransform();
		if (isMapMode()){
			g.translate(vw/2, vh/2);
			g.scale(zoom, zoom);
			g.translate(sx, sy);

			//g.drawLine(0, 0, (int) rX, (int) rY);
			int rule = AlphaComposite.SRC_OVER;
			Composite comp = AlphaComposite.getInstance(rule , .7f );
			g.setComposite(comp );
			Utils.aaOff(g);
			g.drawImage(viewedImg, 0, 0, (int) Math.round( iw ), (int) Math.round( ih ), null);
			comp = AlphaComposite.getInstance(AlphaComposite.SRC_OVER , 1f );
			g.setComposite(comp );
			//drawDef(g, lastDef, 0.3f);
			//g.fill(new Ellipse2D.Double(currDef.x-8/zoom, currDef.y-8/zoom, 16/zoom, 16/zoom));


			double radius= 10/zoom;
			double radius2 = radius*radius;
			double min= Double.MAX_VALUE;

			Color cPos= new Color(1f,1f,0f,.5f);
			double posR= 2/zoom, posD= posR*2;
			
			for(ImgDef id : Database.images.values())
				
				if (id.obs!=null){
					double vx= rX-id.obs.x, vy= rY-id.obs.y;
					double diff= vx*vx+vy*vy;
					if ( min > diff ){
						min= diff;
						nearest= id; }
					if (diff<radius2){
						Utils.aaOn(g, true);
						id.paint( g, (float)(1-diff/radius2));
					} else {
						Utils.aaOff(g);
						g.setColor(cPos);
						g.fill(new Rectangle2D.Double(id.obs.x-posR, id.obs.y-posR, posD, posD)); }
				}

			for ( Area a : Database.areas.values() )
				a.paint(g, rX, rY);


			if (lastDef!=null)
				lastDef.paint(g, .5f);

			Utils.aaOn(g);

			g.setStroke(new BasicStroke((float)(2/zoom)));
			g.draw(new Ellipse2D.Double(rX- radius, rY-radius, radius*2, radius*2));

			if (currDef==null){
			} else {
				if (nearLocs!=null)
					for(ImgDef ic : nearLocs)
						ic.paint(g, 0.3f);
				else 
					if (lastDef!=null)
						lastDef.paint(g,  0.5f);
				if (nearTargs!=null)
					for(ImgDef it : nearTargs)
						it.paint(g, 0.3f);
			}

			if (currDef!=null) 
				currDef.paint(g, 1);

			g.setColor(Color.yellow);
			if (overPoint!=null)
				for (Point p : overPoint)
					g.draw(new Ellipse2D.Double(p.x-5/zoom, p.y-5/zoom, 10/zoom, 10/zoom));

			if (mapEditMode && shift){
				if (selRangeP1!=null && selRangeP2!=null){
					g.setColor(Color.yellow);
					g.setStroke(new BasicStroke((float)(3/zoom), BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{(float)(9/zoom)}, 0));
					g.draw(new Rectangle2D.Double(selRangeP1.x, selRangeP1.y, selRangeP2.x-selRangeP1.x, selRangeP2.y-selRangeP1.y));
				}
			}

			
			g.setTransform( old );
			paintMap(g);
			

		} else

			if (viewedImg!=null){


				int orientation = 1;
				try { orientation= ImgProvider.getOrientation(nearImgF==null?selectedImgF:nearImgF); } catch (Exception e){} 
				double rw= vw, rh= vh;

				switch (orientation) {
				case 1:
				case 2: // Flip X
				case 3: // PI rotation
				case 4: // Flip Y
					break;
				case 5: // - PI/2 and Flip X
				case 6: // -PI/2 and -width
				case 7: // PI/2 and Flip
				case 8: // PI / 2
					rw= vh;
					rh= vw;
					break;
				default:
					break;
				}       

				double zw= rw/iw*imgZoom;
				double zh= rh/ih*imgZoom;	
				double z= zw>zh?zh:zw;

				g.setTransform(old);

				g.translate( view.getWidth()/2+ imgOff.x, view.getHeight()/2+ imgOff.y );

				g.setTransform( ImgProvider.getAffineForOrientation(g.getTransform(), orientation, (int) Math.round(iw*z), (int) Math.round(ih*z)) );
				
				if (z<1) 
					Utils.aaOn(g);

				g.drawImage(viewedImg, 0, 0, (int) Math.round( iw*z ), (int) Math.round( ih*z  ), null);

				g.setTransform(old);
				
				if (mX>view.getWidth()-200 ){ //&& mX<view.getWidth()  && mY>=0 && mY<view.getHeight()){
					try { paintNearThumbs(g, 200); } catch (IOException e){}
				} else {
					nearImgF= null;
				}

				if (currDef!=null){
					if (currDef.obs!=null){
						g.setColor(Color.blue);
						g.fillOval(20, (int)(vh-40), 20, 20);
					}
					if (currDef.targ!=null){
						g.setColor(Color.yellow);
						g.fillOval(20, (int)(vh-70), 20, 20);
					}
				}
			}

		g.setTransform(old);
		
		
		if  (viewedImg!=null)
			if (viewedImg==selectedImg)
				Utils.drawOutlinedString(g, sdf.format(new Date(selectedImgF.lastModified())), 20, 30);
			else if (nearImgF!=null && nearImg==viewedImg)
				Utils.drawOutlinedString(g, sdf.format(new Date(nearImgF.lastModified())), 20, 30);
			else 
				Utils.drawOutlinedString(g, maps.keySet().iterator().next(), 20, 30);
	}
	


	private int lastThumbsHeight;
	private LinkedHashMap<File, BufferedImage> thumbs= new LinkedHashMap<File, BufferedImage>();
	void paintNearThumbs(Graphics2D g, int stripWidth) throws IOException{
		g.setColor(new Color(255,255,255,50));
		g.fillRect(view.getWidth()- stripWidth, 0, 200, getHeight());
		
		int slideX= view.getWidth()- stripWidth+ 5, totalHeight=0;
		int offY= lastThumbsHeight==0 ? 0 : lastThumbsHeight < view.getHeight() ? 0 : (int) Math.round( ( view.getHeight()+40- lastThumbsHeight) * ( mY* 1d/ view.getHeight() ) );
		
		if ( nearLocs != null )
			for (ImgDef def : nearLocs){
				if (def.file==null)
					if (def.path!=null)	def.file= new File(def.path);
					else continue;
				BufferedImage thumb= thumbs.containsKey( def.file ) ? thumbs.get( def.file ) : ImgProvider.provideImage( def.file, true, 2, 2 );
				if (thumb==null){
					System.err.println(def+ " loc null!");
					continue; }
				thumbs.put( def.file, thumb );
				int imgH= (int) Math.round(thumb.getHeight()*((stripWidth-10)*1d/thumb.getWidth()));
				if (offY< view.getHeight() && offY+ totalHeight+ imgH > 0) 
					g.drawImage( thumb, slideX, offY+ totalHeight , stripWidth-10, imgH, null );
				if (mY>offY+totalHeight && mY<offY+imgH+totalHeight){
					nearImgF = def.file;
					selectNearPic(); }
				totalHeight+= imgH+5;
			}
		g.setColor(Color.black);
		g.setStroke(new BasicStroke(30));
		g.drawLine(slideX, offY+ totalHeight+15, view.getWidth(), offY+ totalHeight+15);
		g.drawOval(slideX-35, offY+ totalHeight, 30, 30);
		g.setColor(Color.white);
		g.setStroke(new BasicStroke(10));
		g.drawLine(slideX-20, offY+ totalHeight+15, view.getWidth(), offY+ totalHeight+15);
		g.drawOval(slideX-35, offY+ totalHeight, 30, 30);
		g.fillOval(slideX-35, offY+ totalHeight, 30, 30);
		totalHeight+= 35;
		
		if ( nearTargs != null )
			for (ImgDef def : nearTargs){
				if (def.file==null)
					if (def.path!=null)	def.file= new File(def.path);
					else continue;
				BufferedImage thumb= thumbs.containsKey(def.file) ? thumbs.get( def.file ) : ImgProvider.provideImage(def.file, true, 2, 2);
				if (thumb==null){
					System.err.println(def+ "targ null!");
					continue; }
				thumbs.put( def.file, thumb );
				int imgH= (int) Math.round(thumb.getHeight()*((stripWidth-10)*1d/thumb.getWidth()));
				if (offY< view.getHeight() && offY+ totalHeight+ imgH > 0) 
					g.drawImage( thumb, slideX, offY+ totalHeight , stripWidth-10, imgH, null );
				if (mY>offY+totalHeight && mY<offY+imgH+totalHeight){
					nearImgF = def.file;
					selectNearPic(); }
				totalHeight+= imgH+5;
			}
		
		lastThumbsHeight= totalHeight+80;
	}
	
	Area selectedArea, overArea;
	int selectedLevel=0; // ground level
	private void paintMap(Graphics2D g){
		
		if (selectedArea==null)
			if (Database.areas.size()==0)
				selectedArea= new Area("outside");
			else 
				selectedArea= Database.areas.values().iterator().next();
		
		int offX= view.getWidth()- 40, offY= 20;
		int starLev= -1, levs= 4;
		int maxW= view.getWidth()-60;
		for (int lev=-1; lev<levs; lev++){
			paintSelector(g, mX, mY, starLev+ lev*maxW/levs, 0, maxW/levs, 20, "level "+ (starLev+ lev), lev== selectedLevel, null);
			LinkedList<Area> levAreas= new LinkedList<Area>();
			for ( Area a : Database.areas.values() )
				if (a.level== lev && a.assoc==null) // self standing areas of current level
					levAreas.add(a);
			if (levAreas.size()>0){
				Utils.aaOn(g);
				double aw= maxW/levAreas.size();
				for ( int ai=0; ai< levAreas.size(); ai++ ){
					Area la= levAreas.get(ai);
					if (paintSelector(g, mX, mY, ai*aw, 20, aw, 20, la.label, selectedArea==la, null)) 
						overArea= la; }}}
		paintSelector(g, rX, rY, view.getWidth()- 60, 20, 60, 20,  "add area", false, cAreaAdd);
	}

	private static final Color cArea= new Color( 200, 200, 200 );
	private static final Color cAreaSel= new Color( 220, 220, 200 );
	private static final Color cAreaAdd= new Color( 200, 200, 220 );

	public static boolean paintSelector(Graphics2D g, double mX, double mY, double x, double y, double width, double height, String label, boolean selected, Color color){
		g.setColor( selected ? cAreaSel : color==null ? cArea : color );
		Rectangle2D r= new Rectangle2D.Double( x+1, y+1, width-2, height-2 ) ;
		g.fill( r );
		boolean over= false;
		
		if ( over= ( mX>x && mX<x+width && mY>y && mY<y+height ) ){
			System.err.println( "over area "+ mX+ ":"+ mY);
			g.setColor( Color.yellow );
			g.setStroke( new BasicStroke( (float) (2/ g.getTransform().getScaleX())));
			g.draw( r );
		}
		if (label!=null){
			g.setColor(Color.black);
			g.setFont(new Font("arial", Font.PLAIN, 12));
			g.drawString(label, (float)x - g.getFontMetrics().stringWidth(label)/2 + ((float)width/2), (float) (y+ height/2+ g.getFontMetrics().getHeight()/3) );
		}
		return over;	
	}	

}
