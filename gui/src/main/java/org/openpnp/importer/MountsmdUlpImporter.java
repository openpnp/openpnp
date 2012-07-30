package org.openpnp.importer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openpnp.model.Board;
import org.openpnp.model.Board.Side;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Placement;

//C1 41.91 34.93 180 0.1uF C0805
//C2  8.89 15.24   0 1uF C0805
//C3  8.89 12.70   0 10uF C0805
//C4  4.45 12.70   0 1uF C0805
//C5  4.45 15.24   0 10uF C0805
//C6 52.07 44.45   0 0.1uF C0805
//C7 59.69 21.59 180 0.1uF C0805
//IC1 33.02 39.37   0 ATMEGA168 TQFP32-08
//IC2 18.54 39.24 270  SO18L
//IC3  7.62 22.98   0 7805DT TO252
//IC4 52.07 39.37 270 LTC485S SOIC8
//IC5 51.56 13.97   0 74AC138D SO16
//R9 41.91 43.94 180 10k R0805
//RN1 54.61 22.86 180 1k EXBS8V
//RN2 46.99 22.86 180 1k EXBS8V
//RN3 16.51 22.86 180 1k EXBS8V
//RN4  8.89 42.55 270 47 EXBS8V
//RN5  8.89 37.21 270 47 EXBS8V
//S1 62.23 14.22 270  TACTILE_SWITCH_SMD
//T1 41.91 19.30  90  SOT23-BEC
//T2 36.83 19.30  90  SOT23-BEC
//T3 31.75 19.30  90  SOT23-BEC
//T4 26.67 19.30  90  SOT23-BEC
//T5 21.59 19.30  90  SOT23-BEC
//T6 41.91 14.22  90  SOT23-BEC
//T7 36.83 14.22  90  SOT23-BEC
//T8 31.75 14.22  90  SOT23-BEC
//T9 26.67 14.22  90  SOT23-BEC
//T10 21.59 14.22  90  SOT23-BEC

// printf("%s %5.2f %5.2f %3.0f %s %s\n",


public class MountsmdUlpImporter {
	private Board board;
	private File topFile, bottomFile;
	
	public MountsmdUlpImporter() {
		topFile = new File("/Users/jason/Desktop/BTPD-v6.mnt");
		bottomFile = new File("/Users/jason/Desktop/BTPD-v6.mnb");
	}
	
	public void setBoard(Board board) {
		this.board = board;
	}
	
	private void parse() throws Exception {
		List<Placement> placements = parseFile(topFile, Side.Top);
		placements.addAll(parseFile(bottomFile, Side.Bottom));
		board.getPlacements().addAll(placements);
	}
	
	private static List<Placement> parseFile(File file, Side side) throws Exception {
		BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
		ArrayList<Placement> placements = new ArrayList<Placement>();
		String line;
		while ((line = reader.readLine()) != null) {
			line = line.trim();
			if (line.length() == 0) {
				continue;
			}
			
			Pattern pattern = Pattern.compile("(\\w+\\d+)\\s+(\\d+\\.\\d+)\\s+(\\d+\\.\\d+)\\s+(\\d{1,3})\\s(.*?)\\s(.*)");
			Matcher matcher = pattern.matcher(line);
			matcher.matches();
			Placement placement = new Placement(matcher.group(1));
			placement.getLocation().setUnits(LengthUnit.Millimeters);
			placement.getLocation().setX(Double.parseDouble(matcher.group(2)));
			placement.getLocation().setY(Double.parseDouble(matcher.group(3)));
			placement.getLocation().setRotation(Double.parseDouble(matcher.group(4)));
			placement.setSide(side);
			placements.add(placement);
		}
		return placements;
	}
	
	public static void main(String[] args) throws Exception {
		new MountsmdUlpImporter().parse();
	}
}
