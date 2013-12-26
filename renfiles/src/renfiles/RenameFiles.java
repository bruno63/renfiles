package renfiles;
import java.io.*;
import java.util.*;

/**
 * Utility to automate some regular file conversion tasks.<p>
 * Needs jdberry / tag installed on your system (in /usr/local/bin).<p>
 * see {@link http://github.com/jdberry/tag}<p>
 * install e.g. with <code>brew install tag</code><p>
 * Program uses some filesystem-specific system calls. 
 * It was tested on Mac OS/X 10.9.1 Mavericks. 
 * Mounted Volumes over AppleShare (afp) are supported.<p>
 * All pdf-Files in srcDirName are handled as follows:
 * <ul>
 * <li>rename the file according to certain rules
 * <li>move the file to destDirName
 * <li>add finder tags to the destination file
 * </ul>
 * 
 * @author Bruno Kaiser
 * @version $Id$
 */
public class RenameFiles {
	private static final String CN = "RenameFiles";
	private static boolean testMode = false;  
	// private static boolean testMode = true; // TODO: reset to false after testing completed 
	private static boolean debugMode = true; // TODO: reset to false after testing completed
	private static String srcDirName = ".";
	private static String destDirName = ".";
	private File workDir;

	/**
	 * Constructor takes the command line parameters as arguments.
	 * 
	 * @param args			the command line parameters (@see #printUsage()) for a list of valid arguments.
	 * @throws IOException
	 */
	public RenameFiles(String[] args) throws IOException {
		handleArgs(args);

		// load default configuration in the project root directory
		Properties _props = new Properties();
		_props.load(new FileInputStream("renfiles.properties"));
		destDirName = saveReadProperty(_props, "destDirName", destDirName);
		srcDirName = saveReadProperty(_props, "srcDirName", srcDirName);

		System.out.println("srcDirName=" + srcDirName);
		System.out.println("destDirName=" + destDirName);
		workDir = new File(srcDirName).getCanonicalFile();
	}
	
	/**
	 * Reads a value from configuration properties safely, i.e.
	 * if the value is not set, the default value is returned instead.
	 * 
	 * @param config		the configuration properties
	 * @param key			the key of the configuration attribute
	 * @param defaultValue  the default value of the configuration attribute
	 * @return              a valid configuration value, either from the properties or the default
	 */
	private static String saveReadProperty(Properties config, String key, String defaultValue) {
		String _value = null;
		_value = config.getProperty(key);
		if (_value != null) {
			return _value;
		}
		else {
			return defaultValue;
		}
	}

	/**
	 * Parses the command line arguments into class variables.
	 * 
	 * @param args	the command line parameters (@see #printUsage()) for a list of valid arguments.
	 */
	private void handleArgs(String[] args) {
		for (int i = 0; i < args.length; i++) {
			if (args[i].startsWith("-h") || args[i].startsWith("-H") || args[i].startsWith("-?")) {
				printUsage();
				System.exit(0);
			}
			else if (args[i].startsWith("-d") || args[i].startsWith("-D")) {
				debugMode = true;
			}
			else if (args[i].startsWith("-t") || args[i].startsWith("-T")) {
				testMode = true;
			}
			else {
				System.out.println("unknown argument: " + args[i] + " in class " + CN);
				printUsage();
			}
		}

	}

	/**
	 * Prints a usage message onto stdout.
	 */
	private void printUsage() {
		System.out.println("");
		System.out.println("-----------------------------------------------------------------------------");
		System.out.println("usage:");
		System.out.println("    renfiles -h[elp] | -<options>");
		System.out.println("      options are:");
		System.out.println("        d[ebug Mode]         debug mode; help finding errors");
		System.out.println("        t[est-only]          test mode; nothing is changed");
		System.out.println("        h[elp]               print this usage on stdout");
		System.out.println("-----------------------------------------------------------------------------");
		System.out.println("");

	}

