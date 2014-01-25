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
 * other Ideas:
 * <ul>
 * <li>archive news directly onto server
 * <li>get and archive data from internet (e.g. Netzone access logs, Withings health data, bkaiser Statistics)
 * <li>check for missing news epapers
 * <li>automate download of newspapers
 * <li>email doubletten + Archivierung lokal
 * <li>AddressBook in LDAP (accessable for programs)
 * ≤li>find untagged files
 * <li>
 * </ul>
 * 
 * @author Bruno Kaiser
 * @version $Id$
 */
public class RenameFiles {
	private static final String CN = "RenameFiles";
	private static boolean testMode = false;  
	// private static boolean testMode = true; // TODO: reset to false after testing completed
	private static boolean debugMode = false;
	// private static boolean debugMode = true; // TODO: reset to false after testing completed
	private static String srcDirName = ".";
	private static String destDirName = ".";
	private File workDir = null;

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

		if (debugMode) {
			System.out.println("srcDirName=" + srcDirName);
			System.out.println("destDirName=" + destDirName);
		}
		workDir = new File(srcDirName).getCanonicalFile();
	}

	/** 
	 * Returns the current directory (working directory).
	 * @return   the current directory
	 */
	public File getCurrentDirectory() {
		return workDir;
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
			// TODO: add recursive mode
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
			File[] _fileList = _renfiles.selectFiles(_renfiles.getCurrentDirectory(), ".pdf"); // select all pdf files
			for (int i = 0; i < _fileList.length; i++) {
				if (_fileList[i].isFile()) {  // handle all files
					_renfiles.convertPdfFile(_fileList[i]);
				}
				// else it is a directoy
			}
			_renfiles.saveBentoBackups();
			_renfiles.saveShakehandsBackups();
			_renfiles.saveSoftwareFiles();
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
		String _dateStr = null;

		// handle each file type
		// TODO: externalize this into the configuration and make it generic
		// example:   search criteria, srcDateType, destfile extension, destpath extension
		// NZZS, NZZ, 20min: prefix,  yyyyMMdd, postfix,  subdir
		// ZSZ: postfix, yyyyMMdd, postfix, subdir
		// Tagi, Soze:  prefix, yyyy-MM-dd, postfix, subdir
		// ic, mmb, awuz etc.: prefixDate, yyyyMMdd, subDir | subDiryyyy | subDiryyyyMM

		// overall:
		// search criteria:  prefixString, postfixString, prefixDate:prefixString
		// srcDateType:     yyyyMMdd, yyyy-MM-dd
		// destFile extension:  postfixStr, srcString
		// destPath extension:  subDir, SubDiryyyy, subDiryyyyMM

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
		else if (f.getName().startsWith("EQUITY_")) {  // NZZ Equity
			_destFN = f.getName().substring(7,15) + "nzzEquity.pdf";	
			_destDirName = destDirName + "/nzzEquity";
		}
		else if (f.getName().startsWith("FOLIO_")) {  // NZZ Folio
			_destFN = f.getName().substring(6,14) + "nzzFolio.pdf";	
			_destDirName = destDirName + "/nzzFolio";
		}
		else if (f.getName().startsWith("GESE_")) {  // NZZ Gesellschaft
			_destFN = f.getName().substring(5,13) + "nzzGesellschaft.pdf";	
			_destDirName = destDirName + "/nzzGesellschaft";
		}
		else if (f.getName().startsWith("communications")) {	// ACM Communications
			_destFN = f.getName().substring(14,20) + "00acmCommunications.pdf";
			_destDirName = destDirName + "/acmCommunications"; 
			_tags = "oAcm,dMagazine,tTech";
		}
		else if (f.getName().startsWith("compw-")) {
			_destFN = f.getName().substring(6, 10) + 
					f.getName().substring(11, 13) +
					f.getName().substring(14, 16) + "computerworld.pdf";
			_destDirName = destDirName + "/computerworld";
			_tags = "dNews,tTech";
		}
		else if (f.getName().startsWith("input_gl")) {
			String _userName = "";
			String _meetDate = "";
			String[] _token = f.getName().split("_");
			for (int i = 0; i < _token.length; i++) {
				if (_token[i].equalsIgnoreCase("toms")) _userName = "Toms";
				else if (_token[i].equalsIgnoreCase("kornel")) _userName = "Kornel";
				else if (_token[i].equalsIgnoreCase("ksh")) _userName = "Kornel";
				else if (_token[i].equalsIgnoreCase("christof")) _userName = "Christof";
				else if (_token[i].equalsIgnoreCase("cdo")) _userName = "Christof";
				else if (_token[i].equalsIgnoreCase("christian")) _userName = "Christian";
				else if (_token[i].equalsIgnoreCase("crw")) _userName = "Christian";
				else if (_token[i].equalsIgnoreCase("bruno")) _userName = "Bruno";
				else if (_token[i].equalsIgnoreCase("bka")) _userName = "Bruno";
				else if (_token[i].length() == 12 && _token[i].startsWith("2")) _meetDate = _token[i].substring(0,8);
				else {
					if (debugMode) {
						System.out.println("token not recognized: " + _token[i]);
					}
				}
			}
			_destFN = _meetDate + "glinput" + _userName + ".pdf";
			_destDirName = destDirName + "/" + _meetDate + "gl";
			_tags = "oAdnovum,dMinutes,lZuerich";
		}
		else if (f.getName().startsWith("kw")) {
			String _meetDate = f.getName().substring(13, 21);
			_destFN = _meetDate + "mmgl.pdf";
			_destDirName = destDirName +"/" + _meetDate + "gl";
			_tags = "oAdnovum,dMinutes,lZuerich";
		}
		else if ((_dateStr = getLeadingDateFromString(f.getName())) != null) { // file name starts with date
			_destFN = f.getName();
			if (f.getName().substring(8).startsWith("ic") && _dateStr.length()>=6) {		// Inside Channels
				_destDirName = destDirName + "/insideChannels/" + _dateStr.substring(0, 4) 
						+ "/" + _dateStr.substring(4,6);
				_tags = "dNews,tTech";
			}
			else if (f.getName().substring(8).startsWith("rg") && _dateStr.length()>=6) {		// Rechnung / Bill
				_destDirName = destDirName + "/finance";
				_tags = "tFinance,dBill";
			}
			else if (f.getName().substring(8).startsWith("zkb") && _dateStr.length()>=6) {		// ZKB
				_destDirName = destDirName + "/finance";
				_tags = "tFinance,dBill,oZkb";
			}
			else if (f.getName().substring(8).startsWith("pf") && _dateStr.length()>=6) {		// PostFinance
				_destDirName = destDirName + "/finance";				
				_tags = "tFinance,dBill,oPost";
			}
			else if (f.getName().substring(8).startsWith("lohn") && _dateStr.length()>=6) {		// Lohn / Salary
				_destDirName = destDirName + "/finance";
				_tags = "tFinance,dStatement";
			}
			else if (f.getName().substring(8).startsWith("slkk") && _dateStr.length()>=6) {		// SLKK
				_destDirName = destDirName + "/finance";
				_tags = "tFinance,tInsurance,oSlkk";
			}
			else if (f.getName().substring(8).startsWith("mmb") && _dateStr.length()>=6) {		// meeting minutes
				_destDirName = destDirName + "/adnovum";
				_tags = "dMinutes,oAdnovum";
			}
			else if (f.getName().substring(8).startsWith("karte") && _dateStr.length()>=6) {	// Postcard 
				_destDirName = destDirName + "/corr";
				_tags = "dCorr,DPcard";
			}
			else if (f.getName().substring(8).startsWith("diary") && _dateStr.length()>=6) {	// diary
				_destDirName = destDirName + "/diary";
				_tags = "oBruno,dDiary";
			}
			else if (f.getName().substring(8).startsWith("abstract_") && _dateStr.length()>=4) {	// abstracts
				_destDirName = destDirName + "/abstract/" + _dateStr.substring(0,4);	
				_tags = "dAbstract";
			}
			else if (f.getName().substring(8).startsWith("kof") && _dateStr.length()>=6) {	// KOF reports
				_destDirName = destDirName + "/kof";	
				_tags = "dReport,tEco";
			}
			else if (f.getName().substring(8).startsWith("book") && _dateStr.length()>=6) {	// ebook
				_destDirName = destDirName + "/book";	
				_tags = "dBook";
			}
			else if (f.getName().substring(8).startsWith("sise") && _dateStr.length()>=6) {		// SI-SE
				_destDirName = destDirName + "/sise";
				_tags = "oSise";
			}
			else if (f.getName().substring(8).startsWith("awuz") && _dateStr.length()>=6) {		// AWUZ
				_destDirName = destDirName + "/awuz";	
				_tags = "oAwuzUzha";
			}
			else if (f.getName().substring(8).startsWith("informatikSpektrum") && _dateStr.length()>=6) {		// Informatik Spektruml
				_destDirName = destDirName + "/informatikSpektrum";	
				_tags = "tTech,dArticle";
			}
			else if (f.getName().toLowerCase().endsWith("pres.pdf")) {						// presentation
				_destDirName = destDirName;
				_tags = "dPres";
			}
			else if (f.getName().substring(8).startsWith("itc") && _dateStr.length()>=6) {		// IT consulting contract
				_destDirName = destDirName;
				_tags = "dContract,dItc";
			}
			else if (f.getName().substring(8).startsWith("swd") && _dateStr.length()>=6) {		// SW development contract
				_destDirName = destDirName;
				_tags = "dContract,dSwd";
			}
			else if (f.getName().substring(8).startsWith("sla") && _dateStr.length()>=6) {		// maintenance contract
				_destDirName = destDirName;
				_tags = "dContract,dSla";
			}
			else if (f.getName().substring(8).startsWith("nda") && _dateStr.length()>=6) {		// non disclosure agreement
				_destDirName = destDirName;
				_tags = "dContract,dNda";
			}
			else if (f.getName().substring(8).startsWith("offer") && _dateStr.length()>=6) {		// proposal
				_destDirName = destDirName;
				_tags = "dContract,dOffer";
			}

			else {  // there is a leading date, but no special meaning
				if (debugMode) {
					System.out.println(f.getName() + " has leading date, but no meaning");
				}
				_destDirName = destDirName;  // move file as is, do not add tags
				_tags = null;
			}

		}
		else {		// _dateStr = null, i.e. no leading date found; such a file is not moved, needs to be renamed first
			if (debugMode) {
				System.out.println("not recognized: " + f.getName());
			}
			_destFN = "";
		}

		if (_destFN.isEmpty() == false) { // convert only known files
			// create all parent directories if they do not already exist
			if (debugMode) { // just print out what would be done
				System.out.println("mkdir " + new File(_destDirName).getCanonicalPath());
			}
			if (testMode == false) {
				new File(_destDirName).mkdirs(); 			
			}
			File _destF = new File(_destDirName + "/" + _destFN);
			if (testMode) {  // just print out what would be done
				System.out.print("mv " + f.getName() + " " + _destF.getCanonicalPath());
				if (_tags != null && _tags.length() > 0) {
					System.out.println(", adding tags: " + _tags);
					if (debugMode) {
						System.out.println("/usr/local/bin/tag -a " + _tags + " " + _destF.getCanonicalPath());
					}
				}
				else { 
					System.out.println(", no tags added");
				}
			}
			else {  // execute the conversion
				if (f.renameTo(_destF) == true) {
					if (_tags != null && _tags.length() > 0) {
						Runtime.getRuntime().exec("/usr/local/bin/tag -a " + _tags + " " + _destF.getCanonicalPath());
					}
				}
				else {
					System.out.println("conversion of " + f.getName() + " failed.");
				}
			}
		}
	}

	/**
	 * Checks whether a String (typically a file name) starts with a
	 * Date in the form of yyyyMMdd and returns this date as a String.
	 * The parsing is very simply based on wether year, month and day are integers.
	 * Days = 00 and / or Months = 00 are allowed and will be ignored.
	 * Based on the length of the returned String, the format of the date can be easily 
	 * determines. The method guarantees to return only the following formats:
	 * ≤ul>
	 * <li>""    no leading date
	 * <li>yyyy  only a year
	 * <li>yyyyMM  year and month
	 * <li>yyyyMMdd  year, month and day
	 * </ul> 
	 * @param s			the String to parse
	 * @return 			the parsed Date or null if there is no leading date
	 */
	private String getLeadingDateFromString(String s)  {
		String _dateStr = null;
		int i = 0;
		// TODO: handle before christ (ad) dates
		try {
			if (s != null && s.length()>= 4) {  // check for yyyy 
				i = new Integer(s.substring(0,4));
				if (i > 0 && i < 3000) {
					_dateStr = s.substring(0,4);
					if (s.length() >= 6) {			// check for MM in yyyyMM
						i = new Integer(s.substring(4,6));
						if (i > 0 && i < 13) {   // month is correct: 01 .. 12
							_dateStr = s.substring(0,6);
							if (s.length() >= 8) {  // check for dd in yyyyMMdd
								i = new Integer(s.substring(6,8));
								if (i > 0 && i < 32) {   // day is correct: 01 .. 31 (special cases are ignored)
									_dateStr = s.substring(0, 8);
								}
							}
						}
					}
				}
			}
		}
		catch (NumberFormatException _ex) {
			if (debugMode) {
				System.out.println(s + " leads to parse error in getLeadingDateFromString()");
			}
		}
		if (debugMode) {
			System.out.println("leading date in <" + s + "> -> <" + _dateStr + ">");
		}

		return _dateStr;
	}

	/**
	 * Filters all pdf files within directory dir
	 * @param   dir         the current directory to look for the files
	 * @param   extension   the file name extension is the selection criteria
	 * @return	an array of pdf files
	 */
	private File[] selectFiles(File dir, String extension) {
		try {
			if (debugMode == true) {
				System.out.println("selectFiles("+ dir.toString() + ", " + extension + ")");
			}
			FilenameFilter _filter = new RenameFileFilter(extension);
			return (dir.listFiles(_filter));
		}
		catch (Exception _ex) {
			System.out.println("******** failed in selectFiles() with " + _ex.toString() + "*********");
			return null;
		}
	}

	/**
	 * save Bento backups
	 * 
	 */
	private void saveBentoBackups()
	{
		try {
			FilenameFilter _filter = new RenameFileFilter(".bentodb");
			File[] _backupFiles = new File("/Users/bruno/Documents").listFiles(_filter);
			for (int i = 0; i < _backupFiles.length; i++) {
				if (_backupFiles[i].isDirectory()) {  // handle all directories
					// 	/Users/bruno/Documents/Bento Backup - 2013-12-14.bentodb  -> yyyyMMdd.bentodb
					String _destFN = _backupFiles[i].getName().substring(15,19) + 
							_backupFiles[i].getName().substring(20, 22) +
							_backupFiles[i].getName().substring(23, 25) + ".bentodb";	
					String _destDirName = destDirName + "/backup/bento";
					if (debugMode) { // just print out what would be done
						System.out.println("mkdir " + new File(_destDirName).getCanonicalPath());
					}
					if (testMode == false) {
						new File(_destDirName).mkdirs(); 			
					}
					File _destF = new File(_destDirName + "/" + _destFN);
					if (testMode) {  // just print out what would be done
						System.out.println("mv " + _backupFiles[i].getName() + " " + _destF.getCanonicalPath());
					}
					else {  // execute the conversion
						if (_backupFiles[i].renameTo(_destF) == false) {
							System.out.println("conversion of " + _backupFiles[i].getName() + " failed.");
						}
					}

				}
				// else it is a file
			}
		}
		catch (Exception _ex) {
			System.out.println("******** failed in saveBentoBackups() with " + _ex.toString() + "*********");
		}

	}

	/**
	 * save Shakehands backups
	 * 
	 */
	private void saveShakehandsBackups() {
		try {
			File _proSaldoBackupDir = new File("/Users/bruno/Documents/dfs/sysadm/ProSaldoBackup");
			FilenameFilter _filter = new RenameFileFilter(".sdb");
			File[] _backupDirs = _proSaldoBackupDir.listFiles();
			for (int j = 0; j < _backupDirs.length; j++) {
				if (_backupDirs[j].isDirectory() == true) {
					File[] _backupFiles = _backupDirs[j].listFiles(_filter);
					String _dirName = _backupDirs[j].getName();
					for (int i = 0; i < _backupFiles.length; i++) {
						if (_backupFiles[i].isFile()) {  // handle all files
							// 		yyyy-mm-dd hh-mm-ss / Business.sdb -> yyyyMMdd.sdb	
							String _destFN = _dirName.substring(0,4) + 
									_dirName.substring(5,7) + _dirName.substring(8,10) + ".sdb";
							String _destDirName = destDirName + "/backup/shakehands";
							if (debugMode == true) { // just print out what would be done
								System.out.println("mkdir " + new File(_destDirName).getCanonicalPath());
							}
							if (testMode == false) {
								new File(_destDirName).mkdirs(); 			
							}
							File _destF = new File(_destDirName + "/" + _destFN);
							if (testMode) {  // just print out what would be done
								System.out.println("mv " + _backupFiles[i].getName() + " " + _destF.getCanonicalPath());
							}
							else {  // execute the conversion
								if (_backupFiles[i].renameTo(_destF) == false) {
									System.out.println("conversion of " + _backupFiles[i].getName() + " failed.");
								}
							}
							// TODO: remove the backup directory

						}
						// else it is a directory
					}
				} 
			}
		}
		catch (Exception _ex) {
			System.out.println("******** failed in saveShakehandsBackups() with " + _ex.toString() + "*********");
		}
	}
	
	/**
	 * save Software files
	 * 
	 */
	private void saveSoftwareFiles() {
		try {
			File[] _swFiles = selectFiles(getCurrentDirectory(), ".dmg");
				for (int i = 0; i < _swFiles.length; i++) {
				if (_swFiles[i].isFile()) {  // handle all files
					String _destDirName = destDirName + "/software";
					if (debugMode == true) { // just print out what would be done
						System.out.println("mkdir " + new File(_destDirName).getCanonicalPath());
					}
					if (testMode == false) {
						new File(_destDirName).mkdirs(); 			
					}
					File _destF = new File(_destDirName + "/" + _swFiles[i].getName());
					if (testMode) {  // just print out what would be done
						System.out.println("mv " + _swFiles[i].getName() + " " + _destF.getCanonicalPath());
					}
					else {  // execute the conversion
						if (_swFiles[i].renameTo(_destF) == false) {
							System.out.println("conversion of " + _swFiles[i].getName() + " failed.");
						}
					}
				}
				// else it is a directoy
			}
		}
		catch (Exception _ex) {
			System.out.println("******** failed in saveSoftwareFiles() with " + _ex.toString() + "*********");
		}
	}

}
