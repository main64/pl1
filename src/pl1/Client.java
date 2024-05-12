package pl1;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;

/*クライアントプログラムのうち画面描画に関するものである
  5/7時点でデザインについて最低限必要な機能、ボタンは実装済みのはず
  配置やデザインについては未完成 
  完成次第更新します
                                                             瀬野 拓実*/

public class Client extends JFrame implements ActionListener {
	private static PrintWriter out;//データ送信用オブジェクト
	private NameReceiver name_receiver; //データ受信用オブジェクト
	private ColorReceiver color_receiver; //データ受信用オブジェクト
	private MoveReceiver move_receiver; //データ受信用オブジェクト
	private static String enemyName;//相手の名前
	private static int hand;//操作情報
	private String mode; //対戦モードやコンピュータ難易度を示す
	private static int x, y;//コマの座標

	final String EASY = "やさしい"; //コンピュータレベルを指す 3つのいずれかならば同時にローカル対戦中である
	final String NORMAL = "ふつう";
	final String HARD = "難しい";

	final String LOCAL = "ローカル"; //対戦モードを示す
	final String NETWORK = "ネットワーク";

	final int BOARD_BORDER = 8; //オセロの行数・列数

	Othello othello = new Othello();
	CPU cpu;
	int[] cpuPut = new int[2];//操作情報

	JButton[][] boardBottons = new JButton[BOARD_BORDER][BOARD_BORDER]; //ボタン配列 要素となるボタンはコンストラクタ内で宣言
	boolean isBlack; //プレイヤが黒ならばtrue

	public Client() {  //コンストラクタ 盤面のボタンについて定義している

		super("Othello");
		setResizable(true); //現時点ではフレームのサイズ変更はできない前提で書いている
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		for (int i = 0; i < 64; i++) {
			BoardButton button = new BoardButton(i); //新しく宣言したBoardButtonクラスのインスタンス
			button.addActionListener(new ActionListener() { //盤面のボタンが押されたときの動作を定義
				public void actionPerformed(ActionEvent e) {
					int i = button.getI();
					int state = othello.getGridState(i / BOARD_BORDER, i % BOARD_BORDER); //オセロクラスから盤面の状態を受け取る
					
					if (isBlack && (state == 1 || state == 3)) { //自身が黒かつ黒が置けるならば
						othello.mainboard = Othello.update(othello.getBoard(), i / BOARD_BORDER, i % BOARD_BORDER, 1); //盤面を更新
						reflectMap();//盤面情報を盤面ボタンに反映
						if (mode == NETWORK) { //ネットワーク対戦の場合
							sendMessage(Integer.toString(i));//サーバーに送信
						} else { //ローカル対戦の場合
							cpu.CPUMain(othello.getBoard()); //CPUに盤面情報を引き渡す
							cpuPut = cpu.getMove(); //CPUが探索した結果を受け取る
							Othello.update(othello.getBoard(), cpuPut[0], cpuPut[1], 2); //盤面を更新
							reflectMap();//盤面情報を盤面ボタンに反映
						}

					} else if (!isBlack && (state == 2 || state == 3)) { //自身が白かつ白が置けるならば
						Othello.update(othello.getBoard(), i / BOARD_BORDER, i % BOARD_BORDER, 2);
						reflectMap();//盤面情報を盤面ボタンに反映
						if (mode == NETWORK) {
							sendMessage(Integer.toString(i));//サーバーに送信
						} else {//ローカル対戦の場合
							cpu.CPUMain(othello.getBoard());//CPUに盤面情報を引き渡す
							cpuPut = cpu.getMove();//CPUが探索した結果を受け取る
							Othello.update(othello.getBoard(), cpuPut[0], cpuPut[1], 1); //盤面を更新
							reflectMap();//盤面情報を盤面ボタンに反映
						}

					}

					if (othello.endCheck() == 1) { //ゲームが終了しているか確認
						paintResult(); //結果を出力
					}
				}
			});
			boardBottons[i / BOARD_BORDER][i % BOARD_BORDER] = button;
			button.setBackground(Color.GREEN);
			button.setBorder(new LineBorder(Color.BLACK));
		}
		chooseMode();

	}

	public void chooseMode() { //ローカル対戦かネットワーク対戦か選ばせる

		getContentPane().removeAll(); //画面のボタンやラベルをリセット
		setSize(400, 200);
		JPanel p = new JPanel();
		p.setLayout(null);

		JLabel modeLabel = new JLabel("モードを選択してください");
		modeLabel.setBounds(120, 20, 200, 30);
		p.add(modeLabel);

		JButton localButton = new JButton(LOCAL);
		localButton.setBounds(200, 100, 130, 30);
		localButton.addActionListener(this);
		p.add(localButton);

		JButton networkButton = new JButton(NETWORK);
		networkButton.setBounds(50, 100, 130, 30);
		networkButton.addActionListener(this);
		p.add(networkButton);

		add(p);
		setVisible(true);
	}

