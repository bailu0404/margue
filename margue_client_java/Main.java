import java.io.*;

public class Main implements AudioConference.Callback {
	public void	receiveData(byte[] bs, int len) {
		System.out.println(new String(bs, 0, len));
	}

	public static void main(String[] args) throws Exception {
		if (args.length < 3) {
			System.out.println("Usage: java Main <domain> <port> <up/down>");
			return;
		}
		AudioConference ac = new AudioConference();
		ac.setCallback(new Main());
		if (!ac.connect(args[0], Integer.parseInt(args[1]), "1", "xyz")) {
			System.out.println("Failed to connect server!");
			return;
		}
		try {
			if ("UP".equalsIgnoreCase(args[2])) {
				AudioConference.Stream up = ac.getUpStream();
				if (null != up) {
					try {
						Thread.sleep(50000L);
					} catch (Exception e) {
						e.printStackTrace();
					} finally {
						ac.closeStream(up);
					}
				}
			} else if ("DOWN".equalsIgnoreCase(args[2])) {
				AudioConference.Stream down = ac.getDownStream();
				if (null != down) {
					try {
						Thread.sleep(500000L);
					} catch (Exception e) {
						e.printStackTrace();
					} finally {
						ac.closeStream(down);
					}
				}
			}
		} finally {
			ac.close();
		}
	}
}
