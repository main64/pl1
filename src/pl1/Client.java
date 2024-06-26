import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Graphics;
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
import java.util.Timer;
import java.util.TimerTask;

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

/*
変更点（5/10）時点　　　大嶋
画面遷移に待機中画面を追加(waitMode()メソッド)、（データの通信の判断をしやすくするため）NameReceiverおよびColorReceiverクラスのスレッドがそれぞれの情報を受け取ったら終了するように変更
データの受信は先手後手情報、名前の順番
ネットワーク対局を選択->相手が接続するのを待機画面で待つ->相手が接続したら名前の入力->お互いが入力するまで待機->対局画面


*/

public class Client extends JFrame implements ActionListener {
	private static PrintWriter out;//データ送信用オブジェクト
	private NameReceiver name_receiver; //データ受信用オブジェクト
	private ColorReceiver color_receiver; //データ受信用オブジェクト
	private MoveReceiver move_receiver; //データ受信用オブジェクト
	private ConnectionChecker connectionChecker;
	private static String enemyName;//相手の名前
	private static int hand;//操作情報
	private String mode; //対戦モードやコンピュータ難易度を示す
	private static int x, y;//コマの座標
	Othello othello = new Othello();//オセロクラスのオブジェクトを生成
	
	final String EASY = "やさしい"; //コンピュータレベルを指す 3つのいずれかならば同時にローカル対戦中である
	final String NORMAL = "ふつう";
	final String HARD = "難しい";

	final String LOCAL = "ローカル"; //対戦モードを示す
	final String NETWORK = "ネットワーク";

	final String PASS = "パス";
	JButton pass = new JButton(PASS);//パス
	JLabel turnLabel = new JLabel();//切り替えるためインスタンスで宣言
	
	final int BOARD_BORDER = 8; //オセロの行数・列数

	CPU cpu;
	int[] cpuPut = new int[2];//操作情報

	BoardButton[][] boardBottons = new BoardButton[BOARD_BORDER][BOARD_BORDER]; //ボタン配列 要素となるボタンはコンストラクタ内で宣言
	int map[][] = { 
			{ 0, 0, 0, 0, 0, 0, 0, 0 },
			{ 0, 0, 0, 0, 0, 0, 0, 0 },
			{ 0, 0, 0, 1, 2, 0, 0, 0 },
			{ 0, 0, 1, 5, 4, 2, 0, 0 },
			{ 0, 0, 2, 4, 5, 1, 0, 0 },
			{ 0, 0, 0, 2, 1, 0, 0, 0 },
			{ 0, 0, 0, 0, 0, 0, 0, 0 },
			{ 0, 0, 0, 0, 0, 0, 0, 0 } };
	
	boolean isBlack; //プレイヤが黒ならばtrue
	static boolean myTurn; //自分のターンを管理
	int giveUp = 0;//0:投了が押されていない、1:自分が押した、2:相手が押した
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
					myTurn = !myTurn;//手番の入れ替え
					String data=Integer.toString(i);//ボタン番号をStringに変換
					if (isBlack && (state == 1 || state == 3)) { //自身が黒かつ黒が置けるならば
						//othello.mainboard = Othello.update(othello.getBoard(), i / BOARD_BORDER, i % BOARD_BORDER, 1); //盤面を更新
						othello.putDisc(getY(data), getX(data));//自分の手
						System.out.println(othello.getDiscnum());
						reflectMap();//盤面情報を盤面ボタンに反映
						if (mode == NETWORK) { //ネットワーク対戦の場合
							sendMessage(Integer.toString(i));//サーバーに送信
						} else { //ローカル対戦の場合
							
							TimerTask task = new TimerTask() {
						        public void run() {
						         cpu.CPUMain(othello); //CPUに盤面情報を引き渡す
						         cpuPut = cpu.getMove(); //CPUが探索した結果を受け取る
						         if(cpuPut[0] != -1 & cpuPut[1] != -1) {
						        	 othello.putDisc(cpuPut[0], cpuPut[1]); //盤面を更新
						         }
						         myTurn=!myTurn;//CPUが置いたとき手番を反転
						         reflectMap();//盤面情報を盤面ボタンに反映
						        }
						       };
						       Timer timer = new Timer();
						       timer.schedule(task, 1000);
						}

					} else if (!isBlack && (state == 2 || state == 3)) { //自身が白かつ白が置けるならば
						//Othello.update(othello.getBoard(), i / BOARD_BORDER, i % BOARD_BORDER, 2);
						othello.putDisc(getY(data), getX(data));//自分の手
						System.out.println(othello.getDiscnum());
						reflectMap();//盤面情報を盤面ボタンに反映
						if (mode == NETWORK) {
							sendMessage(Integer.toString(i));//サーバーに送信
						} else {//ローカル対戦の場合
							
							TimerTask task = new TimerTask() {
						        public void run() {
						         cpu.CPUMain(othello); //CPUに盤面情報を引き渡す
						         cpuPut = cpu.getMove(); //CPUが探索した結果を受け取る
						         if(cpuPut[0] != -1 & cpuPut[1] != -1) {
						         	othello.putDisc(cpuPut[0], cpuPut[1]); //盤面を更新
						         }
						         myTurn=!myTurn;//CPUが置いたとき手番を反転
						         reflectMap();//盤面情報を盤面ボタンに反映
						        }
						       };
						       Timer timer = new Timer();
						       timer.schedule(task, 1000);
						}

					}
										
