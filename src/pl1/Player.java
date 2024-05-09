package pl1;



public class Player {

	private static String playerName="あなた";
	private static String blackwhite;

	public Player() {
		// TODO 自動生成されたコンストラクター・スタブ
	}

	public static String getPlayer_name() {
		return playerName;
	}

	public static void setPlayer_name(String name) {
		playerName = name;
		System.out.println(playerName);
		System.out.println("puch");
	}

	public static String getBlackwhite() {
		return blackwhite;
	}

	public static  void setBlackwhite(String color) {
		blackwhite = color;
	}

	
}