	public void setName() {

		getContentPane().removeAll(); //画面のボタンやラベルをリセット
		JPanel p = new JPanel();
		p.setLayout(new FlowLayout());

		JTextArea textArea = new JTextArea();
		textArea.setText("Player ");
		p.add(textArea);

		JButton okButton = new JButton("OK");
		okButton.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				if (textArea.getText() != null) { //空列は許さない
					Player.setPlayer_name(textArea.getText());
					sendMessage(textArea.getText());//サーバに名前を送信
					//対戦OKかどうかサーバから応答がほしい
					paintGame(); //対局画面描画に移行 
				}
			}
		});
		p.add(okButton);

		JButton resetButton = new JButton("リセット");
		resetButton.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				textArea.setText("Player ");

			}
		});
		add(p);

		revalidate();
		repaint();
	}

	public void chooseLevel() {
		setSize(400, 300);
		setLayout(new BorderLayout(1, 3));

		getContentPane().removeAll(); //画面のボタンやラベルをリセット
		JPanel p = new JPanel();
		p.setLayout(new GridBagLayout());

		JLabel titleText = new JLabel("難易度の選択");
		add(titleText);
		titleText.setBounds(120, 20, 200, 30);

		JLabel levelText = new JLabel("難易度を選択してください");
		add(levelText);
		levelText.setBounds(120, 100, 200, 30);

		JButton easyButton = new JButton(EASY);
		p.add(easyButton);

		JButton normalButton = new JButton(NORMAL);
		p.add(normalButton);
		
		JButton hardButton = new JButton(HARD);
		p.add(hardButton);
		
		Color defaultColor = UIManager.getColor("Button.background");
        Border defaultBorder = UIManager.getBorder("Button.border");
        
		ActionListener levelButtonListener = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JButton clickedButton = (JButton) e.getSource();
                mode=e.getActionCommand();
                // すべてのボタンの色と枠線をデフォルトに戻す
                resetButtons(easyButton, defaultColor, defaultBorder);
                resetButtons(normalButton, defaultColor, defaultBorder);
                resetButtons(hardButton, defaultColor, defaultBorder);

                // 押されたボタンの色と枠線を変更
                clickedButton.setBackground(Color.YELLOW);
                clickedButton.setBorder(BorderFactory.createLineBorder(Color.RED, 3));
            }
        };

        // ボタンにイベントリスナーを追加
        easyButton.addActionListener(levelButtonListener);
        normalButton.addActionListener(levelButtonListener);
        hardButton.addActionListener(levelButtonListener);
        
		
		if(mode==LOCAL) {
			JPanel bwp=new JPanel();
			JLabel bwtext=new JLabel("先手後手を選んでください");
			bwp.add(bwtext);
			JButton black=new JButton("先手:黒");
			JButton white=new JButton("後手:白");
			
			
			ActionListener bwButtonListener = new ActionListener() {
	            public void actionPerformed(ActionEvent e) {
	            	if(e.getActionCommand()=="先手:黒") {
	            		isBlack=true;
	 
	            	}else {
	            		isBlack=false;
	            	}
	            	
	                JButton clickedButton = (JButton) e.getSource();

	                // すべてのボタンの色と枠線をデフォルトに戻す
	                resetButtons(black, defaultColor, defaultBorder);
	                resetButtons(white, defaultColor, defaultBorder);

	                // 押されたボタンの色と枠線を変更
	                clickedButton.setBackground(Color.YELLOW);
	                clickedButton.setBorder(BorderFactory.createLineBorder(Color.RED, 3));
	                
	            }
	        };
	        black.addActionListener(bwButtonListener);
	        white.addActionListener(bwButtonListener);
	        
			bwp.add(black);
			bwp.add(white);
			p.add(bwp);
		}
		

		JButton ok=new JButton("対局開始");
		ok.addActionListener(this);
		p.add(ok);
		add(p);

		revalidate();
		repaint();
	}
	 private static void resetButtons(JButton button, Color defaultColor, Border defaultBorder) {
	        button.setBackground(defaultColor);
	        button.setBorder(defaultBorder);
	    }

	public void paintGame() {

		getContentPane().removeAll(); //画面のボタンやラベルをリセット
		setSize(700, 700);

		JPanel pMain = new JPanel();
		pMain.setLayout(new BorderLayout());
		JPanel pBoard = new JPanel();
		pBoard.setLayout(new GridLayout(BOARD_BORDER, BOARD_BORDER));
		for (int i = 0; i < 64; i++) {
			pBoard.add(boardBottons[i / BOARD_BORDER][i % BOARD_BORDER]);
		}
		pMain.add(pBoard, "Center");

		JLabel modeLabel = new JLabel();
		if (mode == NETWORK) {
			modeLabel.setText(mode);
		} else {
			modeLabel.setText("コンピュータ:" + mode);
		}

		JButton retire = new JButton("投了");
		retire.addActionListener(this);
		JButton pass = new JButton("パス");
		JLabel otherName = new JLabel(enemyName + "　あなたは" + Player.getBlackwhite() + "です");

		JLabel turnLabel = new JLabel("あなたの番です");
		JPanel lowerP = new JPanel();
		lowerP.add(retire);
		lowerP.add(pass);
		lowerP.add(otherName);
		lowerP.add(turnLabel);
		pMain.add(lowerP, "South");

		pMain.add(modeLabel, "North");
		add(pMain);
		setVisible(true);
		reflectMap();

	}

	public void reflectMap() {
		for (int i = 0; i < BOARD_BORDER; i++) {
			for (int j = 0; j < BOARD_BORDER; j++) {
				switch (othello.getGridState(i, j)) {
				case 0:
					boardBottons[i][j].setText("");
					boardBottons[i][j].setEnabled(false);
					break;
				case 1:
					boardBottons[i][j].setText("");
					boardBottons[i][j].setEnabled(isBlack);
					break;
				case 2:
					boardBottons[i][j].setText("");
					boardBottons[i][j].setEnabled(!isBlack);
					break;
				case 3:
					boardBottons[i][j].setText("");
					boardBottons[i][j].setEnabled(true);
					break;
				case 4:
					boardBottons[i][j].setText("●");
					boardBottons[i][j].setEnabled(true);
					break;
				case 5:
					boardBottons[i][j].setText("〇");
					boardBottons[i][j].setEnabled(true);
					break;
				}
			}
		}
		revalidate();
		repaint();

	}

	class BoardButton extends JButton { //盤面のボタンに用いる 何番目のボタンかを示す整数型変数 i を保持

		int i;

		public BoardButton(int i) {
			setI(i);
		}

		public int getI() {
			return i;
		}

		public void setI(int i) {
			this.i = i;
		}

	}

	public void paintResult() {
		String winner = null;
		switch (othello.judge()) {
		case "black":
			winner = "勝者" + Player.getPlayer_name();
			break;
		case "white":
			winner = enemyName;
			break;
		case "draw":
			winner = "引き分け";
			break;
		default: System.out.println("Error@paintResult");
		}

		setSize(400, 200);
		getContentPane().removeAll();

		JPanel p = new JPanel();

		JLabel winnerLabel = new JLabel(winner);
		p.add(winnerLabel);

		JLabel resultLabel = new JLabel();
		resultLabel.setText("黒 " + othello.getBlack() + "-" + othello.getWhite() + " 白");
		p.add(resultLabel);

		add(p);

		revalidate();
		repaint();
	}

	public static void main(String[] args) {
		// TODO 自動生成されたメソッド・スタブ
		Client client = new Client();

	}

	@Override
	public void actionPerformed(ActionEvent e) {
		
		switch (e.getActionCommand()) {
		case LOCAL: //モード設定段階
			mode = LOCAL;
			chooseLevel(); //コンピュータのレベル選びに移行
			break;
		case NETWORK: //モード設定段階
			mode = NETWORK;
			connectServer("133.34.216.184", 10000);//引数は仮
			setName(); //プレイヤの名前設定に移行
			break;
		case "投了":
			paintResult();
			break;
		case "パス": //パスの処理
			break;
		case "対局開始":
			if(isBlack) {
				cpu = new CPU(mode,"black");
				Player.setBlackwhite("black");
			}else {
				cpu=new CPU(mode,"white");
				Player.setBlackwhite("white");
			}
		
			enemyName = mode;
			paintGame();
			break;
		}

	}

	// メソッド
	public void connectServer(String ipAddress, int port) { // サーバに接続
		Socket socket = null;
		try {
			socket = new Socket(ipAddress, port); //サーバ(ipAddress, port)に接続
			out = new PrintWriter(socket.getOutputStream(), true); //データ送信用オブジェクトの用意
			name_receiver = new NameReceiver(socket); //受信用オブジェクトの準備
			name_receiver.start();//受信用オブジェクト(スレッド)起動
			color_receiver = new ColorReceiver(socket); //受信用オブジェクトの準備
			color_receiver.start();//受信用オブジェクト(スレッド)起動
		} catch (UnknownHostException e) {
			System.err.println("ホストのIPアドレスが判定できません: " + e);
			System.exit(-1);
		} catch (IOException e) {
			System.err.println("サーバ接続時にエラーが発生しました: " + e);
			System.exit(-1);
		}
	}

	public static void sendMessage(String msg) { // サーバに操作情報を送信
		out.println(msg);//送信データをバッファに書き出す
		out.flush();//送信データを送る
		System.out.println("サーバにメッセージ " + msg + " を送信しました"); //テスト標準出力
	}

	// プレイヤ名データ受信用スレッド(内部クラス)
	class NameReceiver extends Thread {
		private InputStreamReader sisr; //受信データ用文字ストリーム
		private BufferedReader br; //文字ストリーム用のバッファ

		// 内部クラスReceiverのコンストラクタ
		NameReceiver(Socket socket) {
			try {
				sisr = new InputStreamReader(socket.getInputStream()); //受信したバイトデータを文字ストリームに
				br = new BufferedReader(sisr);//文字ストリームをバッファリングする
			} catch (IOException e) {
				System.err.println("データ受信時にエラーが発生しました: " + e);
			}
		}

		// 内部クラス Receiverのメソッド
		public void run() {
			try {
				while (true) {//データを受信し続ける
					String inputLine = br.readLine();//受信データを一行分読み込む
					if (inputLine != null) {//データを受信したら
						receiveMessage(inputLine);//データ受信用メソッドを呼び出す
						enemyName = inputLine;
					}
				}
			} catch (IOException e) {
				System.err.println("データ受信時にエラーが発生しました: " + e);
				//コンピュータ対戦に移行？
			}
		}
	}

	// 先手後手データ受信用スレッド(内部クラス)
	class ColorReceiver extends Thread {
		private InputStreamReader sisr; //受信データ用文字ストリーム
		private BufferedReader br; //文字ストリーム用のバッファ

		// 内部クラスColorReceiverのコンストラクタ
		ColorReceiver(Socket socket) {
			try {
				sisr = new InputStreamReader(socket.getInputStream()); //受信したバイトデータを文字ストリームに
				br = new BufferedReader(sisr);//文字ストリームをバッファリングする
			} catch (IOException e) {
				System.err.println("データ受信時にエラーが発生しました: " + e);
			}
		}

		// 内部クラス ColorReceiverのメソッド
		public void run() {
			try {
				while (true) {//データを受信し続ける
					String inputLine = br.readLine();//受信データを一行分読み込む
					if (inputLine != null) {//データを受信したら
						receiveMessage(inputLine);//データ受信用メソッドを呼び出す
						Player.setBlackwhite(inputLine);
					}
				}
			} catch (IOException e) {
				System.err.println("データ受信時にエラーが発生しました: " + e);
				//コンピュータ対戦に移行？
			}
		}
	}

	// 捜査情報データ受信用スレッド(内部クラス)
	class MoveReceiver extends Thread {
		private InputStreamReader sisr; //受信データ用文字ストリーム
		private BufferedReader br; //文字ストリーム用のバッファ

		// 内部クラスColorReceiverのコンストラクタ
		MoveReceiver(Socket socket) {
			try {
				sisr = new InputStreamReader(socket.getInputStream()); //受信したバイトデータを文字ストリームに
				br = new BufferedReader(sisr);//文字ストリームをバッファリングする
			} catch (IOException e) {
				System.err.println("データ受信時にエラーが発生しました: " + e);
			}
		}

		// 内部クラス MoveReceiverのメソッド
		public void run() {
			try {
				while (true) {//データを受信し続ける
					String inputLine = br.readLine();//受信データを一行分読み込む
					if (inputLine != null) {//データを受信したら
						receiveMessage(inputLine);//データ受信用メソッドを呼び出す

					}
				}
			} catch (IOException e) {
				System.err.println("データ受信時にエラーが発生しました: " + e);
				//コンピュータ対戦に移行？
			}
		}
	}

	public void receiveMessage(String msg) { // メッセージの受信
		hand = Integer.parseInt(msg);
		y = hand / 8;
		x = hand % 8;
		System.out.println("サーバからメッセージ " + msg + " を受信しました"); //テスト用標準出力
	}

	public int getX() {
		return x;
	}

	public int getY() {
		return y;
	}

}