					if (othello.endCheck() == 1) { //ゲームが終了しているか確認
						paintResult(); //結果を出力
					}
					
					
					reflectMap();
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

	public void setName() {//ネットワーク対戦を選択時にプレイヤ名を入力


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
					waitMode();//対戦準備
					sendMessage(textArea.getText());//サーバに名前を送信
					//対戦OKかどうかサーバから応答がほしい
					
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
	public void waitMode() {
		System.out.println("待機中");//テスト用出力
		getContentPane().removeAll(); //画面のボタンやラベルをリセット
		JPanel p = new JPanel();
		p.setLayout(new FlowLayout());

		JLabel label = new JLabel("待機中...");
		p.add(label);

		add(p);
		setVisible(true);
	}
	public void toCPU() {//cpu対戦に移行するとき
		getContentPane().removeAll(); //画面のボタンやラベルをリセット
		System.out.println("サーバーからエラーメッセージを受信");//テスト用出力
		JPanel p = new JPanel();
		p.setLayout(new FlowLayout());

		JLabel label = new JLabel("相手プレイヤーが切断しました。ローカルモードに移行します");
		p.add(label);
		
		JButton okButton = new JButton("OK");
		p.add(okButton);
		add(p);
		setVisible(true);
		ActionListener ButtonListener = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				chooseLevel();
			}
			
		};
		okButton.addActionListener(ButtonListener);
		
	}
	
	
	
	public void chooseLevel() {
		setSize(1200, 300);
		setLayout(new GridLayout(5, 1));

		getContentPane().removeAll(); //画面のボタンやラベルをリセット
		JPanel p = new JPanel();
		p.setLayout(new GridBagLayout());

		JLabel titleText = new JLabel("難易度の選択");
		add(titleText);
		

		JLabel levelText = new JLabel("難易度を選択してください");
		add(levelText);
		

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
	            		myTurn=true;
	 
	            	}else {
	            		isBlack=false;
	            		myTurn=false;
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
			add(bwp);
		}
		
		
        add(p);
		JButton ok=new JButton("対局開始");
		ok.addActionListener(this);
		add(ok);
		

		revalidate();
		repaint();
	}
	 private static void resetButtons(JButton button, Color defaultColor, Border defaultBorder) {
	        button.setBackground(defaultColor);
	        button.setBorder(defaultBorder);
	 }

	 public String getTurnText() {//画面に手番の表示をするためのメソッド
			if (myTurn) {
				return "あなたの番です";
			} else {
				return "相手の番です";
			}
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
		JLabel otherName = new JLabel("相手：" + enemyName + "　あなたは" + Player.getBlackwhite() + "です");

		//JLabel turnLabel = new JLabel("あなたの番です");

		turnLabel.setText(getTurnText());//どちらの手番かを表示
		JPanel lowerP = new JPanel();
		pass.addActionListener(this);
		lowerP.add(retire);
		lowerP.add(pass);
		lowerP.add(otherName);
		lowerP.add(turnLabel);
		pMain.add(lowerP, "South");
		pMain.add(modeLabel, "North");
		add(pMain);
		pass.setEnabled(false);//パスを無効
		setVisible(true);
		reflectMap();


	}

	public void reflectMap() {
		
		map = othello.getBoard();//盤面状況をオセロクラスから取得
		
		turnLabel.setText(getTurnText());//どちらの手番かを表示
		if (!Othello.passCheck(othello.getBoard(),othello.turn+1) && myTurn) {//置けるところがないかつ自分のターン
			pass.setEnabled(true);//パスできるならボタン有効
		}
		for (int i = 0; i < BOARD_BORDER; i++) {
			for (int j = 0; j < BOARD_BORDER; j++) {
				boardBottons[i][j].reflectMapButton(map[i][j]);
			}
		}
		revalidate();
		repaint();
		if (othello.endCheck() == 1) {//終了なら
			TimerTask task = new TimerTask() {
		        public void run() {
		        	paintResult();//リザルト画面
		        }
		       };
		       Timer timer = new Timer();
		       timer.schedule(task, 2000);
			
		}

	}

	class BoardButton extends JButton { //盤面のボタンに用いる 何番目のボタンかを示す整数型変数 i を保持

		int i, preMapState, nowMapState;

		public int getNowMapState() {
			return nowMapState;
		}

		public void setNowMapState(int preState) {
			this.nowMapState = preState;
		}

		public BoardButton(int i) {
			setI(i);
		}

		public int getPreMapState() {
			return preMapState;
		}

		public void setPreMapState(int premapState) {
			this.preMapState = premapState;
		}

		public int getI() {
			return i;
		}

		public void setI(int i) {
			this.i = i;
		}

		public void reflectMapButton(int nowState) {
			this.setNowMapState(nowState);
			this.repaint();

			switch (nowState) {
			case 0:
				this.setEnabled(false);
				break;
			case 1:
				this.setEnabled(isBlack);
				if (!myTurn) {//自分のターンで無ければ押せない
					this.setEnabled(false);
				}
				break;
			case 2:
				this.setEnabled(!isBlack);
				if (!myTurn) {
					this.setEnabled(false);
				}
				break;
			case 3:
				this.setEnabled(true);
				if (!myTurn) {
					this.setEnabled(false);
				}
				break;
			case 4:
				this.setEnabled(false);
				break;
			case 5:
				this.setEnabled(false);
				break;
			}
			this.setPreMapState(nowState);

		}

		protected void paintComponent(Graphics g) {
			super.paintComponent(g);
			int diameter = Math.min(getWidth(), getHeight()) - 10;
			int x = (getWidth() - diameter) / 2;
			int y = (getHeight() - diameter) / 2;
			switch (this.getNowMapState()) {
			case 0:
				g.setColor(Color.GREEN);
				g.fillRect(0, 0, getWidth(), getHeight());
				break;
			case 1:
				if (myTurn && isBlack) {
					g.setColor(Color.RED);
					
				} else {
					g.setColor(Color.GREEN);
				}
				g.fillRect(0, 0, getWidth(), getHeight());
				break;
			case 2:
				if (myTurn && !isBlack) {
					g.setColor(Color.RED);
				} else {
					g.setColor(Color.GREEN);
				}
				g.fillRect(0, 0, getWidth(), getHeight());
				break;
			case 3:
				if (myTurn) {
					g.setColor(Color.RED);
				} else {
					g.setColor(Color.GREEN);
				}
				g.fillRect(0, 0, getWidth(), getHeight());
				break;
			case 4:
				g.setColor(Color.GREEN);
				g.fillRect(0, 0, getWidth(), getHeight());
				g.setColor(Color.BLACK);
				g.fillOval(x, y, diameter, diameter);
				break;
			case 5:
				g.setColor(Color.GREEN);
				g.fillRect(0, 0, getWidth(), getHeight());
				g.setColor(Color.WHITE);

				g.fillOval(x, y, diameter, diameter);
				break;
			}
			if ((this.getNowMapState() == 1 && isBlack)
					|| (this.getNowMapState() == 2 && !isBlack)
					|| this.getNowMapState() == 3) {
				g.setColor(Color.RED);
			} else {
				g.setColor(Color.GREEN);
			}
			g.fillRect(0, 0, getWidth(), getHeight());

			//コマを描く
			if (this.getNowMapState() == 4) {
				g.setColor(Color.BLACK);
				g.fillOval(x, y, diameter, diameter);
			} else if (this.getNowMapState() == 5) {
				g.setColor(Color.WHITE);
				g.fillOval(x, y, diameter, diameter);

			}
		}

	}

	public void paintResult() {
		if(Player.getPlayer_name()==null) {
			Player.setPlayer_name("Player");
		}
		
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
		}
		

		setSize(400, 200);
		//白と黒の数をカウント
		int black = othello.getBlack();
		int white = othello.getWhite();
		getContentPane().removeAll();
		
		JPanel p = new JPanel();
		p.setSize(200, 100);
		p.setLayout(new FlowLayout());
		System.out.println("result");
		JLabel resultLabel = new JLabel();
		JLabel winnerLabel = new JLabel();

		if (giveUp == 1) {//自分が投了したら
			resultLabel.setText("投了しました");
			winnerLabel.setText(enemyName + "の勝ちです");

		} else if (giveUp == 2) {//相手が投了したら
			resultLabel.setText(enemyName + "が投了しました");
			winnerLabel.setText(Player.getPlayer_name() + "の勝ちです");
		}

		else {//普通に対局終了したとき
			resultLabel.setText("黒" + black + "-" + white + " 白");
			if (Player.getBlackwhite().equals(othello.judge())) {
				winnerLabel.setText(Player.getPlayer_name() + "の勝ちです");
			} else if (Player.getBlackwhite().equals("draw")) {
				winnerLabel.setText("引き分け");
			} else {
				winnerLabel.setText(enemyName + "の勝ちです");
			}
		}

		p.add(resultLabel);
		p.add(winnerLabel);
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
		case EASY://コンピュータのレベル選び段階
		case NORMAL:
		case HARD:
			mode = e.getActionCommand(); //それぞれのレベルに設定
			paintGame();
			break;
		case LOCAL: //モード設定段階
			mode = LOCAL;
			chooseLevel(); //コンピュータのレベル選びに移行
			break;
		case NETWORK: //モード設定段階
			mode = NETWORK;
			waitMode();//対戦準備
			connectServer("172.20.10.11", 10000);//引数は仮
			//setName(); //プレイヤの名前設定に移行
			break;
		case "投了":
			giveUp = 1;//自身が投了したことを判定
			if(mode==NETWORK) {
				sendMessage("投了");//サーバーに送信
			}
			paintResult();
			break;
		case PASS: //パスの処理
			System.out.println("パスが押されました");
			myTurn = !myTurn;//手番を反転
			othello.pass();//オセロクラスのターンを変更
			if(mode==NETWORK) {//ネットワーク対戦なら
				sendMessage("パス");//パス情報をサーバーに送信
			}else {//ローカルの時
				TimerTask task = new TimerTask() {
			        public void run() {
			         cpu.CPUMain(othello); //CPUに盤面情報を引き渡す
			         cpuPut = cpu.getMove(); //CPUが探索した結果を受け取る
			         if(cpuPut[0] != -1 & cpuPut[1] != -1) {
			        	 othello.putDisc(cpuPut[0], cpuPut[1]); //盤面を更新
			         }
			         myTurn=!myTurn;//CPUが置いたとき手番を反転
			         reflectMap();//盤面情報を盤面ボタンに反映
			        }
				};
				Timer timer = new Timer();
			    timer.schedule(task, 1000);
			}
			
			pass.setEnabled(false);//パスボタンを無効
			reflectMap();
			break;
		case "対局開始":
			if(isBlack) {
				cpu = new CPU(mode,"black");
				Player.setBlackwhite("black");
				enemyName = mode;
				paintGame();
				if(!myTurn) {
					TimerTask task = new TimerTask() {
				        public void run() {
				         cpu.CPUMain(othello); //CPUに盤面情報を引き渡す
				         cpuPut = cpu.getMove(); //CPUが探索した結果を受け取る
				         if(cpuPut[0] != -1 & cpuPut[1] != -1) {
				        	 othello.putDisc(cpuPut[0], cpuPut[1]); //盤面を更新
				         }
				         myTurn=!myTurn;//CPUが置いたとき手番を反転
				         reflectMap();//盤面情報を盤面ボタンに反映
				        }
					};
					Timer timer = new Timer();
				    timer.schedule(task, 1000);
				}
				
			}
			else {
				cpu=new CPU(mode,"white");
				Player.setBlackwhite("white");
				enemyName = mode;
				paintGame();
				if(!myTurn) {
					
					TimerTask task = new TimerTask() {
				        public void run() {
				         cpu.CPUMain(othello); //CPUに盤面情報を引き渡す
				         cpuPut = cpu.getMove(); //CPUが探索した結果を受け取る
				         if(cpuPut[0] != -1 & cpuPut[1] != -1) {
				        	 othello.putDisc(cpuPut[0], cpuPut[1]); //盤面を更新
				         }
				         myTurn=!myTurn;//CPUが置いたとき手番を反転
				         reflectMap();//盤面情報を盤面ボタンに反映
				        }
					};
					Timer timer = new Timer();
					timer.schedule(task, 1000);
				}
			}
		
			break;
		}

	}

	// メソッド
	public void connectServer(String ipAddress, int port) { // サーバに接続
		Socket socket = null;
		Socket checkSocket = null;
		
		try {
			socket = new Socket(ipAddress, port); //サーバ(ipAddress, port)に接続
			checkSocket = new Socket(ipAddress, port+1);
			out = new PrintWriter(socket.getOutputStream(), true); //データ送信用オブジェクトの用意
			color_receiver = new ColorReceiver(socket); //先手後手受信用オブジェクトの準備
			color_receiver.start();//受信用オブジェクト(スレッド)起動
			name_receiver = new NameReceiver(socket); //名前受信用オブジェクトの準備
			move_receiver = new MoveReceiver(socket); //操作情報データ受信用オブジェクト
			connectionChecker = new ConnectionChecker(checkSocket);
			connectionChecker.start();
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
		private boolean running = true;//スレッド制御用変数

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
				while (running) {//データを受信し続ける
					String inputLine = br.readLine();//受信データを一行分読み込む
					if (inputLine != null) {//データを受信したら
						enemyName = inputLine;//相手の名前を代入
						stopRunning();//runメソッドの停止
						paintGame();//対局画面の描画
						move_receiver.start();//操作情報用スレッド起動
					}
				}
			} catch (IOException e) {
				System.err.println("データ受信時にエラーが発生しました: " + e);
				//コンピュータ対戦に移行？
			}
		}

		public void stopRunning() {//スレッド停止用メソッド
			running = false;
		}
	}

	// 先手後手データ受信用スレッド(内部クラス)
	class ColorReceiver extends Thread {
		private InputStreamReader sisr; //受信データ用文字ストリーム
		private BufferedReader br; //文字ストリーム用のバッファ
		private boolean running = true;//スレッド制御用変数

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
				while (running) {//データを受信し続ける
					String inputLine = br.readLine();//受信データを一行分読み込む
					if (inputLine != null) {//データを受信したら
						Player.setBlackwhite(inputLine);
						if (inputLine.equals("black")) {//先手後手を判定して変数を設定
							isBlack = true;
							myTurn = true;//先手
						}
						else {
							isBlack = false;
							myTurn = false;//後手
						}
						
						stopRunning();//スレッド停止
						Client.this.setName();
						//System.out.println(myTurn);//テスト用
						name_receiver.start();//名前受信用オブジェクト(スレッド)起動

					}
				}
			} catch (IOException e) {
				System.err.println("データ受信時にエラーが発生しました: " + e);
				//コンピュータ対戦に移行？
			}
		}

		public void stopRunning() {//スレッド停止用メソッド
			running = false;
		}
	}

	// 操作情報データ受信用スレッド(内部クラス)
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
						myTurn = !myTurn;//データ受信したらターンを変更
						receiveMessage(inputLine);//データ受信用メソッドを呼び出す
					}
				}
			} catch (IOException e) {
				System.err.println("データ受信時にエラーが発生しました: " + e);
				//コンピュータ対戦に移行？
			}
		}
	}
	
	class ConnectionChecker extends Thread {
		  private InputStreamReader isr; //受信データ用文字ストリーム
		  private BufferedReader br; //文字ストリーム用バッファ
		  private PrintWriter out; //データ送信用オブジェクト
		  private Socket socket;
		  
		  //内部クラスConnectionCheckerのコンストラクタ
		  ConnectionChecker(Socket socket) {
		   try {
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
		     out.println("check"); 
		              out.flush();
		     Thread.sleep(2000);
		     
		     String inputline = br.readLine(); //データを1行分読み込む
		     if(inputline != null) { //データを受信したら
		      socket.setSoTimeout(10000);
		      System.out.println("from server" + ": " + inputline); //読み込んだ内容をサーバに出力する(確認用)
		      out.println("check"); 
		                  out.flush();
		                  Thread.sleep(2000);
		     }
		    }
		   } catch(Exception e) { //接続が切れたとき
			   
		    System.err.println(e);
		   }
		  }
		 }

	public void receiveMessage(String msg) { // メッセージの受信
		System.out.println("サーバからメッセージ " + msg + " を受信しました"); //テスト用標準出力

		if (msg.equals("パス")) {//受け取った情報がパスなら
			othello.pass();//オセロクラスのターンを変更
		} else if (msg.equals("投了")) {
			giveUp = 2;//相手が押したことを判定
			paintResult();//相手が投了したらリザルト画面
		}else if(msg.equals("-1")) {
			System.out.println("ローカルに移りマス");
			System.out.println("");
			toCPU();//ローカル対戦に
			
			
		}
		else {//パスでも投了でもなければ
			othello.putDisc(getY(msg), getX(msg));//コマを置く
		}
		reflectMap();//盤面を更新
	}

	public int getX(String msg) {//ボタン情報からx座標を取得
		//操作情報をマスに変換
		hand = Integer.parseInt(msg);
		x = hand % 8;
		return x;
	}

	public int getY(String msg) {//ボタン情報からy座標を取得
		//操作情報をマスに変換
		hand = Integer.parseInt(msg);
		y = hand / 8;
		return y;
	}

}
