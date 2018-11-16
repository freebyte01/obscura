package obscura;

import java.awt.AWTException;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Composite;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Shape;
import java.awt.Toolkit;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;
import javax.swing.DefaultListModel;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import obscura.parts.Area;
import obscura.parts.ImgDef;
import obscura.parts.ImgDef.Comparison;
import obscura.parts.ImgProvider;
import obscura.parts.Point;
import obscura.parts.PointStat;
import obscura.parts.Poly;
import obscura.parts.Similarity;

public class Viewer extends JFrame implements KeyListener {
	public boolean mapEditMode;

	public Viewer() {
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		// setUndecorated(true);
		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		GraphicsDevice[] gs = ge.getScreenDevices();
		for (GraphicsDevice curGs : gs) {
			GraphicsConfiguration[] gc = curGs.getConfigurations();
			for (GraphicsConfiguration curGc : gc) {
				Rectangle bounds = curGc.getBounds();

				System.out.println(
						bounds.getX() + "," + bounds.getY() + " " + bounds.getWidth() + "x" + bounds.getHeight());
				setSize((int) Math.round(bounds.getWidth() / 2), (int) Math.round(bounds.getHeight() / 2));
			}
		}
		init();
		setVisible(true);
		updateList();
	}

	private JSplitPane split;
	private JSplitPane splitViewer;
	public DefaultListModel<File> listModel = new DefaultListModel<File>();
	public JList<File> list;
	public JTextField field;
	private JPanel view;
	public JLabel stateLabel= null;
	private Container editor;
	static SimpleDateFormat sdfs = new SimpleDateFormat("dd.MM.YY");
	static SimpleDateFormat sdfl = new SimpleDateFormat("dd MMMM YYYY   HH:mm ");

	static String shortName(String name) {
		for (File d : Obscura.observeDirs)
			if (name.startsWith(d.getPath()))
				return name.substring(d.getPath().length() + 1);
		return name;
	}

	boolean isMapMode() {
		return ctrl || mapEditMode;
	}

	double zoom = 1;
	double sx = 0, sy = 0;
	int splitPos = 200;
	Color definedC = new Color(230, 230, 230);
	Color selectedC = new Color(230, 230, 240);
	Color definedCT = new Color(225, 225, 230);
	Font listFont = new Font("Courier New", Font.PLAIN, 12);
	Font listFontB = new Font("Courier New", Font.BOLD, 12);
	DocumentListener dl= new DocumentListener() {
		public void insertUpdate(DocumentEvent e) { filter(); }
		public void removeUpdate(DocumentEvent e) { filter(); }
		public void changedUpdate(DocumentEvent e) {}
		private void filter() {
			String filter = field.getText();
			//filterModel((DefaultListModel<File>) list.getModel(), filter);
		}

	};
	public void updateList(){ 
		DefaultListModel<File> lm= new DefaultListModel<File>();
		for (File f : Watcher.sorted) 
			lm.addElement(f);
		//filterModel((DefaultListModel<File>) list.getModel(), field.getText());
		list.setModel(lm);
	}

	public void filterModel(DefaultListModel<File> model, String filter) {
		if (true) return;
		for (File f : Obscura.watcher.sorted) {
			if (!f.getName().toLowerCase().contains(filter.toLowerCase())) {
				if (model.contains(f)) {
					model.removeElement(f);
				}
			} else {
				if (!model.contains(f)) {
					model.addElement(f);
				}
			}
		}
	}

