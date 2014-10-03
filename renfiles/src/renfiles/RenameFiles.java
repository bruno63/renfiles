package renfiles;
import java.io.*;
import java.text.SimpleDateFormat;
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
	private static boolean testMode = false;  
	private static boolean debugMode = false;	
	private static String srcDirName = ".";
	private static String docDirName = ".";
	private static String newsDirName = ".";
	private static String podcastSrcDirName = ".";
	private static String podcastDestDirName = ".";
	private File workDir = null;

	/**
	 * Constructor.
	 * The idea is to start this programm out of Eclipse and to control it via properties settings. 
	 * Therefore, command line parameters are not supported.
	 * 
	 * @throws IOException
	 */
	public RenameFiles() throws IOException {
		// load default configuration in the project root directory
		Properties _props = new Properties();
		_props.load(new FileInputStream("renfiles.properties"));
		docDirName = saveReadProperty(_props, "docDirName", docDirName);
		newsDirName = saveReadProperty(_props, "newsDirName", newsDirName);
		srcDirName = saveReadProperty(_props, "srcDirName", srcDirName);
		testMode = saveReadBooleanProperty(_props, "testMode", testMode);
		debugMode = saveReadBooleanProperty(_props, "debugMode", debugMode);
		podcastSrcDirName = saveReadProperty(_props, "podcastSrcDirName", podcastSrcDirName);
		podcastDestDirName = saveReadProperty(_props, "podcastDestDirName", podcastDestDirName);

		if (debugMode) {
			System.out.println("srcDirName=" + srcDirName);
			System.out.println("docDirName=" + docDirName);
			System.out.println("newsDirName=" + newsDirName);
			System.out.println("debugMode=" + debugMode);
			System.out.println("testMode=" + debugMode);
			System.out.println("podcastSrcDirName=" + podcastSrcDirName);
			System.out.println("podcastDestDirName=" + podcastDestDirName);
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
		String _value = config.getProperty(key);
		if (_value != null) {
			return _value;
		}
		else {
			return defaultValue;
		}
	}
	
	/**
	 * Reads a boolean value from configuration properties safely, i.e.
	 * if the value is not set, the default value is returned instead.
	 * 
	 * @param config		the configuration properties
	 * @param key			the key of the configuration attribute
	 * @param defaultValue  the default value of the configuration attribute
	 * @return              a valid boolean configuration value, either from the properties or the default
	 */
	private boolean saveReadBooleanProperty(Properties config, String key, boolean defaultValue) {
		String _value = config.getProperty(key);
		if (_value != null) {
			return new Boolean(_value).booleanValue();
		}
		return defaultValue;
	}

	/**
	 * Static entry point of the program (main function). It instantiates a RenameFiles object,
	 * parses the command line parameters, filters all pdf files in the source directory and executes 
	 * the conversion function on each of the pdf files.
	 * @param args	the command line parameters (@see #printUsage()) for a list of valid arguments.
	 */
	public static void main(String[] args) {
		try {
			RenameFiles _renfiles = new RenameFiles();
			File[] _fileList = _renfiles.selectFiles(_renfiles.getCurrentDirectory(), ".pdf"); // select all pdf files
			for (int i = 0; i < _fileList.length; i++) {
				if (_fileList[i].isFile()) {  // handle all files
					_renfiles.convertPdfFile(_fileList[i]);
				}
				// else it is a directory
			}
			_renfiles.saveBentoBackups();
			_renfiles.saveShakehandsBackups();
			_renfiles.saveSoftwareFiles();
			
			// handle podcast files
			// check the existance of the source and destination directory
			File _podcastSrcDir = new File(podcastSrcDirName);
			File _podcastDestDir = new File(podcastDestDirName);
			if (_podcastSrcDir.exists() && _podcastDestDir.exists()) {
				// apply the conversion for each podcast
				_renfiles.convertPodcast(_podcastSrcDir, _podcastDestDir, "10vor10", "10vor10_", "10vor10");
				_renfiles.convertPodcast(_podcastSrcDir, _podcastDestDir, "DOK", "dok_", "Dok");
				_renfiles.convertPodcast(_podcastSrcDir, _podcastDestDir, "ECO", "eco_", "Eco");
				_renfiles.convertPodcast(_podcastSrcDir, _podcastDestDir, "Einstein", "einstein_", "Einstein");
				_renfiles.convertPodcast(_podcastSrcDir, _podcastDestDir, "Giacobbo---M--ller", "giacobbomueller_", "GiacobboMueller");
				_renfiles.convertPodcast(_podcastSrcDir, _podcastDestDir, "Kassensturz", "kassensturz_", "Kassensturz");
				_renfiles.convertPodcast(_podcastSrcDir, _podcastDestDir, "Reporter", "reporter_", "Reporter");
				_renfiles.convertPodcast(_podcastSrcDir, _podcastDestDir, "Tagesschau", "ts20_", "Tagesschau");
				_renfiles.convertPodcast(_podcastSrcDir, _podcastDestDir, "TEDTalks--video-", "tedtalks", "tedtalks");
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

	private void convertPodcast(File _podcastSrcDir, File _podcastDestDir, String podcastName, String prefix, String destName) throws IOException {
		File _destF = null;
		String _dateStr = null;
		String _tags = "dNews"; // comma-separated list of tags
		File _srcDir = new File(_podcastSrcDir, podcastName);

		File[] _fileList = selectFiles(_srcDir, ".mp4"); // select all mp4 movie files
		for (int i = 0; i < _fileList.length; i++) {
			if (_fileList[i].isFile()) {  // handle all files
				if (prefix.startsWith("tedtalks")) {
					SimpleDateFormat _dateFormat = new SimpleDateFormat("yyyyMMdd");
					_dateStr = _dateFormat.format(_fileList[i].lastModified()); 
					_destF = new File(_podcastDestDir, destName + "/" + _dateStr + destName + _fileList[i].getName().substring(0, _fileList[i].getName().length()-10) + ".mp4");
					_tags = "tTech";
				} else {
					_dateStr = _fileList[i].getName().substring(prefix.length(), prefix.length()+8);
					_destF = new File(_podcastDestDir, destName.toLowerCase() + "/" + _dateStr + "sfdrs" + destName + ".mp4");
				}				
				if (testMode) {  // just print out what would be done
					System.out.print("mv " + _fileList[i].getName() + " " + _destF.getCanonicalPath());
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
					new File(_podcastDestDir, destName.toLowerCase()).mkdirs();
					if (_fileList[i].renameTo(_destF) == true) {
						if (_tags != null && _tags.length() > 0) {
							Runtime.getRuntime().exec("/usr/local/bin/tag -a " + _tags + " " + _destF.getCanonicalPath());
						}
					}
					else {
						System.out.println("conversion of " + _fileList[i].getName() + " failed.");
					}
				}
			}
			// else it is a directory 
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
			_destDirName = newsDirName + File.separator + "nzzs" + File.separator + _destFN.substring(0, 4);
		}
		else if (f.getName().startsWith("NZZ_")) {  // NZZ epaper
			_destFN = f.getName().substring(4,12) + "nzz.pdf";	
			_destDirName = newsDirName + File.separator + "nzz" + File.separator + _destFN.substring(0, 4);
		}
		else if (f.getName().endsWith("_zsr.pdf")) {  // Zürichsee Zeitung epaper
			_destFN = f.getName().substring(0, 8) + "zsz.pdf";	
			_destDirName = newsDirName + File.separator + "zsz" + File.separator + _destFN.substring(0, 4);
		}
		else if (f.getName().startsWith("ZH_")) {  // 20 Minuten epaper
			_destFN = f.getName().substring(3,11) + "_20min.pdf";	
			_destDirName = newsDirName + File.separator + "20min" + File.separator + _destFN.substring(0, 4);
		}
		else if (f.getName().startsWith("taz-ges-")) {  // Tages-Anzeiger epaper
			_destFN = f.getName().substring(8,12) + 
					f.getName().substring(13, 15) +
					f.getName().substring(16, 18) + "tagesanzeiger.pdf";	
			_destDirName = newsDirName + File.separator + "tagesanzeiger" + File.separator + _destFN.substring(0, 4);
		}
		else if (f.getName().startsWith("sonze-")) {  // Sonntagszeitung epaper
			_destFN = f.getName().substring(6,10) + 
					f.getName().substring(11, 13) +
					f.getName().substring(14, 16) + "sonntagszeitung.pdf";	
			_destDirName = newsDirName + File.separator + "sonntagszeitung" + File.separator + _destFN.substring(0, 4);
		}
		else if (f.getName().startsWith("EQUITY_")) {  // NZZ Equity
			_destFN = f.getName().substring(7,15) + "nzzEquity.pdf";	
			_destDirName = newsDirName + File.separator + "nzzEquity";
		}
		else if (f.getName().startsWith("FOLIO_")) {  // NZZ Folio
			_destFN = f.getName().substring(6,14) + "nzzFolio.pdf";	
			_destDirName = newsDirName + File.separator + "nzzFolio";
		}
		else if (f.getName().startsWith("GESE_")) {  // NZZ Gesellschaft
			_destFN = f.getName().substring(5,13) + "nzzGesellschaft.pdf";	
			_destDirName = newsDirName + File.separator + "nzzGesellschaft";
		}
		else if (f.getName().startsWith("communications")) {	// ACM Communications
			_destFN = f.getName().substring(14,20) + "00acmCommunications.pdf";
			_destDirName = newsDirName + File.separator + 
					File.separator + "acmCommunications"  + File.separator + _destFN.substring(0, 4);
			_tags = "oAcm,dMagazine,tTech";
		}
		else if (f.getName().startsWith("compw-")) {
			_destFN = f.getName().substring(6, 10) + 
					f.getName().substring(11, 13) +
					f.getName().substring(14, 16) + "computerworld.pdf";
			_destDirName = newsDirName + File.separator + "computerworld" + File.separator + _destFN.substring(0, 4);
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
			_destDirName = docDirName + File.separator + 
					"business" + File.separator + 
					"adnovum" + File.separator +
					"meet" + File.separator + 
					"gl" + File.separator + 
					_destFN.substring(0, 4) + File.separator +
					_meetDate + "glmeet";
			_tags = "oAdnovum,dMinutes,lZuerich";
		}
		else if (f.getName().startsWith("kw")) {
			String _meetDate = f.getName().substring(13, 21);
			_destFN = _meetDate + "mmgl.pdf";
			_destDirName = docDirName + File.separator + 
					"business" + File.separator + 
					"adnovum" + File.separator +
					"meet" + File.separator + 
					"gl" + File.separator + 
					_destFN.substring(0, 4) + File.separator +
					_meetDate + "glmeet";
			_tags = "oAdnovum,dMinutes,lZuerich";
		}
		else if ((_dateStr = getLeadingDateFromString(f.getName())) != null) { // file name starts with date
			_destFN = f.getName();
			if (f.getName().substring(8).startsWith("ic") && _dateStr.length()>=6) {		// Inside Channels
				_destDirName = newsDirName + File.separator + 
						"insideChannels" + File.separator + 
						_dateStr.substring(0, 4) + File.separator + 
						_dateStr.substring(4,6);
				_tags = "dNews,tTech";
			}
			else if (f.getName().substring(8).startsWith("rg") && _dateStr.length()>=6) {		// Rechnung / Bill
				_destDirName = docDirName + File.separator + 
						"finance" + File.separator +
						"rechnungen" + File.separator +
						f.getName().substring(0, 4);
				_tags = "tFinance,dBill";
			}
			else if (f.getName().substring(8).startsWith("zkb") && _dateStr.length()>=6) {		// ZKB
				_destDirName = docDirName + File.separator + 
						"finance" +  File.separator +
						"bank" +  File.separator +
						"zkbPriv";
				_tags = "tFinance,dBill,oZkb";
			}
			else if (f.getName().substring(8).startsWith("pf") && _dateStr.length()>=6) {		// PostFinance
				_destDirName = docDirName + File.separator + 
						"finance" +  File.separator +
						"bank" +  File.separator +
						"pfBruno";				
				_tags = "tFinance,dBill,oPost";
			}
			else if (f.getName().substring(8).startsWith("lohn") && _dateStr.length()>=6) {		// Lohn / Salary
				_destDirName = docDirName + File.separator + 
						"finance" +  File.separator + "lohn";
				_tags = "tFinance,dStatement";
			}
			else if (f.getName().substring(8).startsWith("slkk") && _dateStr.length()>=6) {		// SLKK
				_destDirName = docDirName + File.separator + 
						"finance" + File.separator +
						"versicherungen";
				_tags = "tFinance,tInsurance,oSlkk";
			}
			else if (f.getName().substring(8).startsWith("diary") && _dateStr.length()>=6) {	// diary
				_destDirName = docDirName + File.separator + 
						"diary" + File.separator +
						"201x" + File.separator +
						f.getName().substring(0, 4) + File.separator +
						f.getName().substring(4, 6);
				_tags = "oBruno,dDiary";
			}
			else if (f.getName().substring(8).startsWith("abstract_") && _dateStr.length()>=4) {	// abstracts
				_destDirName = docDirName + File.separator + 
						"abstract" + File.separator +
						_dateStr.substring(0,4);	
				_tags = "dAbstract";
			}
			else if (f.getName().substring(8).startsWith("kof") && _dateStr.length()>=6) {	// KOF reports
				_destDirName = newsDirName + File.separator + "kofBulletin";	
				_tags = "dReport,tEco";
			}
			else if (f.getName().substring(8).startsWith("book") && _dateStr.length()>=6) {	// ebook
				_destDirName = docDirName + File.separator + 
						"temp" + File.separator +
						"topics";	
				_tags = "dBook";
			}
			else if (f.getName().substring(8).startsWith("sise") && _dateStr.length()>=6) {		// SI-SE
				_destDirName = docDirName + File.separator + "siseItgse";
				_tags = "oSise";
			}
			else if (f.getName().substring(8).startsWith("awuz") && _dateStr.length()>=6) {		// AWUZ
				_destDirName = docDirName + File.separator + "awuz";	
				_tags = "oAwuzUzha";
			}
			else if (f.getName().substring(8).startsWith("informatikSpektrum") && _dateStr.length()>=6) {		// Informatik Spektruml
				_destDirName = newsDirName + File.separator + 
						"informatikSpektrum" + File.separator +
						f.getName().substring(0, 4);	
				_tags = "tTech,dArticle";
			}
			else if (f.getName().toLowerCase().endsWith("pres.pdf")) {						// presentation
				_destDirName = docDirName + File.separator + "temp";
				_tags = "dPres";
			}
			else if (f.getName().substring(8).startsWith("itc") && _dateStr.length()>=6) {		// IT consulting contract
				_destDirName = docDirName + File.separator + "temp";
				_tags = "dContract,dItc";
			}
			else if (f.getName().substring(8).startsWith("swd") && _dateStr.length()>=6) {		// SW development contract
				_destDirName = docDirName + File.separator + "temp";
				_tags = "dContract,dSwd";
			}
			else if (f.getName().substring(8).startsWith("sla") && _dateStr.length()>=6) {		// maintenance contract
				_destDirName = docDirName + File.separator + "temp";
				_tags = "dContract,dSla";
			}
			else if (f.getName().substring(8).startsWith("nda") && _dateStr.length()>=6) {		// non disclosure agreement
				_destDirName = docDirName + File.separator + "temp";
				_tags = "dContract,dNda";
			}
			else if (f.getName().substring(8).startsWith("offer") && _dateStr.length()>=6) {		// proposal
				_destDirName = docDirName + File.separator + "temp";
				_tags = "dContract,dOffer";
			}

			else {  // there is a leading date, but no special meaning
				if (debugMode) {
					System.out.println(f.getName() + " has leading date, but no meaning -> moving to temp without tags");
				}
				_destDirName = docDirName + File.separator + "temp";  // move file as is, do not add tags
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
					String _destDirName = docDirName + File.separator + 
							"temp" + File.separator +
							"toPegasus" + File.separator +
							"backup";
					if (debugMode) { // just print out what would be done
						System.out.println("mkdir " + new File(_destDirName).getCanonicalPath());
					}
					if (testMode == false) {
						new File(_destDirName).mkdirs(); 			
					}
					File _destF = new File(_destDirName + File.separator + _destFN);
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
							String _destDirName = docDirName + File.separator + 
									"temp" + File.separator +
									"toPegasus" + File.separator +
									"backup";
							if (debugMode == true) { // just print out what would be done
								System.out.println("mkdir " + new File(_destDirName).getCanonicalPath());
							}
							if (testMode == false) {
								new File(_destDirName).mkdirs(); 			
							}
							File _destF = new File(_destDirName + File.separator + _destFN);
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
					String _destDirName = docDirName + File.separator + 
							"temp" + File.separator +
							"toPegasus" + File.separator +
							"software";
					if (debugMode == true) { // just print out what would be done
						System.out.println("mkdir " + new File(_destDirName).getCanonicalPath());
					}
					if (testMode == false) {
						new File(_destDirName).mkdirs(); 			
					}
					File _destF = new File(_destDirName + File.separator + _swFiles[i].getName());
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
