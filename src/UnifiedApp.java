import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Created by rory on 28/04/15.
 */
public class UnifiedApp {


	public static void main(String[] args) throws IOException {

		startGame();

	}

		public static void startGame() throws IOException {

			final Runtime runtime = Runtime.getRuntime();

			boolean judgeReady = false;
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
			final Process serverProcess = runtime.exec("node server/server_service.js");


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
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			new Thread(new Runnable() {
				@Override
				public void run() {

					JudgeService.main(new String[]{"localhost", "8123", "1"});

					System.out.println("Judge exited");

				}
			}).start();

			new Thread(new Runnable() {
				@Override
				public void run() {
						try {
							Thread.sleep(4000);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}

					try {
						PlayerService.main(new String[]{"localhost", "8122", "ab1234", "cd5678", "ef4321", "gh6543", "ab1234", "cd5678"});
					} catch (IOException e) {
						e.printStackTrace();
					}

					System.out.println("Player exited");
				}
			}).start();


		}

	}
