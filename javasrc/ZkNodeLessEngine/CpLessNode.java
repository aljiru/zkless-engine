import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import com.yahoo.platform.yui.compressor.CssCompressor;



public class CpLessNode {

	public static void main(String[] args) throws IOException {
		long start = new java.util.Date().getTime();
		final File srcdir = new File("C:\\home\\Han\\zk60\\zk\\zul\\src");
		final String theme = "";
		final String sep = File.separator;
		List<File> targets = new ArrayList<File>();
		
		for (File srcFile : getFiles(srcdir, new ArrayList<File>(30))) {
			String result = SyntaxHelper.encodeDsp(loadSource(srcFile), theme);
			String dest = srcFile.getPath();
			dest = dest.replaceAll("src", "codegen");
			File destPath = new File(dest);
			if (!destPath.getParentFile().isDirectory()) {
				destPath.getParentFile().mkdirs();
			}
			writeToFile(result, dest);
			if (srcFile.getName().startsWith("_")){
				continue;
			} else {
				targets.add(destPath);
			}
		}
		
		//2. compile the less files and write to file
		for (File file : targets) {
			String result = compileLess(file);
			String path = file.getPath();
			result = SyntaxHelper.decodeDsp(result);
			// replace folder name
			path = path.replace(sep + "less", sep + "css");
			String dspSrcPath = path.replace(".less", ".css.dsp.src");
			String dspPath = path.replace(".less", ".css.dsp");
			File dspSrcFile = new File(dspSrcPath);
			File dspFile = new File(dspPath);
			if (!dspSrcFile.getParentFile().isDirectory()) {
				dspSrcFile.getParentFile().mkdirs();
			}
			writeToFile(result, dspSrcPath);
			
			/* Compress *.css.dsp.src to *.css.dsp */
			// bug fix for UTF8 BOM issue by TonyQ
			InputStreamReader in = new UnicodeReader(
					new FileInputStream(dspSrcFile), "UTF-8");
			OutputStreamWriter out = new OutputStreamWriter(
					new FileOutputStream(dspFile), "UTF-8");
			CssCompressor compressor = new CssCompressor(in);
			compressor.compress(out, -1);
			in.close();
			out.close();
		}
		
		System.out.println(new java.util.Date().getTime() - start);
	}
	
	private static String compileLess(File path) throws IOException {
		
		List<String> argv = new ArrayList<String>();
		String pathStr = path.getPath();
		log("compiling: " + pathStr);
		argv.add("lessc.cmd");
		argv.add("./" + pathStr.substring(pathStr.lastIndexOf("web")));
		ProcessBuilder builder = new ProcessBuilder("lessc.cmd");
		builder.directory(new File(pathStr.substring(0, pathStr.lastIndexOf("web") - 1)));
		builder.command(argv);						
		Process pr = builder.start();
		BufferedReader input = new BufferedReader(new InputStreamReader(pr.getInputStream()));
		String line = null;
		StringBuffer sb = new StringBuffer();
		while ((line = input.readLine()) != null) {
			sb.append(line + "\n");
		}
		
		return sb.toString();
	}
	
	private static void log(Object... os) {
		for (Object o : os) {
			System.out.print(o + (os[os.length - 1] != o ? "," : ""));
		}
		System.out.println();
	}

	private static List<File> getFiles(File dir, List<File> list) {
		String dirName = dir.getName();
		if (".svn".equals(dirName) || ".git".equals(dirName)
				|| "CVS".equals(dirName))
			return list;
		if (dir.isDirectory()) {
			for (File f : dir.listFiles()) {
				getFiles(f, list);
			}
		} else if (dir.isFile() && dirName.endsWith(".less"))
			list.add(dir);
		return list;
	}
	
	private static void writeToFile(String result, String dest) throws IOException {
		BufferedWriter bw = new BufferedWriter(new FileWriter(dest));
		bw.write(result);
		bw.close();
	}
	
	private  static String loadSource(Object src) {
		Reader reader = null;
		if (src instanceof File)
			try {
				reader = new FileReader((File) src);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		else if (src instanceof InputStream)
			reader = new InputStreamReader((InputStream) src);
		else
			log("Unknow input source");
		reader = new BufferedReader(reader);
		StringWriter w = new StringWriter();
		char[] buf = new char[1024];
		int numRead = 0;
		try {
			while ((numRead = reader.read(buf)) != -1) {
				String readData = String.valueOf(buf, 0, numRead);
				w.append(readData);
				buf = new char[1024];
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return w.toString();
	}
}
