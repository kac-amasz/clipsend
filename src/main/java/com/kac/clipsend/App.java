package com.kac.clipsend;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class App {
	public static class ClipSend {
		Timer timer;
		Socket socket;

		public ClipSend(Clipboard clipboard, String server) throws URISyntaxException {
			timer = new Timer();

			socket = IO.socket(server);
			socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {

				@Override
				public void call(Object... args) {
					System.out.println("connected");
				}

			}).on("event", new Emitter.Listener() {
				@Override
				public void call(Object... args) {
					System.out.println(Arrays.toString(args));
				}
			}).on(Socket.EVENT_CONNECT_ERROR, new Emitter.Listener() {

				@Override
				public void call(Object... arg0) {
					System.out.println("conn err");

				}
			}).on(Socket.EVENT_DISCONNECT, new Emitter.Listener() {

				@Override
				public void call(Object... args) {
					System.out.println("disconn");
				}

			});
			timer.schedule(new TimerTask() {

				@Override
				public void run() {
					if (!socket.connected()) {
						socket.connect();
					}
				}
			}, 0, 10000);

			new ClipToSock().regain(Toolkit.getDefaultToolkit().getSystemClipboard());
		}

		public class ClipToSock implements ClipboardOwner {

			@Override
			public void lostOwnership(final Clipboard clipboard, Transferable contents) {
				System.out.println("lost");
				regain(clipboard);
				timer.schedule(new TimerTask() {

					@Override
					public void run() {
						try {
							Transferable contents = clipboard.getContents(this);
							if (contents.isDataFlavorSupported(DataFlavor.stringFlavor)) {
								String s = (String) clipboard.getData(DataFlavor.stringFlavor);
								s = s.replaceAll("\\s", "");
								if (s.length() == 10)
									try {
										Long.parseLong(s);
										System.out.println("emit " + s + " " + socket.connected());
										socket.emit("clipboard copy", s);
									} catch (Exception e) {
									}
							}
						} catch (Exception e) {
							e.printStackTrace();
						}

					}
				}, 200);

			}

			public void regain(Clipboard c) {
				while (true)
					try {
						Transferable t = c.getContents(this);
						c.setContents(t, this);
						System.out.println("got");
						break;
					} catch (Exception e) {
						try {
							Thread.sleep(200);
						} catch (Exception e2) {
						}
					}
			}

		}
	}

	public static void main(String[] args) throws Exception {
		new ClipSend(Toolkit.getDefaultToolkit().getSystemClipboard(), args[0]);
	}
}
