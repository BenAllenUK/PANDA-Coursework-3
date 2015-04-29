import helpers.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/*
	This class is used to launch the server, judge and client, all together
 */
public class UnifiedApp {


	public static void main(String[] args) throws IOException {

		startGame();

	}

		public static void startGame() throws IOException {

			final Runtime runtime = Runtime.getRuntime();

			boolean killingProcess = false;
			boolean processFound = false;
			String processToKill = null;

			//kill any existing servers
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
			runtime.exec("node server/server_service.js");


			//wait for the service to appear
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

			//launch judge
			new Thread(new Runnable() {
				@Override
				public void run() {

					JudgeService.main(new String[]{"localhost", "8123", "1"});

					Logger.logInfo("Judge exited");

				}
			}).start();

			//launch player
			new Thread(new Runnable() {
				@Override
				public void run() {
					//wait an arbitrary duration for judge to connect
						try {
							Thread.sleep(2000);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}

					try {
						PlayerService.main(new String[]{"localhost", "8122", "ab1234", "cd5678", "ef4321", "gh6543", "ab1234", "cd5678"});
					} catch (IOException e) {
						e.printStackTrace();
					}

					Logger.logInfo("Player exited");
				}
			}).start();


		}

	}