	/**
	 * Static entry point of the program (main function). It instantiates a RenameFiles object,
	 * parses the command line parameters, filters all pdf files in the source directory and executes 
	 * the conversion function on each of the pdf files.
	 * @param args	the command line parameters (@see #printUsage()) for a list of valid arguments.
	 */
	public static void main(String[] args) {
		try {
			RenameFiles _renfiles = new RenameFiles(args);
			File[] _fileList = _renfiles.getPdfFiles();
			for (int i = 0; i < _fileList.length; i++) {
				if (_fileList[i].isFile()) {  // handle all files
					_renfiles.convertPdfFile(_fileList[i]);
				}
				// else it is a directoy
			}
			System.out.println("****** completed successfully **********");

		}
		catch (Exception _ex) {
			System.out.println("***** failed with " + _ex.toString() + "**********" );
			if (debugMode) {
				_ex.printStackTrace();
			}
		}

	}

	/**
	 * Converts a pdf file in the source directory to a file with a different
	 * name in the destination directory and adds some finder tags.
	 * @param f		the pdf file to convert
	 * @throws IOException
	 */
	private void convertPdfFile(File f) throws IOException {
		String _destFN = "";
		String _tags = "dNews"; // comma-separated list of tags
		String _destDirName = null; // base destination directory

		// handle each file type
		if (f.getName().startsWith("NZZS_")) { // NZZ am Sonntag epaper
			_destFN = f.getName().substring(5, 13) + "nzzs.pdf";
			_destDirName = destDirName + "/nzzs";
		}
		else if (f.getName().startsWith("NZZ_")) {  // NZZ epaper
			_destFN = f.getName().substring(4,12) + "nzz.pdf";	
			_destDirName = destDirName + "/nzz";
		}
		else if (f.getName().endsWith("_zsr.pdf")) {  // Zürichsee Zeitung epaper
			_destFN = f.getName().substring(0, 8) + "zsz.pdf";	
			_destDirName = destDirName + "/zsz";
		}
		else if (f.getName().startsWith("ZH_")) {  // 20 Minuten epaper
			_destFN = f.getName().substring(3,11) + "_20min.pdf";	
			_destDirName = destDirName + "/20min";
		}
		else if (f.getName().startsWith("taz-ges-")) {  // Tages-Anzeiger epaper
			_destFN = f.getName().substring(8,12) + 
					f.getName().substring(13, 15) +
					f.getName().substring(16, 18) + "tagesanzeiger.pdf";	
			_destDirName = destDirName + "/tagesanzeiger";
		}
		else if (f.getName().startsWith("sonze-")) {  // Sonntagszeitung epaper
			_destFN = f.getName().substring(6,10) + 
					f.getName().substring(11, 13) +
					f.getName().substring(14, 16) + "sonntagszeitung.pdf";	
			_destDirName = destDirName + "/sonntagszeitung";
		}

		if (! _destFN.isEmpty()) { // convert only known files
			// create all parent directories if they do not already exist
			if (testMode) { // just print out what would be done
				System.out.println("mkdir " + new File(_destDirName).getCanonicalPath());
			}
			else {
				new File(_destDirName).mkdirs(); 			
			}
			File _destF = new File(_destDirName + "/" + _destFN);
			if (testMode) {  // just print out what would be done
				System.out.println("mv " + f.getName() + " " + _destF.getCanonicalPath());
				System.out.println("/usr/local/bin/tag -a " + _tags + " " + _destF.getCanonicalPath());
			}
			else {  // execute the conversion
				if (f.renameTo(_destF) == true) {
					Runtime.getRuntime().exec("/usr/local/bin/tag -a " + _tags + " " + _destF.getCanonicalPath());
				}
				else {
					System.out.println("conversion of " + f.getName() + " failed.");
				}
			}
		}
	}

/**
 * Filters all pdf files within the source directory.
 * @return	an array of pdf files
 */
	private File[] getPdfFiles() {
		try {
			FilenameFilter filter = new FilenameFilter() {
				public boolean accept(File dir, String name) {
					if (name.toLowerCase().endsWith(".pdf")) { // pdf file
						return true;
					}
					else {
						return false;
					}
				}
			};
			return (workDir.listFiles(filter));
		}
		catch (Exception _ex) {
			System.out.println("******** failed in getPdfFiles() with " + _ex.toString() + "*********");
			return null;
		}
	}

}
