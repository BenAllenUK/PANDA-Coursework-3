import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;

/**
 * Created by rory on 26/04/15.
 */
public class GamePlayer {
	private final Runtime runtime;
	private boolean judgeReady;
	private Process serverProcess;
	private Thread judgeThread;
	private Thread playerThread;

	public static class  CustomPrintStream extends PrintStream {

		enum Winner {DETECITIVES, MR_X, NO_ONE}

		Winner winner = Winner.NO_ONE;

		public CustomPrintStream(final OutputStream out) {
			super(out);
		}

		public CustomPrintStream(final OutputStream out, final boolean autoFlush) {
			super(out, autoFlush);
		}

		public CustomPrintStream(final OutputStream out, final boolean autoFlush, final String encoding) throws UnsupportedEncodingException {
			super(out, autoFlush, encoding);
		}

		public CustomPrintStream(final String fileName) throws FileNotFoundException {
			super(fileName);
		}

		public CustomPrintStream(final String fileName, final String csn) throws FileNotFoundException, UnsupportedEncodingException {
			super(fileName, csn);
		}

		public CustomPrintStream(final File file) throws FileNotFoundException {
			super(file);
		}

		public CustomPrintStream(final File file, final String csn) throws FileNotFoundException, UnsupportedEncodingException {
			super(file, csn);
		}

		@Override
		public void println(final String x) {
			if(x.contains("GAME_OVER [White, Red, Blue, Yellow, Green]")){
				final String[] list = x.substring(8).split(" ");

				System.out.println("game won by "+x.substring(8));
				if(list.length > 1){
					winner = Winner.DETECITIVES;
				}else{
					winner = Winner.MR_X;
				}
			}
			super.println(x);
		}

		public void resetWinner(){
			winner = Winner.NO_ONE;
		}

		public boolean mrXWon(){
			return winner == Winner.MR_X;
		}

		public boolean detectivesWon(){
			return winner == Winner.DETECITIVES;
		}

		//override  print  methods here
	}

	public GamePlayer () throws IOException {

		System.setOut(new CustomPrintStream(System.out));

		runtime = Runtime.getRuntime();

		startGame();


	}

	private void stopGame() {
		serverProcess.destroy();
	}
	private void startGame() throws IOException {

		judgeReady = false;
		boolean killingProcess = false;
		String processToKill = null;
		boolean waiting = true;
		while(waiting) {

			if(processToKill != null && !killingProcess){
				killingProcess = true;
				runtime.exec("kill "+processToKill);
			}
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			waiting = false;
			try {
				String line;
				Process p = runtime.exec("ps -e");
				BufferedReader input =
						new BufferedReader(new InputStreamReader(p.getInputStream()));
				while ((line = input.readLine()) != null) {

					if(line.contains("server_service.js")){
						processToKill = line.split(" ")[0];
						waiting = true;
						break;
					}
				}
				input.close();
			} catch (Exception err) {
				err.printStackTrace();
			}
		}

		serverProcess = runtime.exec("node server/server_service.js");



		judgeThread = new Thread(new Runnable() {
			@Override
			public void run() {
				while(!judgeReady) {
					try {
						serverProcess.getInputStream().read();
					} catch (IOException e) {
						e.printStackTrace();
					}

					try {
						Thread.sleep(200);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}

					judgeReady = true;
				}
			}
		});
		judgeThread.start();

		new Thread(new Runnable() {
			@Override
			public void run() {
				JudgeService.main(new String[]{"localhost", "8123", "1"});
				stopGame();
			}
		}).start();

		playerThread = new Thread(new Runnable() {
			@Override
			public void run() {
				while(!judgeReady){
					try {
						Thread.sleep(200);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				try {
					PlayerService.main(new String[]{"localhost", "8122", "ab1234", "cd5678", "ef4321", "gh6543", "ab1234", "cd5678"});
				} catch (IOException e) {
					e.printStackTrace();
				}
				stopGame();
			}
		});
		playerThread.start();
	}
}
