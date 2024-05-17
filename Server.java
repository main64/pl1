import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {
	private int port; //サーバの待ち受けポート
	private PrintWriter[] out; //データ送信用オブジェクト
	private NameReceiver[] nameReceiver; //データ受信用オブジェクト
	private MoveReceiver[] moveReceiver; //データ受信用オブジェクト
	private ConnectionChecker[] connectionChecker; //通信状況確認用オブジェクト
	private int online[]; //オンライン状態管理用配列(0は接続状態、1は接続されていない状態)
	private String playerName[]; //プレイヤーネーム管理用配列
	
	//コンストラクタ
	public Server(int port) { //待ち受けポートを引数とする
		this.port = port; //待ち受けポートを渡す
		out = new PrintWriter[2]; //データ送信用オブジェクトを2クライアント分用意
		nameReceiver = new NameReceiver[2]; //データ受信用オブジェクトを2クライアント分用意
		moveReceiver = new MoveReceiver[2]; //データ受信用オブジェクトを2クライアント分用意
		connectionChecker = new ConnectionChecker[2]; //通信状況確認用オブジェクトを2クライアント分用意
		online = new int[2]; //オンライン状態管理配列を用意
		playerName = new String[2]; //プレイヤーネーム管理用配列を用意
	}
	
	//プレイヤ名受信用スレッド
	class NameReceiver extends Thread {
		private InputStreamReader isr; //受信データ用文字ストリーム
		private BufferedReader br; //文字ストリーム用バッファ
		private int playerNo; //プレイヤを識別するための番号、0から始める
		
		//内部クラスNameReceiverのコンストラクタ
		NameReceiver(Socket socket, int playerNo) {
			try {
				this.playerNo = playerNo; //プレイヤ番号を渡す
				isr = new InputStreamReader(socket.getInputStream()); //受信データ用ストリームを用意
				br = new BufferedReader(isr); //受信データ用バッファを用意
			} catch(IOException e) {
				System.err.println("データ受信時にエラーが発生しました: " + e);
			}
		}
		
		//内部クラスNameReceiverのメソッド
		public void run() {
			try {
				while(true) { //初めにプレイヤ名のデータを受信する
					String inputline = br.readLine(); //データを1行分読み込む
					if(inputline != null) { //データを受信したら
						System.out.println("Name" + playerNo + ": " + inputline); //読み込んだ内容をサーバに出力する(確認用)
						playerName[playerNo] = inputline;
						break; //データを1つ受信したらループを終了
					}
				}
			} catch(IOException e) { //接続が切れたとき
				System.err.println("クライアントとの接続が切れました");
				online[playerNo] = 1; //プレイヤの接続状態を更新する
				printStatus(); //接続状態を出力する
			}
		}
	}
	
	//操作情報受信用スレッド
	class MoveReceiver extends Thread {
		private InputStreamReader isr; //受信データ用文字ストリーム
		private BufferedReader br; //文字ストリーム用バッファ
		private int playerNo; //プレイヤを識別するための番号、0から始める
		
		//内部クラスMoveReceiverのコンストラクタ
		MoveReceiver(Socket socket, int playerNo) {
			try {
				this.playerNo = playerNo; //プレイヤ番号を渡す
				isr = new InputStreamReader(socket.getInputStream()); //受信データ用ストリームを用意
				br = new BufferedReader(isr); //受信データ用バッファを用意
			} catch(IOException e) {
				System.err.println("データ受信時にエラーが発生しました: " + e);
			}
		}
		
		//内部クラスMoveReceiverのメソッド
		public void run() {
			try {
				while(true) { //データを受信し続ける
					String inputline = br.readLine(); //データを1行分読み込む
					if(inputline != null) { //データを受信したら
						System.out.println("from" + playerNo + ": " + inputline); //読み込んだ内容をサーバに出力する(確認用)
						forwardMessage(inputline, playerNo); //もう一方に転送する
					}
					if(online[0] == 1 && online[1] == 1) {
						break;
					}
				}
			} catch(IOException e) { //接続が切れたとき
				System.err.println("クライアントとの接続が切れました");
				online[playerNo] = 1; //プレイヤの接続状態を更新する
				printStatus(); //接続状態を出力する
			}
		}
	}
	
	//通信状態確認用スレッド
		class ConnectionChecker extends Thread {
			private InputStreamReader isr; //受信データ用文字ストリーム
			private BufferedReader br; //文字ストリーム用バッファ
			private PrintWriter out; //データ送信用オブジェクト
			private int playerNo; //プレイヤを識別するための番号、0から始める
			private Socket socket;
			
			//内部クラスConnectionCheckerのコンストラクタ
			ConnectionChecker(Socket socket, int playerNo) {
				try {
					this.playerNo = playerNo; //プレイヤ番号を渡す
					isr = new InputStreamReader(socket.getInputStream()); //受信データ用ストリームを用意
					br = new BufferedReader(isr); //受信データ用バッファを用意
					this.socket = socket;
				} catch(IOException e) {
					System.err.println("データ受信時にエラーが発生しました: " + e);
				}
			}
			
			//内部クラスConnectionCheckerのメソッド
			public void run() {
				try {
					out = new PrintWriter(socket.getOutputStream(), true);
					while(true) { //データを受信し続ける
						String inputline = br.readLine(); //データを1行分読み込む
						if(inputline != null) { //データを受信したら
							socket.setSoTimeout(5000);
							System.out.println("from client" + ": " + inputline); //読み込んだ内容をサーバに出力する(確認用)
							out.println("check"); 
	                		out.flush();
						}
					}
				} catch(IOException e) { //接続が切れたとき
					System.err.println("クライアントとの接続が切れました");
					online[playerNo] = 1; //プレイヤの接続状態を更新する
					printStatus(); //接続状態を出力する
				}
			}
		}
	
	//メソッド
	
	public void acceptClient() { //サーバの起動し、クライアントの接続を受け付ける
		try {
			System.out.println("サーバが起動しました");
			ServerSocket ss = new ServerSocket(port); //サーバソケットを用意
			ServerSocket css = new ServerSocket(port + 1); //通信確認用のサーバソケットを用意
			int i=0; //接続クライアント数を数えるための変数
			while(i < 2) { //接続しているクライアントが2以下の場合のみ以下の処理
				Socket socket = ss.accept(); //新規接続を受け付ける
				System.out.println("クライアント"+ (i+1) +"とデータポートで接続しました");
				Socket checkSocket = css.accept(); //
				System.out.println("クライアント"+ (i+1) +"とチェックポートで接続しました");
				System.out.println("クライアント"+ (i+1) +"と接続しました");
				out[i] = new PrintWriter(socket.getOutputStream(), true); //データ送信オブジェクト(スレッド)を用意
				nameReceiver[i] = new NameReceiver(socket, i); //データ受信オブジェクト(スレッド)を用意
				moveReceiver[i] = new MoveReceiver(socket, i); //データ受信オブジェクト(スレッド)を用意
				connectionChecker[i] = new ConnectionChecker(checkSocket, i); //通信状況確認用オブジェクト(スレッド)を用意
				i++; //次のクライアントを受け入れるためにカウンタを1増やす
			}
			ss.close();
			css.close();
			connectionChecker[0].start();
			connectionChecker[1].start();
			sendColor(); //それぞれの色を通知
			nameReceiver[0].start(); //2クライアントが接続したら、データ受信オブジェクト(スレッド)を起動
			nameReceiver[1].start();
			nameReceiver[0].join(); //両者のプレイヤ名が入力されるまで待機
			nameReceiver[1].join();
			sendOpponentPlayerName();
			moveReceiver[0].start(); //2クライアントが接続したら、データ受信オブジェクト(スレッド)を起動
			moveReceiver[1].start();
		} catch(Exception e) {
			System.err.println("ソケット作成時にエラーが発生しました" + e);
		}
	}
	
	public void printStatus() { //クライアント接続状態の確認
		for(int i=0;i<online.length;i++) { //それぞれのクライアントについて、接続状態を示す
			if(online[i] == 0) { //すべてのクライアント情報を表示するための条件分岐
				System.out.println("playerNo " + i + " は接続されています");
			} else if(online[i] == 1) {
				System.out.println("playerNo " + i + " は接続されていません");
				if(i == 0) {
					out[1].println("-1");
					out[1].flush();
				} else if(i == 1) {
					out[0].println("-1");
					out[0].flush();
				}
			} else {
				System.err.println("<printStatus>未定義のonline[]です, online[]: " + online[i] + ", i: " + i);
			}
		}
	}
	
	public void sendColor() { //先手後手情報(白黒)の送信
		out[0].println("black"); //それぞれに先手後手情報を送信する
		out[0].flush();
		out[1].println("white");
		out[1].flush();
	}
	
	public void forwardMessage(String msg, int playerNo) { //操作情報の転送
		if(playerNo == 0) { //先に接続したプレイヤのメッセージを後手に送る
			out[1].println(msg); 
			out[1].flush();
		} else if(playerNo == 1) { //後手プレイヤのメッセージを先手に送る
			out[0].println(msg);
			out[0].flush();
		} else {
			System.err.println("<forwardMessage>未定義のplayerNoです, playerNo: " + playerNo);
		}
	}
	
	public void sendOpponentPlayerName() { //対戦相手のプレイヤ名を送信
		out[0].println(playerName[1]);
		out[0].flush();
		out[1].println(playerName[0]);
		out[1].flush();
	}
	
	public static void main(String[] args) { //mainメソッド
		Server server = new Server(10000); //待ち受けポート10000番でサーバオブジェクトを準備
		server.acceptClient(); //クライアント受け入れを開始
	}

}

