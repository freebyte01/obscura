package obscura;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.Reader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.channels.FileLock;
import java.util.ArrayList;

public class Obscura {
	static boolean running= true;
	static Watcher watcher;
	static Viewer viewer;
	public static Database data;
	static FileOutputStream lockStream;
	static FileLock lock;
	
	static ArrayList<File> observeDirs= new ArrayList<File>();
	static ArrayList<File> currentImages= new ArrayList<File>();
	public static void main(String[] args) {
		System.err.println("starting Obscura");
		try{
			File lockFile= new File(".lock");
			if (lockFile.exists() && !lockFile.renameTo(lockFile))
				throw new RuntimeException("lock file locked..");
			lockStream = new FileOutputStream(lockFile);
		    lock = lockStream.getChannel().lock();
		} catch (Exception e){
			e.printStackTrace();
			System.err.println("Obscura is running already!");
			System.exit(1);
		}
		for (String s : args){
			System.out.println("testing watch dir "+ s);
			File f= new File(s);
			if (f.isDirectory() && f.exists())
				observeDirs.add(f);
		}
		
		Thread t = new Thread() {
            public void run() {
            	//data.writeDatabase("imageDefs.dat");
            	try{ lockStream.close(); } catch(Exception e){ e.printStackTrace(); }
            	try{ lock.release(); } catch (Exception e){ e.printStackTrace(); }
            	System.out.println("closing Obscura");
                Obscura.running= false;
            }
        };
        Runtime.getRuntime().addShutdownHook(t);

        data= new Database("imageDefs.dat");
		viewer= new Viewer();

		watcher = new Watcher(observeDirs);
        watcher.start();
	}
}