	void init() {

		getContentPane().add(split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT));
		JPanel jp= new JPanel();
		jp.setLayout(new BorderLayout());
		jp.add(new JScrollPane(list = new JList<File>()), BorderLayout.CENTER);
		field= new JTextField(15);
		field.getDocument().addDocumentListener(dl);
		field.addKeyListener(new KeyListener() {
			public void keyTyped(KeyEvent e) {
			}

			public void keyReleased(KeyEvent e) {
			}

			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode()==27){
					System.err.println("es pressed in text field");
					split.requestFocus();
				}
			}
		});
		jp.add(field, BorderLayout.NORTH);
		split.setLeftComponent(jp);
		list.setModel(listModel);
		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		list.setCellRenderer(new ListCellRenderer<File>() {
			public Component getListCellRendererComponent(JList<? extends File> list, File file, int index,
					boolean isSelected, boolean cellHasFocus) {
				int hash = Database.getHashCode(file);
				ImgDef def= Database.imgInfos.get(file.getName().toLowerCase());
				boolean hasThumb = new File(ImgProvider.getThumbsPath(file), hash + ".jpg").exists();
				JLabel c = new JLabel(sdfs.format(new Date(file.lastModified())) + (hasThumb ? "[] " : "   ")+ (def!=null?(def.POIs.size()+100+" ").substring(1).replaceAll("^0", " "):"   ")
						+ shortName(file.getPath()));
				c.setFont(isSelected ? listFontB : listFont);
				c.setForeground(isSelected ? Color.black : Color.gray);

				//System.err.println(file.getName().toLowerCase() + ":"+ Database.imgInfos.containsKey(file.getName().toLowerCase()));

				if (def!=null) {
					c.setOpaque(true);
					c.setBackground(hasThumb ? definedCT : definedC);
				}
				return c;
			}
		});
		split.setRightComponent(splitViewer = new JSplitPane(JSplitPane.VERTICAL_SPLIT));
		split.setDividerLocation(200);
		split.setContinuousLayout(true);

		splitViewer.addPropertyChangeListener(JSplitPane.DIVIDER_LOCATION_PROPERTY, new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent evt) {
				if (splitViewer != null && view != null
						&& splitViewer.getHeight() - view.getMinimumSize().height > splitPos){
					splitPos = getHeight() - splitViewer.getDividerLocation();
					System.err.println(" new split pos: "+ splitPos);
				}
			}
		});


		splitViewer.addComponentListener(new ComponentListener() {

			public void componentShown(ComponentEvent e) {
			}

			public void componentResized(ComponentEvent e) {
				if (getHeight() - view.getMinimumSize().height > splitPos )
					splitViewer.setDividerLocation(getHeight() - splitPos); }

			public void componentMoved(ComponentEvent e) {
			}

			public void componentHidden(ComponentEvent e) {
			}
		});
		splitViewer.setTopComponent(view = new JPanel() {
			public void paint(Graphics g) {
				super.paint(g);
				paintView((Graphics2D) g);
			}
		});


		view.setMinimumSize(new Dimension(200, 200));
		view.setBackground(Color.black);
		view.addMouseWheelListener(new MouseWheelListener() {
			public void mouseWheelMoved(MouseWheelEvent e) {
				if (isMapMode() || alt || overButton>=POI_POS) {
					System.err.println(zoom);
					if (isMapMode()){
						zoom -= e.getWheelRotation() * zoom / 10d;
						sx = (mX - view.getWidth() / 2) / zoom - rX;
						sy = (mY - view.getHeight() / 2) / zoom - rY;
					} else { 
						double zoomRatio= imgZoom;
						imgZoom -= e.getWheelRotation() * imgZoom / 10d;
						onMouseMove(e);
						zoomRatio= imgZoom/zoomRatio;
						imgOff.x = ( 1- zoomRatio )* (mX- view.getWidth()/2)+ imgOff.x*zoomRatio ;
						imgOff.y = ( 1- zoomRatio )* (mY- view.getHeight()/2)+ imgOff.y*zoomRatio; }
					repaint();
				} else {
					if (e.getX() > view.getWidth() - 200) {
						// nearestSel+= e.getWheelRotation();
						// //nearestSel= nearestSel<0?0:nearestSel;
						// nearPic= shift ?
						// nearTargs.length > 0 ? Watcher.images.get(
						// nearTargs[Math.abs(nearestSel%nearTargs.length)].hash
						// ) : null
						// : nearLocs.length>0? Watcher.images.get(
						// nearLocs[Math.abs(nearestSel%nearLocs.length)].hash )
						// : null;
						// selectNearPic();
						//
					} else {
						alternativeImgF = null;
						ImgDef viewedDef= Database.imgInfos.get( viewedImgF.getName().toLowerCase() );
						if (shift && viewedDef!=null && viewedDef.similar!=null ){
							int newIndex = (viewedDef.similar.register.indexOf(viewedDef.file.getName().toLowerCase()) + e.getWheelRotation()) % viewedDef.similar.register.size();
							newIndex+= newIndex<0? viewedDef.similar.register.size() : 0;
							int listPos= Arrays.binarySearch( Watcher.sorted, Database.imgInfos.get(viewedDef.similar.register.get(newIndex)).file, new Comparator<File>() {
								public int compare(File o1, File o2) { 
									//return (int) o1.lastModified()>=o2.lastModified()?1:-1;
									boolean o1Comp= o1.getName().indexOf('-')>-1;
									boolean o2Comp= o2.getName().indexOf('-')>-1;
									return o1Comp ? o2Comp ? o1.getName().compareTo(o2.getName()) 
											:1
											:o2Comp ? -1 
													: o1.getName().compareTo(o2.getName());
								}
							});
							if (listPos>0){
								selectInList(listPos);

							}
						} else {
							int newIndex = list.getSelectedIndex() + e.getWheelRotation();
							selectInList( newIndex  % list.getModel().getSize() );
						}
					}
				}
			}
		});
		view.addMouseMotionListener(new MouseMotionListener() {
			public void mouseMoved(MouseEvent e) {
				if (shift && drag && overButton>=POI_POS) view.setCursor(blankCursor);
				else view.setCursor(Cursor.getDefaultCursor());

				onMouseMove(e);
			}

			public void mouseDragged(MouseEvent e) {

				if (!mb1) 
					return;
				
				drag = true; // !mapEditMode && shift;
				// if (!ctrl)
				// return;
				System.err.println(">>>"+overButton);
				mouseMoved(e);

				if (!isMapMode()) {
					if (selButton>-1){
						if (selButton>=POI_POS){
							int poiPos= selButton- POI_POS;
							if (!Database.imgInfos.containsKey(viewedImgF.getName().toLowerCase())){ // add definition for image if missing
								selectedDef= new ImgDef(viewedImgF);
								selectedDef.oldPOI= false; }
							if (selectedDef!=null && poiPos<Database.POIs.size()){
								String poi= Database.POIs.get(poiPos);
								if (!selectedDef.POIs.containsKey(poi))
									selectedDef.POIs.put(poi, new Point((mX-view.getWidth()/2-imgOff.x)/currRelScale,(mY-view.getHeight()/2-imgOff.y)/currRelScale));
								else
									selectedDef.POIs.get(poi).add((mX-mXo)/currRelScale, (mY-mYo)/currRelScale);
							}
						}

					} else 
						imgOff.add(mX - mXo, mY - mYo);
				} else if (mapEditMode){
					if (selectedArea != null && pickedPoint != null && pickedPoint.length > 0) {
						if (shift)
							selectedArea.getPolyFor(pickedPoint).moveBy(rX - rXo, rY - rYo);
						else if (pickedPoint.length == 1)
							pickedPoint[0].set(rX, rY);
						else
							for (Point p : pickedPoint)
								p.add(rX - rXo, rY - rYo);
						selectedArea.update(false); 
					} else {
						sx += (rX - rXo);
						sy += (rY - rYo); }
				} else if (selMap!=null)
					switch(selButton){
					case MOVE_INDICATOR: selMap.x+= rX - rXo; selMap.y+= rY - rYo; break;
					case SCALE_INDICATOR: { 
						Point vo= new Point(rXo, rYo).sub(overCenter);
						Point v= new Point(rX, rY).sub(overCenter);
						selMap.scale/= vo.length()/v.length(); } break;
					case ROTATE_INDICATOR: {
						Point vo= new Point(rXo, rYo).sub(overCenter);
						Point v= new Point(rX, rY).sub(overCenter);
						selMap.rot+= v.angle()- vo.angle(); } break;
					}
				else {
					sx += (rX - rXo);
					sy += (rY - rYo); }

				mouseMoved(e);
				// System.err.println("dragging");
				repaint();
			}
		});

		splitViewer.setBottomComponent( stateLabel= new JLabel("status") );
		stateLabel.setBorder(new EmptyBorder(5, 20, 5, 20));
		//splitViewer.setBottomComponent(editor = new Container());
		splitViewer.setContinuousLayout(true);
		splitViewer.setDividerLocation(getHeight()- (splitPos=100));

		list.addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent e) {
				selectImage(list.getSelectedValue());
			}
		});

		new Thread() { // slideshow timer
			public void run() {
				while (true) {
					try {
						sleep(5);
					} catch (InterruptedException e) {
					}
					if (slideShow && System.currentTimeMillis() > (lastSlideTime + 25))
						goNextPic();
				}
			}
		}.start();

		new Thread() { // lazy img reader
			public void run() {
				while (true) {
					try { sleep(200); } catch (InterruptedException e) {}
					/*					stateLabel.setText(
							" viewedImgF != null : "+(viewedImgF != null)+
							"    viewedOrigF != viewedImgF : "+ (viewedOrigF != viewedImgF)+
							"    System.currentTimeMillis() > (imgSelectedTime + 500) : "+(System.currentTimeMillis() > (imgSelectedTime + 500)));
					 */
					if ( viewedImgF != null 
							&& viewedOrigF != viewedImgF
							&& System.currentTimeMillis() > (imgSelectedTime + 500)) { 
						// if still on the same pic, show it in it's original resolution (refine) if not already orig
						try {
							System.err.println("lazy loading orig " + selectedImgF);
							if (viewedImgF == selectedImgF)
								selectedImg = ImgProvider.provideImage(viewedOrigF = selectedImgF, false, 0, 0);
							else if (viewedImgF == alternativeImgF)
								alternativeImg = ImgProvider.provideImage(viewedOrigF = alternativeImgF, false, 0, 0);
							view.repaint();
						} catch (IOException e) { e.printStackTrace(); }}}}
		}.start();

		addKeyListener(this);
		list.addKeyListener(this);
		view.addKeyListener(this);
		// map.addKeyListener(this);
		view.addMouseListener(new MouseListener() {

			public void mouseReleased(MouseEvent e) {
				if (pickedArea != null && pickedPoint != null && drag)
					pickedArea.update(true);
				if (mapEditMode && shift) {
					selRangeP2 = new Point(rX, rY);
				}
				if (selMap!=null && selButton>-1 || drag && overButton>=POI_POS ){
					selMap= null; selButton= -1;
					//Database.updateRelations();
					if (selectedDef!=null)
						matchingDef= selectedDef.findSimilarPatterns();
					Obscura.data.writeDatabase(); }
				drag = false;
				pickedPoint = null;
				mb1 &= e.getButton()!=MouseEvent.BUTTON1;
				mb2 &= e.getButton()!=MouseEvent.BUTTON2;
				mb3 &= e.getButton()!=MouseEvent.BUTTON3;

				view.repaint();

			}

			synchronized public void mousePressed(MouseEvent e) {
				
				onMouseMove(e);
				mousePressedTime= System.currentTimeMillis();
				
				mb1 |= e.getButton()==MouseEvent.BUTTON1;
				mb2 |= e.getButton()==MouseEvent.BUTTON2;
				mb3 |= e.getButton()==MouseEvent.BUTTON3;
				

				selRangeP1 = selRangeP2 = null;

				if (mapEditMode) {
					pickedPoint = overPoint;
					if (selectedArea != null)
						selectedArea.activePoly = selectedArea.getPolyFor(pickedPoint);
				} else if (shift && e.getButton()==MouseEvent.BUTTON3)
					hintsCenter= new Point(mX, mY);
					
				selMap= overMap; 
				selButton= overButton;
				if (selButton>=POI_POS){
					if (lastPOIs.contains(selButton))
						lastPOIs.remove(lastPOIs.indexOf(selButton));
					lastPOIs.push(selButton);
					while (lastPOIs.size()>20)
						lastPOIs.removeLast(); }
				repaint();
			}

			public void mouseExited(MouseEvent e) {
				mb1 = false;
				mb2 = false;
				mb3 = false;
				System.err.println("mouse exited " + e.getX() + view.getWidth());
				selMap= null; selButton=-1;
				onMouseMove(e);
				view.repaint();
			}

			public void mouseEntered(MouseEvent e) {
				mb1 |= e.getButton()==MouseEvent.BUTTON1;
				mb2 |= e.getButton()==MouseEvent.BUTTON2;
				mb3 |= e.getButton()==MouseEvent.BUTTON3;
			}

			public void mouseClicked(MouseEvent e) {
				System.err.println("clicked " + e.getClickCount());
				if (!mapEditMode) {
					if (ctrl) {
						if (e.getClickCount() > 1) {
							clickSelect = null;
							if (selectedImgF != null) {
								//currDef = Obscura.data.getImgDef(Database.getHashCode(selectedImgF), true);
								selectedDef = Obscura.data.getImgDef(selectedImgF, true);
								if (next) {
									if (selectedDef.targ != null)
										selectedDef.targ.set(rX, rY);
									else
										selectedDef.targ = new Point(rX, rY);
									nearTargets = selectedDef == null ? null : selectedDef.sameTarget();
								} else {
									if (selectedDef.pos != null)
										selectedDef.pos.set(rX, rY);
									else
										selectedDef.pos = new Point(rX, rY);
									nearPositions = selectedDef == null ? null : selectedDef.samePosition();
								}
								next = !next;
								selectedDef.path = selectedImgF.getAbsolutePath();
								Obscura.data.writeDatabase();

							}
						} else {
							clickSelect = nearest;

							System.err.println("clicked on "+ clickSelect+ ":"+ clickedButton);
							new Thread() {
								public void run() {
									try {
										sleep(500);
										System.err.println("clicked only once : csl: "+ ( clickSelect == null ? "" : clickSelect.file.getAbsolutePath() )+ "   nea: "+ ( nearest == null ? "" : nearest.file.getAbsolutePath() ));
										if (clickSelect != null) {
											//for (int i=0; i<list.getModel().getSize(); i++) 
											//System.out.println(list.getModel().getElementAt(i));
											list.setSelectedValue( clickSelect.file, true);
											list.ensureIndexIsVisible( list.getSelectedIndex() );
										}
									} catch (InterruptedException e) {
									}
								}
							}.start(); }

					} else {
						System.err.println(overButton);
						clickedButton= overButton;
						switch (overButton){
						case OBSERVER_INDICATOR: sx= selectedDef.pos == null ? sx : selectedDef.pos.x; sy= selectedDef.pos == null ? sx : selectedDef.pos.y; break;
						case TARGET_INDICATOR: sx= selectedDef.targ == null ? sx : selectedDef.targ.x; sy= selectedDef.targ == null ? sy : selectedDef.targ.y; break;
						case POI_ADD_BUTTON: 
							String poi= JOptionPane.showInputDialog("new POI name");
							if (poi!=null) 
								if (poi.length()<3)
									JOptionPane.showMessageDialog(view, "wrong name, should be longer min 3 chars");
								else {
									if (selectedDef!=null)
										if (selectedDef.similar!=null && selectedDef.similar.POIs.indexOf(poi)<0)
											selectedDef.similar.POIs.add(poi);
									Database.addPOI(poi); }
							break;
						default:
							if ( overButton >= POI_POS ){
								if (e.getClickCount()>1){
									int poiPos= overButton-POI_POS;
									if ( poiPos < Database.POIs.size()){
										String oldName= Database.POIs.get(poiPos);
										if (e.getButton()==MouseEvent.BUTTON3){
											if (JOptionPane.showConfirmDialog(view, "do you want to delete POI "+ oldName+ " from definition?")==JOptionPane.OK_OPTION){
												selectedDef.POIs.remove(oldName);
												
											}
										}else { 
											String newName= JOptionPane.showInputDialog("new POI name", oldName);
											if (newName!=null && newName.trim().length()>3){
												if (Database.POIs.indexOf(newName)>=0)
													if ( JOptionPane.showConfirmDialog(view, "Do you really want to rename "+ oldName+ " POI to "+ newName + " which already exist?") != JOptionPane.OK_OPTION )
														return;
												Database.POIs.remove(oldName);
												for (ImgDef def : Database.imgInfos.values()){
													Point poiP= def.POIs.remove(oldName);
													if (poiP!=null)
														def.POIs.put(newName, poiP); }
												for (Similarity sim : Database.similarities.values())
													if (sim.POIs.contains(oldName)){
														if (!sim.POIs.contains(newName))
															sim.POIs.add(newName);
														sim.POIs.remove(oldName); }														
												Database.POIs.remove(oldName);
												Database.addPOI(newName);
												Obscura.data.writeDatabase(); }}}
								}
							} else 
								if (alternativeImgF != null) {
									System.err.println("clicked on near imag thumb "+ alternativeImgF + " "+ Database.imgInfos.get(alternativeImgF.getName().toLowerCase()));
									list.setSelectedValue(
											Database.imgInfos.get(alternativeImgF.getName().toLowerCase())==null ? alternativeImgF: Database.imgInfos.get(alternativeImgF.getName().toLowerCase()).file
													, true);
									list.ensureIndexIsVisible(list.getSelectedIndex()); }}}
				} else { // MAP EDIT

					if (e.getClickCount() > 1) {
						if (overPoint != null)
							if (overPoint.length == 1)
								selectedArea.removePoint(overPoint[0]);
							else
								pickedPoint = new Point[] { selectedArea.addPoint(new Point(rX, rY), overPoint) };
						else if (shift) {
							Poly pl = selectedArea.addPoly();
							pickedPoint = new Point[] { selectedArea.addPoint(new Point(rX, rY), pl, 0) };
						} else
							pickedPoint = new Point[] { selectedArea.addPoint(new Point(rX, rY)) };
						view.repaint();
					}
				}
			}
		});
		readMaps();
	}
	long mousePressedTime;
	boolean mb1, mb2, mb3;

	ImgDef.Comparison[] matchingDef= new ImgDef.Comparison[0];
	void selectInList(int pos){
		list.setSelectedIndex(pos);
		list.ensureIndexIsVisible(list.getSelectedIndex());
		selectedDef= Database.imgInfos.get(list.getSelectedValue().getName().toLowerCase());
		matchingDef= new ImgDef.Comparison[0];
		if (selectedDef!=null)
			matchingDef= selectedDef.findSimilarPatterns();
	}

	void genKey(int keyCode, boolean ctrl, boolean shift, boolean alt ){
		try {
			Robot robot = new Robot();

			// Simulate a mouse click
			//robot.mousePress(InputEvent.BUTTON1_MASK);
			//robot.mouseRelease(InputEvent.BUTTON1_MASK);

			// Simulate a key press
			robot.keyPress(keyCode); //KeyEvent.VK_A);
			robot.keyRelease(keyCode); //KeyEvent.VK_A);

		} catch (AWTException e) {
			e.printStackTrace();
		}

	}

	static Point selRangeP1, selRangeP2;

	double imgX, imgY, oImgX, oImgY;
	void onMouseMove(MouseEvent e) {
		ctrl = e.isControlDown();
		shift = e.isShiftDown();
		alt = e.isAltDown();
		mXo = mX;
		mYo = mY;
		mX = e.getX();
		mY = e.getY();
		rXo = rX;
		rYo = rY;
		rX = (mX - view.getWidth() / 2) / zoom - sx;
		rY = (mY - view.getHeight() / 2) / zoom - sy;

		oImgX= imgX; oImgY= imgY;
		imgX= (mX - view.getWidth() / 2- imgOff.x)/imgZoom;
		imgY= (mY - view.getHeight() / 2- imgOff.y)/imgZoom;

		int b1 = MouseEvent.BUTTON1_DOWN_MASK;
		int b2 = MouseEvent.BUTTON2_DOWN_MASK;
		if ((e.getModifiersEx() & (b1 | b2)) == 0) {
			drag = false;
		} else {
			if (mapEditMode && shift) {
				selRangeP2 = new Point(rX, rY);
			}
		}

		if (!drag) {
			overTime = System.currentTimeMillis();
			overPoint = null;
			if (mapEditMode) {
				for (Area a : Database.areas.values())
					if ((overPoint = a.checkMouse(rX, rY, 100 / zoom / zoom)) != null) {
						overArea = a;
						break;
					}
			}
			repaint();
		} else if (mapEditMode) {

		} else if (selectedImgF != null) {
			ImgDef def = Database.imgInfos.get(Database.getHashCode(selectedImgF));
			if (def != null && def.pos != null) {
				double vx = rXo - def.pos.x, vy = rYo - def.pos.y;
				double radius = 10 / zoom;
				double radius2 = radius * radius;
				if (vx * vx + vy * vy < radius2) {
					def.pos.x += rX - rXo;
					def.pos.y += rY - rYo;
				} else if (def.targ != null) {
					vx = rXo - def.targ.x;
					vy = rYo - def.targ.y;
					if (vx * vx + vy * vy < radius2) {
						def.targ.x += rX - rXo;
						def.targ.y += rY - rYo;
					}
				}
			}
		}

	}

	long imgSelectedTime, lastSlideTime;

	synchronized void goNextPic() {
		alternativeImgF = null;
		selectInList((list.getSelectedIndex() + 1) % list.getModel().getSize());
	}

	public Point[] pickedPoint, overPoint;

	boolean isPicked(Object o) {
		if (o instanceof Point) {
			if (pickedPoint != null)
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
	ImgDef[] nearPositions, nearTargets;
	ImgDef clickSelect, nearest;
	int nearestSel;
	double imgZoom = 1;
	Point imgOff = new Point(0, 0);

	void selectImage(File imgF) {
		if (selectedImgF == imgF)
			return;
		imgOff.set(0, 0);
		imgZoom = 1;
		nearestSel = 0;
		selectedImgF = imgF;
		selectedImg = viewedImgF == selectedImgF ? viewedImg : null;
		viewedOrigF = ImgProvider.providerImgOrigF == selectedImgF ? selectedImgF : null;
		lastDef = selectedDef;
		if (selectedImgF != null && selectedImgF.exists()) {
			//currDef = Obscura.data.getImgDef(Database.getHashCode(selectedImgF));
			selectedDef = Obscura.data.getImgDef(selectedImgF);
		}
		next = false;
		nearPositions = selectedDef == null ? null : selectedDef.samePosition();
		nearTargets = selectedDef == null ? null : selectedDef.sameTarget();
		System.out.println("pic changed to " + selectedImgF.getName() + " : " + Database.getHashCode(selectedImgF));
		imgSelectedTime = lastSlideTime = System.currentTimeMillis();
		lastThumbsHeight = 0;
		thumbs.clear();
		view.repaint();

	}

	void selectNearPic() {
		if (alternativeImgF != null) {
			if (alternativeImgF != lastNear) {
				try {
					//System.err.println("reading near pic " + nearImgF);
					alternativeImg = ImgProvider.provideImage(alternativeImgF);
					imgSelectedTime = System.currentTimeMillis();
					lastNear = alternativeImgF;
					setTitle(shortName(selectedImgF.getPath()) + " -> " + shortName(alternativeImgF.getPath()));
				} catch (Exception e) {
					e.printStackTrace();
				}
				view.repaint();
			}
		} else if (alternativeImg != null) {
			alternativeImg = null;
			view.repaint();
		}

	}

	boolean ctrl, shift, alt, drag, next, slideShow;
	private Poly polyBuffer;

	int lastKey=0;

	public void keyPressed(KeyEvent e) {
		ctrl = e.isControlDown();
		shift = e.isShiftDown();
		alt = e.isAltDown();

		int keyCode= e.getKeyCode()+(ctrl?1000:0)+(shift?10000:0)+(alt?100000:0);

		if (lastKey==keyCode)
			return;

		lastKey= keyCode;

		System.err.println("key pressed "+ lastKey + " : "+ e.getKeyCode()+ ":"+ e.getKeyChar() );

		if (e.getKeyCode() == '\27'){
			System.err.println("escape pressed");
		}else if (e.getKeyCode() == '=') {
			imgZoom *= 1.2;
			view.repaint();
		} else if (e.getKeyCode() == '-') {
			imgZoom *= .8;
			view.repaint();
		} else if (e.getKeyCode() == 's' || e.getKeyCode() == 'S') {
			if ( alternativeImgF!=null && selectedImgF!=null){
				String selKey= selectedImgF.getName().toLowerCase();
				String nearKey= alternativeImgF.getName().toLowerCase();
				if ( nearKey.compareTo( selKey )!=0 ){ // pics must be different to be be considered further
					ImgDef inf= Database.imgInfos.get( selKey );
					if (inf==null)
						inf= new ImgDef(selKey);
					if (!inf.isSimilarTo( nearKey )){
						System.err.println("added current near image "+ nearKey+ " as similar image to "+ selKey);
						Database.pairAsSimilar( inf.getKey(), nearKey ); }
					else
						Database.rejectFromSimilar( nearKey ); 
					repaint(); }}
		} else if (e.getKeyCode() == 'm' || e.getKeyCode() == 'M') {
			mapEditMode = !mapEditMode;
			view.repaint();
		} else if (ctrl && (e.getKeyCode() == 'c' || e.getKeyCode() == 'C')) { // COPY
			if (mapEditMode && selectedArea != null && selectedArea.activePoly != null) {
				polyBuffer = selectedArea.activePoly;
				System.err.println("polygon copied to buffer ");
			}
		} else if (ctrl && (e.getKeyCode() == 'x' || e.getKeyCode() == 'X')) { // CUT
			System.err.println("slide show mode " + slideShow);
			if (mapEditMode && selectedArea != null && selectedArea.activePoly != null) {
				polyBuffer = selectedArea.removePoly(selectedArea.activePoly);
				System.err.println("polygon moved to buffer ");
			}
		} else if (ctrl && (e.getKeyCode() == 'v' || e.getKeyCode() == 'V')) { // PASTE
			System.err.println("polygon copied to buffer ");
			if (mapEditMode && selectedArea != null && polyBuffer != null) {
				Poly nPoly = selectedArea.addPoly();
				Point diff = new Point(rX, rY).sub(Point.avg(polyBuffer.getPoints()));
				for (Point p : polyBuffer.points)
					nPoly.addPoint(p.dup().add(diff), -1, false);
				nPoly.update();
				selectedArea.update(true);
				System.err.println("polygon applied from buffer to selected area");
			}

		} else if (e.getKeyCode() == 's' || e.getKeyCode() == 'S') {
			lastSlideTime = System.currentTimeMillis();
			slideShow = !slideShow;
			System.err.println("slide show mode " + slideShow);
		} else if (e.getKeyCode() == ' ') {
			System.err.println("space pressed");
			if (lastDef != null)
				//currDef = lastDef.cloneTo(Database.getHashCode(selectedImgF), selectedImgF.getAbsolutePath());
				selectedDef = lastDef.cloneTo(selectedImgF);
			view.repaint();
			list.repaint();
		} else
			repaint();
	}

	public void keyReleased(KeyEvent e) {
		System.out.println("key released");
		if (!e.isAltDown() && alt){
			view.grabFocus();
			genKey(KeyEvent.VK_ESCAPE, e.isControlDown(), e.isShiftDown(), e.isAltDown());
		}
		ctrl = e.isControlDown();
		shift = e.isShiftDown();
		alt = e.isAltDown();
		next = false;
		view.repaint();
		lastKey= 0;
	}

	public void keyTyped(KeyEvent arg0) {
	}

	HashMap<String, BufferedImage> maps = new HashMap<String, BufferedImage>();
	static final Pattern allowedFiles = Pattern.compile(".*\\.jpg|.*\\.png");

	void readMaps() {
		File mapsDir = new File("maps");
		if (!mapsDir.exists() || !mapsDir.isDirectory()) {
			System.err.println("no maps found!");
			return;
		}
		for (File m : mapsDir.listFiles()) {
			if (m.isDirectory() || m.getName().startsWith(".")
					|| !allowedFiles.matcher(m.getName().toLowerCase()).matches() || m.getName().indexOf("_map") < 0)
				continue;

			try {
				BufferedImage img = ImageIO.read(m);
				maps.put(m.getName(), img);
				System.out.println("found map file " + m.getName());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public File viewedOrigF, selectedImgF, viewedImgF, alternativeImgF, lastNear;
	public ImgDef selectedDef, lastDef, alternativeDef, lastAlternativeDef;
	BufferedImage selectedImg, viewedImg, alternativeImg;
	Map overMap, selMap;
	Point overCenter, selCenter, hintsCenter;
	int overButton= -1, selButton= -1, lastOverButton=-1, clickedButton=-1;
	public static final int OBSERVER_INDICATOR= 10; 
	public static final int SIMILAR_INDICATOR= 15; 
	public static final int POIS_INDICATOR= 17; 
	public static final int TARGET_INDICATOR= 20; 
	public static final int MOVE_INDICATOR= 30; 
	public static final int ROTATE_INDICATOR= 40; 
	public static final int SCALE_INDICATOR= 50; 

	public static final int POI_ADD_BUTTON= 1000; 
	public static final int POI_POS= 1100; 


	BufferedImage lastNearestImg;
	File lastNearestF;

	double currRelScale= 1, currImgScale=1;
	boolean showPOImenu;
	LinkedList<Integer> lastPOIs= new LinkedList<Integer>();
	int lastPOIpos= 0;

	String selChapter=null;
	Cursor blankCursor = Toolkit.getDefaultToolkit().createCustomCursor( new BufferedImage( 1, 1, BufferedImage.TYPE_INT_ARGB ), new java.awt.Point(0, 0), "blank cursor");


	synchronized void paintView(Graphics2D g) {

		if (shift){
			if (!mapEditMode)
				if (hintsCenter==null)
					hintsCenter= new Point(mX, mY);
		} else
			hintsCenter= null;


		if ( alternativeImgF == null && selectedImgF != null && selectedImg == null)
			if (viewedImgF != selectedImgF)
				try {
					selectedImg = ImgProvider.provideImage(selectedImgF);
					viewedOrigF = ImgProvider.providerImgOrigF == selectedImgF ? selectedImgF : null; // no need to refine image later - already in original size
					if (selectedImg != null)
						setTitle(shortName(selectedImgF.getPath()));
					view.repaint();
				} catch (Exception e) {
					e.printStackTrace();
				}

		lastAlternativeDef= alternativeDef;
		alternativeDef= null;


		lastOverButton= overButton;
		overButton= selButton>-1?selButton:-1;

		viewedImg = alternativeImgF != null && alternativeImgF != selectedImgF ? alternativeImg : selectedImg;
		viewedImgF = alternativeImgF != null && alternativeImgF != selectedImgF ? alternativeImgF : selectedImgF;

		double vw = view.getWidth();
		double vh = view.getHeight();


		AffineTransform old = g.getTransform();
		if (isMapMode()) {

			g.translate(vw / 2, vh / 2);

			g.scale(zoom, zoom);
			g.translate(sx, sy);


			// g.drawLine(0, 0, (int) rX, (int) rY);
			int rule = AlphaComposite.SRC_OVER;
			Composite compA = AlphaComposite.getInstance(rule, .6f);
			Composite compAO = AlphaComposite.getInstance(rule, .8f);
			Composite compS = AlphaComposite.getInstance(rule, 1f);
			Utils.aaOff(g);


			g.setStroke(new BasicStroke((float)(3/zoom)));
			Map lastMap= selMap==null?overMap:selMap;
			overMap= selMap;

			for (int lev=1; lev<3; lev++)
				if (selectedArea!=null)
					try{
						Map m= selectedArea.maps.get(lev);
						if (m==null)
							selectedArea.maps.put(lev, m= new Map());
						g.setColor( new Color(200,200,100, lastMap==m?255:200) );
						if (m.getImg()==null){ // jpg or png map
							m.setImg( ImgProvider.provideImage(new File("maps/lev_"+lev+".png"), false, 1, 1)); 
							if (m.getImg()==null)
								m.setImg( ImgProvider.provideImage(new File("maps/lev_"+lev+".jpg"), false, 1, 1)); }
						if (m.getImg()!=null){
							AffineTransform o= g.getTransform();
							int w= m.getImg().getWidth();
							int h= m.getImg().getHeight();
							g.translate(m.x, m.y);
							g.rotate(m.rot);
							g.scale(m.scale, m.scale);
							double mZoom= zoom * m.scale;
							g.setComposite(lastMap==m?compAO:compA);
							g.drawImage(m.getImg(), -w/2, -h/2, w, h, null);
							g.setComposite(compS);
							if (mapEditMode && ctrl){
								double rrX= rX- m.x, rrY= rY- m.y;
								rrX/= m.scale; rrY/= m.scale; 
								double rrrX= Math.cos( -m.rot)* rrX- Math.sin( -m.rot)* rrY, rrrY= Math.sin( -m.rot)* rrX+ Math.cos( -m.rot)* rrY;
								if (Utils.doEllipse(g, w/2-5/mZoom, h/2-5/mZoom, 10/mZoom, 10/mZoom, true).contains(rrrX, rrrY) && selMap==null){ 
									lastMap= overMap= m; overButton= SCALE_INDICATOR; overCenter= new Point(m.x, m.y); }
								if (Utils.doEllipse(g, w/2+20/mZoom, -5/mZoom, 10/mZoom, 10/mZoom, null, true).contains(rrrX, rrrY) && selMap==null){ 
									lastMap= overMap= m; overButton= ROTATE_INDICATOR; overCenter= new Point(m.x, m.y); }
								g.setStroke(new BasicStroke((float)(3/mZoom)));
								if (Utils.doEllipse(g, -20/mZoom, -20/mZoom, 40/mZoom, 40/mZoom, false).contains(rrrX, rrrY) && selMap==null){ 
									lastMap= overMap= m; overButton= MOVE_INDICATOR; }
								g.draw(new Line2D.Double(w/2, 0, w/2+20/mZoom, 0));
								if (overMap==m)
									Utils.doRectangle(g, -w/2, -h/2, w, h, false);
								Utils.doRectangle(g, rrrX-2/mZoom, rrrY-2/mZoom, 4/mZoom, 4/mZoom, false); }
							g.setTransform(o);
						}
					} catch (Exception e) { e.printStackTrace();}			

			if (mapEditMode){
				g.setStroke(new BasicStroke(1f));
				g.setColor( new Color(200,200,100, 30) );
				for (int i=-10; i<=10; i++) 
					g.draw(new Line2D.Double(i*30/Utils.ratioMetric, -300/Utils.ratioMetric, i*30/Utils.ratioMetric, 300/Utils.ratioMetric ));
				for (int j=-10; j<=10; j++) 
					g.draw(new Line2D.Double(-300/Utils.ratioMetric, j*30/Utils.ratioMetric, 300/Utils.ratioMetric, j*30/Utils.ratioMetric ));
			}

			g.setComposite(compS);
			// drawDef(g, lastDef, 0.3f);
			// g.fill(new Ellipse2D.Double(currDef.x-8/zoom, currDef.y-8/zoom,
			// 16/zoom, 16/zoom));

			double radius = 10 / zoom;
			double radius2 = radius * radius;
			double min = Double.MAX_VALUE;

			Color cPos = new Color(1f, 1f, 0f, .5f);
			double posR = 2 / zoom, posD = posR * 2;

			ImgDef lastNearest= nearest;

			for (ImgDef def : Database.imgInfos.values())

				if (def.pos != null) {
					double vx = rX - def.pos.x, vy = rY - def.pos.y;
					double diff = vx * vx + vy * vy;
					if (min > diff) {
						min = diff;
						nearest = def;
					}
					if (diff < radius2) {
						Utils.aaOn(g, true);
						def.paint(g, (float) (1 - diff / radius2 + (lastNearest==def ? 1:0)));
					} else {
						Utils.aaOff(g);
						g.setColor(cPos);
						g.fill(new Rectangle2D.Double(def.pos.x - posR, def.pos.y - posR, posD, posD));
					}
				}

			for (Area a : Database.areas.values())
				a.paint(g, rX, rY);

			if (lastDef != null)
				lastDef.paint(g, .5f);

			Utils.aaOn(g);

			g.setStroke(new BasicStroke((float) (2 / zoom)));
			g.draw(new Ellipse2D.Double(rX - radius, rY - radius, radius * 2, radius * 2));

			if (selectedDef == null) {
			} else {
				if (nearPositions != null)
					for (ImgDef ic : nearPositions)
						ic.paint(g, 0.3f);
				else if (lastDef != null)
					lastDef.paint(g, 0.5f);
				if (nearTargets != null)
					for (ImgDef it : nearTargets)
						it.paint(g, 0.3f);
			}

			if (selectedDef != null)
				selectedDef.paint(g, 1);

			g.setColor(Color.yellow);
			if (overPoint != null)
				for (Point p : overPoint)
					g.draw(new Ellipse2D.Double(p.x - 5 / zoom, p.y - 5 / zoom, 10 / zoom, 10 / zoom));

			if (mapEditMode && shift) {
				if (selRangeP1 != null && selRangeP2 != null) {
					g.setColor(Color.yellow);
					g.setStroke(new BasicStroke((float) (3 / zoom), BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0,
							new float[] { (float) (9 / zoom) }, 0));
					g.draw(new Rectangle2D.Double(selRangeP1.x, selRangeP1.y, selRangeP2.x - selRangeP1.x,
							selRangeP2.y - selRangeP1.y));
				}
			}


			g.setTransform(old);

			paintMap(g);

			if (nearest!=null && shift && !mapEditMode){
				try{ 
					//System.err.println(nearest.file);
					BufferedImage nearestImg= lastNearestF!=null && lastNearestF== nearest.file ? lastNearestImg 
							: ImgProvider.provideImage( nearest.file, 1);
					lastNearestF= nearest.file;
					lastNearestImg= nearestImg;
					if (nearestImg!=null){
						int ww= view.getWidth()/2;
						int hh= (int) Math.round(1d*ww/nearestImg.getWidth()*nearestImg.getHeight());
						if (hh>view.getHeight()/2){
							ww*= 1d*view.getHeight()/2/hh;
							hh= view.getHeight()/2;
						}
						g.setColor(new Color(255, 255, 255, 50));
						g.fill(new Rectangle2D.Double(
								(mX < ww/2 + 20 ? 20 : mX > view.getWidth() - ww/2 -20 ? view.getWidth()- ww -40 : mX- ww/2)-10, 
								(mY > view.getHeight() - 100 - hh ? mY -100 - hh: mY + 100)-10, ww+20, hh+20));
						g.drawImage(nearestImg, 
								mX < ww/2 + 20 ? 20 : mX > view.getWidth() - ww/2 -20 ? view.getWidth()- ww -40 : mX- ww/2, 
										mY > view.getHeight() - 100 - hh ? mY -100 - hh: mY + 100, 
												ww, hh, null);
						//System.err.println(nearest.file+ " : "+ nearestImg.getWidth()+ ":"+ + nearestImg.getHeight());
					}


				} catch (IOException e) {}
			}

		} else

			if (viewedImg != null) {
				double iw = viewedImg.getWidth(null);
				double ih = viewedImg.getHeight(null);

				int orientation = 1;
				try {
					orientation = ImgProvider.getOrientation(alternativeImgF == null ? selectedImgF : alternativeImgF);
				} catch (Exception e) {
				}
				double rw = vw, rh = vh, rmX= mX, rmY= mY;

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
					rw = vh;
					rh = vw;

					rmY = mX;
					break;
				default:
					break;
				}

				double zw = rw / iw * imgZoom;
				double zh = rh / ih * imgZoom;
				currImgScale = zw > zh ? zh : zw;

				currRelScale= iw * currImgScale;

				g.setTransform(old);

				g.translate(vw/2 + imgOff.x, vh/2 + imgOff.y); // center to screen

				g.setColor(Color.yellow);

				g.setTransform(ImgProvider.getAffineForOrientation(g.getTransform(), orientation, (int) Math.round(currRelScale),
						(int) Math.round(ih * currImgScale)));



				if (currImgScale < 1)
					Utils.aaOn(g);

				g.drawImage(viewedImg, 0, 0, (int) Math.round(iw * currImgScale), (int) Math.round(ih * currImgScale), null);



				if (shift && drag){
					g.setTransform(old);
					//Utils.doLine(g, vw/2, 0,  vw/2, vh );
					//Utils.doLine(g, 0, vh/2, vw,  vh/2 );
					//g.setClip(null);
					//g.scale(1/currImgScale,1/currImgScale);

					g.setClip(new Ellipse2D.Double(-200+mX, -200+mY, 400, 400));

					double magnify= 2;

					g.translate(vw/2 + imgOff.x+ (vw/2-mX*magnify/2+ imgOff.x), vh/2 + imgOff.y+ (vh/2-mY*magnify/2+imgOff.y)); // center to screen

					g.setTransform(ImgProvider.getAffineForOrientation(g.getTransform(), orientation, (int) Math.round( currRelScale*magnify),
							(int) Math.round(ih * currImgScale*magnify)));

					if (currImgScale < 1)
						Utils.aaOn(g);

					g.drawImage(viewedImg, 0, 0, (int) Math.round(iw * currImgScale*magnify), (int) Math.round(ih * currImgScale*magnify), null);
					//g.drawImage(viewedImg, 0, 0, (int)vw, (int)vh, null);
					g.setClip(null);
					g.setTransform(old);
					Utils.doEllipse(g, mX-200, mY-200, 400, 400, false);
				}


				g.setTransform(old);


				if (!drag && mX > view.getWidth() - 200) { 
					try { paintNearThumbs(g, 200);
					} catch (IOException e) {}
				} else 
					alternativeImgF = null;

				if (shift){ // show/edit similarity POIs

					if (mX<50) 
						showPOImenu= true;

					ImgDef poiDef= Database.imgInfos.get(viewedImgF.getName().toLowerCase()); //lastAlternativeDef==null ? selectedDef : lastAlternativeDef;

					System.err.println(poiDef + ":" + alternativeDef);

					Point p;

					g.setColor(lastOverButton==POI_ADD_BUTTON?Color.YELLOW:Color.BLUE);

					p= new Point(30, 30);

					Utils.doEllipse(g, p.x-10, p.y-10, 20, 20, true);

					if ( Math.abs(mX-p.x)<10 && Math.abs(mY-p.y) < 10 ){
						System.err.println("POI ADD BUTTON");
						overButton= POI_ADD_BUTTON; }

					int poiPos= 0, offX=0, offY=0;

					String[] tree= new String[10];

					Color dragPoint= new Color( 255, 255, 0, 200);
					Color dragPointDark= new Color( 50, 50, 50, 200);

					Color overPoint= Color.yellow;
					Color overPointBack= Color.yellow;

					Color normalPoint= new Color(250,250,255);
					Color normalPointBack= Color.black;

					Color menuChapterFront= Color.BLACK;
					Color menuChapterBack= Color.WHITE;

					Color overMenu= Color.BLACK;
					Color overMenuBack= Color.YELLOW;

					Color hintMenu= new Color(0,0,0);
					Color hintMenuBack= new Color(255,255,200);

					Color normalMenu= new Color(200,200,255);
					Color normalMenuBack= new Color(30,30,30, 200);

					Color disabledMenu= new Color(200,200,255, 150);
					Color disabledMenuBack= new Color(30,30,30, 100);


					Font normal= g.getFont();
					Font bold= new Font(normal.getName(), Font.BOLD, normal.getSize());

					double[] maxWidths= new double[20];
					double[] colPos= new double[20];

					int menuCounter=0;
					int menuBoxHeight= 20;
					int menuRowHeight= 25;

					int maxListHeight= view.getHeight() - 140;

					int menuItemsPerColumn= maxListHeight / menuRowHeight;

					LinkedList<String> chapters= new LinkedList<String>();
					
					
					double rotate= 0;
					double scale= 1;
					Point shift= new Point();
					Point imgCenter= new Point(view.getWidth()/2,view.getHeight()/2).add(imgOff);
					System.err.println(shift+":"+scale+":"+rotate);

					if ( matchingDef.length>0 && matchingDef[0]!=null ){
						rotate= (Double) matchingDef[0].generalRotation;
						scale= (Double) matchingDef[0].generalScale;
						shift= new Point(); // ((Point) matchingDef[0][4]).dup().mul(currRelScale);
						System.err.println(shift+":"+scale+":"+rotate);

					}

					for (String poi : Database.POIs){

						int currPoiPos= POI_POS+ poiPos++;

						int column= menuCounter / menuItemsPerColumn;

						if (column>10)
							continue;

						boolean over= lastOverButton == currPoiPos;

						boolean active= poiDef!=null && poiDef.POIs.containsKey( poi ) ;

						boolean hinted= lastPOIs.indexOf( currPoiPos )>=0;

						String[] parts= poi.split(" "); // chapters
						if (!parts[0].equals(tree[0]))
							chapters.add(menuCounter + "/"+ (tree[0]=parts[0]));

						boolean inChapter= selChapter==null || parts[0].equals(selChapter);
						String poiName= parts.length==1 || active || selChapter==null ? poi : poi.substring(parts[0].length()+1);
						Point matchPoi= matchingDef.length>0 && matchingDef[0]!=null ? matchingDef[0].def.POIs.get(poiName) : null;

						if (!drag || over){

							double ppx, ppy, mppx=0, mppy=0;

							if ( active ) {
								p= poiDef.POIs.get(poi);
								if (poiDef.oldPOI){ // recalc to relative pois from old absolute pois
									p.x= (p.x - view.getWidth()/2- imgOff.x)/currRelScale;
									p.y= (p.y - view.getHeight()/2- imgOff.y)/currRelScale; }
								ppx= view.getWidth()/2+ imgOff.x+ p.x*currRelScale;
								ppy= view.getHeight()/2+ imgOff.y+ p.y*currRelScale;

							} else {
								if (alt) // zooming so don't show the menu
									continue;
								p= new Point( 150+30+ colPos[column], ( menuCounter % menuItemsPerColumn) * menuRowHeight + 70 );
								ppx= p.x;
								ppy= p.y; }
							
							if (matchPoi!=null){
								mppx = matchPoi.x*currRelScale;//* ((Double) matchingDef[2]);
								mppy= matchPoi.y*currRelScale;//* ((Double) matchingDef[2]); 
							}

							Shape txtBack;
							if ( !drag && ( !active && showPOImenu && inChapter || active && over )){
								double w= g.getFontMetrics().stringWidth(poiName)+10;
								if( maxWidths[column] < w ){
									maxWidths[column]= w;
									for (int i=1; i<colPos.length; i++)
										colPos[i]= colPos[i-1]+ maxWidths[i-1]+ menuRowHeight; }
								txtBack = Utils.getRoundRectangle(ppx+ (active?15:0), ppy-menuBoxHeight/2+ (active?menuBoxHeight:0), w, menuBoxHeight, 5);
								Rectangle2D rr= txtBack.getBounds2D();
								if ( mX>rr.getX() && mX<rr.getMaxX() && mY>rr.getY() && mY<rr.getMaxY() ){
									lastOverButton= overButton= currPoiPos; 
									over= true; }
								g.setColor( over ? overMenuBack : hinted ? hintMenuBack : normalMenuBack );
								g.fill(txtBack); }

							Shape pointBack=null;
							int r= over ? 6 : 3;
							
							boolean matched= matchingDef.length>0 && matchingDef[0]!=null && matchingDef[0].contains( poi );

							if (matchPoi!=null && !drag && matched){
								
								AffineTransform gg= (AffineTransform) g.getTransform();

								g.translate(imgCenter.x, imgCenter.y);
								
								pointBack= Utils.getEllipse( mppx- r*2, mppy-r*2, r* 4, r* 4);
								g.setColor(Color.black);
								g.fill(pointBack);
								Utils.doLine(g, mppx, mppy, ppx- imgCenter.x, ppy- imgCenter.y);
								//g.drawString(poiName+"_r", (float) mppx, (float) mppy);
								g.setColor(Color.white);
								g.draw(pointBack);
								
								g.setColor(new Color(255,255,255,100));
								//Utils.doLine(g, mppx, mppy+5, mppx+shift.x, mppy+shift.y+5);

								Utils.doLine(g, -10, 0, 10, 0 );
								Utils.doLine(g, 0, -10, 0, 10 );
								
								g.translate(shift.x, shift.y);

								g.scale( scale, scale );

								g.rotate((Double) matchingDef[0].generalRotation);

								g.setColor(matched?new Color(255,255,0,100):new Color(255,255,255,100));
								g.fill(pointBack);

								//g.translate(matchVect.x, matchVect.y);
								
								//g.draw(pointBack);
								//Utils.doLine(g, mppx, mppy, 0, 0);
								//g.drawString(poiName+"_t", (float) mppx, (float) mppy);
								//Utils.doLine(g, mppx, mppy, ppx- winnCenter.x, ppy-winnCenter.y); // line from matchPOI to current POI

								
//								g.translate( ((Point) matchingDef[5]).x*currRelScale, ((Point) matchingDef[5]).y*currRelScale );
//								g.setColor(new Color(255,255,0,150));
//								g.fill(pointBack);
//								g.translate( -((Point) matchingDef[5]).x*currRelScale, -((Point) matchingDef[5]).y*currRelScale );
//								g.rotate(-(Double) matchingDef[1]);
//								g.scale( 1/((Double) matchingDef[2]), 1/((Double) matchingDef[2]) );

								g.setTransform(gg);
							}


							if (active){
								pointBack= Utils.getEllipse( ppx- r, ppy-r, r* 2, r* 2);
								Rectangle2D rr= pointBack.getBounds2D();
								if ( mX>rr.getX() && mX<rr.getMaxX() && mY>rr.getY() && mY<rr.getMaxY()){
									lastOverButton= overButton= currPoiPos;
									over= true; }


								if (drag){
									g.setColor(dragPointDark);
									g.setStroke(new BasicStroke(3));
									Utils.doLine(g, ppx, ppy-100, ppx, ppy-20);
									Utils.doLine(g, ppx, ppy+100, ppx, ppy+20);
									Utils.doLine(g, ppx-100, ppy, ppx-20, ppy);
									Utils.doLine(g, ppx+100, ppy, ppx+20, ppy);
									Utils.doEllipse(g, ppx-2, ppy-2, 4, 4, true);
									g.setColor(dragPoint);
									g.setStroke(new BasicStroke(1));
									Utils.doLine(g, ppx, ppy-100, ppx, ppy-20);
									Utils.doLine(g, ppx, ppy+100, ppx, ppy+20);
									Utils.doLine(g, ppx-100, ppy, ppx-20, ppy);
									Utils.doLine(g, ppx+100, ppy, ppx+20, ppy);
									g.setColor(Color.white);
									Utils.doEllipse(g, ppx-1, ppy-1, 2, 2, true);
									//g.setXORMode(null);
								} else {
									g.setColor( over ? overPointBack: normalPointBack );
									g.fill( pointBack );
									g.setColor( over ?  overPoint : normalPoint );
									g.setStroke(new BasicStroke(2));
									g.draw( pointBack ); }}

							if ( !drag && ( !active && showPOImenu && inChapter || active && over ) ){
								g.setColor( over ?  overMenu : active ? normalMenu : hinted ? hintMenu : normalMenu );
								Utils.drawString( g, ppx+ ( active ? r+ 15:5), ppy+ 5+ (active ? menuBoxHeight: 0), poiName); } 


							// neigbouring stats pointers
							g.setColor(new Color(0,0,0,50));
							g.setStroke(new BasicStroke(1));
							if (over && active && !drag){ 
								PointStat ps= Database.avgs.get(poi);
								if (ps!=null)
									for (Entry<String, Point> d : ps.distances.entrySet())
										if (poiDef.POIs.containsKey(d.getKey())){
											int cnt= ps.counts.get(d.getKey());
											double l= d.getValue().length()*currRelScale/cnt;
											//Utils.doEllipse( g, ppx- l/2, ppy- l/2, l, l, false);
											Utils.doLine(g, ppx, ppy, ppx+ d.getValue().x* currRelScale/ cnt, ppy+ d.getValue().y* currRelScale/ cnt); }}

							if (inChapter)
								menuCounter++;

						} else if (drag && over)
							overButton= lastOverButton= currPoiPos; 

					}

					if (poiDef!=null) 
						poiDef.oldPOI= false;

					if (!alt){
						g.setFont(bold);
						int pos=0;
						for (String chapter : chapters){
							String[] ch= chapter.split("/");

							//int pos= Integer.parseInt(ch[0]);
							if (!drag && showPOImenu){
								p= new Point( 20, pos* menuRowHeight*1.5+ menuBoxHeight/4 + 70 );
								g.setColor( selChapter==null || !selChapter.equals(ch[1]) ? menuChapterBack : overMenuBack );
								if (Utils.doRoundRectangle(g, p.x-5, p.y-15, 150, menuBoxHeight*1.5, 5, true).contains(mX, mY)){
									System.err.println("over chapter "+ ch[1]);
									selChapter= ch[1];
								}

								g.setColor( menuChapterFront );
								//Utils.doRectangle(g, p.x-5, p.y-15, maxWidths[column], menuBoxHeight, false);
								Utils.drawString( g, p.x+ 150/2- g.getFontMetrics().stringWidth(ch[1])/2-10, p.y+ menuBoxHeight/4, ch[1] ); }
							pos++; }

						g.setFont(bold);
						pos= showPOImenu ? 200 : 50 ;
						LinkedList<String> hints= new LinkedList<String>();
						for (int i=0; i<lastPOIs.size(); i++)
							if (lastPOIs.get(i)!=null){
								int pp= lastPOIs.get(i)-POI_POS;
								String poi= pp<Database.POIs.size() ? Database.POIs.get( pp ) : null;
								if (poi!=null)
									hints.add(poi); }
						
						if (mb3 ){//&& (mousePressedTime+500< System.currentTimeMillis())){
							String[] hintsSorted= hints.toArray(new String[hints.size()]);
							Arrays.sort(hintsSorted);
							String lastHintChapter= null;
							int hintPos= 0;
							for (String hint : hintsSorted){
								//int pos= Integer.parseInt(ch[0]);
								if (!drag){
									int pp= Database.POIs.indexOf(hint);
									String[] pa= hint.split(" ");
									if (showPOImenu && pa[0].equals(selChapter))
										continue;
									if (pa.length>1 && !pa[0].equals(lastHintChapter)){
										menuCounter++;
										lastHintChapter= pa[0]; }
									boolean active= poiDef!=null && poiDef.POIs.containsKey( hint ) ;
									int column= menuCounter++ / menuItemsPerColumn;
									double w= g.getFontMetrics().stringWidth(lastPOIs.indexOf(Database.POIs.indexOf(hint)+POI_POS)+ " "+hint)+10;
									if( maxWidths[column] < w ){
										maxWidths[column]= w;
										for (int i=1; i<colPos.length; i++)
											colPos[i]= colPos[i-1]+ maxWidths[i-1]+ menuRowHeight; }

									p= showPOImenu || hintsCenter==null ? 
											new Point( pos+ colPos[ (int) Math.floor(menuCounter / menuItemsPerColumn)], ( menuCounter % menuItemsPerColumn) * menuRowHeight + 70 )
											: hintsCenter.dup().add(50, (hintPos++ -hintsSorted.length/2)*25) ;
									Shape sh= Utils.getRoundRectangle(p.x-10, p.y-15, w+20, menuBoxHeight, 5);
									if (sh.contains(mX, mY)){
										System.err.println("over poi "+ hint );
										overButton= pp+POI_POS; }
									g.setColor( active? disabledMenuBack : lastOverButton==pp || overButton==pp ? overMenuBack : hintMenuBack );
									g.fill(sh);
									g.setColor( active? disabledMenu : lastOverButton==pp || overButton==pp ? overMenu : hintMenu );
									Utils.drawString( g, p.x, p.y, lastPOIs.indexOf(Database.POIs.indexOf(hint)+POI_POS)+ " "+hint ); }}}}
				} else {
					showPOImenu= false;
				}


				if (selectedDef != null) {

					if (selectedDef.pos != null) {
						g.setColor(Color.blue);
						paintButton(g, 20, vh - 20, 10, OBSERVER_INDICATOR );

						if (selectedDef.targ != null) {
							g.setColor(Color.yellow); 					}
						paintButton(g, 50, vh - 20, 10, TARGET_INDICATOR ); }

					if (alternativeImgF==null && selectedDef.similar!=null  || alternativeImgF!=null && selectedDef.isSimilarTo(alternativeImgF.getName().toLowerCase())) {
						g.setColor(Color.green);
						paintButton(g, 80, vh - 20, 10, SIMILAR_INDICATOR ); }

					if (selectedDef.POIs.size() > 0) {
						g.setColor(Color.yellow);
						paintButton(g, 110, vh - 20, 10, POIS_INDICATOR, selectedDef.POIs.size()+"" ); }

					stateLabel.setText(mb1+":"+mb2+":"+mb3);
					
				}}

		g.setTransform(old);

		if (viewedImg != null){
			String dateLabel="";
			if (viewedImg == selectedImg)
				dateLabel= sdfl.format(new Date(selectedImgF.lastModified()));
			else if (alternativeImgF != null && alternativeImg == viewedImg)
				dateLabel= sdfl.format(new Date(alternativeImgF.lastModified()));
			int w= g.getFontMetrics().stringWidth(dateLabel);
			Utils.drawOutlinedString(g, dateLabel, getWidth()/2- w/2 , 30);
		}

	}

	public void paintButton(Graphics2D g, double x, double y,double rad, int overValue){ paintButton(g, x, y, rad, overValue, null); }
	public void paintButton(Graphics2D g, double x, double y,double rad, int overValue, String txt){
		if (txt!=null) Utils.drawString(g, x+rad, y+4, txt);
		if (Utils.doEllipse( g, x-rad/2, y-rad/2, rad, rad, true).contains(mX, mY)){
			overButton= TARGET_INDICATOR; } }

	private int lastThumbsHeight;
	private LinkedHashMap<File, BufferedImage> thumbs = new LinkedHashMap<File, BufferedImage>();
	
//	{
//	new Thread(){
//		public void run() {
//			try { sleep(3000); }catch (InterruptedException e){}
//			while (true) {
//					System.err.println("repainting pattern match");
//					Viewer.this.repaint();
//				try { sleep(400); }catch (InterruptedException e){}
//			}
//		};
//		
//	}.start();
//	}
	void paintNearThumbs(Graphics2D g, int stripWidth) throws IOException {


		view.requestFocus();
		g.setColor(new Color(255, 255, 255, 50));
		g.fillRect(view.getWidth() - stripWidth, 0, 200, getHeight());

		int slideX = view.getWidth() - stripWidth + 5, totalHeight = 40;
		int offY = lastThumbsHeight == 0 ? 40
				: lastThumbsHeight < view.getHeight() ? 40
						: (int) Math.round((view.getHeight() - lastThumbsHeight - 80) * (mY * 1d / view.getHeight()))
						+ 40;

		if ( shift ){
			//System.err.println( "paint similar .. "+ (selectedDef!=null && selectedDef.similar != null) + selectedDef + selectedDef.similar );

			if ( selectedDef!=null && matchingDef.length>0 ){
				for ( Comparison o : matchingDef )
					if ( o != null )
						totalHeight= paintThumbs(g, new ImgDef[]{ o.otherDef }, stripWidth, slideX, offY, totalHeight);
				totalHeight= paintThumbsDelimiter(g, stripWidth, slideX, offY, totalHeight);
			}

			if ( selectedDef!=null && selectedDef.similar != null ){				

				selectedDef.similar.sort();

				for (String sim : selectedDef.similar.register) {

					ImgDef def= Database.imgInfos.get( sim );

					if (def==null)
						continue;
					if (def.file == null)
						if (def.path != null)
							def.file = new File(def.path);
						else
							continue;
					BufferedImage thumb = thumbs.containsKey(def.file) ? thumbs.get(def.file)
							: ImgProvider.provideImage(def.file, true, 2, 2);
					if (thumb == null) {
						System.err.println(def + " loc null!");
						continue;
					}
					thumbs.put(def.file, thumb);
					int imgH = (int) Math.round(thumb.getHeight() * ((stripWidth - 10) * 1d / thumb.getWidth()));
					if (offY < view.getHeight() && offY + totalHeight + imgH > 0)
						g.drawImage(thumb, slideX, offY + totalHeight, stripWidth - 10, imgH, null);
					if (mY > offY + totalHeight -2 && mY < offY + imgH + totalHeight + 2 ) {
						alternativeImgF = def.file;
						alternativeDef= def;
						selectNearPic();
					}
					g.setColor(Color.green);
					g.fillRect(slideX+1, offY + totalHeight+ 1, 5, imgH-2);
					totalHeight += imgH + 4;
				}
			}
		} else {

			if (nearTargets!=null)
				totalHeight= paintThumbs(g, nearTargets, stripWidth, slideX, offY, totalHeight);

			totalHeight= paintThumbsDelimiter(g, stripWidth, slideX, offY, totalHeight);

			if (nearPositions != null)
				totalHeight= paintThumbs(g, nearPositions, stripWidth, slideX, offY, totalHeight);

		}
		lastThumbsHeight = totalHeight + 40;
	}

	int paintThumbs(Graphics2D g, ImgDef[] thumbsSet, int stripWidth, int slideX, int offY, int totalHeight) throws IOException{
		if (thumbsSet != null)
			for (ImgDef def : thumbsSet) {
				//if (shift && (!def.isSimilarTo(selectedDef)))
				//	continue;
				if (def.file == null)
					if (def.path != null)
						def.file = new File(def.path);
					else
						continue;
				BufferedImage thumb = thumbs.containsKey(def.file) ? thumbs.get(def.file)
						: ImgProvider.provideImage(def.file, true, 2, 2);
				if (thumb == null) {
					System.err.println(def + "targ null!");
					continue;
				}
				thumbs.put(def.file, thumb);
				int imgH = (int) Math.round(thumb.getHeight() * ((stripWidth - 10) * 1d / thumb.getWidth()));
				if (offY < view.getHeight() && offY + totalHeight + imgH > 0)
					g.drawImage(thumb, slideX, offY + totalHeight, stripWidth - 10, imgH, null);
				if (selectedDef.isSimilarTo(def.file.getName().toLowerCase())) {
					g.setColor(Color.green);
					g.fillRect(slideX+1, offY + totalHeight+ 1, 5, imgH-2);
				}
				if (mY > offY + totalHeight - 2 && mY < offY + imgH + totalHeight+ 2) {
					alternativeImgF = def.file;
					alternativeDef= def;
					selectNearPic();
					System.err.println("alternative pic "+ def);
				}
				totalHeight += imgH + 4;
			}
		return totalHeight; }

	int paintThumbsDelimiter(Graphics2D g, int stripWidth, int slideX, int offY, int totalHeight){
		g.setColor(Color.black);
		g.setStroke(new BasicStroke(30));
		g.drawLine(slideX, offY + totalHeight + 15, view.getWidth(), offY + totalHeight + 15);
		g.drawOval(slideX - 35, offY + totalHeight, 30, 30);
		g.setColor(Color.white);
		g.setStroke(new BasicStroke(10));
		g.drawLine(slideX - 20, offY + totalHeight + 15, view.getWidth(), offY + totalHeight + 15);
		g.drawOval(slideX - 35, offY + totalHeight, 30, 30);
		g.fillOval(slideX - 35, offY + totalHeight, 30, 30);
		return totalHeight += 35;
	}

	Area selectedArea, overArea;
	int selectedLevel = 0; // ground level

	private void paintMap(Graphics2D g) {

		if (selectedArea == null)
			if (Database.areas.size() == 0)
				selectedArea = new Area("outside");
			else
				selectedArea = Database.areas.values().iterator().next();

		int starLev = -1, levs = 4;
		int maxW = view.getWidth() - 60;
		for (int lev = -1; lev < levs; lev++) {
			paintSelector(g, mX, mY, starLev + lev * maxW / levs, 0, maxW / levs, 20, "level " + (starLev + lev),
					lev == selectedLevel, null);
			LinkedList<Area> levAreas = new LinkedList<Area>();
			for (Area a : Database.areas.values())
				if (a.level == lev && a.assoc == null) // self standing areas of
					// current level
					levAreas.add(a);
			if (levAreas.size() > 0) {
				Utils.aaOn(g);
				double aw = maxW / levAreas.size();
				for (int ai = 0; ai < levAreas.size(); ai++) {
					Area la = levAreas.get(ai);
					if (paintSelector(g, mX, mY, ai * aw, 20, aw, 20, la.label, selectedArea == la, null))
						overArea = la;
				}
			}
		}
		paintSelector(g, rX, rY, view.getWidth() - 60, 20, 60, 20, "add area", false, cAreaAdd);
	}

	private static final Color cArea = new Color(200, 200, 200);
	private static final Color cAreaSel = new Color(220, 220, 200);
	private static final Color cAreaAdd = new Color(200, 200, 220);

	public static boolean paintSelector(Graphics2D g, double mX, double mY, double x, double y, double width,
			double height, String label, boolean selected, Color color) {
		g.setColor(selected ? cAreaSel : color == null ? cArea : color);
		Rectangle2D r = new Rectangle2D.Double(x + 1, y + 1, width - 2, height - 2);
		g.fill(r);
		boolean over = false;

		if (over = (mX > x && mX < x + width && mY > y && mY < y + height)) {
			System.err.println("over area " + mX + ":" + mY);
			g.setColor(Color.yellow);
			g.setStroke(new BasicStroke((float) (2 / g.getTransform().getScaleX())));
			g.draw(r);
		}
		if (label != null) {
			g.setColor(Color.black);
			g.setFont(new Font("arial", Font.PLAIN, 12));
			g.drawString(label, (float) x - g.getFontMetrics().stringWidth(label) / 2 + ((float) width / 2),
					(float) (y + height / 2 + g.getFontMetrics().getHeight() / 3));
		}
		return over;
	}

}
