package helpers;

import com.google.gson.Gson;
import models.DataSave;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.util.List;

/**
 * Created by rory on 20/04/15.
 */
public class DataParser {

	private final Gson gson;

	public DataParser () {
		gson = new Gson();
	}


	public DataSave loadV3Data(File file) throws IOException {

		List<String> lines = Files.readAllLines(file.toPath());

		StringBuilder sb = new StringBuilder("");

		for(String line : lines) {
			sb.append(line);
		}

		return gson.fromJson(sb.toString(), DataSave.class);
	}

	public void saveV3Data(DataSave data, File file) throws FileNotFoundException, UnsupportedEncodingException {

		PrintWriter writer = new PrintWriter(file, "UTF-8");
		writer.write(gson.toJson(data));
		writer.close();

	}
}