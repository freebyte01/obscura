package obscura;

import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.regex.Pattern;

import javax.swing.DefaultListModel;

public class Watcher extends Thread{
	HashSet<File> knownFiles= new HashSet<File>();
	HashSet<File> rejectedFiles= new HashSet<File>();
	public static HashMap<Integer, File> images= new HashMap<Integer, File>();
	
	ArrayList<File> toWatch= null;
	public Watcher(ArrayList<File> watch) {
		toWatch= watch;
	}
	SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.YY HH:mm:ss");
	public void run() {
		super.run();
		while (Obscura.running){
			int s= images.size();
			for (File d : toWatch){
				scanDir(d);
				File tmp= new File(d,".t");
				try{ FileWriter fw= new FileWriter(tmp); fw.write(System.currentTimeMillis()+""); fw.close(); tmp.delete(); } catch (Exception e) {}
				System.err.print(".");
			}
			if (images.size()!=s){
				System.out.println("added "+ (images.size()-s)+ " photos");
				File[] sort= new File[images.size()];
				images.values().toArray(sort);
				Arrays.sort( sort, new Comparator<File>() {
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
				Obscura.viewer.list.setListData(sort);
				Obscura.viewer.listModel= Obscura.viewer.list.getModel();
			}
			try{ Thread.currentThread().sleep(10000);				
			}catch(InterruptedException e){}
		}
	}
	static final Pattern allowedFiles= Pattern.compile(".*\\.jpg|.*\\.png");
	static final Pattern notAllowedFiles= Pattern.compile(".*zdroje.*|.*nakup.*|.*krajina.*");
	private int scanDir(File d){
		int changed=0;
		if (d==null || !d.isDirectory() || !d.exists())
			return changed;
		// System.out.println("scanning "+ d);
		for (File f : d.listFiles()){
			int hash= f.hashCode();
			String name= f.getName();
			if (name.startsWith("."))
				continue;
			if (f.isDirectory())
				changed+= scanDir(f);
			else {
				if (knownFiles.contains(f))
					continue;
				if (notAllowedFiles.matcher(f.getPath().toLowerCase()).matches() || !allowedFiles.matcher(f.getName().toLowerCase()).matches())
					continue;
				if (rejectedFiles.contains(f) || f.length()==0)
					continue;
				knownFiles.add(f);
				images.put(hash, f);
				changed++;
				//System.err.println("added img "+ f);
			}
		}
		return changed;
	}
}
