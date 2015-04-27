package MachineLearning;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.Permission;

/**
 * Created by rory on 26/04/15.
 */
public class GameInstance {

	enum Winner {DETECITIVES, MR_X, NO_ONE}

	private final Runtime runtime;
	private final CustomPrintStream logger;
	private final PrintStream stdout;
	private boolean judgeReady;
	private boolean playing;
	private Process serverProcess;
	private Thread judgeThread;
	private Thread playerThread;

	public class  CustomPrintStream extends PrintStream {
		private int currentRound;


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

			if(x.contains("ROUND:")){
				final String[] list = x.split(" ");
				currentRound = Math.max(currentRound, Integer.parseInt(list[1]));
			}
			if(x.contains("GAME_OVER")){
				final String[] list = x.substring(8).split(" ");

				System.out.println("game won by " + x.substring(8));
				if(list.length > 1){
					winner = Winner.DETECITIVES;
				}else{
					winner = Winner.MR_X;
				}
				gameEnded();
			}
			super.println(x);

		}

		public void reset(){
			winner = Winner.NO_ONE;
			currentRound = 0;
		}

		public boolean mrXWon(){
			return winner == Winner.MR_X;
		}

		public boolean detectivesWon(){
			return winner == Winner.DETECITIVES;
		}

		public int getCurrentRound() {
			return currentRound;
		}

		//override  print  methods here
	}

	protected static class ExitException extends SecurityException
	{
		public final int status;
		public ExitException(int status)
		{
			super("There is no escape!");
			this.status = status;
		}
	}

	private static class NoExitSecurityManager extends SecurityManager
	{
		@Override
		public void checkPermission(Permission perm)
		{
			// allow anything.
		}
		@Override
		public void checkPermission(Permission perm, Object context)
		{
			// allow anything.
		}
		@Override
		public void checkExit(int status)
		{
			super.checkExit(status);
			throw new ExitException(status);
		}
	}

	public GameInstance() throws IOException {

		stdout = System.out;
		logger = new CustomPrintStream(System.out);
		System.setOut(logger);
		System.setSecurityManager(new NoExitSecurityManager());

		runtime = Runtime.getRuntime();

	}

	public void destroy(){
		System.setOut(stdout);

		stopGame();

	}

	private void stopGame() {
		serverProcess.destroy();
	}

	public GameResult startGame() throws IOException {

		logger.reset();
		judgeReady = false;
		playing = true;
		boolean killingProcess = false;
		boolean processFound = false;
		String processToKill = null;
		while(processFound) {

			if(processToKill != null && !killingProcess){
				killingProcess = true;
				runtime.exec("kill "+processToKill);
			}
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			processFound = false;
			try {
				String line;
				Process p = runtime.exec("ps aux");
				BufferedReader input =
						new BufferedReader(new InputStreamReader(p.getInputStream()));
				while ((line = input.readLine()) != null) {

					if(line.contains("server_service.js")){
						processToKill = line.split(" ")[0];
						processFound = true;
						break;
					}
				}
				input.close();
			} catch (Exception err) {
				err.printStackTrace();
			}
		}

		processFound = false;
		serverProcess = runtime.exec("node server/server_service.js");


		while(!processFound) {

			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			try {
				String line;
				Process p = runtime.exec("ps aux");
				BufferedReader input =
						new BufferedReader(new InputStreamReader(p.getInputStream()));
				while ((line = input.readLine()) != null) {

					if(line.contains("server_service.js")){
						processFound = true;
						break;
					}
				}
				input.close();
			} catch (Exception err) {
				err.printStackTrace();
			}
		}

		new Thread(new Runnable() {
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
		}).start();

		judgeThread = new Thread(new Runnable() {
			@Override
			public void run() {

				try {
					Class<?> cls = Class.forName("JudgeService");
					Method main = cls.getMethod("main", String[].class);
					String[] params = new String[]{"localhost", "8123", "1"};
					main.invoke(null, (Object) params);
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
				} catch (NoSuchMethodException e) {
					e.printStackTrace();
				} catch (InvocationTargetException e) {
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				} catch (Exception e) {

				}

				System.out.println("Judge exited");
				gameEnded();

			}
		});

		judgeThread.start();

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
					Class<?> cls = Class.forName("PlayerService");
					Method main = cls.getMethod("main", String[].class);
					String[] params = new String[]{"localhost", "8122", "ab1234", "cd5678", "ef4321", "gh6543", "ab1234", "cd5678"};
					main.invoke(null, (Object) params);
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
				} catch (NoSuchMethodException e) {
					e.printStackTrace();
				} catch (InvocationTargetException e) {
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				}

				System.out.println("Player exited");
				gameEnded();
			}
		});
		playerThread.start();


		while(playerThread.isAlive() || judgeThread.isAlive()){
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		return new GameResult(!logger.mrXWon() && !logger.detectivesWon(), logger.getCurrentRound());
	}

	private void gameEnded() {

		if(!playing){
			return;
		}

		playing = false;

		if(logger.mrXWon()){
			System.out.println("Mr X Won");
		}else if(logger.detectivesWon()){
			System.out.println("Detectives won");
		}else{
			System.err.println("No one won - error");
		}
	}
}
