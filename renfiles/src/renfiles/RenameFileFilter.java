package renfiles;
import java.io.File;

/**
 * A FilenameFilter with a configurable selection criteria.
 * Instances of classes that implement this interface are used to
 * filter filenames. These instances are used to filter directory
 * listings in the <code>list</code> method of class
 * <code>File</code>, and by the Abstract Window Toolkit's file
 * dialog component.
 *
 * @author  Bruno Kaiser
 * @see     java.io.FilenameFilter
 * @see     java.io.File#list(java.io.FilenameFilter)
 * @since   JDK1.0
 */
public class RenameFileFilter implements java.io.FilenameFilter {
	private String fileNameExtension = "";
	/**
	 * Constructor.
	 * 
	 * @param ext			the command line parameters (@see #printUsage()) for a list of valid arguments.
	 */
	public RenameFileFilter(String ext) {
		fileNameExtension = ext;
	}

    /**
     * Tests if a specified file should be included in a file list.
     *
     * @param   dir    the directory in which the file was found.
     * @param   name   the name of the file.
     * @return  <code>true</code> if and only if the name should be
     * included in the file list; <code>false</code> otherwise.
     */
	public boolean accept(File dir, String name) {
	//	System.out.print("accept(" + dir.toString() + ", " + name + ") <-" + fileNameExtension);
		if (name.toLowerCase().endsWith(fileNameExtension)) { 
	//		System.out.println(" = true");
			return true;
		}
		else {
	//		System.out.println(" = false");
			return false;
		}
	}

}
